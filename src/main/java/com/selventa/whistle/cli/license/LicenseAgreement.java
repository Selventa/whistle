package com.selventa.whistle.cli.license;

import static java.lang.String.format;
import static java.lang.System.in;
import static java.lang.System.out;
import static org.openbel.framework.common.BELUtilities.asPath;
import static org.openbel.framework.common.BELUtilities.noLength;
import static org.openbel.framework.common.BELUtilities.readable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.openbel.framework.common.InvalidArgument;

/**
 * LicenseAgreement provides a license prompt mechanism for a user of an
 * application.
 *
 * <p>
 * The license agreement captured in {@link String license} is presented to the
 * user of the {@link String applicationName}. The license prompts are
 * controlled by the {@link LicensePromptOptions options} bean.
 * </p>
 *
 * <p>
 * The user is prompted with the {@link String license} and must accept it to
 * use the {@link String application}. When the user accepts the license the
 * {@link LicenseCallback#onAcceptance() acceptance callback method} is called.
 * If the user rejects the license then the
 * {@link LicenseCallback#onRejection() rejection callback method} is called.
 * This allows the caller to respond to the user's action in an
 * application-specific manner.
 * </p>
 *
 * <p>
 * The {@link String license} content can come from a {@link String},
 * {@link InputStream}, or {@link File}.
 * </p>
 *
 * @author Anthony Bargnesi
 */
public class LicenseAgreement {

    private static final String TERMINATOR = System
            .getProperty("line.separator");
    private static final String USER_HOME = System.getProperty("user.home");
    private static final String PROMPT_MARGIN = " ";
    private static final String RESPONSE_FMT = "[ %s/%s ]";
    private static final String LIC_FILE_FMT = ".%s.license";

    /**
     * Indicates the user was prompted.  Value: {@value}
     */
    private static final boolean PROMPTED = true;
    /**
     * Indicates the user was not prompted.  Value: {@value}
     */
    private static final boolean NOT_PROMPTED = true;

    private final String application;
    private final String license;
    private final LicensePromptOptions options;
    private final LicenseCallback callback;

    /**
     * Create the {@link LicenseAgreement} from a {@link String} license.
     *
     * @param application the {@link String application name} which cannot be
     * {@code null}
     * @param license the {@link String license} text which cannot be
     * {@code null} or empty
     * @param options the license {@link LicensePromptOptions options} which cannot
     * be {@code null}
     * @param callback the license {@link LicenseCallback callback} which
     * cannot be {@code null}
     * @throws InvalidArgument Thrown if the parameters are invalid as
     * described
     */
    public LicenseAgreement(final String application, final String license,
            final LicensePromptOptions options,
            final LicenseCallback callback) {
        if (noLength(application)) {
            throw new InvalidArgument("applicationName is null or empty");
        }

        if (noLength(license)) {
            throw new InvalidArgument("license is null or empty");
        }

        if (options == null) {
            throw new InvalidArgument("options in null");
        }

        if (callback == null) {
            throw new InvalidArgument("callback is null");
        }

        this.application = application;
        this.license = license;
        this.options = options;
        this.callback = callback;
    }

    /**
     * Create the {@link LicenseAgreement} from a {@link InputStream} license.
     *
     * @param application the {@link String application name} which cannot be
     * {@code null}
     * @param licenseStream the {@link InputStream license stream} which cannot
     * be {@code null}
     * @param options the license {@link LicensePromptOptions options} which cannot
     * be {@code null}
     * @param callback the license {@link LicenseCallback callback} which
     * cannot be {@code null}
     * @throws InvalidArgument Thrown if the parameters are invalid as
     * described
     * @throws IOException Thrown if an IO error occurred reading from the
     * {@link InputStream stream}
     */
    public LicenseAgreement(final String application,
            final InputStream licenseStream,
            final LicensePromptOptions options, final LicenseCallback callback)
            throws IOException {
        this(application, read(licenseStream), options, callback);
    }

