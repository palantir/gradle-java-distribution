package com.palantir.slspackaging.versions;

import java.util.regex.Pattern;

/**
 * Utility methods for checking whether version strings are valid SLS version strings,
 * c.f. https://github.com/palantir/sls-spec/
 */
public class SlsProductVersions {
    private static final Pattern NON_ORDERABLE_VERSION = Pattern.compile("^[0-9]+\\.[0-9]+\\.[0-9]+-[a-z0-9-.]*$");
    private static final Pattern[] ORDERABLE_VERSION = new Pattern[]{
            Pattern.compile("^[0-9]+\\.[0-9]+\\.[0-9]+-[0-9]+-g[a-f0-9]+$"),
            Pattern.compile("^[0-9]+\\.[0-9]+\\.[0-9]$"),
            Pattern.compile("^[0-9]+\\.[0-9]+\\.[0-9]+-rc[0-9]+$"),
            Pattern.compile("^[0-9]+\\.[0-9]+\\.[0-9]+-beta[0-9]+$")
    };

    /**
     * Returns true iff the given string is a valid "orderable" SLS version.
     */
    public static boolean isOrderableVersion(String version) {
        for (Pattern p : ORDERABLE_VERSION) {
            if (p.matcher(version).matches()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true iff the given string is a valid "non-orderable" SLS version.
     */
    public static boolean isNonOrderableVersion(String version) {
        return NON_ORDERABLE_VERSION.matcher(version).matches();
    }

    /**
     * Returns true iff the given string is a valid "orderable" or "non-orderable" SLS version.
     */
    public static boolean isValidVersion(String version) {
        // Note: Technically this condition is redundant this isOrderableVersion ==> isNonOrderableVersion.
        // Will check both for maintainability and legibility.
        return isOrderableVersion(version) || isNonOrderableVersion(version);
    }
}
