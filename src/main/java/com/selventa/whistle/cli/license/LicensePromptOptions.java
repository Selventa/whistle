package com.selventa.whistle.cli.license;

import static org.openbel.framework.common.BELUtilities.noLength;

import org.openbel.framework.common.InvalidArgument;

/**
 * LicenseOptions defines prompt options for a {@link LicenseAgreement}.
 *
 * <p>
 * The user must provide the {@link String prompt statement} and the
 * corresponding {@link String accept} and {@link String reject} responses.
 * </p>
 *
 * @author Anthony Bargnesi
 */
public class LicensePromptOptions {
    private final String prompt;
    private final String acceptResponse;
    private final String rejectResponse;

    /**
     * Create the {@link LicensePromptOptions} with the
     * {@link String prompt statement} and the corresponding
     * {@link String accept} and {@link String reject} respondes.
     *
     * @param prompt the {@link String prompt} which cannot be {@code null}
     * @param acceptResponse the {@link String accept response} which cannot be
     * {@code null} or empty
     * @param rejectResponse the {@link String reject response} which cannot be
     * {@code null} or empty
     */
    public LicensePromptOptions(final String prompt, final String acceptResponse,
            final String rejectResponse) {
        if (prompt == null) {
            throw new InvalidArgument("prompt", prompt);
        }

        if (noLength(acceptResponse)) {
            throw new InvalidArgument("acceptResponse is null or empty");
        }

        if (noLength(rejectResponse)) {
            throw new InvalidArgument("rejectResponse is null or empty");
        }

        this.prompt = prompt;
        this.acceptResponse = acceptResponse;
        this.rejectResponse = rejectResponse;
    }

    /**
     * Return the license prompt.
     *
     * @return the {@link String prompt} which will not be {@code null}
     */
    public String getPrompt() {
        return prompt;
    }

    /**
     * Return the license accept response.
     *
     * @return the {@link String license accept response} which will not be
     * {@code null} or empty
     */
    public String getAcceptResponse() {
        return acceptResponse;
    }

    /**
     * Return the license reject response.
     *
     * @return the {@link String license reject response} which will not be
     * {@code null} or empty
     */
    public String getRejectResponse() {
        return rejectResponse;
    }
}
