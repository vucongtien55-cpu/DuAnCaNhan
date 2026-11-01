package com.lms.exception;

import java.util.Set;

/** Client yêu cầu sắp xếp theo trường không nằm trong danh sách cho phép của API. */
public class InvalidSortException extends RuntimeException {

    private final String property;
    private final Set<String> allowed;

    public InvalidSortException(String property, Set<String> allowed) {
        super("Không được sắp xếp theo '" + property + "'");
        this.property = property;
        this.allowed = allowed;
    }

    public String getProperty() {
        return property;
    }

    public Set<String> getAllowed() {
        return allowed;
    }
}