    /**
     * Create the {@link LicenseAgreement} from a {@link File} license.
     *
     * @param application the {@link String application name} which cannot be
     * {@code null}
     * @param licenseFile the {@link File license file} which cannot
     * be {@code null}, must exist, and must be readable
     * @param options the license {@link LicensePromptOptions options} which cannot
     * be {@code null}
     * @param callback the license {@link LicenseCallback callback} which
     * cannot be {@code null}
     * @throws InvalidArgument Thrown if the parameters are invalid as
     * described
     * @throws IOException Thrown if an IO error occurred reading from the
     * {@link File file}
     */
    public LicenseAgreement(final String application,
            final File licenseFile,
            final LicensePromptOptions options, final LicenseCallback callback)
            throws IOException {
        this(application, read(licenseFile), options, callback);
    }

    /**
     * Prompt the user with the {@code license} followed by the prompt as
     * captured by the {@link LicensePromptOptions options}.
     *
     * <p>
     * If the {@link File license marker file} exists in
     * {@code $HOME/.application.license} then the user will not be prompted
     * and {@link LicenseAgreement#NOT_PROMPTED} will be returned.  The
     * {@link LicenseCallback#onAcceptance() acceptance callback} is still
     * called for further application-specific actions.
     * </p>
     *
     * <p>
     * The user sees the license and prompt if the
     * {@link File license marker file} is not present.  If the user provides
     * the {@link LicensePromptOptions#getAcceptResponse() accept response} then the
     * {@link File license marker file} is created at
     * {@code $HOME/.application.license} and the
     * {@link LicenseCallback#onAcceptance() acceptance callback} is called.
     * The {@link LicenseCallback#onRejection() rejection callback} is called
     * if the response corresponds to
     * {@link LicensePromptOptions#getRejectResponse()}.  The prompt is printed until
     * an acceptance or rejection response if supplied.
     * </p>
     *
     * @return {@code true} if the user was
     * {@link LicenseAgreement#PROMPTED prompted},
     * {@code false} if the user was
     * {@link LicenseAgreement#NOT_PROMPTED not prompted}
     * @throws IOException Thrown if an IO error occurred reading from
     * {@link System#in} or writing the license marker file to {@code $HOME}
     */
    public boolean promptLicense() throws IOException {
        final File licenseMarker = new File(asPath(USER_HOME,
                format(LIC_FILE_FMT, application.toLowerCase())));
        if (readable(licenseMarker)) {
            callback.onAcceptance();
            return NOT_PROMPTED;
        }

        out.println(license);

        boolean responded = false;
        while (!responded) {
            out.print(options.getPrompt()
                    + PROMPT_MARGIN
                    + format(RESPONSE_FMT,
                            options.getAcceptResponse(),
                            options.getRejectResponse())
                    + PROMPT_MARGIN);

            final BufferedReader r = new BufferedReader(new InputStreamReader(in));
            final String response = r.readLine().trim();

            if (response.equalsIgnoreCase(options.getAcceptResponse())) {
                responded = true;
                licenseMarker.createNewFile();
                callback.onAcceptance();
            } else if (response.equalsIgnoreCase(options.getRejectResponse())) {
                responded = true;
                callback.onRejection();
            } else {
                out.println("Did not recognize response, try again.");
                out.println();
            }
        }

        return PROMPTED;
    }

    private static String read(final InputStream licenseStream)
            throws IOException {
        if (licenseStream == null) {
            throw new InvalidArgument("licenseStream", licenseStream);
        }

        BufferedReader r = null;
        final StringBuilder b = new StringBuilder();
        try {
            r = new BufferedReader(new InputStreamReader(licenseStream));
            String line = "";
            while ((line = r.readLine()) != null) {
                b.append(line + TERMINATOR);
            }
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (Exception e) {/*silent*/}
            }
        }

        return b.toString();
    }

    private static String read(final File licenseFile) throws IOException {
        if (licenseFile == null) {
            throw new InvalidArgument("licenseFile", licenseFile);
        }

        if (!(readable(licenseFile))) {
            throw new InvalidArgument(format(
                    "licenseFile cannot be read from %s",
                    licenseFile.getAbsolutePath()));
        }

        BufferedReader r = null;
        final StringBuilder b = new StringBuilder();
        try {
            r = new BufferedReader(new FileReader(licenseFile));
            String line = "";
            while ((line = r.readLine()) != null) {
                b.append(line + TERMINATOR);
            }
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (Exception e) {/*silent*/}
            }
        }

        return b.toString();
    }
}
