package com.ferox.renderer.impl.jogl;

import com.ferox.effect.EffectType.Type;
import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.UnsupportedResourceException;
import com.ferox.renderer.impl.AbstractRenderer;
import com.ferox.renderer.impl.EffectDriver;
import com.ferox.renderer.impl.ResourceDriver;
import com.ferox.renderer.impl.jogl.drivers.effect.JoglAlphaTestEffectDriver;
import com.ferox.renderer.impl.jogl.drivers.effect.JoglBlendModeEffectDriver;
import com.ferox.renderer.impl.jogl.drivers.effect.JoglColorMaskEffectDriver;
import com.ferox.renderer.impl.jogl.drivers.effect.JoglDepthTestEffectDriver;
import com.ferox.renderer.impl.jogl.drivers.effect.JoglFogEffectDriver;
import com.ferox.renderer.impl.jogl.drivers.effect.JoglGlobalLightingEffectDriver;
import com.ferox.renderer.impl.jogl.drivers.effect.JoglGlslShaderEffectDriver;
import com.ferox.renderer.impl.jogl.drivers.effect.JoglLightingEffectDriver;
import com.ferox.renderer.impl.jogl.drivers.effect.JoglLineStyleEffectDriver;
import com.ferox.renderer.impl.jogl.drivers.effect.JoglMaterialEffectDriver;
import com.ferox.renderer.impl.jogl.drivers.effect.JoglPointStyleEffectDriver;
import com.ferox.renderer.impl.jogl.drivers.effect.JoglPolygonStyleEffectDriver;
import com.ferox.renderer.impl.jogl.drivers.effect.JoglStencilTestEffectDriver;
import com.ferox.renderer.impl.jogl.drivers.effect.JoglTextureEffectDriver;
import com.ferox.renderer.impl.jogl.drivers.geom.JoglIndexedArrayGeometryDriver;
import com.ferox.renderer.impl.jogl.drivers.resource.JoglGlslProgramResourceDriver;
import com.ferox.renderer.impl.jogl.drivers.resource.JoglGlslUniformResourceDriver;
import com.ferox.renderer.impl.jogl.drivers.resource.JoglTexture1DResourceDriver;
import com.ferox.renderer.impl.jogl.drivers.resource.JoglTexture2DResourceDriver;
import com.ferox.renderer.impl.jogl.drivers.resource.JoglTexture3DResourceDriver;
import com.ferox.renderer.impl.jogl.drivers.resource.JoglTextureCubeMapResourceDriver;
import com.ferox.renderer.impl.jogl.drivers.resource.JoglTextureRectangleResourceDriver;
import com.ferox.resource.GlslProgram;
import com.ferox.resource.GlslUniform;
import com.ferox.resource.IndexedArrayGeometry;
import com.ferox.resource.Resource;
import com.ferox.resource.Texture1D;
import com.ferox.resource.Texture2D;
import com.ferox.resource.Texture3D;
import com.ferox.resource.TextureCubeMap;
import com.ferox.resource.TextureRectangle;

/**
 * <p>
 * Provides a full implementation of Renderer that is based on the functionality
 * provided by AbstractRenderer and the JOGL binding of OpenGL. OnscreenSurfaces
 * created by this Renderer will return Frame objects from their getWindowImpl()
 * methods.
 * </p>
 * <p>
 * This Renderer is strictly single-threaded and undefined results will occur if
 * any of its methods are called outside of the thread it was created in.
 * Because of this requirement, it must not be created or used in the AWT event
 * threads.
 * </p>
 * 
 * @author Michael Ludwig
 */
public final class BasicJoglRenderer extends AbstractRenderer {
	private EffectDriver[] effectDrivers;

	// resource drivers
	private JoglTexture1DResourceDriver t1dDriver;
	private JoglTexture2DResourceDriver t2dDriver;
	private JoglTexture3DResourceDriver t3dDriver;
	private JoglTextureCubeMapResourceDriver tcmDriver;
	private JoglTextureRectangleResourceDriver trtDriver;

	private JoglGlslUniformResourceDriver uniformDriver;
	private JoglGlslProgramResourceDriver glslDriver;

	private JoglIndexedArrayGeometryDriver geomDriver;

	/** Construct a renderer that does no debugging. */
	public BasicJoglRenderer() {
		this(false);
	}

