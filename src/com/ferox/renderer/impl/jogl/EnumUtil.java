package com.ferox.renderer.impl.jogl;

import javax.media.opengl.GL;

import com.ferox.resource.TextureCubeMap;
import com.ferox.resource.TextureFormat;
import com.ferox.resource.BufferData.DataType;
import com.ferox.resource.BufferedGeometry.PolygonType;
import com.ferox.resource.GlslUniform.UniformType;
import com.ferox.resource.GlslVertexAttribute.AttributeType;
import com.ferox.resource.TextureImage.DepthMode;
import com.ferox.resource.TextureImage.Filter;
import com.ferox.resource.TextureImage.TextureTarget;
import com.ferox.resource.TextureImage.TextureWrap;
import com.ferox.resource.VertexBufferObject.UsageHint;
import com.ferox.scene.Fog.FogEquation;
import com.ferox.state.BlendMode.BlendEquation;
import com.ferox.state.BlendMode.BlendFactor;
import com.ferox.state.FogReceiver.FogCoordSource;
import com.ferox.state.PolygonStyle.DrawStyle;
import com.ferox.state.State.PixelTest;
import com.ferox.state.State.Quality;
import com.ferox.state.StencilTest.StencilOp;
import com.ferox.state.Texture.CombineAlpha;
import com.ferox.state.Texture.CombineOperand;
import com.ferox.state.Texture.CombineRgb;
import com.ferox.state.Texture.CombineSource;
import com.ferox.state.Texture.EnvMode;
import com.ferox.state.Texture.TexCoordGen;

/** EnumUtil provides conversions for the commonly used enums
 * in States/Resources to their appropriate GL enum values. 
 * 
 * @author Michael Ludwig
 *
 */
public class EnumUtil {
	/** Return the UniformType enum value associated with the returned GL enum
	 * for uniform variable type.  Returns null if there's no matching UniformType. */
	public static UniformType getUniformType(int type) {
		switch(type) {
		case GL.GL_FLOAT: return UniformType.FLOAT;
		case GL.GL_FLOAT_VEC2: return UniformType.FLOAT_VEC2;
		case GL.GL_FLOAT_VEC3: return UniformType.FLOAT_VEC3;
		case GL.GL_FLOAT_VEC4: return UniformType.FLOAT_VEC4;
		
		case GL.GL_FLOAT_MAT2: return UniformType.FLOAT_MAT2;
		case GL.GL_FLOAT_MAT3: return UniformType.FLOAT_MAT3;
		case GL.GL_FLOAT_MAT4: return UniformType.FLOAT_MAT4;
		
		case GL.GL_INT: return UniformType.INT;
		case GL.GL_INT_VEC2: return UniformType.INT_VEC2;
		case GL.GL_INT_VEC3: return UniformType.INT_VEC3;
		case GL.GL_INT_VEC4: return UniformType.INT_VEC4;
		
		case GL.GL_BOOL: return UniformType.BOOL;
		case GL.GL_BOOL_VEC2: return UniformType.BOOL_VEC2;
		case GL.GL_BOOL_VEC3: return UniformType.BOOL_VEC3;
		case GL.GL_BOOL_VEC4: return UniformType.BOOL_VEC4;
		
		case GL.GL_SAMPLER_1D: return UniformType.SAMPLER_1D;
		case GL.GL_SAMPLER_2D: return UniformType.SAMPLER_2D;
		case GL.GL_SAMPLER_3D: return UniformType.SAMPLER_3D;
		case GL.GL_SAMPLER_CUBE: return UniformType.SAMPLER_CUBEMAP;
		case GL.GL_SAMPLER_2D_RECT_ARB: return UniformType.SAMPLER_RECT;
		case GL.GL_SAMPLER_2D_SHADOW: return UniformType.SAMPLER_2D_SHADOW;
		case GL.GL_SAMPLER_1D_SHADOW: return UniformType.SAMPLER_1D_SHADOW;
		case GL.GL_SAMPLER_2D_RECT_SHADOW_ARB: return UniformType.SAMPLER_RECT_SHADOW;
		}

		return null;
	}
	
