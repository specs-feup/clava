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

package pt.up.fe.specs.clang.transforms;

import java.util.Collections;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.enums.BuiltinKind;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.ast.type.BuiltinType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.ast.type.data.TypeData;
import pt.up.fe.specs.clava.ast.type.data2.BuiltinTypeData;
import pt.up.fe.specs.clava.ast.type.data2.TypeDataV2;
import pt.up.fe.specs.clava.ast.type.legacy.BuiltinTypeLegacy;
import pt.up.fe.specs.clava.transform.SimplePostClavaRule;
import pt.up.fe.specs.clava.utils.Typable;
import pt.up.fe.specs.util.treenode.transform.TransformQueue;

/**
 * Clang always parse bool types as _Bool, even if we are parsing C++ objects.
 * 
 * @author JoaoBispo
 *
 */
public class AdaptBoolTypes implements SimplePostClavaRule {

    @Override
    public void applySimple(ClavaNode node, TransformQueue<ClavaNode> queue) {
        if (!(node instanceof Typable)) {
            return;
        }

        // System.out.println("TYPABLE");
        Typable typable = (Typable) node;
        Type type = typable.getType();
        if (type == null) {
            return;
        }

        // System.out.println("TYPE NO NULL");
        if (!(type instanceof BuiltinType)) {
            return;
        }

        // System.out.println("BUILTIN");
        // Check if a boolean
        if (!type.getCode().equals("_Bool")) {
            return;
        }
        // System.out.println("BOOL");

        // Check if a C++ object
        if (!node.getAncestor(TranslationUnit.class).isCXXUnit()) {
            return;
        }
        // System.out.println("CXX");

        // Replace BuiltinType
        BuiltinType newBuiltin = newBoolBuiltin(node);

        typable.setType(newBuiltin);

    }

    private BuiltinType newBoolBuiltin(ClavaNode node) {
        if (node.hasData()) {
            BuiltinTypeData data = new BuiltinTypeData(-1, BuiltinKind.BOOL, false, (TypeDataV2) node.getData());
            return new BuiltinType(data, Collections.emptyList());
        }

        return new BuiltinTypeLegacy(new TypeData("bool"), node.getInfo());
    }

}