	/**
	 * Construct a renderer that uses the JOGL bindings for OpenGL. debugGL is
	 * true, the renderer will check openGL error conditions after every gl
	 * call, and will throw an exception if there's a problem. This can be
	 * useful when debugging, but may hurt performance.
	 * 
	 * @param debugGL Whether or not to debug each OpenGL call
	 */
	public BasicJoglRenderer(boolean debugGL) {
		RenderCapabilities caps = new JoglCapabilitiesDetector().detect();

		JoglContextManager factory =
			new JoglContextManager(this, caps, debugGL);
		init(factory, factory.getTransformDriver(), caps, Type.values());
		buildEffectDrivers(factory);
		buildResourceDrivers(factory);
	}

	private void buildEffectDrivers(JoglContextManager factory) {
		Type[] values = Type.values();
		effectDrivers = new EffectDriver[values.length];
		for (int i = 0; i < values.length; i++) {
			switch (values[i]) {
			case ALPHA:
				effectDrivers[i] = new JoglAlphaTestEffectDriver(factory);
				break;
			case BLEND:
				effectDrivers[i] = new JoglBlendModeEffectDriver(factory);
				break;
			case COLOR_MASK:
				effectDrivers[i] = new JoglColorMaskEffectDriver(factory);
				break;
			case DEPTH:
				effectDrivers[i] = new JoglDepthTestEffectDriver(factory);
				break;
			case FOG:
				effectDrivers[i] = new JoglFogEffectDriver(factory);
				break;
			case GLOBAL_LIGHTING:
				effectDrivers[i] = new JoglGlobalLightingEffectDriver(factory);
				break;
			case LIGHT:
				effectDrivers[i] = new JoglLightingEffectDriver(factory);
				break;
			case LINE:
				effectDrivers[i] = new JoglLineStyleEffectDriver(factory);
				break;
			case MATERIAL:
				effectDrivers[i] = new JoglMaterialEffectDriver(factory);
				break;
			case POINT:
				effectDrivers[i] = new JoglPointStyleEffectDriver(factory);
				break;
			case POLYGON:
				effectDrivers[i] = new JoglPolygonStyleEffectDriver(factory);
				break;
			case SHADER:
				effectDrivers[i] = new JoglGlslShaderEffectDriver(factory);
				break;
			case STENCIL:
				effectDrivers[i] = new JoglStencilTestEffectDriver(factory);
				break;
			case TEXTURE:
				effectDrivers[i] = new JoglTextureEffectDriver(factory);
				break;
			}
		}
	}

	private void buildResourceDrivers(JoglContextManager factory) {
		geomDriver = new JoglIndexedArrayGeometryDriver(factory);

		glslDriver = new JoglGlslProgramResourceDriver(factory);
		uniformDriver = new JoglGlslUniformResourceDriver(factory);

		t1dDriver = new JoglTexture1DResourceDriver(factory);
		t2dDriver = new JoglTexture2DResourceDriver(factory);
		t3dDriver = new JoglTexture3DResourceDriver(factory);
		tcmDriver = new JoglTextureCubeMapResourceDriver(factory);
		trtDriver = new JoglTextureRectangleResourceDriver(factory);
	}

	@Override
	protected EffectDriver getEffectDriver(Type effectType) {
		return effectDrivers[effectType.ordinal()];
	}

	@Override
	protected ResourceDriver getResourceDriver(
		Class<? extends Resource> resourceType) {
		if (IndexedArrayGeometry.class.isAssignableFrom(resourceType))
			return geomDriver;
		else if (GlslProgram.class.isAssignableFrom(resourceType))
			return glslDriver;
		else if (GlslUniform.class.isAssignableFrom(resourceType))
			return uniformDriver;
		else if (Texture1D.class.isAssignableFrom(resourceType))
			return t1dDriver;
		else if (Texture2D.class.isAssignableFrom(resourceType))
			return t2dDriver;
		else if (Texture3D.class.isAssignableFrom(resourceType))
			return t3dDriver;
		else if (TextureCubeMap.class.isAssignableFrom(resourceType))
			return tcmDriver;
		else if (TextureRectangle.class.isAssignableFrom(resourceType))
			return trtDriver;

		// if we've gotten here, we're unsupported
		throw new UnsupportedResourceException("Unsupported resource type: "
			+ resourceType);
	}
}
