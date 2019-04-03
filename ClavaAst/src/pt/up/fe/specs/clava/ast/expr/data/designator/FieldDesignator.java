/**
 * Copyright 2019 SPeCS.
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

package pt.up.fe.specs.clava.ast.expr.data.designator;

import java.util.List;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.enums.DesignatorKind;
import pt.up.fe.specs.util.SpecsCheck;

public class FieldDesignator extends Designator {

    /// DATAKEYS START

    public static final DataKey<String> FIELD_NAME = KeyFactory.string("fieldName");

    /// DATAKEYS END

    public FieldDesignator(String fieldName) {
        set(DESIGNATOR_KIND, DesignatorKind.Field);
        set(FIELD_NAME, fieldName);
    }

    @Override
    public String getCode(List<ClavaNode> nodes) {
        SpecsCheck.checkSize(nodes, getNumNodes());

        return "." + get(FIELD_NAME);
    }

    @Override
    public int getNumNodes() {
        return 0;
    }
}
