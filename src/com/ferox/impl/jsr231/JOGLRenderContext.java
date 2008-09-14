package com.ferox.impl.jsr231;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.HashMap;

import javax.media.opengl.GL;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLJPanel;
import javax.media.opengl.GLPbuffer;

import org.openmali.vecmath.AxisAngle4f;
import org.openmali.vecmath.Vector3f;

import com.ferox.core.renderer.RenderContext;
import com.ferox.core.renderer.RenderPass;
import com.ferox.core.renderer.RenderPassPeer;
import com.ferox.core.scene.Plane;
import com.ferox.core.scene.Transform;
import com.ferox.core.scene.View;
import com.ferox.core.scene.bounds.AxisAlignedBox;
import com.ferox.core.scene.bounds.BoundingSphere;
import com.ferox.core.scene.bounds.BoundingVolume;
import com.ferox.core.states.FragmentTest;
import com.ferox.core.states.Quality;
import com.ferox.core.states.StateAtom;
import com.ferox.core.states.StateAtomPeer;
import com.ferox.core.states.atoms.BufferData;
import com.ferox.core.states.atoms.TextureData;
import com.ferox.core.states.atoms.VertexArray;
import com.ferox.core.states.atoms.TextureCubeMap.Face;
import com.ferox.core.states.atoms.TextureData.TextureFormat;
import com.ferox.core.states.atoms.TextureData.TextureType;
import com.ferox.core.states.manager.Geometry;
import com.ferox.core.system.DisplayOptions;
import com.ferox.core.system.OnscreenRenderSurface;
import com.ferox.core.system.RenderSurface;
import com.ferox.core.system.SystemCapabilities;
import com.ferox.core.util.BufferUtil;
import com.ferox.core.util.FeroxException;
import com.ferox.core.util.DataTransfer.Block;
import com.ferox.core.util.DataTransfer.Slice;
import com.ferox.impl.jsr231.peers.JOGLTextureDataPeer;
import com.sun.opengl.util.GLUT;

public abstract class JOGLRenderContext extends RenderContext {
	private static final float RAD_TO_DEGREES = (float)(180f / Math.PI);
	
	protected static SystemCapabilities joglCaps;
	
	private static int getGLConnectivity(Geometry.PolygonType conn) throws FeroxException {
		switch(conn) {
		case POINTS: return GL.GL_POINTS;
		case LINES: return GL.GL_LINES;
		case TRIANGLES: return GL.GL_TRIANGLES;
		case TRIANGLE_FAN: return GL.GL_TRIANGLE_FAN;
		case TRIANGLE_STRIP: return GL.GL_TRIANGLE_STRIP;
		case QUADS: return GL.GL_QUADS;
		case QUAD_STRIP: return GL.GL_QUAD_STRIP;
		default:
			throw new FeroxException("Illegal geometry connectivity: " + conn);
		}
	}
	private static int getGLTypeEnum(BufferData.DataType type) throws FeroxException {
		switch(type) {
		case UNSIGNED_BYTE: return GL.GL_UNSIGNED_BYTE;
		case UNSIGNED_INT: return GL.GL_UNSIGNED_INT;
		case UNSIGNED_SHORT: return GL.GL_UNSIGNED_SHORT;
		default:
			throw new FeroxException("Illegal buffer data type: " + type);
		}
	}
	
	public static int getGLQuality(Quality qual) throws FeroxException {
		switch(qual) {
		case DONT_CARE: return GL.GL_DONT_CARE;
		case FASTEST: return GL.GL_FASTEST;
		case NICEST: return GL.GL_NICEST;
		default:
			throw new FeroxException("Illegal quality: " + qual);
		}
	}
	
	public static int getGLFragmentTest(FragmentTest test) throws FeroxException {
		switch(test) {
		case ALWAYS: return GL.GL_ALWAYS;
		case EQUAL: return GL.GL_EQUAL;
		case GEQUAL: return GL.GL_GEQUAL;
		case GREATER: return GL.GL_GREATER;
		case LEQUAL: return GL.GL_LEQUAL;
		case LESS: return GL.GL_LESS;
		case NEVER: return GL.GL_NEVER;
		case NOT_EQUAL: return GL.GL_NOTEQUAL;
		default:
			throw new FeroxException("Illegal fragment test: " + test);
		}
	}
	
