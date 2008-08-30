package com.ferox.core.util;

import java.nio.*;

import com.ferox.core.util.TextureUtil.Sampler;

public class Samplers {
	public static class FloatUnpackedSampler extends Sampler {
		private static float lerpColor1 = 0;
		private static float lerpColor2 = 0;
		
		FloatBuffer data;
		int numComps;
		float[] sampleTexel;
		
		// method vars
		private float uLerp, vLerp, wLerp;
		private int tx, ty, tz, ltx, lty, ltz;
		private float[][] cLU;
		private float[][] cLD;
		private float[][] cRU;
		private float[][] cRD;
		private boolean colorsReady;
		
		private int zFactor, yFactor;
		
		public FloatUnpackedSampler(Buffer data, int width, int height, int depth, int numComps) {
			super(data, width, height, depth);
			this.data = (FloatBuffer)data;
			this.numComps = numComps;
			this.sampleTexel = new float[this.numComps];
			
			this.cLD = new float[this.numComps][2];
			this.cLU = new float[this.numComps][2];
			this.cRD = new float[this.numComps][2];
			this.cRU = new float[this.numComps][2];
			
			this.zFactor = this.width * this.height * this.numComps;
			this.yFactor = this.width * this.numComps;
			this.colorsReady = false;
			
			this.tx = -1;
			this.ty = -1;
			this.tz = -1;
		}

		@Override
		public final void getTexel(float u, float v, float w) {
			this.uLerp = u * this.width;
			this.vLerp = v * this.height;
			this.wLerp = w * this.depth;
			
			if ((int)this.uLerp != this.tx) {
				this.tx = (int)this.uLerp;
				this.ltx = Math.min(this.tx + 1, this.width - 1);
				this.colorsReady = false;
			}
			if ((int)this.vLerp != this.ty) {
				this.ty = (int)this.vLerp;
				this.lty = Math.min(this.ty + 1, this.height - 1);
				this.colorsReady = false;
			}
			if ((int)this.wLerp != this.tz) {
				this.tz = (int)this.wLerp;
				this.ltz = Math.min(this.tz + 1, this.depth - 1);
				this.colorsReady = false;
			}		
			
			this.uLerp -= this.tx;
			this.vLerp -= this.ty;
			this.wLerp -= this.tz;
			
			for (int i = 0; i < this.sampleTexel.length; i++) {
				if (!this.colorsReady) {
					this.cLU[i][0] = this.getColor(this.tx, this.ty, this.tz, i);
					this.cRU[i][0] = this.getColor(this.ltx, this.ty, this.tz, i);
					this.cRD[i][0] = this.getColor(this.ltx, this.lty, this.tz, i);
					this.cLD[i][0] = this.getColor(this.tx, this.lty, this.tz, i);
					
					if (this.tz != this.ltz) {
						this.cLU[i][1] = this.getColor(this.tx, this.ty, this.ltz, i);
						this.cRU[i][1] = this.getColor(this.ltx, this.ty, this.ltz, i);
						this.cRD[i][1] = this.getColor(this.ltx, this.lty, this.ltz, i);
						this.cLD[i][1] = this.getColor(this.tx, this.lty, this.ltz, i);
					}
				}
				lerpColor1 = this.cLU[i][0] * (1 - this.uLerp) + this.cRU[i][0] * this.uLerp;
				lerpColor2 = this.cLD[i][0] * (1 - this.uLerp) + this.cRD[i][0] * this.uLerp;
				this.sampleTexel[i] = lerpColor1 * (1 - this.vLerp) + lerpColor2 * this.vLerp;
				
				if (this.tz != this.ltz) {
					// lerp in next layer, too
					lerpColor1 = this.cLU[i][1] * (1 - this.uLerp) + this.cRU[i][1] * this.uLerp;
					lerpColor2 = this.cLD[i][1] * (1 - this.uLerp) + this.cRD[i][1] * this.uLerp;
					lerpColor1 = lerpColor1 * (1 - this.vLerp) + lerpColor2 * this.vLerp;
					this.sampleTexel[i] = this.sampleTexel[i] * (1 - this.wLerp) + lerpColor1 * this.wLerp;
				}
			}
			this.colorsReady = true;
		}
		
		private final float getColor(int x, int y, int z, int i) {
			return this.data.get(this.pos + i + z * this.zFactor + y * this.yFactor + x * this.numComps);
		}
		
		@Override
		public final void setTexel(int x, int y, int z, Sampler inValue) {
			int offset = z * this.zFactor + y * this.yFactor + x * this.numComps;
			float[] t = ((FloatUnpackedSampler)inValue).sampleTexel;
			for (int i = 0; i < t.length; i++)
				this.data.put(this.pos + offset + i, t[i]);
		}
	}
	
	public static class IntUnpackedSampler extends Sampler {
		private static final long converter = (long)Integer.MAX_VALUE - (long)Integer.MIN_VALUE + 1;
		private static float lerpColor1 = 0;
		private static float lerpColor2 = 0;
		
