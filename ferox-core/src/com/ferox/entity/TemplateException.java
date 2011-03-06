package com.ferox.entity;

/**
 * TemplateExceptions are thrown when a Template is unable to create a new
 * Entity from its current configuration. The most common cause of this is if
 * the Components in the Template cannot be cloned, or if the ComponentProviders
 * throw exceptions.
 * 
 * @author Michael Ludwig
 */
public class TemplateException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public TemplateException() {
        super();
    }

    public TemplateException(String message) {
        super(message);
    }

    public TemplateException(Throwable cause) {
        super(cause);
    }

    public TemplateException(String message, Throwable cause) {
        super(message, cause);
    }
}
