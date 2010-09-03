package com.ferox.input;

public class KeyEvent implements Event {
    public static enum Type {
        PRESS, RELEASE
    }

    /**
     * The special character meaning that no character was translated for the
     * event.
     */
    public static final int CHAR_UNKNOWN = '\0';

    public static enum KeyCode {
        UNKNOWN("Unknown"), 
        
        ESCAPE("Esc"), 
        BACK_QUOTE("`"), 
        TAB("Tab"), 
        LEFT_BRACKET("["), 
        RIGHT_BRACKET("]"),
        BACK_SLASH("\\"), 
        FORWARD_SLASH("/"), 
        RETURN("Return"), 
        SEMICOLON(";"), 
        QUOTE("'"), 
        COMMA(","), 
        PERIOD("."), 
        MINUS("-"), 
        EQUALS("="), 
        BACK_SPACE("Back Space"),
        DELETE("Delete"), 
        SPACE("Space"),
        INSERT("Insert"), 
        PAUSE("Pause"), 
        HOME("Home"), 
        END("End"), 
        PAGE_UP("Page Up"), 
        PAGE_DOWN("Page Down"),
        
        UP("Up"), LEFT("Left"), RIGHT("Right"), DOWN("Down"), 
        
        F1("F1"), F2("F2"), F3("F3"), F4("F4"), F5("F5"), F6("F6"), F7("F7"), F8("F8"), F9("F9"), F10("F10"), F11("F11"), F12("F12"), 
        
        N1("1"), N2("2"), N3("3"), N4("4"), N5("5"), N6("6"), N7("7"), N8("8"), N9("9"), N0("0"), 
        
        A("a"), B("b"), C("c"), D("d"), E("e"), F("f"), G("g"), H("h"), I("i"), J("j"), K("k"), L("l"), M("m"), 
        N("n"), O("o"), P("p"), Q("q"), R("r"), S("s"), T("t"), U("u"), V("v"), W("w"), X("x"), Y("y"), Z("z"), 
        
        NUM_LOCK("Num Lock"), SCROLL_LOCK("Scroll Lock"), CAPS_LOCK("Caps Lock"), 
        
        NUMPAD_0("Numpad 0"), NUMPAD_1("Numpad 1"), NUMPAD_2("Numpad 2"), NUMPAD_3("Numpad 3"), NUMPAD_4("Numpad 4"), 
        NUMPAD_5("Numpad 5"), NUMPAD_6("Numpad 6"), NUMPAD_7("Numpad 7"), NUMPAD_8("Numpad 8"), NUMPAD_9("Numpad 9"), 
        NUMPAD_ADD("Numpad +"), NUMPAD_SUBTRACT("Numpad -"), NUMPAD_DECIMAL("Numpad ."), NUMPAD_DIVIDE("Numpad /"), NUMPAD_MULTIPLY("NUMPAD *"), 
        
        RIGHT_ALT("Right Alt"), LEFT_ALT("Left Alt"),
        RIGHT_CONTROL("Right Control"), LEFT_CONTROL("Left Control"), 
        RIGHT_SHIFT("Right Shift"), LEFT_SHIFT("Left Shift"),
        RIGHT_META("Right Meta"), LEFT_META("Left Meta");
        
        private final String descr;
        private KeyCode(String descr) { this.descr = descr; }
        
        public String getDescription() { return descr; }
    }
    
    private final KeyCode keyCode;
    private final char charValue;
    private final Type type;
    private final KeyEventSource source;
    
    public KeyEvent(Type type, KeyEventSource source, KeyCode keyCode, char charValue) {
        if (source == null)
            throw new NullPointerException("Event source cannot be null");
        if (type == null)
            throw new NullPointerException("Event type cannot be null");
        if (keyCode == null)
            throw new NullPointerException("KeyCode cannot be null");
        
        this.source = source;
        this.type = type;
        this.keyCode = keyCode;
        this.charValue = charValue;
    }
    
    public char getCharacter() {
        return charValue;
    }
    
    public KeyCode getKeyCode() {
        return keyCode;
    }
    
    public Type getEventType() {
        return type;
    }
    
    @Override
    public KeyEventSource getSource() {
        return source;
    }
    
    @Override
    public String toString() {
        return "[Key " + type + " on " + keyCode.getDescription() + "]";
    }
}