		IntBuffer data;
		int numComps;
		float[] sampleTexel;
		
		// method vars
		private float uLerp, vLerp, wLerp;
		private int tx, ty, tz, ltx, lty, ltz;
		private float[][] cLU;
		private float[][] cLD;
		private float[][] cRU;
		private float[][] cRD;
		private boolean colorsReady;
		
		private int zFactor, yFactor;
		
		public IntUnpackedSampler(Buffer data, int width, int height, int depth, int numComps) {
			super(data, width, height, depth);
			this.data = (IntBuffer)data;
			this.numComps = numComps;
			this.sampleTexel = new float[this.numComps];
			
			this.cLD = new float[this.numComps][2];
			this.cLU = new float[this.numComps][2];
			this.cRD = new float[this.numComps][2];
			this.cRU = new float[this.numComps][2];
			
			this.zFactor = this.width * this.height * this.numComps;
			this.yFactor = this.width * this.numComps;
			this.colorsReady = false;
			
			this.tx = -1;
			this.ty = -1;
			this.tz = -1;
		}

		@Override
		public final void getTexel(float u, float v, float w) {
			this.uLerp = u * this.width;
			this.vLerp = v * this.height;
			this.wLerp = w * this.depth;
			
			if ((int)this.uLerp != this.tx) {
				this.tx = (int)this.uLerp;
				this.ltx = Math.min(this.tx + 1, this.width - 1);
				this.colorsReady = false;
			}
			if ((int)this.vLerp != this.ty) {
				this.ty = (int)this.vLerp;
				this.lty = Math.min(this.ty + 1, this.height - 1);
				this.colorsReady = false;
			}
			if ((int)this.wLerp != this.tz) {
				this.tz = (int)this.wLerp;
				this.ltz = Math.min(this.tz + 1, this.depth - 1);
				this.colorsReady = false;
			}		
			
			this.uLerp -= this.tx;
			this.vLerp -= this.ty;
			this.wLerp -= this.tz;
			
			for (int i = 0; i < this.sampleTexel.length; i++) {
				if (!this.colorsReady) {
					this.cLU[i][0] = this.getColor(this.tx, this.ty, this.tz, i);
					this.cRU[i][0] = this.getColor(this.ltx, this.ty, this.tz, i);
					this.cRD[i][0] = this.getColor(this.ltx, this.lty, this.tz, i);
					this.cLD[i][0] = this.getColor(this.tx, this.lty, this.tz, i);
										
					if (this.tz != this.ltz) {
						this.cLU[i][1] = this.getColor(this.tx, this.ty, this.ltz, i);
						this.cRU[i][1] = this.getColor(this.ltx, this.ty, this.ltz, i);
						this.cRD[i][1] = this.getColor(this.ltx, this.lty, this.ltz, i);
						this.cLD[i][1] = this.getColor(this.tx, this.lty, this.ltz, i);
					}
				}
				lerpColor1 = this.cLU[i][0] * (1 - this.uLerp) + this.cRU[i][0] * this.uLerp;
				lerpColor2 = this.cLD[i][0] * (1 - this.uLerp) + this.cRD[i][0] * this.uLerp;
				this.sampleTexel[i] = lerpColor1 * (1 - this.vLerp) + lerpColor2 * this.vLerp;
				
				if (this.tz != this.ltz) {
					// lerp in next layer, too
					lerpColor1 = this.cLU[i][1] * (1 - this.uLerp) + this.cRU[i][1] * this.uLerp;
					lerpColor2 = this.cLD[i][1] * (1 - this.uLerp) + this.cRD[i][1] * this.uLerp;
					lerpColor1 = lerpColor1 * (1 - this.vLerp) + lerpColor2 * this.vLerp;
					this.sampleTexel[i] = this.sampleTexel[i] * (1 - this.wLerp) + lerpColor1 * this.wLerp;
				}
			}
			this.colorsReady = true;
		}
		
		private final float getColor(int x, int y, int z, int i) {
			long signed = this.data.get(this.pos + i + z * this.zFactor + y * this.yFactor + x * this.numComps);
			return (signed & 0xffffffffL) / (float)converter;
		}
		
		@Override
		public final void setTexel(int x, int y, int z, Sampler inValue) {
			int offset = z * this.zFactor + y * this.yFactor + x * this.numComps;
			float[] t = ((IntUnpackedSampler)inValue).sampleTexel;
			for (int i = 0; i < t.length; i++)
				this.data.put(this.pos + offset + i, (int)((long)(t[i] * (double)(converter - 1))));
		}
	}
	
	public static class ShortUnpackedSampler extends Sampler {
		private static final int converter = Short.MAX_VALUE - Short.MIN_VALUE + 1;
		private static float lerpColor1 = 0;
		private static float lerpColor2 = 0;
		
		ShortBuffer data;
		int numComps;
		float[] sampleTexel;
		
