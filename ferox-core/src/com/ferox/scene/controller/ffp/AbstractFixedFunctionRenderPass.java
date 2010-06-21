package com.ferox.scene.controller.ffp;

import com.ferox.entity.Component;
import com.ferox.entity.ComponentId;
import com.ferox.entity.Entity;
import com.ferox.math.Color4f;
import com.ferox.math.Matrix4f;
import com.ferox.math.Vector3f;
import com.ferox.math.Vector4f;
import com.ferox.math.bounds.Frustum;
import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.RenderPass;
import com.ferox.renderer.Renderer;
import com.ferox.renderer.Surface;
import com.ferox.resource.Geometry;
import com.ferox.resource.Texture;
import com.ferox.scene.AmbientLight;
import com.ferox.scene.BlinnPhongLightingModel;
import com.ferox.scene.DirectionLight;
import com.ferox.scene.Renderable;
import com.ferox.scene.SceneElement;
import com.ferox.scene.Shape;
import com.ferox.scene.SolidLightingModel;
import com.ferox.scene.SpotLight;
import com.ferox.scene.TexturedMaterial;
import com.ferox.util.geom.Box;

/**
 * AbstractFixedFunctionRenderPass is an abstract RenderPass that provides many
 * utilities and support for more specific pass implementations that render
 * Entities based off of the Components defined in com.ferox.scene.
 * 
 * @author Michael Ludwig
 */
public abstract class AbstractFixedFunctionRenderPass implements RenderPass {
    private static final ComponentId<Renderable> R_ID = Component.getComponentId(Renderable.class);
    private static final ComponentId<Shape> S_ID = Component.getComponentId(Shape.class);
    private static final ComponentId<SceneElement> SE_ID = Component.getComponentId(SceneElement.class);
    private static final ComponentId<TexturedMaterial> T_ID = Component.getComponentId(TexturedMaterial.class);
    private static final ComponentId<BlinnPhongLightingModel> BP_ID = Component.getComponentId(BlinnPhongLightingModel.class);
    private static final ComponentId<SolidLightingModel> SL_ID = Component.getComponentId(SolidLightingModel.class);
    
	protected static final Color4f BLACK = new Color4f(0f, 0f, 0f, 1f);
	protected static final Color4f GRAY = new Color4f(.5f, .5f, .5f, 1f);
	
	protected static final Geometry DEFAULT_GEOM = new Box(1f);
	
	// constant configuration variables
	protected final String vertexBinding;
	protected final String normalBinding;
	protected final String texCoordBinding;
	
	protected final int maxMaterialTexUnits;
	
	protected final RenderConnection connection;
	
	// cached variables
	private final Vector4f lightPos;
	private final Vector3f lightDir;
	
	private final Matrix4f view;
	private final Matrix4f modelView;
	private final Matrix4f projection;
	
	// per-pass variables
	private FixedFunctionRenderer renderer;

    /**
     * Create a new AbstractFixedFunctionRenderPass. The specified parameters
     * are not validated but should not be null. <tt>maxMaterialTexUnits</tt> is
     * expected to be 0, 1, or 2. The provided RenderConnection must be the
     * connection that provides the Entity data to be rendered.
     * 
     * @param connection
     * @param maxMaterialTexUnits
     * @param vertexBinding
     * @param normalBinding
     * @param texCoordBinding
     */
	public AbstractFixedFunctionRenderPass(RenderConnection connection, int maxMaterialTexUnits, 
	                                       String vertexBinding, String normalBinding, String texCoordBinding) {
	    this.connection = connection;
		this.maxMaterialTexUnits = maxMaterialTexUnits;

		this.vertexBinding = vertexBinding;
		this.normalBinding = normalBinding;
		this.texCoordBinding = texCoordBinding;

		// cached variables for computing lighting, etc.
		lightPos = new Vector4f();
		lightDir = new Vector3f();

		modelView = new Matrix4f();
		view = new Matrix4f();
		projection = new Matrix4f();
	}

	@Override
	public void render(Renderer renderer, Surface surface) {
		if (renderer instanceof FixedFunctionRenderer) {
			// render only when we have an ffp renderer
			this.renderer = (FixedFunctionRenderer) renderer;
			notifyPassBegin();
			init(surface);
			render(this.renderer);
			notifyPassEnd();
			this.renderer = null;
		}
	}
	
	private void init(Surface surface) {
		// setup attribute bindings
		renderer.setVertexBinding(vertexBinding);
		renderer.setNormalBinding(normalBinding);
		
		switch(maxMaterialTexUnits) {
		case 2:
			renderer.setTextureCoordinateBinding(1, texCoordBinding);
		case 1:
			renderer.setTextureCoordinateBinding(0, texCoordBinding);
			break;
		case 0:
			// no textures, so just disable everything
			renderer.setTextureCoordinateBinding(0, null);
			break;
		}
		
		// delegate to configure viewport
		configureViewport(renderer, surface);
		
		// get matrix forms from the frustum
		Frustum frustum = getFrustum();
		frustum.getProjectionMatrix(projection);
		frustum.getViewMatrix(view);
		
		// set projection matrix and view matrices on renderer
		renderer.setProjectionMatrix(projection);
		renderer.setModelViewMatrix(view);
	}

