package com.ferox;

import java.awt.Font;
import java.awt.Frame;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

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
import com.ferox.resource.BufferData;
import com.ferox.resource.Geometry;
import com.ferox.resource.VertexArray;
import com.ferox.resource.VertexArrayGeometry;
import com.ferox.resource.VertexBufferGeometry;
import com.ferox.resource.VertexBufferObject;
import com.ferox.resource.BufferedGeometry.PolygonType;
import com.ferox.resource.VertexBufferObject.UsageHint;
import com.ferox.resource.text.CharacterSet;
import com.ferox.resource.text.Text;
import com.ferox.scene.Group;
import com.ferox.scene.SceneElement;
import com.ferox.scene.Shape;
import com.ferox.scene.ViewNode;
import com.ferox.scene.Node.CullMode;
import com.ferox.state.DepthTest;
import com.ferox.state.State.PixelTest;
import com.sun.opengl.util.BufferUtil;

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
	
	public BasicApplication(boolean debug) {
		super(debug);
	}
	
	public static Geometry buildCube(Renderer renderer, float side, boolean vbo) {
		float[] v = new float[72];
		float[] n = new float[72];
		float[] t = new float[48];
		
		// front
		v[0] = 1f; v[1] = 1f; v[2] = 1f; n[0] = 0f; n[1] = 0f; n[2] = 1f; t[0] = 1f; t[1] = 1f;
		v[3] = -1f; v[4] = 1f; v[5] = 1f; n[3] = 0f; n[4] = 0f; n[5] = 1f; t[2] = 0f; t[3] = 1f;
		v[6] = -1f; v[7] = -1f; v[8] = 1f; n[6] = 0f; n[7] = 0f; n[8] = 1f; t[4] = 0f; t[5] = 0f;
		v[9] = 1f; v[10] = -1f; v[11] = 1f; n[9] = 0f; n[10] = 0f; n[11] = 1f; t[6] = 1f; t[7] = 0f;
		//back
		v[12] = -1f; v[13] = -1f; v[14] = -1f; n[12] = 0f; n[13] = 0f; n[14] = -1f; t[8] = 1f; t[9] = 1f;
		v[21] = 1f; v[22] = -1f; v[23] = -1f; n[21] = 0f; n[22] = 0f; n[23] = -1f; t[10] = 0f; t[11] = 1f;
		v[18] = 1f; v[19] = 1f; v[20] = -1f; n[18] = 0f; n[19] = 0f; n[20] = -1f; t[12] = 0f; t[13] = 0f;
		v[15] = -1f; v[16] = 1f; v[17] = -1f; n[15] = 0f; n[16] = 0f; n[17] = -1f; t[14] = 1f; t[15] = 0f;
		//right
		v[24] = 1f; v[25] = 1f; v[26] = -1f; n[24] = 1f; n[25] = 0f; n[26] = 0f; t[16] = 1f; t[17] = 1f;
		v[27] = 1f; v[28] = 1f; v[29] = 1f; n[27] = 1f; n[28] = 0f; n[29] = 0f; t[18] = 0f; t[19] = 1f;
		v[30] = 1f; v[31] = -1f; v[32] = 1f; n[30] = 1f; n[31] = 0f; n[32] = 0f; t[20] = 0f; t[21] = 0f;
		v[33] = 1f; v[34] = -1f; v[35] = -1f; n[33] = 1f; n[34] = 0f; n[35] = 0f; t[22] = 1f; t[23] = 0f;
		//left
		v[36] = -1f; v[37] = -1f; v[38] = 1f; n[36] = -1f; n[37] = 0f; n[38] = 0f; t[24] = 1f; t[25] = 1f;
		v[45] = -1f; v[46] = -1f; v[47] = -1f; n[45] = -1f; n[46] = 0f; n[47] = 0f; t[26] = 0f; t[27] = 1f;
		v[42] = -1f; v[43] = 1f; v[44] = -1f; n[42] = -1f; n[43] = 0f; n[44] = 0f; t[28] = 0f; t[29] = 0f;
		v[39] = -1f; v[40] = 1f; v[41] = 1f; n[39] = -1f; n[40] = 0f; n[41] = 0f; t[30] = 1f; t[31] = 0f;
		//top
		v[48] = -1f; v[49] = 1f; v[50] = -1f; n[48] = 0f; n[49] = 1f; n[50] = 0f; t[32] = 1f; t[33] = 1f;
		v[57] = 1f; v[58] = 1f; v[59] = -1f; n[57] = 0f; n[58] = 1f; n[59] = 0f; t[34] = 0f; t[35] = 1f;
		v[54] = 1f; v[55] = 1f; v[56] = 1f; n[54] = 0f; n[55] = 1f; n[56] = 0f; t[36] = 0f; t[37] = 0f;
		v[51] = -1f; v[52] = 1f; v[53] = 1f; n[51] = 0f; n[52] = 1f; n[53] = 0f; t[38] = 1f; t[39] = 0f;
		//bottom
		v[60] = 1f; v[61] = -1f; v[62] = 1f; n[60] = 0f; n[61] = -1f; n[62] = 0f; t[40] = 1f; t[41] = 1f;
		v[63] = -1f; v[64] = -1f; v[65] = 1f; n[63] = 0f; n[64] = -1f; n[65] = 0f; t[42] = 0f; t[43] = 1f;
		v[66] = -1f; v[67] = -1f; v[68] = -1f; n[66] = 0f; n[67] = -1f; n[68] = 0f; t[44] = 0f; t[45] = 0f;
		v[69] = 1f; v[70] = -1f; v[71] = -1f; n[69] = 0f; n[70] = -1f; n[71] = 0f; t[46] = 1f; t[47] = 0f;
		
		for (int i = 0; i < v.length; i++)
			v[i] = v[i] * side / 2f;
		
		int[] i = new int[24];
		for (int u = 0; u < 24; u++)
			i[u] = u;
		
		if (vbo) {
			VertexBufferObject vbd = new VertexBufferObject(new BufferData(v, false), UsageHint.STATIC);
			VertexBufferObject nbd = new VertexBufferObject(new BufferData(n, false), UsageHint.STATIC);
			VertexBufferObject tbd = new VertexBufferObject(new BufferData(t, false), UsageHint.STATIC);
			
			VertexBufferObject ibd = new VertexBufferObject(new BufferData(i, true), UsageHint.STATIC);
			
			VertexBufferGeometry geom = new VertexBufferGeometry(vbd, new VertexArray(3), ibd, new VertexArray(1), PolygonType.QUADS);
			geom.setNormals(nbd, new VertexArray(3));
			geom.setTextureCoordinates(0, tbd, new VertexArray(2));
			
			// have to request updates to, best do it here
			renderer.requestUpdate(vbd, false);
			renderer.requestUpdate(nbd, false);
			renderer.requestUpdate(tbd, false);
			renderer.requestUpdate(ibd, false);
			
			renderer.requestUpdate(geom, false);
			return geom;
		} else {
			FloatBuffer vbd = BufferUtil.newFloatBuffer(v.length);
			vbd.put(v).rewind();
			FloatBuffer nbd = BufferUtil.newFloatBuffer(n.length);
			nbd.put(n).rewind();
			FloatBuffer tbd = BufferUtil.newFloatBuffer(t.length);
			tbd.put(t).rewind();
			
			IntBuffer ibd = BufferUtil.newIntBuffer(i.length);
			ibd.put(i).rewind();
			
			VertexArrayGeometry geom = new VertexArrayGeometry(vbd, new VertexArray(3), ibd, new VertexArray(1), PolygonType.QUADS);
			geom.setNormals(nbd, new VertexArray(3));
			geom.setTextureCoordinates(0, tbd, new VertexArray(2));
			
			// have to request updates to, best do it here			
			renderer.requestUpdate(geom, false);
			return geom;
		}
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
		
		//this.window = renderer.createFullscreenSurface(new DisplayOptions(), 800, 600);
		this.window = renderer.createWindowSurface(this.createOptions(), 10, 10, 1024, 768, false, false);
		this.window.addRenderPass(this.pass);
		this.window.setTitle(this.getClass().getSimpleName());
		
		v.setPerspective(60f, (float) this.window.getWidth() / this.window.getHeight(), 1f, 1000f);
		
		this.scene = this.buildScene(renderer, this.view);
		this.pass.setScene(this.scene);
		
		CharacterSet charSet = new CharacterSet(Font.decode("Arial-Bold-16"), true, false);
		this.fpsText = new Text(charSet, "FPS: \nMeshes: \nPolygons: \nUsed: \nFree: ");
		
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
	
	@Override
	protected boolean update() {
		this.view.lookAt(new Vector3f(), new Vector3f(0f, 1f, 0f), true);
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
				this.fpsText.setText("FPS: " + this.stats.getFramesPerSecond() + "\nMeshes: " + this.stats.getMeshCount() + "\nPolygons: " + this.stats.getPolygonCount() 
									 + "\nUsed: " + run.totalMemory() / 1e6f + " M\nFree: " + run.freeMemory() / 1e6f + " M");
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