		// method vars
		private float uLerp, vLerp, wLerp;
		private int tx, ty, tz, ltx, lty, ltz;
		private float[][] cLU;
		private float[][] cLD;
		private float[][] cRU;
		private float[][] cRD;
		private boolean colorsReady;
		
		private int zFactor, yFactor;
		
		public ShortUnpackedSampler(Buffer data, int width, int height, int depth, int numComps) {
			super(data, width, height, depth);
			this.data = (ShortBuffer)data;
			this.numComps = numComps;
			this.sampleTexel = new float[this.numComps];
			
			this.cLD = new float[this.numComps][2];
			this.cLU = new float[this.numComps][2];
			this.cRD = new float[this.numComps][2];
			this.cRU = new float[this.numComps][2];
			
			this.zFactor = this.width * this.height * this.numComps;
			this.yFactor = this.width * this.numComps;
			this.colorsReady = false;
			
			this.tx = -1;
			this.ty = -1;
			this.tz = -1;
		}

		@Override
		public final void getTexel(float u, float v, float w) {
			this.uLerp = u * this.width;
			this.vLerp = v * this.height;
			this.wLerp = w * this.depth;
			
			if ((int)this.uLerp != this.tx) {
				this.tx = (int)this.uLerp;
				this.ltx = Math.min(this.tx + 1, this.width - 1);
				this.colorsReady = false;
			}
			if ((int)this.vLerp != this.ty) {
				this.ty = (int)this.vLerp;
				this.lty = Math.min(this.ty + 1, this.height - 1);
				this.colorsReady = false;
			}
			if ((int)this.wLerp != this.tz) {
				this.tz = (int)this.wLerp;
				this.ltz = Math.min(this.tz + 1, this.depth - 1);
				this.colorsReady = false;
			}		
			
			this.uLerp -= this.tx;
			this.vLerp -= this.ty;
			this.wLerp -= this.tz;
			
			for (int i = 0; i < this.sampleTexel.length; i++) {
				if (!this.colorsReady) {
					this.cLU[i][0] = this.getColor(this.tx, this.ty, this.tz, i);
					this.cRU[i][0] = this.getColor(this.ltx, this.ty, this.tz, i);
					this.cRD[i][0] = this.getColor(this.ltx, this.lty, this.tz, i);
					this.cLD[i][0] = this.getColor(this.tx, this.lty, this.tz, i);
					
					if (this.tz != this.ltz) {
						this.cLU[i][1] = this.getColor(this.tx, this.ty, this.ltz, i);
						this.cRU[i][1] = this.getColor(this.ltx, this.ty, this.ltz, i);
						this.cRD[i][1] = this.getColor(this.ltx, this.lty, this.ltz, i);
						this.cLD[i][1] = this.getColor(this.tx, this.lty, this.ltz, i);
					}
				}
				lerpColor1 = this.cLU[i][0] * (1 - this.uLerp) + this.cRU[i][0] * this.uLerp;
				lerpColor2 = this.cLD[i][0] * (1 - this.uLerp) + this.cRD[i][0] * this.uLerp;
				this.sampleTexel[i] = lerpColor1 * (1 - this.vLerp) + lerpColor2 * this.vLerp;
				
				if (this.tz != this.ltz) {
					// lerp in next layer, too
					lerpColor1 = this.cLU[i][1] * (1 - this.uLerp) + this.cRU[i][1] * this.uLerp;
					lerpColor2 = this.cLD[i][1] * (1 - this.uLerp) + this.cRD[i][1] * this.uLerp;
					lerpColor1 = lerpColor1 * (1 - this.vLerp) + lerpColor2 * this.vLerp;
					this.sampleTexel[i] = this.sampleTexel[i] * (1 - this.wLerp) + lerpColor1 * this.wLerp;
				}
			}
			this.colorsReady = true;
		}
		
		private final float getColor(int x, int y, int z, int i) {
			int signed = this.data.get(this.pos + i + z * this.zFactor + y * this.yFactor + x * this.numComps);
			return (signed & 0xffff) / (float)converter;
		}
		
		@Override
		public final void setTexel(int x, int y, int z, Sampler inValue) {
			int offset = z * this.zFactor + y * this.yFactor + x * this.numComps;
			float[] t = ((ShortUnpackedSampler)inValue).sampleTexel;
			for (int i = 0; i < t.length; i++)
				this.data.put(this.pos + offset + i, (short)(t[i] * (converter - 1)));
		}
	}
	
	public static class ByteUnpackedSampler extends Sampler {
		private static final short converter = Byte.MAX_VALUE - Byte.MIN_VALUE + 1;
		private static float lerpColor1 = 0;
		private static float lerpColor2 = 0;
		
		ByteBuffer data;
		int numComps;
		float[] sampleTexel;
		
		// method vars
		private float uLerp, vLerp, wLerp;
		private int tx, ty, tz, ltx, lty, ltz;
		private float[][] cLU;
		private float[][] cLD;
		private float[][] cRU;
		private float[][] cRD;
		private boolean colorsReady;
		
		private int zFactor, yFactor;
		