	/** Return the AttributeType enum value associated with the returned GL enum
	 * for attribute variable type.  Returns null if there's no matching AttributeType. */
	public static AttributeType getAttributeType(int type) {
		switch(type) {
		case GL.GL_FLOAT: return AttributeType.FLOAT;
		case GL.GL_FLOAT_VEC2: return AttributeType.VEC2F;
		case GL.GL_FLOAT_VEC3: return AttributeType.VEC3F;
		case GL.GL_FLOAT_VEC4: return AttributeType.VEC4F;
		case GL.GL_FLOAT_MAT2: return AttributeType.MAT2F;
		case GL.GL_FLOAT_MAT3: return AttributeType.MAT3F;
		case GL.GL_FLOAT_MAT4: return AttributeType.MAT4F;
		}

		return null;
	}
	
	/** Type can't be null. */
	public static int getGLPolygonConnectivity(PolygonType type) {
		switch(type) {
		case LINES: return GL.GL_LINES;
		case POINTS: return GL.GL_POINTS;
		case QUAD_STRIP: return GL.GL_QUAD_STRIP;
		case QUADS: return GL.GL_QUADS;
		case TRIANGLE_STRIP: return GL.GL_TRIANGLE_STRIP;
		case TRIANGLES: return GL.GL_TRIANGLES;
		}
		
		return -1;
	}
	
	/** Hint must not be null. */
	public static int getGLUsageHint(UsageHint hint) {
		switch(hint) {
		case STATIC: return GL.GL_STATIC_DRAW_ARB;
		case STREAM: return GL.GL_STREAM_DRAW_ARB;
		}
		
		return -1;
	}
	
	/** Src must not be null. */
	public static int getGLFogCoordSrc(FogCoordSource src) {
		switch(src) {
		case FOG_COORDINATE: return GL.GL_FOG_COORD;
		case FRAGMENT_DEPTH: return GL.GL_FRAGMENT_DEPTH;
		}
		
		return -1;
	}
	
	/** Qual must not be null. */
	public static int getGLHint(Quality qual) {
		switch(qual) {
		case BEST: return GL.GL_NICEST;
		case DONT_CARE: return GL.GL_DONT_CARE;
		case FAST: return GL.GL_FASTEST;
		}
		
		return -1;
	}
	
	/** Test must not be null. */
	public static int getGLPixelTest(PixelTest test) {
		switch(test) {
		case ALWAYS: return GL.GL_ALWAYS;
		case EQUAL: return GL.GL_EQUAL;
		case GEQUAL: return GL.GL_GEQUAL;
		case GREATER: return GL.GL_GREATER;
		case LEQUAL: return GL.GL_LEQUAL;
		case LESS: return GL.GL_LESS;
		case NEVER: return GL.GL_NEVER;
		case NOT_EQUAL: return GL.GL_NOTEQUAL;
		}
		
		return -1;
	}
	
	/** Return the gl enum associated with the given filter for
	 * minification.  filter must not be null. */
	public static int getGLMinFilter(Filter filter) {
		switch(filter) {
		case LINEAR:
			return GL.GL_LINEAR;
		case NEAREST:
			return GL.GL_NEAREST;
		case MIPMAP_LINEAR:
				return GL.GL_LINEAR_MIPMAP_LINEAR;
		case MIPMAP_NEAREST:
			return GL.GL_NEAREST_MIPMAP_NEAREST;
		}
		
		return -1;
	}
	
	/** Return the gl enum associated with the given filter for
	 * magnification.  filter must not be null. */
	public static int getGLMagFilter(Filter filter) {
		switch(filter) {
		case LINEAR: case MIPMAP_LINEAR:
			return GL.GL_LINEAR;
		case NEAREST: case MIPMAP_NEAREST:
			return GL.GL_NEAREST;
		}
		
		return -1;
	}
	
	/** Wrap must not be null. */
	public static int getGLWrapMode(TextureWrap wrap) {
		switch(wrap) {
		case CLAMP:
			return GL.GL_CLAMP_TO_EDGE;
		case MIRROR:
			return GL.GL_MIRRORED_REPEAT;
		case REPEAT:
			return GL.GL_REPEAT;
		}
		
		return -1;
	}
	
