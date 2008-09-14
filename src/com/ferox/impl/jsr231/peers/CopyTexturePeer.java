package com.ferox.impl.jsr231.peers;

import com.ferox.core.renderer.RenderToTexturePass;
import com.ferox.core.states.atoms.Texture2D;
import com.ferox.core.states.atoms.Texture3D;
import com.ferox.core.states.atoms.TextureCubeMap;
import com.ferox.core.states.atoms.TextureData;
import com.ferox.core.states.atoms.TextureCubeMap.Face;
import com.ferox.core.states.atoms.TextureData.TextureTarget;
import com.ferox.core.util.DataTransfer.Block;
import com.ferox.impl.jsr231.JOGLRenderContext;
import com.ferox.impl.jsr231.peers.JOGLRenderToTexturePassPeer.RTTPeer;

public class CopyTexturePeer implements RTTPeer {
	public void finish(RenderToTexturePass pass, JOGLRenderContext context) {
		int width = pass.getWidth();
		int height = pass.getHeight();
		int maxColors = RenderToTexturePass.getMaxColorAttachments();
		
		TextureData attach = pass.getDepthBinding();
		if (attach != null)
			copyPixels(attach, pass.getDepthBindingSlice(), pass.getDepthBindingFace(), width, height, context);
		
		for (int i = 0; i < maxColors; i++) {
			attach = pass.getColorBinding(i);
			if (attach != null)
				copyPixels(attach, pass.getColorBindingSlice(i), pass.getColorBindingFace(i), width, height, context);
		}
	}

	private static void copyPixels(TextureData data, int slice, Face face, int width, int height, JOGLRenderContext context) {
		//FIXME: center region onto the screen if the texture is smaller than the context dims.
		Block region = new Block(0, 0, slice, Math.min(width, context.getContextWidth()), Math.min(height, context.getContextHeight()), 1);
		if (data.getTarget() == TextureTarget.TEX2D)
			context.copyFramePixels((Texture2D)data, region, 0, 0, 0);
		else if (data.getTarget() == TextureTarget.TEX3D)
			context.copyFramePixels((Texture3D)data, region, 0, 0, 0);
		else
			context.copyFramePixels((TextureCubeMap)data, region, face, 0, 0, 0);
	}
	
	public void prepare(RenderToTexturePass pass, JOGLRenderContext context) {
		// do nothing
	}
}
