/**
 * Copyright 2016 SPeCS.
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

package pt.up.fe.specs.clava.language;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.util.SpecsCollections;
import pt.up.fe.specs.util.enums.EnumHelperWithValue;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.providers.StringProvider;

public enum Standard implements StringProvider {

    C89,
    C90,
    C99,
    C11,
    GNU90,
    GNU99,
    GNU11,
    CXX98("c++98", true),
    CXX03("c++03", true),
    CXX11("c++11", true),
    CXX14("c++14", true),
    GNUXX98("gnu++98", true),
    GNUXX11("gnu++11", true),
    GNUXX14("gnu++14", true);
    // CL20;

    private static final Set<String> C_EXTENSIONS = new HashSet<>(Arrays.asList("c"));
    private static final Set<String> CPP_EXTENSIONS = new HashSet<>(Arrays.asList("cpp, cc, hpp"));

    private static final Lazy<EnumHelperWithValue<Standard>> ENUM_HELPER = EnumHelperWithValue
            .newLazyHelperWithValue(Standard.class);

    private static final Set<Standard> GNU_STANDARDS = SpecsCollections.asSet(GNU90, GNU99, GNU11, GNUXX98, GNUXX11,
            GNUXX14);

    public static EnumHelperWithValue<Standard> getEnumHelper() {
        return ENUM_HELPER.get();
    }

    private static final String STD_PREFIX = "-std=";

    private final String standard;
    private final boolean isCxx;

    private Standard(String standard, boolean isCxx) {
        this.standard = standard;
        this.isCxx = isCxx;
    }

    private Standard(String standard) {
        this(standard, false);
    }

    private Standard() {
        this.standard = name().toLowerCase();
        this.isCxx = false;
    }

    @Override
    public String getString() {
        return standard;
    }

    @Override
    public String toString() {
        return getString();
    }

    public boolean isCxx() {
        return isCxx;
    }

    public String getImplementionExtension() {
        if (isCxx()) {
            return "cpp";
        }

        return "c";
    }

    public String getFlag() {
        return "-std=" + standard;
    }

    public boolean isGnu() {
        return GNU_STANDARDS.contains(this);
    }

    /**
     * Extracts a C/C++ standard from the given list of arguments.
     * 
     * @param arguments
     * @return
     */
    public static Optional<Standard> parseStandard(List<String> arguments) {

        Standard previousStandard = null;

        // Search options

        for (String arg : arguments) {

            if (!arg.startsWith(STD_PREFIX)) {
                continue;
            }

            Standard standard = getEnumHelper().fromValue(arg.substring(STD_PREFIX.length()));

            if (previousStandard != null) {
                ClavaLog.info("Overriding previous standard " + previousStandard + " with " + standard);
            }

            previousStandard = standard;

        }

        return Optional.ofNullable(previousStandard);
    }

    public static Optional<Standard> fromExtension(String extension) {
        if (C_EXTENSIONS.contains(extension)) {
            return Optional.of(C99);
        }

        if (CPP_EXTENSIONS.contains(extension)) {
            return Optional.of(Standard.CXX11);
        }

        return Optional.empty();
    }

}