	/** Face must be one of the constants in TextureCubeMap (0 - 5). */
	public static int getGLCubeFace(int face) {
		switch(face) {
		case TextureCubeMap.PX: return GL.GL_TEXTURE_CUBE_MAP_POSITIVE_X;
		case TextureCubeMap.NX: return GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_X;
		case TextureCubeMap.PY: return GL.GL_TEXTURE_CUBE_MAP_POSITIVE_Y;
		case TextureCubeMap.NY: return GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y;
		case TextureCubeMap.PZ: return GL.GL_TEXTURE_CUBE_MAP_POSITIVE_Z;
		case TextureCubeMap.NZ: return GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z;
		}
		
		return -1;
	}
	
	/** Target must not be null. */
	public static int getGLTextureTarget(TextureTarget tar) {
		switch(tar) {
		case T_1D:
			return GL.GL_TEXTURE_1D;
		case T_2D:
			return GL.GL_TEXTURE_2D;
		case T_3D:
			return GL.GL_TEXTURE_3D;
		case T_CUBEMAP:
			return GL.GL_TEXTURE_CUBE_MAP;
		case T_RECT:
			return GL.GL_TEXTURE_RECTANGLE_ARB;
		}
		
		return -1;
	}
	
	/** Format must not be null.  Returns an enum for the src format in glTexImage. 
	 * Returns -1 for compressed formats. */
	public static int getGLSrcFormat(TextureFormat format) {
		switch(format) {
		// packed of the RGBA variety (packed type distinguishes them)
		case ABGR_1555:
		case ABGR_4444:
		case ABGR_8888:
			return GL.GL_RGBA;
		case RGBA_4444:
		case RGBA_5551:
		case RGBA_8888:
			return GL.GL_RGBA;
		
		// packed of the BGRA variety (packed type distinguishes them)
		case ARGB_1555:
		case ARGB_4444:
		case ARGB_8888:
			return GL.GL_BGRA;
		case BGRA_4444: 
		case BGRA_5551: 
		case BGRA_8888:
			return GL.GL_BGRA;
		
		// packed and unpacked RGB types
		case RGB:
		case RGB_565:
		case RGB_FLOAT:
			return GL.GL_RGB;
			
		// packed and unpacked BGR types	
		case BGR:
			return GL.GL_BGR;
		case BGR_565:
			return GL.GL_RGB; // type swaps the ordering
			
		// unpacked RGBA and BGRA types
		case RGBA:
		case RGBA_FLOAT:
			return GL.GL_RGBA;
		case BGRA:
			return GL.GL_BGRA;
		
		// depth formats
		case DEPTH:
			return GL.GL_DEPTH_COMPONENT;
			
		// alpha formats
		case ALPHA:
		case ALPHA_FLOAT:
			return GL.GL_ALPHA;
			
		// luminance formats
		case LUMINANCE:
		case LUMINANCE_FLOAT:
			return GL.GL_LUMINANCE;
			
		// LA formats
		case LUMINANCE_ALPHA:
		case LUMINANCE_ALPHA_FLOAT:
			return GL.GL_LUMINANCE_ALPHA;
			
		}
		
		// a compressed type
		return -1;
	}
	
