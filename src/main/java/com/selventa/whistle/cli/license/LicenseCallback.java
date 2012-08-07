package com.selventa.whistle.cli.license;

/**
 * LicenseCallback provides callbacks for license acceptance and rejection.
 *
 * @author Anthony Bargnesi
 */
public interface LicenseCallback {

    /**
     * Callback indicating that a {@link LicenseAgreement license agreement}
     * was accepted.
     */
    public void onAcceptance();

    /**
     * Callback indicating that a {@link LicenseAgreement license agreement}
     * was rejected.
     */
    public void onRejection();
}
