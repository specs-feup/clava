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

package pt.up.fe.specs.clang.parsers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.suikasoft.jOptions.DataStore.DataClass;
import org.suikasoft.jOptions.Datakey.DataKey;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.attr.Attribute;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.context.ClavaFactory;
import pt.up.fe.specs.util.SpecsCheck;
import pt.up.fe.specs.util.exceptions.CaseNotDefinedException;

public class ClavaNodes {

    // interface TriConsumer<A, B, C> {
    // void accept(A a, B b, C c);
    // }

    // private static final String NULLPRT = "nullptr";
    private static final String NULLPRT_DECL = "nullptr_decl";
    private static final String NULLPRT_STMT = "nullptr_stmt";
    private static final String NULLPRT_EXPR = "nullptr_expr";
    private static final String NULLPRT_TYPE = "nullptr_type";
    private static final String NULLPRT_ATTR = "nullptr_attr";

    private static final Set<String> NULL_IDS = new HashSet<>(
            Arrays.asList(NULLPRT_DECL, NULLPRT_STMT, NULLPRT_EXPR, NULLPRT_TYPE, NULLPRT_ATTR));

    // private final ClangParserKeys data;
    private final Map<String, ClavaNode> clavaNodes;
    private final List<Runnable> delayedNodesToAdd;
    private final ClavaFactory factory;

    public ClavaNodes(ClavaFactory factory) {
        // this.data = data;
        this.clavaNodes = new HashMap<>();
        this.delayedNodesToAdd = new ArrayList<>();
        this.factory = factory;
    }

    public Map<String, ClavaNode> getNodes() {
        return clavaNodes;
    }

    public List<Runnable> getDelayedNodesToAdd() {
        return delayedNodesToAdd;
    }

    public ClavaNode get(String nodeId) {
        ClavaNode clavaNode = clavaNodes.get(nodeId);

        Preconditions.checkNotNull(clavaNode, "Could not find ClavaNode with id '" + nodeId
                + "'. Check if node is being visited, or if there is a cycle in the tree.");

        return clavaNode;

    }

    public Type getType(String parsedTypeId) {
        if (NULLPRT_TYPE.equals(parsedTypeId)) {
            return factory.nullType();
            // return ClavaNodeFactory.nullType(ClavaNodeInfo.undefinedInfo());
        }

        ClavaNode node = get(parsedTypeId);

        SpecsCheck.checkArgument(node instanceof Type,
                () -> "Expected id '" + parsedTypeId + "' to be a Type, is a " + node.getClass().getSimpleName());

        return (Type) node;
    }

    public Expr getExpr(String parsedExprId) {
        if (NULLPRT_EXPR.equals(parsedExprId)) {
            return factory.nullExpr();
        }

        ClavaNode node = get(parsedExprId);

        SpecsCheck.checkArgument(node instanceof Expr,
                () -> "Expected id '" + parsedExprId + "' to be an Expr, is a " + node.getClass().getSimpleName());

        return (Expr) node;
    }

    public Attribute getAttr(String parsedAttrId) {
        Preconditions.checkArgument(!NULLPRT_ATTR.equals(parsedAttrId), "Did not expect 'nullptr'");

        ClavaNode node = get(parsedAttrId);

        SpecsCheck.checkArgument(node instanceof Attribute,
                () -> "Expected id '" + parsedAttrId + "' to be an Attribute, is a " + node.getClass().getSimpleName());
        return (Attribute) node;
    }

    public ClavaNode nullNode(String nullId) {
        switch (nullId) {
        case NULLPRT_DECL:
            return factory.nullDecl();
        case NULLPRT_STMT:
            return factory.nullStmt();
        case NULLPRT_EXPR:
            return factory.nullExpr();
        case NULLPRT_TYPE:
            return factory.nullType();
        default:
            throw new CaseNotDefinedException(nullId);
        }
    }

    public void addNodeAtClosing(DataClass<?> dataClass, DataKey<? extends ClavaNode> key, String nodeIdToAdd) {

        @SuppressWarnings("unchecked") // Check is being done manually
        Runnable nodeToAdd = () -> {
            ClavaNode clavaNode = get(nodeIdToAdd);

            // Check if node is compatible with key
            Preconditions.checkArgument(key.getValueClass().isInstance(clavaNode), "Value of type '"
                    + clavaNode.getClass() + "' not compatible with key accepts values of type '" + key.getValueClass()
                    + "'");

            dataClass.set((DataKey<ClavaNode>) key, clavaNode);
        };

        delayedNodesToAdd.add(nodeToAdd);
    }

    public static boolean isNullId(String nullId) {
        return NULL_IDS.contains(nullId);
    }

}
