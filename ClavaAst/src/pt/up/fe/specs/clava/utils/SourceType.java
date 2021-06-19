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

package pt.up.fe.specs.clava.utils;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import pt.up.fe.specs.util.SpecsEnums;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.lazy.Lazy;

public enum SourceType {

    HEADER("h", "hpp", "h++"),
    IMPLEMENTATION("c", "cpp", "cl", "cc", "cu"),
    // Files that are not parsed by Clava (e.g., include files such as .incl or .inc)
    OUT_OF_SOURCE();

    // private final static Set<String> PERMITTED_EXTENSIONS = new HashSet<>(
    // SpecsCollections.concat(HEADER.getExtensions(), IMPLEMENTATION.getExtensions()));
    private final static Lazy<Set<String>> PERMITTED_EXTENSIONS = Lazy
            .newInstance(SourceType::buildPermittedExtensions);

    private final static Lazy<Map<String, SourceType>> EXTENSIONS_MAP = Lazy
            .newInstance(SourceType::buildExtensionsMap);

    private static final Set<String> C_EXTENSIONS = new HashSet<>(Arrays.asList("c", "h"));
    private static final Set<String> CXX_EXTENSIONS = new HashSet<>(Arrays.asList("cpp", "hpp", "cc"));

    private static Set<String> buildPermittedExtensions() {
        return SpecsEnums.extractValues(SourceType.class).stream()
                .filter(sourceType -> sourceType != OUT_OF_SOURCE)
                .flatMap(sourceType -> sourceType.getExtensions().stream())
                .collect(Collectors.toSet());
    }

    private static Map<String, SourceType> buildExtensionsMap() {

        Map<String, SourceType> extensionsMap = new HashMap<>();

        for (SourceType sourceType : SpecsEnums.extractValues(SourceType.class)) {
            sourceType.getExtensions().stream()
                    .forEach(extension -> extensionsMap.put(extension, sourceType));
        }

        return extensionsMap;
    }

    public static Set<String> getPermittedExtensions() {
        return PERMITTED_EXTENSIONS.get();
    }

    public static Set<String> getHeaderExtensions() {
        return HEADER.getExtensions();
    }

    public static Map<String, SourceType> getExtensionsMap() {
        return EXTENSIONS_MAP.get();
    }

    public static boolean isCxxExtension(String extension) {
        return CXX_EXTENSIONS.contains(extension.toLowerCase());
    }

    public static boolean isCExtension(String extension) {
        return C_EXTENSIONS.contains(extension.toLowerCase());
    }

    private final Set<String> extensions;

    private SourceType(String... extensions) {
        this(new HashSet<>(Arrays.asList(extensions)));
    }

    private SourceType(Set<String> extensions) {
        this.extensions = extensions;
    }

    public Set<String> getExtensions() {
        return extensions;
    }

    public static SourceType getType(String filepath) {
        return getTypeTry(filepath)
                .orElse(OUT_OF_SOURCE);
        // return getTypeTry(filepath)
        // .orElseThrow(() -> new RuntimeException("Given filepath '" + filepath
        // + "' is not valid, permitted extensions: " + getPermittedExtensions()));
    }

    public static Optional<SourceType> getTypeTry(String filepath) {

        String extension = SpecsIo.getExtension(filepath);

        // If no extension, use the name of the filepath
        if (extension.isEmpty()) {
            extension = new File(filepath).getName();

        }

        SourceType sourceType = getExtensionsMap().get(extension);

        if (sourceType == null) {
            // Check if Clang out-of-source (e.g., <built-in>)
            if (extension.startsWith("<") && extension.endsWith(">")) {
                sourceType = SourceType.OUT_OF_SOURCE;
            }

        }

        return Optional.ofNullable(sourceType);

        // if (IMPLEMENTATION.getExtensions().contains(extension)) {
        // return Optional.of(IMPLEMENTATION);
        // }
        //
        // if (HEADER.getExtensions().contains(extension)) {
        // return Optional.of(HEADER);
        // }

        // return Optional.empty();
    }

    public boolean hasExtension(String extension) {
        // Check if it contains a .
        int lastIndexOfDot = extension.lastIndexOf('.');
        if (lastIndexOfDot != -1) {
            extension = extension.substring(lastIndexOfDot + 1);
        }

        return extensions.contains(extension.toLowerCase());
    }

    public static boolean isHeader(File file) {
        return SourceType.HEADER.hasExtension(SpecsIo.getExtension(file));
    }

}
