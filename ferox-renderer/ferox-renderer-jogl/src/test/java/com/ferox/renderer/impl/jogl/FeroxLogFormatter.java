package com.ferox.renderer.impl.jogl;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class FeroxLogFormatter extends Formatter {
    @Override
    public String format(LogRecord record) {
        return String.format("%S %tT - Thread %s: %s\n", record.getLevel(), record.getMillis(), 
                             record.getThreadID(), record.getMessage());
    }
}
