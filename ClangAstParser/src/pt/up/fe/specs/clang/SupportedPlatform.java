/**
 * Copyright 2017 SPeCS.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
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
 */
public enum SupportedPlatform implements StringProvider {

    WINDOWS,
    LINUX,
    MAC_OS;

    private static final Lazy<EnumHelperWithValue<SupportedPlatform>> HELPER = EnumHelperWithValue
            .newLazyHelperWithValue(SupportedPlatform.class);


    private static final double SUPPORTED_MAC_VERSION = 11.5;

    public static EnumHelperWithValue<SupportedPlatform> getHelper() {
        return HELPER.get();
    }

    private static final Lazy<SupportedPlatform> CURRENT_PLATFORM = Lazy.newInstance(() -> {
        var platform = calculateCurrentPlatform();
        ClavaLog.debug(() -> "Detected Clava platform: " + platform);
        return platform;
    });

    public static SupportedPlatform getCurrentPlatform() {
        return CURRENT_PLATFORM.get();
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
                throw new RuntimeException("ARM-based platforms are not currently supported");
            }

            return LINUX;
        }

        throw new RuntimeException("Platform currently not supported: " + System.getProperty("os.name"));
    }

    private static Double getMacOSVersion() {
        var macosVersion = System.getProperty("os.version");

        // Extract major.minor (e.g., "14.7" from "14.7.6")
        var versionParts = macosVersion.split("\\.");
        String majorMinor = versionParts.length >= 2 ? versionParts[0] + "." + versionParts[1] : macosVersion;

        var versionNumber = SpecsStrings.parseDouble(majorMinor);

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
        return this == LINUX;
    }

    public boolean isMacOs() {
        return this == MAC_OS;
    }

    public String getName() {
        return name().toLowerCase();
    }

    @Override
    public String toString() {
        return getName();
    }

}
