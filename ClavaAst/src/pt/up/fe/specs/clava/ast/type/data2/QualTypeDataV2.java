/**
 * Copyright 2018 SPeCS.
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

package pt.up.fe.specs.clava.ast.type.data2;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import pt.up.fe.specs.clava.ast.type.enums.AddressSpaceQualifierV2;
import pt.up.fe.specs.clava.ast.type.enums.C99Qualifier;

public class QualTypeDataV2 extends TypeDataV2 {

    private final List<C99Qualifier> c99Qualifiers;
    private final AddressSpaceQualifierV2 addressSpaceQualifier;
    private final long addressSpace;

    // public QualTypeDataV2(Standard standard, List<C99Qualifier> c99Qualifiers, TypeDataV2 data) {
    public QualTypeDataV2(List<C99Qualifier> c99Qualifiers, AddressSpaceQualifierV2 addressSpaceQualifier,
            long addressSpace, TypeDataV2 data) {
        super(data);

        // this.standard = standard;
        this.c99Qualifiers = c99Qualifiers;
        this.addressSpaceQualifier = addressSpaceQualifier;
        this.addressSpace = addressSpace;
    }

    public QualTypeDataV2(QualTypeDataV2 data) {
        // this(data.standard, data.c99Qualifiers, data);
        this(data.c99Qualifiers, data.addressSpaceQualifier, data.addressSpace, data);
    }

    public List<C99Qualifier> getC99Qualifiers() {
        return c99Qualifiers;
    }

    public List<String> getQualifiers() {
        List<String> qualifiers = new ArrayList<>();

        qualifiers.addAll(getC99Qualifiers().stream().map(C99Qualifier::getCode).collect(Collectors.toList()));

        return qualifiers;
    }

    @Override
    public String toString() {
        // return toString(super.toString(), "standard: " + standard + ", C99 qualifiers: " + c99Qualifiers);
        return toString(super.toString(), "C99 qualifiers: " + c99Qualifiers + ", address space: "
                + addressSpaceQualifier + " (" + addressSpace + ")");
    }

}
