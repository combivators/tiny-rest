package net.tiny.ws.mvc;

import java.io.Serializable;

public class Violation implements Serializable {

    private static final long serialVersionUID = 1L;
    private String property;
    private String message;

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(property);
        sb.append(": '");
        sb.append(message);
        sb.append("'");
        return sb.toString();
    }
}
