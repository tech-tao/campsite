package com.techtao.campsite.domain.exception;

/**
 * This class represents the exceptions when the reservation date range is not available with formatted messages.
 *
 * @author rantao
 */
public class NotAvailableException extends Exception {

    private String errorMessage;
    private final static String ERROR_MESSAGE = "The given dates are unavailable:";

    public NotAvailableException(String errorMessage) {
        this.errorMessage = new StringBuilder().append(ERROR_MESSAGE).append(errorMessage).toString();
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }

}
