package com.ferox.impl.jsr231.peers;

import com.ferox.core.states.FragmentTest;
import com.ferox.core.states.StateAtom.StateRecord;
import com.ferox.core.states.atoms.TextureData.*;

class TextureRecord extends StateRecord {
	int texID;
	int validFrame;
	int target; // gl int
	
	// dst format should be updated with actual format after texture image creation
	// src format < 0 if src data is already compressed (use dstFormat instead)
	int srcFormat; // gl int
	int dstFormat; // gl int
	int dataType; // gl int
	
	// texture parameters (not GL ints)
	MinFilter minFilter;
	MagFilter magFilter;
	DepthMode depthMode;
	DepthCompare compareMode;
	FragmentTest compareFunc;
	TexClamp wrapR;
	TexClamp wrapT;
	TexClamp wrapS;
	float aniso;
}