    /**
     * Utility function to render the given Entity.
     * 
     * @param atom The Entity to render
     * @throws NullPointerException if atom is null
     */
	protected void render(Entity atom) {
		// set draw mode
	    Renderable render = atom.get(R_ID);
	    if (render == null)
	        return; // not supposed to render it 
		renderer.setDrawStyle(render.getDrawStyleFront(), render.getDrawStyleBack());
		
		// set materials
		BlinnPhongLightingModel bp = atom.get(BP_ID);
		SolidLightingModel sl = atom.get(SL_ID);
		if (bp != null) {
		    renderer.setLightingEnabled(true);
		    renderer.setMaterialShininess(bp.getShininess());
		    renderer.setMaterial(bp.getAmbient(), bp.getDiffuse(), bp.getSpecular(), BLACK);
		} else if (sl != null) {
		    renderer.setLightingEnabled(false);
		    renderer.setMaterial(BLACK, sl.getColor(), BLACK, BLACK);
		} else {
		    renderer.setLightingEnabled(true);
		    renderer.setMaterialShininess(1f);
		    renderer.setMaterial(BLACK, GRAY, BLACK, BLACK);
		}
		
		// set textures
		TexturedMaterial tm = atom.get(T_ID);
		Texture decal = (tm == null ? null : tm.getDecalTexture());
		Texture primary = (tm == null ? null : tm.getPrimaryTexture());
		switch(maxMaterialTexUnits) {
		case 2:
			if (decal != null) {
				renderer.setTexture(1, decal);
				renderer.setTextureEnabled(1, true);
			} else
				renderer.setTextureEnabled(1, false);
			// fall through
		case 1:
			if (primary != null) {
				renderer.setTexture(0, primary);
				renderer.setTextureEnabled(0, true);
			} else
				renderer.setTextureEnabled(0, false);
			break;
		}
		
		// render it
		renderGeometry(atom);
	}

    /**
     * Utility function to render the {@link Geometry} attached to <tt>atom</tt>
     * via a {@link Shape} at the world transform of the Entity, defined by its
     * {@link SceneElement}. If it has no Shape, a default Geometry is used. If
     * it has no transform, the identity is used. These transforms are properly
     * converted into camera space by the view matrix of the Frustum returned by
     * {@link #getFrustum()}.
     * 
     * @param atom The atom to render
     */
	protected void renderGeometry(Entity atom) {
	    // get the shape
	    Shape shape = atom.get(S_ID);
        Geometry geom = (shape == null ? DEFAULT_GEOM : shape.getGeometry());
        
        // determine complete model view
	    SceneElement se = atom.get(SE_ID);
	    if (se == null)
	        modelView.setIdentity();
	    else
	        se.getTransform().get(modelView);
	    view.mul(modelView, modelView);

	    // render with correct transform
		renderer.setModelViewMatrix(modelView);
		renderer.render(geom);
		
		// restore the view
		renderer.setModelViewMatrix(view);
	}

    /**
     * Utility function to update the light state at the given light to match
     * that of <tt>lightComponent</tt>. If the light component is null, the
     * light index is disabled. Otherwise, the renderer's state is updated
     * appropriately depending on if the component is an {@link AmbientLight}, a
     * {@link SpotLight}, or a {@link DirectionLight}.
     * 
     * @param light The light's index to update
     * @param lightComponent The Component specifying the light properties
     */
	protected void setLight(int light, Component lightComponent) {
	    if (lightComponent == null) {
	        // disable the light and return
	        renderer.setLightEnabled(light, false);
	        return;
	    }
	    
	    // handle 3 supported types of light
	    if (lightComponent instanceof AmbientLight) {
	        // ambient lights don't use an index so disable the unit
	        renderer.setGlobalAmbientLight(((AmbientLight) lightComponent).getColor());
	        renderer.setLightEnabled(light, false);
	    } else if (lightComponent instanceof DirectionLight) {
	        DirectionLight dl = (DirectionLight) lightComponent;
	        
	        lightPos.set(-dl.getDirection().x, -dl.getDirection().y, -dl.getDirection().z, 0f);
	        renderer.setLightPosition(light, lightPos);
	        renderer.setLightColor(light, BLACK, dl.getColor(), dl.getColor());
	        renderer.setLightEnabled(light, true);
	    } else if (lightComponent instanceof SpotLight) {
	        SpotLight sl = (SpotLight) lightComponent;
	        
	        lightPos.set(sl.getPosition().x, sl.getPosition().y, sl.getPosition().z, 1f);
	        sl.getDirection().normalize(lightDir);
	        renderer.setLightPosition(light, lightPos);
	        renderer.setSpotlight(light, lightDir, sl.getCutoffAngle());
	        renderer.setLightColor(light, BLACK, sl.getColor(), sl.getColor());
	        renderer.setLightEnabled(light, true);
	    }
	}

    /**
     * Perform the meat of the rendering operation, using the provided renderer.
     * This is invoked after geometry attribute bindings are configured, the
     * view/projection matrices are set up, and the viewport is properly
     * cleared. The view port set up clearing is delegated to
     * {@link #configureViewport(FixedFunctionRenderer, Surface)}
     * 
     * @param renderer The renderer to use
     */
	protected abstract void render(FixedFunctionRenderer renderer);

    /**
     * Configure the given renderer's viewport as needed for this pass. This may
     * clear the viewport, it may define its extents based on this pass's
     * connection's defined view bounds.
     * 
     * @param renderer The renderer to use
     * @param surface The Surface currently being rendered into
     */
	protected abstract void configureViewport(FixedFunctionRenderer renderer, Surface surface);

	/**
     * @return A non-null Frustum to use that defines the view and projection
     *         matrices
     */
	protected abstract Frustum getFrustum();

    /**
     * Invoke the appropriate notifyXBegin() on this pass's connection, or do
     * nothing, based on the stage of this pass within the pipeline.
     */
	protected abstract void notifyPassBegin();

    /**
     * Invoke the appropriate notifyXEnd() on this pass's connection, or do
     * nothing, based on the stage of this pass within the pipeline.
     */
	protected abstract void notifyPassEnd();
}
