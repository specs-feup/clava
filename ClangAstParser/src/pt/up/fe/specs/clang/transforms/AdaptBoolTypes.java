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

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.LegacyToDataStore;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.ast.type.BuiltinType;
import pt.up.fe.specs.clava.ast.type.NullType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.ast.type.enums.BuiltinKind;
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
        if (type == null || type instanceof NullType) {
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
        BuiltinType boolType = LegacyToDataStore.getFactory()
                .builtinType(BuiltinKind.BOOL);

        boolType.setNodeData(node);
        // Legacy support
        // If all types had DataStore, we could have used node.getFactoryWithNode()
        // In any case, this transformation is deprecated for the new parser
        // boolType.setId(node.getExtendedId().get());
        // boolType.setLocation(node.getLocation());

        return boolType;

        /*
        if (node.hasDataI()) {
            return node.getFactoryWithNode().builtinType(BuiltinKind.BOOL);
        
            // DataStore builtinData = node.getDataI().copy()
            // .put(BuiltinType.KIND, BuiltinKind.BOOL);
        
            // return new BuiltinType(builtinData, Collections.emptyList());
        }
        
        // if (node.hasData()) {
        // BuiltinTypeData data = new BuiltinTypeData(-1, BuiltinKind.BOOL, false, (TypeDataV2) node.getData());
        // return new BuiltinType(data, Collections.emptyList());
        // }
        
        return new BuiltinTypeLegacy(new TypeData("bool"), node.getInfo());
        */
    }

}
