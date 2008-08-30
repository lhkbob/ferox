package com.ferox.impl.jsr231.peers;

import com.ferox.core.states.StateAtom.StateRecord;

class GLSLObjectRecord extends StateRecord {
	int id;
	int type;
	String infoLog;
	boolean compiled;
}
