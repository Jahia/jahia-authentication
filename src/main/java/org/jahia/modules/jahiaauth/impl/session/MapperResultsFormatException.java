package org.jahia.modules.jahiaauth.impl.session;

/**
 * Raised when the mapper results of a session cannot be read.
 * <p>
 * The document is written by this module and read by it, so a document it cannot read means the
 * session carries something this version did not produce. A reader refuses the sign-in that depends
 * on it, and a writer refuses to replace it.
 */
public class MapperResultsFormatException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public MapperResultsFormatException(String message) {
        super(message);
    }

    public MapperResultsFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
