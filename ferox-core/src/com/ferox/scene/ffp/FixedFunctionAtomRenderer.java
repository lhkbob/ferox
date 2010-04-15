package com.ferox.scene.ffp;

import java.util.WeakHashMap;

import com.ferox.math.Frustum;
import com.ferox.renderer.DisplayOptions;
import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.RenderSurface;
import com.ferox.renderer.RenderThreadingOrganizer;
import com.ferox.renderer.TextureSurface;
import com.ferox.renderer.DisplayOptions.DepthFormat;
import com.ferox.renderer.DisplayOptions.PixelFormat;
import com.ferox.renderer.Renderer.Comparison;
import com.ferox.resource.TextureImage;
import com.ferox.resource.TextureImage.DepthMode;
import com.ferox.resource.TextureImage.Filter;
import com.ferox.resource.TextureImage.TextureTarget;
import com.ferox.resource.TextureImage.TextureWrap;
import com.ferox.scene.ffp.RenderConnection.Stream;
import com.ferox.util.Bag;

public class FixedFunctionAtomRenderer {
	private final WeakHashMap<RenderSurface, RenderConnection> connections;
	private final RenderThreadingOrganizer organizer;
	
	private final TextureSurface shadowMap;
	private final int maxMaterialTexUnits; // 0, 1, or 2
	
	private final String vertexBinding;
	private final String normalBinding;
	private final String texCoordBinding;
	
	// FIXME: use a ThreadingOrganizer, and make sure the surface is assigned to 
	// an appropriate group -> this is tricky because 1 surface must go into multiple groups
	// add an explicit group name to the queue operations, but then we must figure out how
	// to handle multithreading that all need shadow map data
	// FIXME: maybe this is less an issue if we generate the shadow map once for all views in a system
	// - this would change how shadow atoms come into the system
	// - open another render connection-like thing for shadow atoms, could just be stream<shadowAtom>
	// - shadow generator pass gets dumped into every RS's group
	// - has synchronization inside itself to block other rendering threads until it's completed, and then
	//   skip rendering when not needed
	public FixedFunctionAtomRenderer(RenderThreadingOrganizer organizer, int shadowMapSize, 
									 String vertexBinding, String normalBinding, String texCoordBinding) {
		if (organizer == null)
			throw new NullPointerException("RenderThreadingOrganizer cannot be null");
		if (vertexBinding == null || normalBinding == null || texCoordBinding == null)
			throw new NullPointerException("Attribute bindings cannot be null");
		
		RenderCapabilities caps = organizer.getFramework().getCapabilities();
		if (!caps.hasFixedFunctionRenderer())
			throw new IllegalArgumentException("Framework must support a FixedFunctionRenderer");
		
		this.organizer = organizer;
		this.vertexBinding = vertexBinding;
		this.normalBinding = normalBinding;
		this.texCoordBinding = texCoordBinding;
		
		int numTex = caps.getMaxFixedPipelineTextures();
		boolean shadowsRequested = shadowMapSize > 0; // size is positive
		boolean shadowSupport = (caps.getFboSupport() || caps.getPbufferSupport()) && 
								numTex > 1 && caps.getVersion() > 1.4f;
						
		if (shadowsRequested && shadowSupport) {
			maxMaterialTexUnits = (numTex > 2 ? 2 : 1);
			// convert size to a power of two
			int sz = 1;
			while(sz < shadowMapSize)
				sz = sz << 1;
			// create the shadow map
			shadowMap = organizer.getFramework().createTextureSurface(new DisplayOptions(PixelFormat.NONE, DepthFormat.DEPTH_24BIT), TextureTarget.T_2D, 
																	  sz, sz, 1, 0, 0, false);
			
			// set up the depth comparison
			TextureImage sm = shadowMap.getDepthBuffer();
			sm.setFilter(Filter.LINEAR);
			sm.setWrapSTR(TextureWrap.CLAMP);
			sm.setDepthCompareEnabled(true);
			sm.setDepthCompareTest(Comparison.LEQUAL);
			sm.setDepthMode(DepthMode.ALPHA);
		} else {
			maxMaterialTexUnits = Math.min(2, numTex);
			shadowMap = null;
		}
		
		connections = new WeakHashMap<RenderSurface, RenderConnection>();
	}
	
	public RenderConnection getConnection(RenderSurface surface) {
		synchronized(connections) {
			RenderConnection con = connections.get(surface);
			if (con == null) {
				con = new RenderConnectionImpl(surface);
				connections.put(surface, con);
			}

			return con;
		}
	}
	
	public RenderThreadingOrganizer getRenderThreadingOrganizer() {
		return organizer;
	}
	
	public TextureSurface getShadowMap() {
		return shadowMap;
	}
	
	private class RenderConnectionImpl implements RenderConnection {
		private final RenderSurface surface;
		
		// streams that contain render data
		private final RenderAtomStream renderAtomStream;
		private boolean raStreamNeedsReset;
		
		private final LightAtomStream lightAtomStream;
		private boolean laStreamNeedsReset;
		
		private final ShadowAtomStream shadowAtomStream;
		private boolean saStreamNeedsReset;
		
		private final FogAtomStream fogAtomStream;
		private boolean faStreamNeedsReset;
		
		// configured shadow mapping and viewports
		private Frustum viewFrustum;
		private Frustum shadowFrustum;
		private LightAtom shadowLight;
		
		private int left, right, top, bottom; // viewport
		
