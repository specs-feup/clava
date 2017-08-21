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

package pt.up.fe.specs.clava.utils;

import java.util.regex.Pattern;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.util.SpecsStrings;

/**
 * Represents a C/C++ identifier.
 * 
 * @author JoaoBispo
 *
 */
public class Identifier {

    private static final Pattern C_ID_REGEX = Pattern.compile("^[_a-zA-Z]([_a-zA-Z0-9])*$");

    private final String idName;
    private final boolean isReference;

    /**
     * Base constructor.
     * 
     * @param idName
     */
    private Identifier(String idName) {
        this(idName, false);
    }

    private Identifier(String idName, boolean isReference) {
        this.idName = idName;
        this.isReference = isReference;
    }

    public boolean isAnonymous() {
        return idName == null;
    }

    public String getIdName() {
        if (isAnonymous()) {
            return "";
        }

        return idName;
    }

    public boolean isReference() {
        return isReference;
    }

    public Identifier setReference(boolean isReference) {
        return new Identifier(idName, isReference);
    }

    public static Identifier newInstance(String id) {

        // Ensure name is a valid C identifier
        Preconditions.checkArgument(SpecsStrings.matches(id, C_ID_REGEX),
                "Expected a C/C++ identifier: " + id);

        return new Identifier(id);
    }

    public static Identifier newAnonymousId() {
        return new Identifier(null);
    }
}