	// tempory cache value for matrices
	private final AxisAngle4f aa = new AxisAngle4f();
	
	private FloatBuffer matrix;
	private RenderSurface surface;
	private double[] plane;
	private HashMap<Class<? extends StateAtom>, StateAtomPeer> peerMap;
	private HashMap<Class<? extends RenderPass>, RenderPassPeer> renderMap;
	private StateAtomPeer[] peerCache;
	
	public JOGLRenderContext(DisplayOptions options) {
		super(options);
		
		this.peerCache = null;
		this.peerMap = new HashMap<Class<? extends StateAtom>, StateAtomPeer>();
		this.renderMap = new HashMap<Class<? extends RenderPass>, RenderPassPeer>();
		
		this.matrix = BufferUtil.newFloatBuffer(16);
		this.plane = new double[4];
		
		GLCapabilities caps = new GLCapabilities();
		
		caps.setAccumAlphaBits(options.getAlphaBits());
		caps.setAccumBlueBits(options.getBlueBits());
		caps.setAccumGreenBits(options.getGreenBits());
		caps.setAccumRedBits(options.getRedBits());
		
		caps.setAlphaBits(options.getAlphaBits());
		caps.setBlueBits(options.getBlueBits());
		caps.setGreenBits(options.getGreenBits());
		caps.setRedBits(options.getRedBits());
		
		if (options.getNumMultiSamples() > 1) {
			caps.setNumSamples(options.getNumMultiSamples());
			caps.setSampleBuffers(true);
		} else {
			caps.setSampleBuffers(false);
		}
		
		caps.setHardwareAccelerated(true);
		caps.setDoubleBuffered(options.isDoubleBuffered());
		caps.setStereo(options.isStereo());
		
		if (options.isHeavyweight() && !options.isHeadless()) {
			this.surface = new GLCanvasSurface(caps, options);
			GLCanvas canvas = (GLCanvas)((OnscreenRenderSurface)this.surface).getRenderSurface();
			canvas.setSize(options.getWidth(), options.getHeight());
		} else if (!options.isHeavyweight() && !options.isHeadless()) {
			this.surface = new GLJPanelSurface(caps, options);
			GLJPanel canvas = (GLJPanel)((OnscreenRenderSurface)this.surface).getRenderSurface();
			canvas.setSize(options.getWidth(), options.getHeight());
		} else if (options.isHeadless()) {
			this.surface = new PbufferSurface(caps, options);
		}
	}
	
	public GLUT glut = new GLUT();
	public void debugDrawBounds(BoundingVolume v) {
		GL gl = this.getGL();
		boolean l = gl.glIsEnabled(GL.GL_LIGHTING);
		
		gl.glDisable(GL.GL_LIGHTING);
		//gl.glColor3f(.7f, .7f, .7f);
		
		switch(v.getBoundType()) {
		case AA_BOX:
			AxisAlignedBox b = (AxisAlignedBox)v;
			Vector3f center = new Vector3f();
			b.getCenter(center);
			Vector3f max = b.getMax();
			Vector3f min = b.getMin();
			
			gl.glPushMatrix();
			gl.glTranslatef(center.x, center.y, center.z);
			gl.glScalef(max.x - min.x, max.y - min.y, max.z - min.z);
			glut.glutWireCube(1f);
			gl.glPopMatrix();
			break;
		case SPHERE:
			BoundingSphere s = (BoundingSphere)v;
			center = s.getCenter();
			gl.glPushMatrix();
			gl.glTranslatef(center.x, center.y, center.z);
			glut.glutWireSphere(s.getRadius(), 6, 6);
			gl.glPopMatrix();
			
			break;
		}
		
		if (l)
			gl.glEnable(GL.GL_LIGHTING);
	}
	
	@Override
	protected void copyTextureData(TextureData data, Block region, Face face, int level, int sx, int sy) {
		JOGLTextureDataPeer peer = (JOGLTextureDataPeer)this.getStateAtomPeer(TextureData.class);
		peer.copyTextureData(data, region, face, level, sx, sy);
	}

	@Override
	protected void getTextureData(TextureData data, Face face, int level, Buffer out, Slice slice) {
		JOGLTextureDataPeer peer = (JOGLTextureDataPeer)this.getStateAtomPeer(TextureData.class);
		peer.getTextureData(data, face, level, out, slice);
	}

