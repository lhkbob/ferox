package com.ferox.resource.text;

import java.awt.Font;
import java.awt.GraphicsEnvironment;

import com.ferox.BasicApplication;
import com.ferox.math.BoundSphere;
import com.ferox.math.Color;
import com.ferox.renderer.Renderer;
import com.ferox.resource.Geometry;
import com.ferox.resource.texture.Texture2D;
import com.ferox.resource.texture.TextureImage.Filter;
import com.ferox.scene.Group;
import com.ferox.scene.SceneElement;
import com.ferox.scene.Shape;
import com.ferox.scene.ViewNode;
import com.ferox.state.PolygonStyle;
import com.ferox.state.PolygonStyle.DrawStyle;

public class TextTest extends BasicApplication {
	public static final boolean DEBUG = false;
	public static final boolean USE_VBO = true;
	
	public static final Color bgColor = new Color(0f, 0f, 0f);
	
	protected Geometry geom;
	
	public static void main(String[] args) {
		new TextTest(DEBUG).run();
	}
	
	public TextTest(boolean debug) {
		super(debug);
	}

	@Override
	protected SceneElement buildScene(Renderer renderer, ViewNode view) {
		Font[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
		for (Font f: fonts) {
			System.out.println(f.getName() + " " + f.getStyle());
		}
		CharacterSet charSet = new CharacterSet(Font.decode("Times-Roman-Plain-64"), true, true);
		Texture2D sheet = charSet.getCharacterSet();
		sheet.setFilter(Filter.LINEAR);
		sheet.setAnisotropicFiltering(1f);
		
		System.out.println(sheet.getWidth(0) + " " + sheet.getHeight(0));
		renderer.requestUpdate(sheet, true);
		
		Group root = new Group();
		root.add(view);
		
		Text text = new Text(charSet, "Hello World! This is my text renderer, \thow awesome is that? \n\rMy name isé Michael Ludwig.");
		text.setWrapWidth(this.window.getWidth() / 1f);
		renderer.requestUpdate(text, true);

		Shape t = new Shape(text, text.createAppearance(new Color(1f, 0f, 0f)));
		PolygonStyle ps = new PolygonStyle();
		ps.setBackStyle(DrawStyle.SOLID);
		t.getAppearance().addState(ps);
		
		t.setLocalBounds(new BoundSphere());
		t.getLocalTransform().getTranslation().set(0f, 0f, 0f);
		t.getLocalTransform().setScale(.3f);
		root.add(t);
		
		view.getLocalTransform().setTranslation(0f, 0f, 50f);
		
		this.window.setClearColor(bgColor);
		return root;
	}
}
