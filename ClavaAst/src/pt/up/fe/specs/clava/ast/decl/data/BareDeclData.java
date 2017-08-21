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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class BareDeclData {

    private final String kindName;
    private final Long pointer;
    private String declName;
    private final List<String> valueDeclType;

    public BareDeclData(String kindName, Long pointer, String declName, List<String> valueDeclType) {
        this.kindName = kindName;
        this.pointer = pointer;
        this.declName = declName;
        this.valueDeclType = valueDeclType;
    }

    public static BareDeclData newInstance(String declName) {
        return new BareDeclData(null, null, declName, Collections.emptyList());
    }

    public BareDeclData copy() {
        return new BareDeclData(kindName, pointer, declName, new ArrayList<>(valueDeclType));
    }

    public Optional<String> getDeclNameTry() {
        return Optional.ofNullable(declName);
    }

    public String getDeclName() {
        return getDeclNameTry().get();
    }

    public String getKindName() {
        return kindName;
    }

    public Long getPointer() {
        return pointer;
    }

    public List<String> getValueDeclType() {
        return valueDeclType;
    }

    public void setDeclName(String declName) {
        this.declName = declName;
    }

    @Override
    public String toString() {
        StringBuilder string = new StringBuilder();

        string.append("declname: ").append(declName);
        string.append(", kindname: ").append(kindName);
        // string.append(", pointer: 0x").append(Long.toHexString(pointer));
        // string.append(", type: ").append(valueDeclType);

        return string.toString();
    }
}
