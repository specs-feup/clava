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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.DataStore.DataClass;
import org.suikasoft.jOptions.Datakey.DataKey;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.ClavaNode;
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

    public List<Runnable> getQueuedNodesToSet() {
        return delayedNodesToAdd;
    }

    public ClavaNode get(String nodeId) {
        ClavaNode clavaNode = clavaNodes.get(nodeId);

        Preconditions.checkNotNull(clavaNode, "Could not find ClavaNode with id '" + nodeId
                + "'. Check if node is being visited, or if there is a cycle in the tree.");

        return clavaNode;

    }
    //
    // public Type getType(String parsedTypeId) {
    // if (NULLPRT_TYPE.equals(parsedTypeId)) {
    // return factory.nullType();
    // // return ClavaNodeFactory.nullType(ClavaNodeInfo.undefinedInfo());
    // }
    //
    // ClavaNode node = get(parsedTypeId);
    //
    // SpecsCheck.checkArgument(node instanceof Type,
    // () -> "Expected id '" + parsedTypeId + "' to be a Type, is a " + node.getClass().getSimpleName());
    //
    // return (Type) node;
    // }

    // public void setType(DataClass<?> dataClass, DataKey<? extends Type> key, String valueNodeId) {
    // setNodeDelayed(dataClass, key, valueNodeId, id -> getType(id));
    // }
    //
    // public void setType(DataStore data, DataKey<? extends Type> key, String valueNodeId) {
    // setType(new GenericDataClass<>(data), key, valueNodeId);
    // }

    // public Expr getExpr(String parsedExprId) {
    // if (NULLPRT_EXPR.equals(parsedExprId)) {
    // return factory.nullExpr();
    // }
    //
    // ClavaNode node = get(parsedExprId);
    //
    // SpecsCheck.checkArgument(node instanceof Expr,
    // () -> "Expected id '" + parsedExprId + "' to be an Expr, is a " + node.getClass().getSimpleName());
    //
    // return (Expr) node;
    // }
    //
    // public void setExpr(DataClass<?> dataClass, DataKey<? extends Expr> key, String valueNodeId) {
    // setNodeDelayed(dataClass, key, valueNodeId, id -> getExpr(id));
    // }
    //
    // public void setExpr(DataStore data, DataKey<? extends Expr> key, String valueNodeId) {
    // setExpr(new GenericDataClass<>(data), key, valueNodeId);
    // }

    // public Attribute getAttr(String parsedAttrId) {
    // Preconditions.checkArgument(!NULLPRT_ATTR.equals(parsedAttrId), "Did not expect 'nullptr'");
    //
    // ClavaNode node = get(parsedAttrId);
    //
    // SpecsCheck.checkArgument(node instanceof Attribute,
    // () -> "Expected id '" + parsedAttrId + "' to be an Attribute, is a " + node.getClass().getSimpleName());
    // return (Attribute) node;
    // }

    // public void setAttr(DataClass<?> dataClass, DataKey<? extends Attr> key, String valueNodeId) {
    // setNodeDelayed(dataClass, key, valueNodeId, id -> getAttr(id));
    // }
    //
    // public void setAttr(DataStore data, DataKey<? extends Attr> key, String valueNodeId) {
    // setAttr(new GenericDataClass<>(data), key, valueNodeId);
    // }

    // public Decl getDecl(String parsedDeclId) {
    // if (NULLPRT_DECL.equals(parsedDeclId)) {
    // return factory.nullDecl();
    // }
    //
    // ClavaNode node = get(parsedDeclId);
    //
    // SpecsCheck.checkArgument(node instanceof Decl,
    // () -> "Expected id '" + parsedDeclId + "' to be a Decl, is a " + node.getClass().getSimpleName());
    //
    // return (Decl) node;
    // }
    //
    // public void setDecl(DataClass<?> dataClass, DataKey<? extends Decl> key, String valueNodeId) {
    // setNodeDelayed(dataClass, key, valueNodeId, id -> getDecl(id));
    // }
    //
    // public void setDecl(DataStore data, DataKey<? extends Decl> key, String valueNodeId) {
    // setDecl(new GenericDataClass<>(data), key, valueNodeId);
    // }

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
    //
    // private void setNodeDelayed(DataClass<?> dataClass, DataKey<?> key, String nodeIdToAdd,
    // Function<String, ClavaNode> nodeSupplier) {
    //
    // @SuppressWarnings("unchecked") // Check is being done manually
    // Runnable nodeToAdd = () -> {
    // ClavaNode clavaNode = nodeSupplier.apply(nodeIdToAdd);
    //
    // // Check if node is compatible with key
    // Preconditions.checkArgument(key.getValueClass().isInstance(clavaNode), "Value of type '"
    // + clavaNode.getClass() + "' not compatible with key accepts values of type '" + key.getValueClass()
    // + "'");
    //
    // dataClass.set((DataKey<ClavaNode>) key, clavaNode);
    // };
    //
    // delayedNodesToAdd.add(nodeToAdd);
    // }

    public static boolean isNullId(String nullId) {
        return NULL_IDS.contains(nullId);
    }

    // private <T> void setNodesDelayed(DataClass<?> dataClass, DataKey<List<T>> key, List<String> nodeIds,
    // Function<String, T> nodeSupplier) {
    //
    // // @SuppressWarnings("unchecked") // Check is being done manually
    // Runnable nodeToAdd = () -> {
    // List<T> nodes = nodeIds.stream().map(nodeSupplier::apply).collect(Collectors.toList());
    // // ClavaNode clavaNode = nodeSupplier.apply(nodeIdToAdd);
    //
    // // Check if each node is compatible with key
    // for (T clavaNode : nodes) {
    // Preconditions.checkArgument(key.getValueClass().isInstance(clavaNode), "Value of type '"
    // + clavaNode.getClass() + "' not compatible with key accepts values of type '"
    // + key.getValueClass()
    // + "'");
    // }
    //
    // dataClass.set(key, nodes);
    // };
    //
    // delayedNodesToAdd.add(nodeToAdd);
    // }
    //
    // public void setTypes(DataClass<?> dataClass, DataKey<List<Type>> key,
    // List<String> valueNodeIds) {
    //
    // setNodesDelayed(dataClass, key, valueNodeIds, id -> getType(id));
    // }
    //
    // public void setTypes(DataStore data, DataKey<List<Type>> key, List<String> valueNodeIds) {
    // setTypes(new GenericDataClass<>(data), key, valueNodeIds);
    // }
    //
    // public <T extends ClavaNode> void queueSetNode(DataClass<?> dataClass, DataKey<T> key, String nodeId) {
    // queueSetNode(dataClass.getData(), key, nodeId);
    // }

    public <T extends ClavaNode> void queueSetNode(DataClass<?> data, DataKey<T> key, String nodeId) {

        Runnable nodeToAdd = () -> {

            // If null id, just return
            if (isNullId(nodeId)) {
                return;
            }

            // Get node
            ClavaNode node = get(nodeId);

            Class<T> valueClass = key.getValueClass();

            SpecsCheck.checkArgument(valueClass.isInstance(node),
                    () -> "Expected id '" + nodeId + "' to be '" + valueClass.getSimpleName() + "', is "
                            + node.getClass().getSimpleName());

            data.set(key, valueClass.cast(node));
        };

        delayedNodesToAdd.add(nodeToAdd);
    }

    // public <T extends ClavaNode> void queueSetOptionalNode(DataClass<?> dataClass, DataKey<Optional<T>> key,
    // String nodeId) {
    //
    // queueSetOptionalNode(dataClass.getData(), key, nodeId);
    // }

    public <T extends ClavaNode> void queueSetOptionalNode(DataClass<?> data, DataKey<Optional<T>> key,
            String nodeId) {

        Runnable nodeToAdd = () -> {

            Optional<T> value = isNullId(nodeId) ? Optional.empty()
                    : key.getValueClass().cast(Optional.of(get(nodeId)));

            /*
            Optional<T> value;
            
            // If null id, empty
            if (isNullId(nodeId)) {
                value = Optional.empty();
            }
            // Otherwise, get node
            else {
            
                // Get node
                // ClavaNode node = ;
            
                Class<Optional<T>> valueClass = key.getValueClass();
                value = valueClass.cast(Optional.of(get(nodeId)));
            
                // SpecsCheck.checkArgument(valueClass.isInstance(node),
                // () -> "Expected id '" + nodeId + "' to be '" + valueClass.getSimpleName() + "', is "
                // + node.getClass().getSimpleName());
                //
                // value = Optional.of(valueClass.cast(node));
            }
            */
            data.set(key, value);
        };

        delayedNodesToAdd.add(nodeToAdd);
    }

    // public <T extends ClavaNode> void queueSetNodeList(DataClass<?> dataClass, DataKey<List<T>> key,
    // List<String> nodeIds) {
    //
    // queueSetNodeList(dataClass.getData(), key, nodeIds);
    // }

    public <T extends ClavaNode> void queueSetNodeList(DataClass<?> data, DataKey<List<T>> key,
            List<String> nodeIds) {

        Runnable nodeToAdd = () -> {

            @SuppressWarnings("unchecked") // If the nodes exist, they should be of the requested type
            List<T> nodes = nodeIds.stream().map(id -> (T) get(id)).collect(Collectors.toList());

            data.set(key, nodes);
        };

        delayedNodesToAdd.add(nodeToAdd);
    }

}
