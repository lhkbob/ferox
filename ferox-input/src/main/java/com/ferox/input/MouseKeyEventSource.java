package com.ferox.input;

/**
 * MouseKeyEventSource is a combined interface for both MouseEvents and
 * KeyEvents. When both types of events are required, or produced from a source,
 * this interface should be used.
 * 
 * @author Michael Ludwig
 */
public interface MouseKeyEventSource extends MouseEventSource, KeyEventSource {

}
