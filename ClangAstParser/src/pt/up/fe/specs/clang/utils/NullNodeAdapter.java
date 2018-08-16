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

package pt.up.fe.specs.clang.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.extra.NullNodeOld;
import pt.up.fe.specs.clava.context.ClavaFactory;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

public class NullNodeAdapter {

    public static enum NullNodeType {
        DECL,
        EXPR,
        STMT,
        TYPE;
    }

    private final List<NullNodeType> childrenToAdapt;
    private final NullNodeType singleType;

    public static NullNodeAdapter newEmpty() {
        return new NullNodeAdapter(Collections.emptyList(), null);
    }

    public static NullNodeAdapter newSingleClassInstance(NullNodeType childrenClass) {
        return new NullNodeAdapter(Collections.emptyList(), childrenClass);
    }

    public static NullNodeAdapter newInstance(NullNodeType... childrenClasses) {
        return new NullNodeAdapter(Arrays.asList(childrenClasses), null);
    }

    private NullNodeAdapter(List<NullNodeType> childrenToAdapt, NullNodeType singleClass) {
        this.childrenToAdapt = childrenToAdapt;
        this.singleType = singleClass;
    }

    public List<ClavaNode> adapt(ClavaFactory factory, ClavaNode parentNode, List<ClavaNode> children) {

        boolean needsAdaptation = children.stream().filter(child -> child instanceof NullNodeOld).findFirst()
                .isPresent();

        if (!needsAdaptation) {
            return children;
        }

        // Adapt all null nodes
        List<ClavaNode> adaptedChildren = new ArrayList<>(children.size());

        if (singleType != null) {
            for (ClavaNode child : children) {
                if (child instanceof NullNodeOld) {
                    child = getNullNode(singleType, factory);
                }

                adaptedChildren.add(child);
            }

            return adaptedChildren;
        }

        for (int i = 0; i < children.size(); i++) {
            ClavaNode child = children.get(i);
            if (child instanceof NullNodeOld) {
                if (i >= childrenToAdapt.size()) {
                    throw new RuntimeException("Found NullNodeOld that is not being adapted for parent node of class "
                            + parentNode.getClass());
                }

                NullNodeType nullNodeType = childrenToAdapt.get(i);
                if (nullNodeType == null) {
                    throw new RuntimeException(
                            "NullNodeType node defined for node of class " + parentNode.getClass() + ", index " + i);
                }

                child = getNullNode(nullNodeType, factory);
            }

            adaptedChildren.add(child);
        }

        return adaptedChildren;
    }

    private ClavaNode getNullNode(NullNodeType nullType, ClavaFactory factory) {
        switch (nullType) {
        case DECL:
            return factory.nullDecl();
        case EXPR:
            return factory.nullExpr();
        case STMT:
            return factory.nullStmt();
        case TYPE:
            return factory.nullType();
        default:
            throw new NotImplementedException(nullType);
        }
    }

}