		public ByteUnpackedSampler(Buffer data, int width, int height, int depth, int numComps) {
			super(data, width, height, depth);
			this.data = (ByteBuffer)data;
			this.numComps = numComps;
			this.sampleTexel = new float[this.numComps];
			
			this.cLD = new float[this.numComps][2];
			this.cLU = new float[this.numComps][2];
			this.cRD = new float[this.numComps][2];
			this.cRU = new float[this.numComps][2];
			
			this.zFactor = this.width * this.height * this.numComps;
			this.yFactor = this.width * this.numComps;
			this.colorsReady = false;
			
			this.tx = -1;
			this.ty = -1;
			this.tz = -1;
		}

		@Override
		public final void getTexel(float u, float v, float w) {
			this.uLerp = u * this.width;
			this.vLerp = v * this.height;
			this.wLerp = w * this.depth;
			
			if ((int)this.uLerp != this.tx) {
				this.tx = (int)this.uLerp;
				this.ltx = Math.min(this.tx + 1, this.width - 1);
				this.colorsReady = false;
			}
			if ((int)this.vLerp != this.ty) {
				this.ty = (int)this.vLerp;
				this.lty = Math.min(this.ty + 1, this.height - 1);
				this.colorsReady = false;
			}
			if ((int)this.wLerp != this.tz) {
				this.tz = (int)this.wLerp;
				this.ltz = Math.min(this.tz + 1, this.depth - 1);
				this.colorsReady = false;
			}		
			
			this.uLerp -= this.tx;
			this.vLerp -= this.ty;
			this.wLerp -= this.tz;
			
			for (int i = 0; i < this.sampleTexel.length; i++) {
				if (!this.colorsReady) {
					this.cLU[i][0] = this.getColor(this.tx, this.ty, this.tz, i);
					this.cRU[i][0] = this.getColor(this.ltx, this.ty, this.tz, i);
					this.cRD[i][0] = this.getColor(this.ltx, this.lty, this.tz, i);
					this.cLD[i][0] = this.getColor(this.tx, this.lty, this.tz, i);
					
					if (this.tz != this.ltz) {
						this.cLU[i][1] = this.getColor(this.tx, this.ty, this.ltz, i);
						this.cRU[i][1] = this.getColor(this.ltx, this.ty, this.ltz, i);
						this.cRD[i][1] = this.getColor(this.ltx, this.lty, this.ltz, i);
						this.cLD[i][1] = this.getColor(this.tx, this.lty, this.ltz, i);
					}
				}
				lerpColor1 = this.cLU[i][0] * (1 - this.uLerp) + this.cRU[i][0] * this.uLerp;
				lerpColor2 = this.cLD[i][0] * (1 - this.uLerp) + this.cRD[i][0] * this.uLerp;
				this.sampleTexel[i] = lerpColor1 * (1 - this.vLerp) + lerpColor2 * this.vLerp;
				
				if (this.tz != this.ltz) {
					// lerp in next layer, too
					lerpColor1 = this.cLU[i][1] * (1 - this.uLerp) + this.cRU[i][1] * this.uLerp;
					lerpColor2 = this.cLD[i][1] * (1 - this.uLerp) + this.cRD[i][1] * this.uLerp;
					lerpColor1 = lerpColor1 * (1 - this.vLerp) + lerpColor2 * this.vLerp;
					this.sampleTexel[i] = this.sampleTexel[i] * (1 - this.wLerp) + lerpColor1 * this.wLerp;
				}
			}
			this.colorsReady = true;
		}
		
		private final float getColor(int x, int y, int z, int i) {
			short signed = this.data.get(this.pos + i + z * this.zFactor + y * this.yFactor + x * this.numComps);
			return (signed & 0xff) / (float)converter;
		}
		
		@Override
		public final void setTexel(int x, int y, int z, Sampler inValue) {
			int offset = z * this.zFactor + y * this.yFactor + x * this.numComps;
			float[] t = ((ByteUnpackedSampler)inValue).sampleTexel;
			for (int i = 0; i < t.length; i++)
				this.data.put(this.pos + offset + i, (byte)(t[i] * (converter - 1)));
		}
	}
	
	public static class Short5551Sampler extends Sampler {
		private static final byte converter = 31;
		private static final int c1Mask = Integer.parseInt("1111100000000000", 2);
		private static final int c2Mask = Integer.parseInt("0000011111000000", 2);
		private static final int c3Mask = Integer.parseInt("0000000000111110", 2);
		private static final int c4Mask = Integer.parseInt("0000000000000001", 2);
		
		private static float lerpColor1 = 0;
		private static float lerpColor2 = 0;
		
		ShortBuffer data;
		int numComps;
		float[] sampleTexel;
		
		// method vars
		private float uLerp, vLerp, wLerp;
		private int tx, ty, tz, ltx, lty, ltz;
		private float[][] cLU;
		private float[][] cLD;
		private float[][] cRU;
		private float[][] cRD;
		private boolean colorsReady;
		
