package com.ferox.core.util;

public class TimeSmoother {
	protected static final long FRAME_RATE_RECALC_PERIOD=500;
	protected static final long MAX_ELAPSED_TIME=100;
	protected static final long AVERAGE_PERIOD=100;
	
	protected static final int NUM_SAMPLES_BITS=6;
	protected static final int NUM_SAMPLES=1<<NUM_SAMPLES_BITS;
	protected static final int NUM_SAMPLES_MASK=NUM_SAMPLES-1;
	
	protected long[] samples;
	protected int numSamples=0;
	protected int firstIndex=0;
	
	protected int numFrames=0;
	protected long startTime;
	protected float frameRate;
	
	public TimeSmoother() {
		samples=new long[NUM_SAMPLES];
	}
	
	public long getTime(long elapsedTime) {
		addSample(elapsedTime);
		return getAverage();
	}
	
	public void addSample(long elapsedTime) {
		numFrames++;
		elapsedTime=Math.min(elapsedTime,MAX_ELAPSED_TIME);
		samples[(firstIndex+numSamples)&NUM_SAMPLES_MASK]=elapsedTime;
		if (numSamples==samples.length) {
			firstIndex=(firstIndex+1)&NUM_SAMPLES_MASK;
		} else {
			numSamples++;
		}
	}
	
	public long getAverage() {
		long sum=0;
		for (int i=numSamples-1;i>=0;i--) {
			sum+=samples[(firstIndex+i)&NUM_SAMPLES_MASK];
			
			if (sum>=AVERAGE_PERIOD) {
				return Math.round((double)sum/(numSamples-i));
			}
		}
		return Math.round((double)sum/numSamples);
	}
	
	public float getFrameRate() {
		long currTime=System.currentTimeMillis();
		
		if (currTime>startTime+FRAME_RATE_RECALC_PERIOD) {
			frameRate=(float)numFrames*1000/(currTime-startTime);
			startTime=currTime;
			numFrames=0;
		}
		return frameRate;
	}
}