	@Override
	protected void readPixels(Buffer in, Slice slice, TextureType type, TextureFormat format, Block region) {
		JOGLTextureDataPeer peer = (JOGLTextureDataPeer)this.getStateAtomPeer(TextureData.class);
		peer.readPixels(in, slice, type, format, region);
	}

	@Override
	protected void setTextureData(TextureData data, Block region, Face face, int level, Buffer in, Slice slice) {
		JOGLTextureDataPeer peer = (JOGLTextureDataPeer)this.getStateAtomPeer(TextureData.class);
		peer.setTextureData(data, region, face, level, in, slice);
	}
	
	@Override
	public void destroyContext() {
		GLContext context = this.getGLContext();
		
		if (context != null) {
			if (GLContext.getCurrent() != context) {
				context.destroy();
			} else {
				while (GLContext.getCurrent() == context) {
					context.release();
					try {
						Thread.sleep(10);
					} catch (InterruptedException ie) {
				
					}
				}
				context.destroy();
			}
		}
	}
	
	@Override
	public void clearBuffers(boolean color, float[] clearColor, boolean depth,
			float clearDepth, boolean stencil, int clearStencil) {
		this.callValidate();
		GL gl = this.getGL();
		int clearBits = 0;
		
		if (color) {
			clearBits |= GL.GL_COLOR_BUFFER_BIT;
			gl.glClearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3]);
		}
		if (depth) {
			clearBits |= GL.GL_DEPTH_BUFFER_BIT;
			gl.glClearDepth(clearDepth);
		}
		if (stencil) {
			clearBits |= GL.GL_STENCIL_BUFFER_BIT;
			gl.glClearStencil(clearStencil);
		}
		
		if (clearBits != 0)
			gl.glClear(clearBits);
	}
	
	@Override
	public void renderGeometry(Geometry geom) throws FeroxException {
		this.callValidate();
		VertexArray indices = geom.getIndices();
		if (geom.getIndices() == null) 
			this.getGL().glDrawArrays(getGLConnectivity(geom.getConnectivity()), 0, geom.getVertices().getNumElements());
		else {
			BufferData iD = indices.getBufferData();
			if (geom.getIndices().getBufferData().isVBO()) 
				this.getGL().glDrawRangeElements(getGLConnectivity(geom.getConnectivity()), 0, geom.getVertices().getNumElements(), indices.getNumElements(), getGLTypeEnum(iD.getDataType()), indices.getOffset() * iD.getByteSize());
			else {
				Buffer data = iD.getData();
				if (data == null)
					throw new FeroxException("Can't render an index geometry that has a non-vbo buffer data with a null backing buffer");
				int posCache = data.position();
				int limCache = data.limit();
				data.limit(iD.getCapacity());
				data.position(geom.getIndices().getOffset());
				
				this.getGL().glDrawRangeElements(getGLConnectivity(geom.getConnectivity()), 0, geom.getVertices().getNumElements(), indices.getNumElements(), getGLTypeEnum(iD.getDataType()), data);
				
				data.position(posCache);
				data.limit(limCache);
			}
		}
	}
	
	@Override
	public SystemCapabilities getCapabilities() {
		if (joglCaps == null) {
			if (!GLDrawableFactory.getFactory().canCreateGLPbuffer())
				return null; // We'll create it later
			GLPbuffer pBuffer = GLDrawableFactory.getFactory().createGLPbuffer(new GLCapabilities(), null, 1, 1, null);
			JOGLCapabilitiesFetcher fetch = new JOGLCapabilitiesFetcher();
			pBuffer.addGLEventListener(fetch);
			pBuffer.display();
			joglCaps = fetch.getCapabilities();
			pBuffer.destroy();
		}
		return joglCaps;
	}
	
	@Override
	public RenderPassPeer getRenderPassPeer(Class<? extends RenderPass> type) throws RuntimeException {
		if (type == null)
			throw new NullPointerException("Can't have a peer for a null type");
		RenderPassPeer peer = this.renderMap.get(type);
		if (peer != null)
			return peer;
		
		String name = this.getClass().getPackage().getName() + ".peers.JOGL" + type.getSimpleName() + "Peer";
		try {
			peer = (RenderPassPeer)Class.forName(name).getDeclaredConstructor(JOGLRenderContext.class).newInstance(this);
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		this.renderMap.put(type, peer);
		return peer;
	}

	@Override
	public StateAtomPeer getStateAtomPeer(Class<? extends StateAtom> type) throws RuntimeException {
		if (type == null)
			throw new NullPointerException("Can't have a peer for null type");
		StateAtomPeer peer = this.peerMap.get(type);
		if (peer != null)
			return peer;
		String name = this.getClass().getPackage().getName() +".peers.JOGL" + type.getSimpleName() + "Peer";
		
		try {
			peer = (StateAtomPeer)Class.forName(name).getDeclaredConstructor(JOGLRenderContext.class).newInstance(this);
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		this.peerMap.put(type, peer);
		return peer;
	}
	
	@Override
	public StateAtomPeer getStateAtomPeer(StateAtom atom) throws NullPointerException {
		if (atom == null)
			throw new NullPointerException("Atom can't be null");
		int type = atom.getDynamicType();
		
		if (this.peerCache == null || this.peerCache.length <= type) {
			StateAtomPeer[] temp = new StateAtomPeer[type + 1];
			if (this.peerCache != null)
				System.arraycopy(this.peerCache, 0, temp, 0, this.peerCache.length);
			this.peerCache = temp;
		}
		if (this.peerCache[type] == null)
			this.peerCache[type] = this.getStateAtomPeer(atom.getAtomType());
		return this.peerCache[type];
	}
	
	@Override
	public RenderSurface getRenderSurface() {
		return this.surface;
	}

	@Override
	public void enableUserClipPlane(int i) {
		this.callValidate();
		this.getGL().glEnable(GL.GL_CLIP_PLANE0 + i);
	}
	
	@Override
	public void disableUserClipPlane(int i) {
		this.callValidate();
		this.getGL().glDisable(GL.GL_CLIP_PLANE0 + i);
	}
	
	@Override
	public void setUserClipPlane(Plane plane, int i) {
		this.callValidate();
		Vector3f n = plane.getNormal();
		this.plane[0] = n.x;
		this.plane[1] = n.y;
		this.plane[2] = n.z;
		this.plane[3] = plane.getConstant();
		this.getGL().glClipPlane(GL.GL_CLIP_PLANE0 + i, this.plane, 0);
	}

	@Override
	public void pushModelTransform(Transform trans) {
		this.callValidate();
		
		this.getGL().glPushMatrix();
		
		Vector3f p = trans.getTranslation();
		this.getGL().glTranslatef(p.x, p.y, p.z);
		this.aa.set(trans.getRotation());
		this.getGL().glRotatef(this.aa.angle * RAD_TO_DEGREES, this.aa.x, this.aa.y, this.aa.z);
		Vector3f s = trans.getScale();
		this.getGL().glScalef(s.x, s.y, s.z);
	}
	
	@Override
	public void popModelTransform() {
		this.callValidate();
		
		this.getGL().glPopMatrix();
	}

	@Override
	public void setProjectionViewTransform(View view) {
		this.callValidate();
		
		GL gl = this.getGL();
		gl.glMatrixMode(GL.GL_PROJECTION);
		Transform.getOpenGLMatrix(view.getProjectionMatrix(), this.matrix);
		gl.glLoadMatrixf((FloatBuffer)this.matrix.rewind());
		
		gl.glMatrixMode(GL.GL_MODELVIEW);
		gl.glLoadIdentity();

		this.matrix.rewind();
		view.getInverseWorldTransform().getOpenGLMatrix(this.matrix);
		this.getGL().glMultMatrixf(this.matrix);
	}

	@Override
	public void setViewport(float left, float right, float top, float bottom) {
		this.callValidate();
		
		this.getGL().glViewport((int)(left * this.getContextWidth()), (int)(bottom * this.getContextHeight()), (int)((right - left) * this.getContextWidth()), (int)((top - bottom) * this.getContextHeight()));
		// just in case the viewport won't clip things
		this.getGL().glScissor((int)(left * this.getContextWidth()), (int)(bottom * this.getContextHeight()), (int)((right - left) * this.getContextWidth()), (int)((top - bottom) * this.getContextHeight()));
	}
	
	public abstract GL getGL();
	public abstract GLContext getGLContext();
}
