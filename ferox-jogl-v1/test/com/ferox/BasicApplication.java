package com.ferox;

import java.awt.Font;
import java.awt.Frame;
import java.util.Formatter;

import com.ferox.effect.BlendMode;
import com.ferox.effect.DepthTest;
import com.ferox.effect.Effect;
import com.ferox.effect.Material;
import com.ferox.effect.Texture;
import com.ferox.effect.Effect.PixelTest;
import com.ferox.math.Color4f;
import com.ferox.math.Transform;
import com.ferox.math.Vector3f;
import com.ferox.renderer.AtomRenderPass;
import com.ferox.renderer.DisplayOptions;
import com.ferox.renderer.Framework;
import com.ferox.renderer.OnscreenSurface;
import com.ferox.renderer.RenderAtom;
import com.ferox.renderer.View;
import com.ferox.resource.Geometry.CompileType;
import com.ferox.scene.ViewNode;
import com.ferox.scene.fx.SceneCompositor;
import com.ferox.util.Bag;
import com.ferox.util.text.CharacterSet;
import com.ferox.util.text.Text;

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
	protected AtomRenderPass fpsPass;
	protected Text fpsText;

	protected ViewNode view;
	protected SceneCompositor sceneCompositor;

	private long lastFpsUpdate;
	private int fpsCount;
	private float avgFps;

	private StringBuilder fpsStringBuilder;
	private Formatter fpsFormatter;

	public BasicApplication(boolean debug) {
		super(debug);
	}

	/** Called after window has been created. */
	protected abstract SceneCompositor buildScene(Framework renderer, ViewNode view);

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

		window = renderer.createWindowSurface(createOptions(), 10, 10, 640, 480,
											  false, false);
		window.setTitle(this.getClass().getSimpleName());

		v.getFrustum().setPerspective(60f, (float) window.getWidth() / window.getHeight(), 1f, 1000f);
		sceneCompositor = buildScene(renderer, view);

		CharacterSet charSet = new CharacterSet(Font.decode("Arial-Bold-16"), true, false);
		fpsText = new Text(charSet, "FPS: \nMeshes: \nPolygons: \nUsed: ", CompileType.NONE);
		fpsStringBuilder = new StringBuilder();
		fpsFormatter = new Formatter(fpsStringBuilder);

		renderer.requestUpdate(charSet.getCharacterSet(), true);
		renderer.requestUpdate(fpsText, true);

		RenderAtom fpsNode = new RenderAtom(new Transform(), fpsText, createTextAppearance(fpsText), null);
		fpsNode.getTransform().getTranslation().set(0f, fpsText.getTextHeight(), 0f);

		View ortho = new View();
		ortho.getFrustum().setOrthogonalProjection(true);
		ortho.getFrustum().setFrustum(0, window.getWidth(), 0, window.getHeight(), -1, 1);
		
		fpsPass = new AtomRenderPass(ortho, fpsNode);

		// somewhat lame to get input working for now
		Frame f = (Frame) window.getWindowImpl();
		configureInputHandling(f.getComponent(0), viewTrans);

		lastFpsUpdate = 0;
	}
	
	private Bag<Effect> createTextAppearance(Text t) {
		Material m = new Material(new Color4f(.8f, .8f, .8f));
		BlendMode bm = new BlendMode();
		Texture td = new Texture(t.getCharacterSet().getCharacterSet());
		
		DepthTest d = new DepthTest();
		d.setTest(PixelTest.ALWAYS);
		d.setWriteEnabled(false);
		
		Bag<Effect> effects = new Bag<Effect>();
		effects.add(m);
		effects.add(bm);
		effects.add(td);
		effects.add(d);
		
		return effects;
	}

	private static Vector3f origin = new Vector3f();
	private static Vector3f up = new Vector3f(0f, 1f, 0f);

	@Override
	protected boolean update() {
		//view.lookAt(origin, up); // FIXME: must do this
		view.setDirty();
		sceneCompositor.getScene().update();
		return false;
	}

	@Override
	protected boolean render(Framework renderer) {
		if (window.isDestroyed()) {
			return true;
		}

		if (window.isVisible()) {
			sceneCompositor.queueAll();
			renderer.queue(window, fpsPass);
			renderer.renderFrame(stats);
			
			fpsCount++;
			avgFps += stats.getFramesPerSecond();

			if (System.currentTimeMillis() - lastFpsUpdate > UPDATE_TIME) {
				lastFpsUpdate = System.currentTimeMillis();
				Runtime run = Runtime.getRuntime();
				fpsStringBuilder.setLength(0);
				fpsFormatter.format("FPS: %.2f\nMeshes: %d\nPolygons: %d\nUsed: %.2f / %.2f M",
									avgFps / fpsCount, stats.getMeshCount(), stats.getPolygonCount(),
									(run.totalMemory() - run.freeMemory()) / (1024f * 1024f), 
									run.totalMemory() / (1024f * 1024f));
				fpsText.setText(fpsStringBuilder.toString());
				fpsText.layoutText();

				fpsCount = 0;
				avgFps = 0;
			}

			return false;
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
		if (!window.isDestroyed()) {
			renderer.destroy(window);
		}
		super.destroy(renderer);
	}
}