	/** Format and type can't be null.  Returns an enum for the dst format in glTexImage. */
	public static int getGLDstFormat(TextureFormat format, DataType type) {
		switch(format) {
		// packed RGB5_A1
		case ABGR_1555: 
		case ARGB_1555:
		case RGBA_5551:
		case BGRA_5551:
			return GL.GL_RGB5_A1;
		
		// packed RGBA4
		case ABGR_4444: 
		case ARGB_4444:
		case RGBA_4444: 
		case BGRA_4444: 
			return GL.GL_RGBA4;
			
		// packed RGBA8
		case ABGR_8888: 
		case ARGB_8888:
		case RGBA_8888:
		case BGRA_8888:
			return GL.GL_RGBA8;

		// packed RGB8
		case RGB_565:
		case BGR_565:
			return GL.GL_RGB5;
			
		// unclamped floating point
		case RGB_FLOAT:
			return GL.GL_RGB32F_ARB;
		case RGBA_FLOAT:
			return GL.GL_RGBA32F_ARB;
		case ALPHA_FLOAT:
			return GL.GL_ALPHA32F_ARB;
		case LUMINANCE_FLOAT:
			return GL.GL_LUMINANCE32F_ARB;
		case LUMINANCE_ALPHA_FLOAT:
			return GL.GL_LUMINANCE_ALPHA32F_ARB;

		// DXT_n compression
		case RGB_DXT1:
			return GL.GL_COMPRESSED_RGB_S3TC_DXT1_EXT;
		case RGBA_DXT1:
			return GL.GL_COMPRESSED_RGBA_S3TC_DXT1_EXT;
		case RGBA_DXT3:
			return GL.GL_COMPRESSED_RGBA_S3TC_DXT3_EXT;
		case RGBA_DXT5:
			return GL.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT;
			
		// if we've gotten here, we have a type-less format, and have to take the type into account
		case ALPHA:
			switch(type) {
			case UNSIGNED_BYTE: return GL.GL_ALPHA8;
			case UNSIGNED_SHORT: case UNSIGNED_INT: return GL.GL_ALPHA16;
			default: return GL.GL_ALPHA;
			}	
		case LUMINANCE:
			switch(type) {
			case UNSIGNED_BYTE: return GL.GL_LUMINANCE8;
			case UNSIGNED_SHORT: case UNSIGNED_INT: return GL.GL_LUMINANCE16;
			default: return GL.GL_LUMINANCE;
			}
		case LUMINANCE_ALPHA:
			switch(type) {
			case UNSIGNED_BYTE: return GL.GL_LUMINANCE8_ALPHA8;
			case UNSIGNED_SHORT: case UNSIGNED_INT: return GL.GL_LUMINANCE16_ALPHA16;
			default: return GL.GL_LUMINANCE_ALPHA;
			}
		case DEPTH:
			switch(type) {
			case UNSIGNED_BYTE: return GL.GL_DEPTH_COMPONENT16;
			case UNSIGNED_SHORT: return GL.GL_DEPTH_COMPONENT24;
			case UNSIGNED_INT: return GL.GL_DEPTH_COMPONENT32;
			default: return GL.GL_DEPTH_COMPONENT;
			}
		case RGB: case BGR:
			switch(type) {
			case UNSIGNED_BYTE: return GL.GL_RGB8;
			case UNSIGNED_SHORT: case UNSIGNED_INT: return GL.GL_RGB16;
			default: return GL.GL_RGB;
			}
		case RGBA: case BGRA:
			switch(type) {
			case UNSIGNED_BYTE: return GL.GL_RGBA8;
			case UNSIGNED_SHORT: case UNSIGNED_INT: return GL.GL_RGBA16;
			default: return GL.GL_RGBA;
			}
		}
	
		return -1;
	}
	
	/** Format must not be null.  Returns an appropriate data type for packed
	 * source format.  Returns -1 if it's not a packed type. 
	 * 
	 * These are chosen with the assumption of big-endian byte ordering. */
	public static int getGLPackedType(TextureFormat format) {
		switch(format) {
		// packed ABGR and ARGB types
		case ABGR_1555: case ARGB_1555:
			return GL.GL_UNSIGNED_SHORT_1_5_5_5_REV;
		case ABGR_4444: case ARGB_4444:
			return GL.GL_UNSIGNED_SHORT_4_4_4_4_REV;
		case ABGR_8888: case ARGB_8888:
			return GL.GL_UNSIGNED_INT_8_8_8_8_REV;
		
		// packed BGRA and RGBA types
		case BGRA_5551: case RGBA_5551:
			return GL.GL_UNSIGNED_SHORT_5_5_5_1;
		case BGRA_4444: case RGBA_4444:
			return GL.GL_UNSIGNED_SHORT_4_4_4_4;
		case BGRA_8888: case RGBA_8888:
			return GL.GL_UNSIGNED_INT_8_8_8_8;
			
		// packed BGR and RGB types
		case BGR_565:
			return GL.GL_UNSIGNED_SHORT_5_6_5_REV;
		case RGB_565:
			return GL.GL_UNSIGNED_SHORT_5_6_5;
		}
		
		// not a packed type
		return -1;
	}
	
