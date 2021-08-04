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
    CENTOS,
    LINUX,
    LINUX_ARMV7,
    MAC_OS;

    private static final Lazy<EnumHelperWithValue<SupportedPlatform>> HELPER = EnumHelperWithValue
            .newLazyHelperWithValue(SupportedPlatform.class);

    /**
     * Currently two major linux versions are supported, 4 (CENTOS) and 5 and above (LINUX)
     */
    private static final int OLDER_LINUX_VERSION = 4;

    public static EnumHelperWithValue<SupportedPlatform> getHelper() {
        return HELPER.get();
    }

    public static SupportedPlatform getCurrentPlatform() {

        // Windows
        if (SpecsPlatforms.isWindows()) {
            return WINDOWS;
        }

        // CentOS 6
        // if (System.getProperty("os.version").contains(".el6.")) {
        if (System.getProperty("os.version").contains(".el") || System.getProperty("os.version").contains(".fc")) {
            return CENTOS;
        }

        // Generic Mac
        if (SpecsPlatforms.isMac()) {
            return MAC_OS;
        }

        // Generic Linux (Debian-based)
        if (SpecsPlatforms.isLinux()) {
            if (SpecsPlatforms.isLinuxArm()) {
                return LINUX_ARMV7;
            }

            // If version 4 or below, use "older" compiled version, the same as CentOS
            var linuxVersion = System.getProperty("os.version");
            var dotIndex = linuxVersion.indexOf('.');
            if (dotIndex != -1) {
                var majorVersionString = linuxVersion.substring(0, dotIndex);
                var majorVersion = SpecsStrings.parseInteger(majorVersionString);

                if (majorVersion != null && majorVersion <= OLDER_LINUX_VERSION) {
                    return CENTOS;
                }
            }

            return LINUX;
        }

        throw new RuntimeException("Platform currently not supported: " + System.getProperty("os.name"));
    }

    @Override
    public String getString() {
        return getName();
    }

    public boolean isWindows() {
        return this == SupportedPlatform.WINDOWS;
    }

    public boolean isLinux() {
        return this == SupportedPlatform.CENTOS || this == MAC_OS || this == LINUX || this == LINUX_ARMV7;
    }

    public String getName() {
        return name().toLowerCase();
    }

    @Override
    public String toString() {
        return getName();
    }

}
