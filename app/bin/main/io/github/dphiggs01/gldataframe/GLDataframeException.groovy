package io.github.dphiggs01.gldataframe

/**
 * Custom exception class for GLDataframe-related exceptions.
 * Extends RuntimeException for unchecked exception behavior.
 */
class GLDataframeException extends RuntimeException {

    /**
     * Constructs a GLDataframeException with the specified error message.
     *
     * @param message The error message describing the exception.
     */
    GLDataframeException(String message) {
        super(message)
    }

    /**
     * Constructs a GLDataframeException with the specified error message and a cause.
     *
     * @param message The error message describing the exception.
     * @param cause   The cause of the exception, which may be null.
     */
    GLDataframeException(String message, Throwable cause) {
        super(message, cause)
    }
}
