package com.paymentsplatform.paymentscore.internal.domain.exceptions;

/**
 * Thrown when a Money value object is constructed with invalid arguments,
 * or when a monetary operation is attempted across mismatched currencies.
 */
public class InvalidMoneyException extends RuntimeException {

    public InvalidMoneyException(String message) {
        super(message);
    }
}
