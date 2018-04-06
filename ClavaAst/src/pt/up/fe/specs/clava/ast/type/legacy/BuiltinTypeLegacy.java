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

package pt.up.fe.specs.clava.ast.type.legacy;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.type.BuiltinType;
import pt.up.fe.specs.clava.ast.type.data.TypeData;
import pt.up.fe.specs.clava.language.BuiltinTypeKeyword;
import pt.up.fe.specs.util.SpecsLogs;

public class BuiltinTypeLegacy extends BuiltinType {

    private final List<BuiltinTypeKeyword> keywords;
    private final String otherBuiltins;

    public BuiltinTypeLegacy(TypeData typeData, ClavaNodeInfo info) {
        super(typeData, info, Collections.emptyList());

        keywords = buildKeywords(typeData.getBareType());
        otherBuiltins = buildOtherBuiltins(typeData.getBareType());
    }

    private static List<BuiltinTypeKeyword> buildKeywords(String bareType) {

        if (bareType.startsWith("<") && bareType.endsWith(">")) {
            return Collections.emptyList();
        }

        try {
            List<BuiltinTypeKeyword> keywords = Arrays.stream(bareType.split(" "))
                    .filter(type -> !type.isEmpty())
                    .map(type -> BuiltinTypeKeyword.getHelper().valueOf(type))
                    .collect(Collectors.toList());
            return keywords;
        } catch (Exception e) {
            throw new RuntimeException("Could not decode type '" + bareType, e);
        }

    }

    private String buildOtherBuiltins(String bareType) {
        if (bareType.startsWith("<") && bareType.endsWith(">")) {
            return bareType;
        }

        return null;
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new BuiltinTypeLegacy(getTypeData(), getInfo());
    }

    @Override
    public String getCode(String name) {
        // String type = getBareType();
        String type = keywords.stream()
                .map(keyword -> keyword.getCode())
                .collect(Collectors.joining(" "));

        String varName = name == null ? "" : " " + name;
        return type + varName;
        /*
        if (name == null) {
            return type;
        }
        
        return type + " " + name;
        */
    }

    private List<BuiltinTypeKeyword> getKeywords() {
        return keywords;
    }

    // private Optional<String> getOtherBuiltins() {
    // return Optional.of(otherBuiltins);
    // }

    @Override
    public String getConstantCode(String constant) {
        boolean isUnsigned = keywords.contains(BuiltinTypeKeyword.UNSIGNED);

        if (isUnsigned) {
            // if (getBareType().startsWith("unsigned")) {
            return constant + "u";
        }

        return constant;
        // System.out.println("TYPE:" + getType());
        // switch (getType()) {
        // case "unsigned int":
        // case "unsigned long":
        // case "unsigned long long":
        // return constant + "u";
        // default:
        // return constant;
        // }

    }

    @Override
    public boolean isVoid() {
        // If return type is not void, add return
        boolean hasVoid = getKeywords().stream()
                .filter(keyword -> keyword == BuiltinTypeKeyword.VOID)
                .findAny()
                .isPresent();

        if (getKeywords().size() > 1) {
            SpecsLogs.msgInfo("'void' type has more than one keyword, check if ok: " + getKeywords());
        }

        return hasVoid;
    }

    /*
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((getTypeData() == null) ? 0 : getTypeData().hashCode());
        result = prime * result + ((keywords == null) ? 0 : keywords.hashCode());
    
        return result;
    }
    */

    /*
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
    
        BuiltinType other = (BuiltinType) obj;
    
        if (keywords == null) {
            if (other.keywords != null) {
                return false;
            }
        } else if (!keywords.equals(other.keywords)) {
            return false;
        }
    
        return true;
    }
    */

    @Override
    public String toContentString() {
        if (getData() != null) {
            // toContentString(super.toContentString(), "kind: " + getData().getKind());
            return getData().toString();
        }
        return super.toContentString() + ", keywords: " + getKeywords();
    }

}