		private int zFactor, yFactor;
		
		public Short5551Sampler(Buffer data, int width, int height, int depth) {
			super(data, width, height, depth);
			this.data = (ShortBuffer)data;
			this.numComps = 4;
			this.sampleTexel = new float[this.numComps];
			
			this.cLD = new float[2][this.numComps];
			this.cLU = new float[2][this.numComps];
			this.cRD = new float[2][this.numComps];
			this.cRU = new float[2][this.numComps];
			
			this.zFactor = this.width * this.height;
			this.yFactor = this.width;
			this.colorsReady = false;
			
			this.tx = -1;
			this.ty = -1;
			this.tz = -1;
		}

		@Override
		public final void getTexel(float u, float v, float w) {
			this.uLerp = u * this.width;
			this.vLerp = v * this.height;
			this.wLerp = w * this.depth;
			
			if ((int)this.uLerp != this.tx) {
				this.tx = (int)this.uLerp;
				this.ltx = Math.min(this.tx + 1, this.width - 1);
				this.colorsReady = false;
			}
			if ((int)this.vLerp != this.ty) {
				this.ty = (int)this.vLerp;
				this.lty = Math.min(this.ty + 1, this.height - 1);
				this.colorsReady = false;
			}
			if ((int)this.wLerp != this.tz) {
				this.tz = (int)this.wLerp;
				this.ltz = Math.min(this.tz + 1, this.depth - 1);
				this.colorsReady = false;
			}		
			
			this.uLerp -= this.tx;
			this.vLerp -= this.ty;
			this.wLerp -= this.tz;
			
			if (!this.colorsReady) {
				this.getColor(this.tx, this.ty, this.tz, this.cLU[0]);
				this.getColor(this.ltx, this.ty, this.tz, this.cRU[0]);
				this.getColor(this.ltx, this.lty, this.tz, this.cRD[0]);
				this.getColor(this.tx, this.lty, this.tz, this.cLD[0]);
				
				if (this.tz != this.ltz) {
					this.getColor(this.tx, this.ty, this.ltz, this.cLU[1]);
					this.getColor(this.ltx, this.ty, this.ltz, this.cRU[1]);
					this.getColor(this.ltx, this.lty, this.ltz, this.cRD[1]);
					this.getColor(this.tx, this.lty, this.ltz, this.cLD[1]);
				}
			}
			
			for (int i = 0; i < this.sampleTexel.length; i++) {
				if (i == this.sampleTexel.length - 1) {
					this.sampleTexel[i] = (this.cLU[0][i] > 0 || this.cRU[0][i] > 0 || this.cLD[0][i] > 0 || this.cRD[0][i] > 0 ? 1f : 0f);
					if (this.tz != this.ltz) 
						this.sampleTexel[i] = (this.sampleTexel[i] > 0 || this.cLU[1][i] > 0 || this.cRU[1][i] > 0 || this.cLD[1][i] > 0 || this.cRD[1][i] > 0 ? 1f : 0f);
				} else {
					lerpColor1 = this.cLU[0][i] * (1 - this.uLerp) + this.cRU[0][i] * this.uLerp;
					lerpColor2 = this.cLD[0][i] * (1 - this.uLerp) + this.cRD[0][i] * this.uLerp;
					this.sampleTexel[i] = lerpColor1 * (1 - this.vLerp) + lerpColor2 * this.vLerp;
				
					if (this.tz != this.ltz) {
						// lerp in next layer, too
						lerpColor1 = this.cLU[1][i] * (1 - this.uLerp) + this.cRU[1][i] * this.uLerp;
						lerpColor2 = this.cLD[1][i] * (1 - this.uLerp) + this.cRD[1][i] * this.uLerp;
						lerpColor1 = lerpColor1 * (1 - this.vLerp) + lerpColor2 * this.vLerp;
						this.sampleTexel[i] = this.sampleTexel[i] * (1 - this.wLerp) + lerpColor1 * this.wLerp;
					}
				}
			}
			this.colorsReady = true;
		}
		
		private final void getColor(int x, int y, int z, float[] colorOut) {
			short signed = this.data.get(this.pos + z * this.zFactor + y * this.yFactor + x);
			colorOut[0] = ((signed & c1Mask) >> 11) / (float)converter;
			colorOut[1] = ((signed & c2Mask) >> 6) / (float)converter;
			colorOut[2] = ((signed & c3Mask) >> 1) / (float)converter;
			colorOut[3] =  (signed & c4Mask) > 0 ? 1f : 0f;
		}
		
		@Override
		public final void setTexel(int x, int y, int z, Sampler inValue) {
			float[] t = ((Short5551Sampler)inValue).sampleTexel;
			this.data.put(this.pos + z * this.zFactor + y * this.yFactor + x, 
						  (short)((((short)(t[0] * converter)) << 11) |
								 (((short)(t[1] * converter)) << 6) |
								 (((short)(t[2] * converter)) << 1) |
								 (t[3] > 0 ? (short)1 : (short)0)));
		}
	}
	public static class Short4444Sampler extends Sampler {
		private static final byte converter = 15;
		private static float lerpColor1 = 0;
		private static float lerpColor2 = 0;
		