		// render passes that are enqueued during close()
		private final DefaultLightingPass defaultLightPass;
		private final ShadowedLightingPass shadowLightPass;
		private final ShadowMapGeneratorPass shadowGenPass;
		
		public RenderConnectionImpl(RenderSurface surface) {
			this.surface = surface;
			renderAtomStream = new RenderAtomStream();
			lightAtomStream = new LightAtomStream();
			shadowAtomStream = new ShadowAtomStream();
			fogAtomStream = new FogAtomStream();
			
			raStreamNeedsReset = false;
			laStreamNeedsReset = false;
			saStreamNeedsReset = false;
			faStreamNeedsReset = false;
			
			defaultLightPass = new DefaultLightingPass(organizer.getFramework().getCapabilities().getMaxActiveLights(),
													   maxMaterialTexUnits, vertexBinding, normalBinding, 
													   texCoordBinding);
			
			if (shadowMap != null) {
				shadowGenPass = new ShadowMapGeneratorPass(maxMaterialTexUnits, vertexBinding);
				shadowLightPass = new ShadowedLightingPass(shadowMap.getDepthBuffer(), maxMaterialTexUnits, 
														   vertexBinding, normalBinding, texCoordBinding);
			} else {
				shadowGenPass = null;
				shadowLightPass = null;
			}
		}
		
		@Override
		public void close() {
			if (shadowMap == null) {
				// clear any configured shadow mapping parameters
				shadowFrustum = null;
				shadowLight = null;
			}
			
			RenderDescription descr = new RenderDescription(renderAtomStream.get(), lightAtomStream.get(), 
															fogAtomStream.get(), shadowAtomStream.get(), 
															shadowLight, shadowFrustum, viewFrustum,
															left, bottom, right - left, top - bottom);
			
			if (shadowFrustum == null) {
				// only configure and queue default lighting
				defaultLightPass.setRenderDescription(descr);
				organizer.queue(surface, defaultLightPass, false, false, false);
			} else {
				// configure and queue all 3 passes
				shadowGenPass.setRenderDescription(descr);
				defaultLightPass.setRenderDescription(descr);
				shadowLightPass.setRenderDescription(descr);
				
				organizer.queue(shadowMap, shadowGenPass, true, true, true); // always clear shadow map
				organizer.queue(surface, defaultLightPass, false, false, false);
				organizer.queue(surface, shadowLightPass, false, false, false);
			}
			
			// mark everything for resets
			raStreamNeedsReset = true;
			laStreamNeedsReset = true;
			faStreamNeedsReset = true;
			saStreamNeedsReset = true;
			
			shadowLight = null;
			shadowFrustum = null;
			viewFrustum = null;
		}

		@Override
		public Stream<LightAtom> getLightAtomStream() {
			if (laStreamNeedsReset) {
				lightAtomStream.reset();
				laStreamNeedsReset = false;
			}
			return lightAtomStream;
		}

		@Override
		public Stream<RenderAtom> getRenderAtomStream() {
			if (raStreamNeedsReset) {
				renderAtomStream.reset();
				raStreamNeedsReset = false;
			}
			return renderAtomStream;
		}

		@Override
		public Stream<ShadowAtom> getShadowAtomStream() {
			if (saStreamNeedsReset) {
				shadowAtomStream.reset();
				saStreamNeedsReset = false;
			}
			return shadowAtomStream;
		}
		
		@Override
		public Stream<FogAtom> getFogAtomStream() {
			if (faStreamNeedsReset) {
				fogAtomStream.reset();
				faStreamNeedsReset = false;
			}
			return fogAtomStream;
		}

		@Override
		public void setShadowLight(Frustum shadowFrustum, LightAtom shadowCaster) {
			this.shadowFrustum = shadowFrustum;
			shadowLight = shadowCaster;
		}

		@Override
		public void setView(Frustum frustum, int left, int right, int bottom, int top) {
			this.left = left;
			this.right = right;
			this.bottom = bottom;
			this.top = top;
			
			viewFrustum = frustum;
		}
	}
	
	private static abstract class BagStream<T> implements Stream<T> {
		private Bag<T> pool; // old instances that can be reused
		private Bag<T> stack; // contains valid T from push()
		
		public BagStream() {
			pool = new Bag<T>();
			stack = new Bag<T>();
		}
		
		@Override
		public T newInstance() {
			if (pool.size() > 0)
				return pool.remove(pool.size() - 1);
			else
				return create();
		}

		@Override
		public void push(T item) {
			stack.add(item);
		}
		
		public void reset() {
			// swap pool and stack, so the next pool
			// uses instances previously pushed into stream
			Bag<T> temp = pool;
			pool = stack;
			stack = temp;
			
			stack.clear(true);
		}
		
		public Bag<T> get() {
			return stack;
		}
		
		protected abstract T create();
	}
	
	/*
	 * Concrete implementations of BagStream for each of the required Atom types. 
	 */
	
	private static class RenderAtomStream extends BagStream<RenderAtom> {
		@Override
		protected RenderAtom create() { return new RenderAtom(); }
	}
	
	private static class LightAtomStream extends BagStream<LightAtom> {
		@Override
		protected LightAtom create() { return new LightAtom(); }
	}
	
	private static class ShadowAtomStream extends BagStream<ShadowAtom> {
		@Override
		protected ShadowAtom create() { return new ShadowAtom(); }
	}
	
	private static class FogAtomStream extends BagStream<FogAtom> {
		@Override
		protected FogAtom create() { return new FogAtom(); }
	}
}
