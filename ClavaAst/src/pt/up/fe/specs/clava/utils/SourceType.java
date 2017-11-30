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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import pt.up.fe.specs.util.SpecsCollections;
import pt.up.fe.specs.util.SpecsIo;

public enum SourceType {

    HEADER("h", "hpp"),
    IMPLEMENTATION("c", "cpp", "cl");

    private final static Set<String> PERMITTED_EXCEPTIONS = new HashSet<>(
            SpecsCollections.concat(HEADER.getExtensions(), IMPLEMENTATION.getExtensions()));

    public static Set<String> getPermittedExceptions() {
        return PERMITTED_EXCEPTIONS;
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
                .orElseThrow(() -> new RuntimeException("Given filepath '" + filepath
                        + "' is not valid, permitted extensions: " + getPermittedExceptions()));
    }

    public static Optional<SourceType> getTypeTry(String filepath) {

        String extension = SpecsIo.getExtension(filepath);

        if (IMPLEMENTATION.getExtensions().contains(extension)) {
            return Optional.of(IMPLEMENTATION);
        }

        if (HEADER.getExtensions().contains(extension)) {
            return Optional.of(HEADER);
        }

        return Optional.empty();
    }

}