		ShortBuffer data;
		int numComps;
		float[] sampleTexel;
		
		// method vars
		private float uLerp, vLerp, wLerp;
		private int tx, ty, tz, ltx, lty, ltz;
		private float[][] cLU;
		private float[][] cLD;
		private float[][] cRU;
		private float[][] cRD;
		private boolean colorsReady;
		
		private int zFactor, yFactor;
		
		public Short4444Sampler(Buffer data, int width, int height, int depth) {
			super(data, width, height, depth);
			this.data = (ShortBuffer)data;
			this.numComps = 4;
			this.sampleTexel = new float[this.numComps];
			
			this.cLD = new float[2][this.numComps];
			this.cLU = new float[2][this.numComps];
			this.cRD = new float[2][this.numComps];
			this.cRU = new float[2][this.numComps];
			
			this.zFactor = this.width * this.height;
			this.yFactor = this.width;
			this.colorsReady = false;
			
			this.tx = -1;
			this.ty = -1;
			this.tz = -1;
		}

		@Override
		public final void getTexel(float u, float v, float w) {
			this.uLerp = u * this.width;
			this.vLerp = v * this.height;
			this.wLerp = w * this.depth;
			
			if ((int)this.uLerp != this.tx) {
				this.tx = (int)this.uLerp;
				this.ltx = Math.min(this.tx + 1, this.width - 1);
				this.colorsReady = false;
			}
			if ((int)this.vLerp != this.ty) {
				this.ty = (int)this.vLerp;
				this.lty = Math.min(this.ty + 1, this.height - 1);
				this.colorsReady = false;
			}
			if ((int)this.wLerp != this.tz) {
				this.tz = (int)this.wLerp;
				this.ltz = Math.min(this.tz + 1, this.depth - 1);
				this.colorsReady = false;
			}		
			
			this.uLerp -= this.tx;
			this.vLerp -= this.ty;
			this.wLerp -= this.tz;
			
			if (!this.colorsReady) {
				this.getColor(this.tx, this.ty, this.tz, this.cLU[0]);
				this.getColor(this.ltx, this.ty, this.tz, this.cRU[0]);
				this.getColor(this.ltx, this.lty, this.tz, this.cRD[0]);
				this.getColor(this.tx, this.lty, this.tz, this.cLD[0]);
				
				if (this.tz != this.ltz) {
					this.getColor(this.tx, this.ty, this.ltz, this.cLU[1]);
					this.getColor(this.ltx, this.ty, this.ltz, this.cRU[1]);
					this.getColor(this.ltx, this.lty, this.ltz, this.cRD[1]);
					this.getColor(this.tx, this.lty, this.ltz, this.cLD[1]);
				}
			}
			
			for (int i = 0; i < this.sampleTexel.length; i++) {
				lerpColor1 = this.cLU[0][i] * (1 - this.uLerp) + this.cRU[0][i] * this.uLerp;
				lerpColor2 = this.cLD[0][i] * (1 - this.uLerp) + this.cRD[0][i] * this.uLerp;
				this.sampleTexel[i] = lerpColor1 * (1 - this.vLerp) + lerpColor2 * this.vLerp;
				
				if (this.tz != this.ltz) {
					// lerp in next layer, too
					lerpColor1 = this.cLU[1][i] * (1 - this.uLerp) + this.cRU[1][i] * this.uLerp;
					lerpColor2 = this.cLD[1][i] * (1 - this.uLerp) + this.cRD[1][i] * this.uLerp;
					lerpColor1 = lerpColor1 * (1 - this.vLerp) + lerpColor2 * this.vLerp;
					this.sampleTexel[i] = this.sampleTexel[i] * (1 - this.wLerp) + lerpColor1 * this.wLerp;
				}
			}
			this.colorsReady = true;
		}
		
		private final void getColor(int x, int y, int z, float[] colorOut) {
			short signed = this.data.get(this.pos + z * this.zFactor + y * this.yFactor + x);
			colorOut[0] = ((signed & 0xf000) >> 12) / (float)converter;
			colorOut[1] = ((signed & 0x0f00) >> 8) / (float)converter;
			colorOut[2] = ((signed & 0x00f0) >> 4) / (float)converter;
			colorOut[3] = ((signed & 0x000f)) / (float)converter;
		}
		
		@Override
		public final void setTexel(int x, int y, int z, Sampler inValue) {
			float[] t = ((Short4444Sampler)inValue).sampleTexel;
			this.data.put(this.pos + z * this.zFactor + y * this.yFactor + x, 
						  (short)((((short)(t[0] * converter)) << 12) |
								(((short)(t[1] * converter)) << 8) |
								(((short)(t[2] * converter)) << 4) |
								((short)(t[3] * converter))));
		}
	}
	public static class Short565Sampler extends Sampler {
		private static final byte converter13 = 31;
		private static final byte converter2 = 63;
		private static final int c1Mask = Integer.parseInt("1111100000000000", 2);
		private static final int c2Mask = Integer.parseInt("0000011111100000", 2);
		private static final int c3Mask = Integer.parseInt("0000000000011111", 2);
		
