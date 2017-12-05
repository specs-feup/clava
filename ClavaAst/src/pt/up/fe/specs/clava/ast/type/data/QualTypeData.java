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

package pt.up.fe.specs.clava.ast.type.data;

import java.util.List;

public class QualTypeData {

    private final AddressSpaceQualifier addressSpaceQualifier;
    private final List<Qualifier> qualifiers;

    public QualTypeData(AddressSpaceQualifier addressSpaceQualifier, List<Qualifier> qualifiers) {
        this.addressSpaceQualifier = addressSpaceQualifier;
        this.qualifiers = qualifiers;
    }

    public AddressSpaceQualifier getAddressSpaceQualifier() {
        return addressSpaceQualifier;
    }

    public List<Qualifier> getQualifiers() {
        return qualifiers;
    }

}
