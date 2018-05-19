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

package pt.up.fe.specs.clava.ast.decl.data;

public class DeclData {

    private static final DeclData EMPTY_DATA = new DeclData(false, false, false, false, false, false);

    public static DeclData empty() {
        return EMPTY_DATA;
    }

    private final boolean hidden;
    private final boolean implicit;
    private final boolean used;
    private final boolean referenced;
    private final boolean invalid;
    private final boolean constexpr;

    public DeclData() {
        this(false, false, false, false, false, false);
    }

    public DeclData(boolean hidden, boolean implicit, boolean used, boolean referenced, boolean invalid,
            boolean constexpr) {

        this.hidden = hidden;
        this.implicit = implicit;
        this.used = used;
        this.referenced = referenced;
        this.invalid = invalid;
        this.constexpr = constexpr;

        // if (constexpr) {
        // SpecsLogs.msgWarn(
        // "ConstExpr true in DeclData, check if there is difference between VarDeclDumperInfo.isConstexpr");
        // }

    }

    public boolean isHidden() {
        return hidden;
    }

    public boolean isImplicit() {
        return implicit;
    }

    public boolean isUsed() {
        return used;
    }

    public boolean isReferenced() {
        return referenced;
    }

    public boolean isInvalid() {
        return invalid;
    }

    public boolean isConstexpr() {
        return constexpr;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append("is hidden: " + hidden);
        builder.append(", is implicit: " + implicit);
        builder.append(", is used: " + used);
        builder.append(", is referenced: " + referenced);
        builder.append(", is invalid: " + invalid);
        builder.append(", is constexpr: " + constexpr);

        return builder.toString();
    }
}