		private static float lerpColor1 = 0;
		private static float lerpColor2 = 0;
		
		ShortBuffer data;
		int numComps;
		float[] sampleTexel;
		
		// method vars
		private float uLerp, vLerp, wLerp;
		private int tx, ty, tz, ltx, lty, ltz;
		private float[][] cLU;
		private float[][] cLD;
		private float[][] cRU;
		private float[][] cRD;
		private boolean colorsReady;
		
		private int zFactor, yFactor;
		
		public Short565Sampler(Buffer data, int width, int height, int depth) {
			super(data, width, height, depth);
			this.data = (ShortBuffer)data;
			this.numComps = 3;
			this.sampleTexel = new float[this.numComps];
			
			this.cLD = new float[2][this.numComps];
			this.cLU = new float[2][this.numComps];
			this.cRD = new float[2][this.numComps];
			this.cRU = new float[2][this.numComps];
			
			this.zFactor = this.width * this.height;
			this.yFactor = this.width;
			this.colorsReady = false;
			
			this.tx = -1;
			this.ty = -1;
			this.tz = -1;
		}

		@Override
		public final void getTexel(float u, float v, float w) {
			this.uLerp = u * this.width;
			this.vLerp = v * this.height;
			this.wLerp = w * this.depth;
			
			if ((int)this.uLerp != this.tx) {
				this.tx = (int)this.uLerp;
				this.ltx = Math.min(this.tx + 1, this.width - 1);
				this.colorsReady = false;
			}
			if ((int)this.vLerp != this.ty) {
				this.ty = (int)this.vLerp;
				this.lty = Math.min(this.ty + 1, this.height - 1);
				this.colorsReady = false;
			}
			if ((int)this.wLerp != this.tz) {
				this.tz = (int)this.wLerp;
				this.ltz = Math.min(this.tz + 1, this.depth - 1);
				this.colorsReady = false;
			}		
			
			this.uLerp -= this.tx;
			this.vLerp -= this.ty;
			this.wLerp -= this.tz;
			
			if (!this.colorsReady) {
				this.getColor(this.tx, this.ty, this.tz, this.cLU[0]);
				this.getColor(this.ltx, this.ty, this.tz, this.cRU[0]);
				this.getColor(this.ltx, this.lty, this.tz, this.cRD[0]);
				this.getColor(this.tx, this.lty, this.tz, this.cLD[0]);
				
				if (this.tz != this.ltz) {
					this.getColor(this.tx, this.ty, this.ltz, this.cLU[1]);
					this.getColor(this.ltx, this.ty, this.ltz, this.cRU[1]);
					this.getColor(this.ltx, this.lty, this.ltz, this.cRD[1]);
					this.getColor(this.tx, this.lty, this.ltz, this.cLD[1]);
				}
			}
			
			for (int i = 0; i < this.sampleTexel.length; i++) {
				lerpColor1 = this.cLU[0][i] * (1 - this.uLerp) + this.cRU[0][i] * this.uLerp;
				lerpColor2 = this.cLD[0][i] * (1 - this.uLerp) + this.cRD[0][i] * this.uLerp;
				this.sampleTexel[i] = lerpColor1 * (1 - this.vLerp) + lerpColor2 * this.vLerp;
					
				if (this.tz != this.ltz) {
					// lerp in next layer, too
					lerpColor1 = this.cLU[1][i] * (1 - this.uLerp) + this.cRU[1][i] * this.uLerp;
					lerpColor2 = this.cLD[1][i] * (1 - this.uLerp) + this.cRD[1][i] * this.uLerp;
					lerpColor1 = lerpColor1 * (1 - this.vLerp) + lerpColor2 * this.vLerp;
					this.sampleTexel[i] = this.sampleTexel[i] * (1 - this.wLerp) + lerpColor1 * this.wLerp;
				}
			}
			this.colorsReady = true;
		}
		
		private final void getColor(int x, int y, int z, float[] colorOut) {
			short signed = this.data.get(this.pos + z * this.zFactor + y * this.yFactor + x);
			colorOut[0] = ((signed & c1Mask) >> 11) / (float)converter13;
			colorOut[1] = ((signed & c2Mask) >> 5) / (float)converter2;
			colorOut[2] = ((signed & c3Mask)) / (float)converter13;
		}
		
		@Override
		public final void setTexel(int x, int y, int z, Sampler inValue) {
			float[] t = ((Short565Sampler)inValue).sampleTexel;
			this.data.put(this.pos + z * this.zFactor + y * this.yFactor + x, 
						  (short)((((short)(t[0] * converter13)) << 11) |
								 (((short)(t[1] * converter2)) << 5) |
								 (((short)(t[2] * converter13)))));
		}
	}
	
