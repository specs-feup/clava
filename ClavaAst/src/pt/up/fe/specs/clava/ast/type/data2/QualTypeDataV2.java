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

import java.util.List;

import pt.up.fe.specs.clava.ast.type.enums.C99Qualifier;
import pt.up.fe.specs.clava.language.Standard;

public class QualTypeDataV2 extends TypeDataV2 {

    private final Standard standard;
    private final List<C99Qualifier> c99Qualifiers;

    public QualTypeDataV2(Standard standard, List<C99Qualifier> c99Qualifiers, TypeDataV2 data) {
        super(data);

        this.standard = standard;
        this.c99Qualifiers = c99Qualifiers;
    }

    public QualTypeDataV2(QualTypeDataV2 data) {
        this(data.standard, data.c99Qualifiers, data);
    }

    @Override
    public String toString() {
        return toString(super.toString(), "standard: " + standard + ", C99 qualifiers: " + c99Qualifiers);
    }

}
