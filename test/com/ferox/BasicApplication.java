package com.ferox;

import java.awt.Font;
import java.awt.Frame;
import java.util.Formatter;

import org.openmali.vecmath.Vector3f;

import com.ferox.effect.Effect.PixelTest;
import com.ferox.math.Color;
import com.ferox.math.Transform;
import com.ferox.renderer.DisplayOptions;
import com.ferox.renderer.EffectSortingRenderQueue;
import com.ferox.renderer.OnscreenSurface;
import com.ferox.renderer.RenderQueue;
import com.ferox.renderer.Framework;
import com.ferox.renderer.View;
import com.ferox.resource.Geometry.CompileType;
import com.ferox.scene.Group;
import com.ferox.scene.Node;
import com.ferox.scene.SceneRenderPass;
import com.ferox.scene.Shape;
import com.ferox.scene.ViewNode;
import com.ferox.scene.Node.CullMode;
import com.ferox.util.geom.CharacterSet;
import com.ferox.util.geom.Text;

/**
 * BasicApplication extends ApplicationBase and imposes more constraints on the
 * test. It automatically creates a WindowSurface with one render pass and a
 * view. The input is configured to move the constructed view.
 * 
 * @author Michael Ludwig
 */
public abstract class BasicApplication extends ApplicationBase {
	public static final long UPDATE_TIME = 100; // ms between updates of fps

	protected OnscreenSurface window;
	protected SceneRenderPass pass;
	protected SceneRenderPass fpsPass;
	protected Text fpsText;

	protected ViewNode view;
	protected Node scene;

	private long lastFpsUpdate;
	private int fpsCount;
	private float avgFps;

	private StringBuilder fpsStringBuilder;
	private Formatter fpsFormatter;

	public BasicApplication(boolean debug) {
		super(debug);
	}

	/** Called after window has been configured with pass. */
	protected abstract Node buildScene(Framework renderer, ViewNode view);

	/**
	 * Return a RenderQueue that will be used with this application's single
	 * render pass.
	 */
	protected RenderQueue createQueue() {
		return new EffectSortingRenderQueue();
	}

	/**
	 * Return a DisplayOptions to use for the created WindowSurface in init().
	 */
	protected DisplayOptions createOptions() {
		return new DisplayOptions();
	}

	@Override
	protected void init(Framework renderer) {
		View v = new View();
		view = new ViewNode(v);
		Transform viewTrans = view.getLocalTransform();
		viewTrans.getTranslation().set(0f, 0f, 15f);

		pass = new SceneRenderPass(null, v, createQueue(), false);

		// this.window = renderer.createFullscreenSurface(new DisplayOptions(),
		// 640, 480);
		window =
			renderer.createWindowSurface(createOptions(), 10, 10, 640, 480,
				false, false);
		window.addRenderPass(pass);
		window.setTitle(this.getClass().getSimpleName());

		v.setPerspective(60f, (float) window.getWidth() / window.getHeight(),
			1f, 1000f);

		scene = buildScene(renderer, view);
		pass.setScene(scene);

		CharacterSet charSet =
			new CharacterSet(Font.decode("Arial-Bold-16"), true, false);
		fpsText =
			new Text(charSet, "FPS: \nMeshes: \nPolygons: \nUsed: ",
				CompileType.NONE);
		fpsStringBuilder = new StringBuilder();
		fpsFormatter = new Formatter(fpsStringBuilder);

		renderer.requestUpdate(charSet.getCharacterSet(), true);
		renderer.requestUpdate(fpsText, true);

		Shape fpsNode =
			new Shape(fpsText, fpsText
				.createAppearance(new Color(.8f, .8f, .8f)));
		fpsNode.setCullMode(CullMode.NEVER);
		fpsNode.getLocalTransform().getTranslation().set(0f,
			fpsText.getTextHeight(), 0f);
		fpsNode.getAppearance().setDepthTest(PixelTest.ALWAYS, false);

		Group g = new Group();
		g.add(fpsNode);

		View ortho = new View();
		ortho.setOrthogonalProjection(true);
		ortho.setFrustum(0, window.getWidth(), 0, window.getHeight(), -1, 1);
		fpsPass = new SceneRenderPass(g, ortho);
		fpsPass.setSceneUpdated(true);
		window.addRenderPass(fpsPass);

		// somewhat lame to get input working for now
		Frame f = (Frame) window.getWindowImpl();
		configureInputHandling(f.getComponent(0), viewTrans);

		lastFpsUpdate = 0;
	}

	private static Vector3f origin = new Vector3f();
	private static Vector3f up = new Vector3f(0f, 1f, 0f);

	@Override
	protected boolean update() {
		view.lookAt(origin, up);
		scene.update();
		return false;
	}

	@Override
	protected boolean render(Framework renderer) {
		if (window.isDestroyed()) {
			return true;
		}

		if (window.isVisible()) {
			renderer.queueRender(window);
			boolean res = super.render(renderer);

			fpsCount++;
			avgFps += stats.getFramesPerSecond();

			if (System.currentTimeMillis() - lastFpsUpdate > UPDATE_TIME) {
				lastFpsUpdate = System.currentTimeMillis();
				Runtime run = Runtime.getRuntime();
				fpsStringBuilder.setLength(0);
				fpsFormatter.format(
					"FPS: %.2f\nMeshes: %d\nPolygons: %d\nUsed: %.2f / %.2f M",
					avgFps / fpsCount, stats.getMeshCount(), stats
						.getPolygonCount(), (run.totalMemory() - run
						.freeMemory()) / (1024f * 1024f), run.totalMemory() / (1024f * 1024f));
				fpsText.setText(fpsStringBuilder.toString());
				fpsText.layoutText();

				fpsCount = 0;
				avgFps = 0;
			}

			return res;
		} else {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
			}
			return false;
		}
	}

	@Override
	protected void destroy(Framework renderer) {
		System.out.println(window.getDisplayOptions());
		if (!window.isDestroyed()) {
			renderer.destroy(window);
		}
		super.destroy(renderer);
	}
}