	public static class Int8888Sampler extends Sampler {
		private static final short converter = Byte.MAX_VALUE - Byte.MIN_VALUE;
		private static float lerpColor1 = 0;
		private static float lerpColor2 = 0;
		
		IntBuffer data;
		int numComps;
		float[] sampleTexel;
		
		// method vars
		private float uLerp, vLerp, wLerp;
		private int tx, ty, tz, ltx, lty, ltz;
		private float[][] cLU;
		private float[][] cLD;
		private float[][] cRU;
		private float[][] cRD;
		private boolean colorsReady;
		
		private int zFactor, yFactor;
		
		public Int8888Sampler(Buffer data, int width, int height, int depth) {
			super(data, width, height, depth);
			this.data = (IntBuffer)data;
			this.numComps = 4;
			this.sampleTexel = new float[this.numComps];
			
			this.cLD = new float[2][this.numComps];
			this.cLU = new float[2][this.numComps];
			this.cRD = new float[2][this.numComps];
			this.cRU = new float[2][this.numComps];
			
			this.zFactor = this.width * this.height;
			this.yFactor = this.width;
			this.colorsReady = false;
			
			this.tx = -1;
			this.ty = -1;
			this.tz = -1;
		}

		@Override
		public final void getTexel(float u, float v, float w) {
			this.uLerp = u * this.width;
			this.vLerp = v * this.height;
			this.wLerp = w * this.depth;
			
			if ((int)this.uLerp != this.tx) {
				this.tx = (int)this.uLerp;
				this.ltx = Math.min(this.tx + 1, this.width - 1);
				this.colorsReady = false;
			}
			if ((int)this.vLerp != this.ty) {
				this.ty = (int)this.vLerp;
				this.lty = Math.min(this.ty + 1, this.height - 1);
				this.colorsReady = false;
			}
			if ((int)this.wLerp != this.tz) {
				this.tz = (int)this.wLerp;
				this.ltz = Math.min(this.tz + 1, this.depth - 1);
				this.colorsReady = false;
			}		
			
			this.uLerp -= this.tx;
			this.vLerp -= this.ty;
			this.wLerp -= this.tz;
			
			if (!this.colorsReady) {
				this.getColor(this.tx, this.ty, this.tz, this.cLU[0]);
				this.getColor(this.ltx, this.ty, this.tz, this.cRU[0]);
				this.getColor(this.ltx, this.lty, this.tz, this.cRD[0]);
				this.getColor(this.tx, this.lty, this.tz, this.cLD[0]);
				
				if (this.tz != this.ltz) {
					this.getColor(this.tx, this.ty, this.ltz, this.cLU[1]);
					this.getColor(this.ltx, this.ty, this.ltz, this.cRU[1]);
					this.getColor(this.ltx, this.lty, this.ltz, this.cRD[1]);
					this.getColor(this.tx, this.lty, this.ltz, this.cLD[1]);
				}
			}
			
			for (int i = 0; i < this.sampleTexel.length; i++) {
				lerpColor1 = this.cLU[0][i] * (1 - this.uLerp) + this.cRU[0][i] * this.uLerp;
				lerpColor2 = this.cLD[0][i] * (1 - this.uLerp) + this.cRD[0][i] * this.uLerp;
				this.sampleTexel[i] = lerpColor1 * (1 - this.vLerp) + lerpColor2 * this.vLerp;
				
				if (this.tz != this.ltz) {
					// lerp in next layer, too
					lerpColor1 = this.cLU[1][i] * (1 - this.uLerp) + this.cRU[1][i] * this.uLerp;
					lerpColor2 = this.cLD[1][i] * (1 - this.uLerp) + this.cRD[1][i] * this.uLerp;
					lerpColor1 = lerpColor1 * (1 - this.vLerp) + lerpColor2 * this.vLerp;
					this.sampleTexel[i] = this.sampleTexel[i] * (1 - this.wLerp) + lerpColor1 * this.wLerp;
				}
			}
			this.colorsReady = true;
		}
		
		private final void getColor(int x, int y, int z, float[] colorOut) {
			int signed = this.data.get(this.pos + z * this.zFactor + y * this.yFactor + x);
			colorOut[0] = ((signed & 0xff000000L) >> 24) / (float)converter;
			colorOut[1] = ((signed & 0x00ff0000) >> 16) / (float)converter;
			colorOut[2] = ((signed & 0x0000ff00) >> 8) / (float)converter;
			colorOut[3] = ((signed & 0x000000ff)) / (float)converter;
		}
		
		@Override
		public final void setTexel(int x, int y, int z, Sampler inValue) {
			float[] t = ((Int8888Sampler)inValue).sampleTexel;
			this.data.put(this.pos + z * this.zFactor + y * this.yFactor + x, 
						  ((((int)(t[0] * converter)) << 24) |
								(((int)(t[1] * converter)) << 16) |
								(((int)(t[2] * converter)) << 8) |
								((int)(t[3] * converter))));
		}
	}
}
