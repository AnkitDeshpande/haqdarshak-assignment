package org.haqdarshak.publishing.exception;

/**
 * Thrown when a user who created or modified a draft version attempts to approve or publish it.
 * Strictly implements the Maker-Checker rule required by the spec.
 */
public class MakerCheckerViolationException extends SecurityException {

    public MakerCheckerViolationException(String message) {
        super(message);
    }
}
