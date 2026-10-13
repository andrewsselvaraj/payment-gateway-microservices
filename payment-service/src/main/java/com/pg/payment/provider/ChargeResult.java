package com.pg.payment.provider;

public record ChargeResult(boolean success, String providerReference, String failureReason) {

    public static ChargeResult success(String providerReference) {
        return new ChargeResult(true, providerReference, null);
    }

    public static ChargeResult failure(String reason) {
        return new ChargeResult(false, null, reason);
    }
}
