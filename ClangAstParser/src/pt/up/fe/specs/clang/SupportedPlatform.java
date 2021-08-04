/**
 * Copyright 2017 SPeCS.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. under the License.
 */

package pt.up.fe.specs.clang;

import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.lang.SpecsPlatforms;
import pt.up.fe.specs.util.SpecsStrings;
import pt.up.fe.specs.util.enums.EnumHelperWithValue;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.providers.StringProvider;

/**
 * Platforms supported by Clava.
 *
 * <p>
 * TODO: Rename to ClavaPlatform
 *
 * @author JoaoBispo
 *
 */
public enum SupportedPlatform implements StringProvider {

    WINDOWS,
    LINUX_4,
    LINUX_5,
    LINUX_ARMV7,
    MAC_OS;

    private static final Lazy<EnumHelperWithValue<SupportedPlatform>> HELPER = EnumHelperWithValue
            .newLazyHelperWithValue(SupportedPlatform.class);

    // private static final int OLDER_LINUX_VERSION = 4;

    private static final double SUPPORTED_MAC_VERSION = 11.5;
    private static final double SUPPORTED_LINUX_VERSION_HIGHER = 5;
    private static final double SUPPORTED_LINUX_VERSION_LOWER = 4;

    public static EnumHelperWithValue<SupportedPlatform> getHelper() {
        return HELPER.get();
    }

    private static final Lazy<SupportedPlatform> CURRENT_PLATFORM = Lazy.newInstance(() -> calculateCurrentPlatform());

    public static SupportedPlatform getCurrentPlatform() {
        var platform = CURRENT_PLATFORM.get();
        ClavaLog.info("Detected Clava platform: " + platform);
        return platform;
    }

    private static SupportedPlatform calculateCurrentPlatform() {
        // Windows
        if (SpecsPlatforms.isWindows()) {
            return WINDOWS;
        }

        // Generic Mac
        if (SpecsPlatforms.isMac()) {
            var macosVersion = getMacOSVersion();

            if (macosVersion != null && macosVersion < SUPPORTED_MAC_VERSION) {
                ClavaLog.info("Current MacOS version is " + macosVersion + " but only " + SUPPORTED_MAC_VERSION
                        + " and above are supported, execution might fail");
            }

            return MAC_OS;
        }

        // Linux
        if (SpecsPlatforms.isLinux()) {
            if (SpecsPlatforms.isLinuxArm()) {
                // return LINUX_ARMV7;
                throw new RuntimeException("ARM-based platforms are not currently supported");
            }

            var linuxMajorVersion = getLinuxMajorVersion();

            if (linuxMajorVersion == null) {
                ClavaLog.info("Could not determine Linux version, running at your own risk");
                return LINUX_5;
            }

            if (linuxMajorVersion == 4) {
                return LINUX_4;
            }

            if (linuxMajorVersion == 5) {
                return LINUX_5;
            }

            if (linuxMajorVersion > SUPPORTED_LINUX_VERSION_HIGHER) {
                ClavaLog.info("Current major Linux version is " + linuxMajorVersion
                        + ", only up to version " + SUPPORTED_LINUX_VERSION_HIGHER
                        + " has been tested, running at your own risk");
                return LINUX_5;
            }

            if (linuxMajorVersion < SUPPORTED_LINUX_VERSION_LOWER) {
                ClavaLog.info("Current major Linux version is " + linuxMajorVersion
                        + ", only down to version " + SUPPORTED_LINUX_VERSION_LOWER
                        + " has been tested, running at your own risk");
                return LINUX_4;
            }

        }

        // CentOS 6
        // if (System.getProperty("os.version").contains(".el6.")) {
        // if (System.getProperty("os.version").contains(".el") || System.getProperty("os.version").contains(".fc")) {
        // return LINUX_4;
        // }

        // Generic Linux (Debian-based)
        // if (SpecsPlatforms.isLinux()) {
        // if (SpecsPlatforms.isLinuxArm()) {
        // return LINUX_ARMV7;
        // }
        //
        // // If version 4 or below, use "older" compiled version, the same as CentOS
        // var linuxVersion = System.getProperty("os.version");
        // var dotIndex = linuxVersion.indexOf('.');
        // if (dotIndex != -1) {
        // var majorVersionString = linuxVersion.substring(0, dotIndex);
        // var majorVersion = SpecsStrings.parseInteger(majorVersionString);
        //
        // if (majorVersion != null && majorVersion <= OLDER_LINUX_VERSION) {
        // return LINUX_4;
        // }
        // }
        //
        // return LINUX_5;
        // }

        throw new RuntimeException("Platform currently not supported: " + System.getProperty("os.name"));
    }

    private static Integer getLinuxMajorVersion() {
        var linuxVersion = System.getProperty("os.version");
        var dotIndex = linuxVersion.indexOf('.');

        if (dotIndex == -1) {
            ClavaLog.info("Could not extract major version from Linux OS version string, expected at least a . : '"
                    + linuxVersion + "'");
            return null;
        }

        var majorVersionString = linuxVersion.substring(0, dotIndex);

        var majorVersion = SpecsStrings.parseInteger(majorVersionString);

        if (majorVersion == null) {
            ClavaLog.info("Could not extract major version from Linux OS version string, '" + majorVersionString
                    + "' is not an integer: '" + linuxVersion + "'");
            return null;
        }

        return majorVersion;
    }

    private static Double getMacOSVersion() {
        var macosVersion = System.getProperty("os.version");

        var versionNumber = SpecsStrings.parseDouble(macosVersion);

        if (versionNumber == null) {
            ClavaLog.info("Could not convert MacOS version to a double: '" + macosVersion + "'");
            return null;
        }

        return versionNumber;
    }

    @Override
    public String getString() {
        return getName();
    }

    public boolean isWindows() {
        return this == SupportedPlatform.WINDOWS;
    }

    public boolean isLinux() {
        return this == SupportedPlatform.LINUX_4 || this == MAC_OS || this == LINUX_5 || this == LINUX_ARMV7;
    }

    public String getName() {
        return name().toLowerCase();
    }

    @Override
    public String toString() {
        return getName();
    }

}
