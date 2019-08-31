package net.tiny.ws.rs;

import java.net.HttpURLConnection;

public class ApplicationException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private final int status;

    /**
     * Construct a new instance with the supplied message, root cause and response.
     *
     * @param message  the detail message (which is saved for later retrieval
     *                 by the {@link #getMessage()} method).
     * @param status   the HTTP status code that will be returned to the client.
     *                 a value of null will be replaced with an internal server error
     *                 response (status code 500).
     * @param cause    the underlying cause of the exception.
     */
    public ApplicationException(final String message, final Throwable cause, final int status) {
        super(message, cause);
        this.status = status;
    }

    /**
     * Construct a new instance with the supplied root cause, HTTP status code
     * and a default message generated from the HTTP status code and the associated HTTP status reason phrase.
     *
     * @param status the HTTP status code that will be returned to the client.
     * @param cause  the underlying cause of the exception.
     */
    public ApplicationException(final Throwable cause, final int status) {
        this(getMessage(status), cause, status);
    }
    /**
     * Construct a new instance with the supplied HTTP status code
     * and a default message generated from the HTTP status code and the associated HTTP status reason phrase.
     *
     * @param status the HTTP status code that will be returned to the client.
     */
    public ApplicationException(final int status) {
        this(getMessage(status), (Throwable) null, status);
    }

    /**
     * Construct a new instance with a supplied message and HTTP status code.
     *
     * @param message the detail message (which is saved for later retrieval
     *                by the {@link #getMessage()} method).
     * @param status  the HTTP status code that will be returned to the client.
     */
    public ApplicationException(final String message, final int status) {
        this(message, null, status);
    }

    public int getStatus() {
        return status;
    }

    static String getMessage(int code) {

        String message = "";
        switch (code) {
        case HttpURLConnection.HTTP_BAD_REQUEST:
            message = "Bad Request";
            break;
        case HttpURLConnection.HTTP_UNAUTHORIZED:
            message = "Unauthorized";
            break;
        case HttpURLConnection.HTTP_PAYMENT_REQUIRED:
            message = "Payment Required";
            break;
        case HttpURLConnection.HTTP_FORBIDDEN:
            message = "Forbidden";
            break;
        case HttpURLConnection.HTTP_NOT_FOUND:
            message = "Not found";
            break;
        case HttpURLConnection.HTTP_BAD_METHOD:
            message = "Method Not Allowed";
            break;
        case HttpURLConnection.HTTP_NOT_ACCEPTABLE:
            message = "Not Acceptable";
            break;
        case HttpURLConnection.HTTP_PROXY_AUTH:
            message = "Proxy Authentication Required";
            break;
        case HttpURLConnection.HTTP_CLIENT_TIMEOUT:
            message = "Request Time-Out";
            break;
        case HttpURLConnection.HTTP_CONFLICT:
            message = "Conflict";
            break;
        case HttpURLConnection.HTTP_GONE:
            message = "Gone";
            break;
        case HttpURLConnection.HTTP_LENGTH_REQUIRED:
            message = "Length Required";
            break;
        case HttpURLConnection.HTTP_PRECON_FAILED:
            message = "Precondition Failed";
            break;
        case HttpURLConnection.HTTP_ENTITY_TOO_LARGE:
            message = "Request Entity Too Large";
            break;
        case HttpURLConnection.HTTP_REQ_TOO_LONG:
            message = "Request-URI Too Large";
            break;
        case HttpURLConnection.HTTP_UNSUPPORTED_TYPE:
            message = "Unsupported Media Type";
            break;
        case HttpURLConnection.HTTP_NOT_IMPLEMENTED:
            message = "Not Implemented";
            break;
        case HttpURLConnection.HTTP_BAD_GATEWAY:
            message = "Bad Gateway";
            break;
        case HttpURLConnection.HTTP_UNAVAILABLE:
            message = "Service Unavailable";
            break;
        case HttpURLConnection.HTTP_GATEWAY_TIMEOUT:
            message = "Gateway Timeout";
            break;
        case HttpURLConnection.HTTP_VERSION:
            message = "HTTP Version Not Supported";
            break;
        case HttpURLConnection.HTTP_INTERNAL_ERROR:
        default:
            message = "Internal error";
            break;
        }
        return message;
    }
}
