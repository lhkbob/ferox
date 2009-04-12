package com.ferox;

import java.awt.Font;
import java.awt.Frame;
import java.util.Formatter;

import org.openmali.vecmath.Vector3f;

import com.ferox.math.Color;
import com.ferox.math.Transform;
import com.ferox.renderer.DisplayOptions;
import com.ferox.renderer.OnscreenSurface;
import com.ferox.renderer.RenderQueue;
import com.ferox.renderer.Renderer;
import com.ferox.renderer.View;
import com.ferox.renderer.util.BasicRenderPass;
import com.ferox.renderer.util.StateSortingRenderQueue;
import com.ferox.resource.text.CharacterSet;
import com.ferox.resource.text.Text;
import com.ferox.scene.Group;
import com.ferox.scene.SceneElement;
import com.ferox.scene.Shape;
import com.ferox.scene.ViewNode;
import com.ferox.scene.Node.CullMode;
import com.ferox.state.DepthTest;
import com.ferox.state.State.PixelTest;

/** BasicApplication extends ApplicationBase and imposes
 * more constraints on the test.
 * 
 * It automatically creates a WindowSurface with one render
 * pass and a view.  The input is configured to move the
 * constructed view.
 * 
 * @author Michael Ludwig
 *
 */
public abstract class BasicApplication extends ApplicationBase {
	public static final long UPDATE_TIME = 100; // ms between updates of fps text
		
	protected OnscreenSurface window;
	protected BasicRenderPass pass;
	protected BasicRenderPass fpsPass;
	protected Text fpsText;
	
	protected ViewNode view;
	protected SceneElement scene;
	
	private long lastFpsUpdate;
	
	private StringBuilder fpsStringBuilder;
	private Formatter fpsFormatter;
	
	public BasicApplication(boolean debug) {
		super(debug);
	}
	
	/** Called after window has been configured with pass. */
	protected abstract SceneElement buildScene(Renderer renderer, ViewNode view);
		
	/** Return a RenderQueue that will be used with this application's
	 * single render pass. */
	protected RenderQueue createQueue() {
		return new StateSortingRenderQueue();
	}
	
	/** Return a DisplayOptions to use for the created
	 * WindowSurface in init(). */
	protected DisplayOptions createOptions() {
		return new DisplayOptions();
	}

	@Override
	protected void init(Renderer renderer) {
		View v = new View();
		this.view = new ViewNode(v);
		Transform viewTrans = this.view.getLocalTransform();
		viewTrans.getTranslation().set(0f, 0f, 15f);
		
		this.pass = new BasicRenderPass(null, v, this.createQueue(), false);
		
		//this.window = renderer.createFullscreenSurface(new DisplayOptions(), 1024, 768);
		this.window = renderer.createWindowSurface(this.createOptions(), 10, 10, 640, 480, false, false);
		this.window.addRenderPass(this.pass);
		this.window.setTitle(this.getClass().getSimpleName());
		
		v.setPerspective(60f, (float) this.window.getWidth() / this.window.getHeight(), 1f, 1000f);
		
		this.scene = this.buildScene(renderer, this.view);
		this.pass.setScene(this.scene);
		
		CharacterSet charSet = new CharacterSet(Font.decode("Arial-Bold-16"), true, false);
		this.fpsText = new Text(charSet, "FPS: \nMeshes: \nPolygons: \nUsed: ");
		this.fpsStringBuilder = new StringBuilder();
		this.fpsFormatter = new Formatter(this.fpsStringBuilder);
		
		renderer.requestUpdate(charSet.getCharacterSet(), true);
		renderer.requestUpdate(this.fpsText, true);
		
		Shape fpsNode = new Shape(this.fpsText, this.fpsText.createAppearance(new Color(.8f, .8f, .8f)));
		fpsNode.setCullMode(CullMode.NEVER);
		fpsNode.getLocalTransform().getTranslation().set(0f, this.fpsText.getTextHeight(), 0f);
		DepthTest dt = new DepthTest();
		dt.setTest(PixelTest.ALWAYS);
		fpsNode.getAppearance().addState(dt);
		
		Group g = new Group();
		g.add(fpsNode);
		
		View ortho = new View();
		ortho.setOrthogonalProjection(true);
		ortho.setFrustum(0, this.window.getWidth(), 0, this.window.getHeight(), -1, 1);
		this.fpsPass = new BasicRenderPass(g, ortho);
		this.fpsPass.setSceneUpdated(true);
		this.window.addRenderPass(this.fpsPass);
		
		// somewhat lame to get input working for now
		Frame f = (Frame) this.window.getWindowImpl();
		this.configureInputHandling(f.getComponent(0), viewTrans);
		
		this.lastFpsUpdate = 0;
	}
	
	private static Vector3f origin = new Vector3f();
	private static Vector3f up = new Vector3f(0f, 1f, 0f);
	@Override
	protected boolean update() {
		this.view.lookAt(origin, up, true);
		this.scene.update(true);
		return false;
	}
	
	@Override
	protected boolean render(Renderer renderer) {
		if (this.window.isDestroyed())
			return true;
		
		if (this.window.isVisible()) {
			renderer.queueRender(this.window);
			boolean res = super.render(renderer);
			
			if (System.currentTimeMillis() - this.lastFpsUpdate > UPDATE_TIME) {
				this.lastFpsUpdate = System.currentTimeMillis();
				Runtime run = Runtime.getRuntime();
				this.fpsStringBuilder.setLength(0);
				this.fpsFormatter.format("FPS: %.2f\nMeshes: %d\nPolygons: %d\nUsed: %.2f / %.2f M", this.stats.getFramesPerSecond(), 
																									 this.stats.getMeshCount(),
																									 this.stats.getPolygonCount(),
																									 (run.totalMemory() - run.freeMemory()) / 1e6f,
																									 run.totalMemory() / 1e6f);
				this.fpsText.setText(this.fpsStringBuilder.toString());
				renderer.requestUpdate(this.fpsText, true);
			}
			
			return res;
		} else {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {}
			return false;
		}
	}
	
	@Override
	protected void destroy(Renderer renderer) {
		System.out.println(this.window.getDisplayOptions());
		if (!this.window.isDestroyed())
			renderer.destroy(this.window);
		super.destroy(renderer);
	}
}