	/** Type must not be null. This shouldn't be used for packed data types. */
	public static int getGLType(DataType type) {
		switch(type) {
		case BYTE: 
			return GL.GL_BYTE;
		case FLOAT:
			return GL.GL_FLOAT;
		case INT:
			return GL.GL_INT;
		case SHORT:
			return GL.GL_SHORT;
		case UNSIGNED_BYTE:
			return GL.GL_UNSIGNED_BYTE;
		case UNSIGNED_INT:
			return GL.GL_UNSIGNED_INT;
		case UNSIGNED_SHORT:
			return GL.GL_UNSIGNED_SHORT;
		}
		
		return -1;
	}
	
	/** Depth must not be null. */
	public static int getGLDepthMode(DepthMode depth) {
		switch(depth) {
		case ALPHA:
			return GL.GL_ALPHA;
		case INTENSITY:
			return GL.GL_INTENSITY;
		case LUMINANCE:
			return GL.GL_LUMINANCE;
		}
		
		return -1;
	}
	
	/** Func must not be null. */
	public static int getGLBlendEquation(BlendEquation func) {
		switch(func) {
		case ADD: return GL.GL_FUNC_ADD;
		case MAX: return GL.GL_MAX;
		case MIN: return GL.GL_MIN;
		case REVERSE_SUBTRACT: return GL.GL_FUNC_REVERSE_SUBTRACT;
		case SUBTRACT: return GL.GL_FUNC_SUBTRACT;
		}
		
		return -1;
	}
	
	/** Src must not be null. */
	public static int getGLBlendFactor(BlendFactor src) {
		switch(src) {
		case ZERO: return GL.GL_ZERO;
		case ONE: return GL.GL_ONE;
		case SRC_COLOR: return GL.GL_SRC_COLOR;
		case ONE_MINUS_SRC_COLOR: return GL.GL_ONE_MINUS_SRC_COLOR;
		case SRC_ALPHA: return GL.GL_SRC_ALPHA;
		case ONE_MINUS_SRC_ALPHA: return GL.GL_ONE_MINUS_SRC_ALPHA;
		case SRC_ALPHA_SATURATE: return GL.GL_SRC_ALPHA_SATURATE;
		}
		
		return -1;
	}
	
	/** eq must not be null. */
	public static int getGLFogMode(FogEquation eq) {
		switch(eq) {
		case EXP: return GL.GL_EXP;
		case EXP_SQUARED: return GL.GL_EXP2;
		case LINEAR: return GL.GL_LINEAR;
		}
		
		return -1;
	}
	
	/** Should not be called with NONE or null, as no gl enum exists to match it. */
	public static int getGLPolygonMode(DrawStyle style) {
		switch(style) {
		case LINE: return GL.GL_LINE;
		case POINT: return GL.GL_POINT;
		case SOLID: return GL.GL_FILL;
		}
		
		return -1;
	}
	
	/** Op must not be null. */
	public static int getGLStencilOp(StencilOp op) {
		switch(op) {
		case DECREMENT: return GL.GL_DECR;
		case DECREMENT_WRAP: return GL.GL_DECR_WRAP;
		case INCREMENT: return GL.GL_INCR;
		case INCREMENT_WRAP: return GL.GL_INCR_WRAP;
		case ZERO: return GL.GL_ZERO;
		case KEEP: return GL.GL_KEEP;
		case REPLACE: return GL.GL_REPLACE;
		case INVERT: return GL.GL_INVERT;
		}
		
		return -1;
	}
	
	/** Mode must not be null. */
	public static int getGLTexEnvMode(EnvMode mode) {
		switch(mode) {
		case REPLACE:
			return GL.GL_REPLACE;
		case DECAL:
			return GL.GL_DECAL;
		case MODULATE:
			return GL.GL_MODULATE;
		case BLEND:
			return GL.GL_BLEND;
		case COMBINE:
			return GL.GL_COMBINE;
		}
		
		return -1;
	}
	
	/** Should not be called with NONE or null, as nothing parallels its meaning. */
	public static int getGLTexGen(TexCoordGen gen) {
		switch(gen) {
		case EYE:
			return GL.GL_EYE_LINEAR;
		case OBJECT:
			return GL.GL_OBJECT_LINEAR;
		case SPHERE:
			return GL.GL_SPHERE_MAP;
		case REFLECTION:
			return GL.GL_REFLECTION_MAP;
		case NORMAL:
			return GL.GL_NORMAL_MAP;
		}
		
		return -1;
	}
	
	/** Func must not be null. */
	public static int getGLCombineRGBFunc(CombineRgb func) {
		switch(func) {
		case ADD:
			return GL.GL_ADD;
		case ADD_SIGNED:
			return GL.GL_ADD_SIGNED;
		case DOT3_RGB:
			return GL.GL_DOT3_RGB;
		case DOT3_RGBA:
			return GL.GL_DOT3_RGBA;
		case INTERPOLATE:
			return GL.GL_INTERPOLATE;
		case MODULATE:
			return GL.GL_MODULATE;
		case REPLACE:
			return GL.GL_REPLACE;
		case SUBTRACT:
			return GL.GL_SUBTRACT;
		}
		
		return -1;
	}
	
	/** Func must not be null. */
	public static int getGLCombineAlphaFunc(CombineAlpha func) {
		switch(func) {
		case ADD:
			return GL.GL_ADD;
		case ADD_SIGNED:
			return GL.GL_ADD_SIGNED;
		case INTERPOLATE:
			return GL.GL_INTERPOLATE;
		case MODULATE:
			return GL.GL_MODULATE;
		case REPLACE:
			return GL.GL_REPLACE;
		case SUBTRACT:
			return GL.GL_SUBTRACT;
		}
		
		return -1;
	}
	
	/** Op must not be null. */
	public static int getGLCombineOp(CombineOperand op) {
		switch(op) {
		case ALPHA:
			return GL.GL_SRC_ALPHA;
		case COLOR:
			return GL.GL_SRC_COLOR;
		case ONE_MINUS_ALPHA:
			return GL.GL_ONE_MINUS_SRC_ALPHA;
		case ONE_MINUS_COLOR:
			return GL.GL_ONE_MINUS_SRC_COLOR;
		}
		
		return -1;
	}
	
	/** Src must not be null. */
	public static int getGLCombineSrc(CombineSource src) {
		switch(src) {
		case BLEND_COLOR:
			return GL.GL_CONSTANT;
		case CURR_TEX:
			return GL.GL_TEXTURE;
		case PREV_TEX:
			return GL.GL_PREVIOUS;
		case VERTEX_COLOR:
			return GL.GL_PRIMARY_COLOR;
		case TEX0:
			return GL.GL_TEXTURE0;
		case TEX1:
			return GL.GL_TEXTURE1;
		case TEX2:
			return GL.GL_TEXTURE2;
		case TEX3:
			return GL.GL_TEXTURE3;
		case TEX4:
			return GL.GL_TEXTURE4;
		case TEX5:
			return GL.GL_TEXTURE5;
		case TEX6:
			return GL.GL_TEXTURE6;
		case TEX7:
			return GL.GL_TEXTURE7;
		case TEX8:
			return GL.GL_TEXTURE8;
		case TEX9:
			return GL.GL_TEXTURE9;
		case TEX10:
			return GL.GL_TEXTURE10;
		case TEX11:
			return GL.GL_TEXTURE11;
		case TEX12:
			return GL.GL_TEXTURE12;
		case TEX13:
			return GL.GL_TEXTURE13;
		case TEX14:
			return GL.GL_TEXTURE14;
		case TEX15:
			return GL.GL_TEXTURE15;
		case TEX16:
			return GL.GL_TEXTURE16;
		case TEX17:
			return GL.GL_TEXTURE17;
		case TEX18:
			return GL.GL_TEXTURE18;
		case TEX19:
			return GL.GL_TEXTURE19;
		case TEX20:
			return GL.GL_TEXTURE20;
		case TEX21:
			return GL.GL_TEXTURE21;
		case TEX22:
			return GL.GL_TEXTURE22;
		case TEX23:
			return GL.GL_TEXTURE23;
		case TEX24:
			return GL.GL_TEXTURE24;
		case TEX25:
			return GL.GL_TEXTURE25;
		case TEX26:
			return GL.GL_TEXTURE26;
		case TEX27:
			return GL.GL_TEXTURE27;
		case TEX28:
			return GL.GL_TEXTURE28;
		case TEX29:
			return GL.GL_TEXTURE29;
		case TEX30:
			return GL.GL_TEXTURE30;
		case TEX31:
			return GL.GL_TEXTURE31;
		}
		
		return -1;
	}
}
