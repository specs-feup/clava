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
import java.util.function.Function;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.DataStore.DataClass;
import org.suikasoft.jOptions.Datakey.DataKey;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clang.utils.NullNodeAdapter;
import pt.up.fe.specs.clang.utils.NullNodeAdapter.NullNodeType;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.context.ClavaFactory;
import pt.up.fe.specs.util.SpecsCheck;
import pt.up.fe.specs.util.exceptions.CaseNotDefinedException;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

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
    // private final Map<String, String> skippedNodes;
    private final List<Runnable> queuedActions;
    private final ClavaFactory factory;

    public ClavaNodes(ClavaFactory factory) {
        // public ClavaNodes(ClavaFactory factory, Map<String, String> skippedNodes) {
        // this.data = data;
        this.clavaNodes = new HashMap<>();
        this.queuedActions = new ArrayList<>();
        this.factory = factory;
        // this.skippedNodes = skippedNodes;
    }

    public Map<String, ClavaNode> getNodes() {
        return clavaNodes;
    }

    public List<Runnable> getQueuedActions() {
        return queuedActions;
    }

    public ClavaNode get(String nodeId) {
        ClavaNode clavaNode = clavaNodes.get(nodeId);

        // Check if in skipped nodes
        // if (clavaNode == null) {
        // String nullptrId = skippedNodes.get(nodeId);
        // if (nullptrId != null) {
        // clavaNode = nullNode(nullptrId);
        // }
        // }

        Preconditions.checkNotNull(clavaNode, "Could not find ClavaNode with id '" + nodeId
                + "'. Check if node is being visited.");

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

    public static NullNodeType getNullNodeType(String nullId) {
        switch (nullId) {
        case NULLPRT_ATTR:
            return NullNodeType.ATTR;
        case NULLPRT_DECL:
            return NullNodeType.DECL;
        case NULLPRT_EXPR:
            return NullNodeType.EXPR;
        case NULLPRT_STMT:
            return NullNodeType.STMT;
        case NULLPRT_TYPE:
            return NullNodeType.TYPE;
        default:
            throw new NotImplementedException(nullId);
        }
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
            // If null id, throw exception
            if (isNullId(nodeId)) {
                // return;
                throw new RuntimeException(
                        "Null id for key '" + key + "' of node '" + data.get(ClavaNode.ID)
                                + "', if node can be null, use queueOptional instead. Node data:\n" + data);
            }

            // Get node
            // ClavaNode node = getWithoutDummy(key, nodeId);
            ClavaNode node = get(nodeId);

            Class<T> valueClass = key.getValueClass();

            ClavaNode adaptedNode = adaptNode(node, valueClass);

            // SpecsCheck.checkArgument(!(adaptedNode instanceof NullNode),
            // () -> "Did not expect NullNode at this point: " + adaptedNode);
            // if (adaptedNode instanceof NullNode) {
            // System.out.println("Adapted node: " + adaptedNode);
            // System.out.println("NODE: " + node);
            //
            // throw new RuntimeException("NullNode!");
            // }

            // if (adaptedNode != node) {
            // System.out.println("NODE: " + node.getNodeName());
            // System.out.println("ADAPTED NODE: " + adaptedNode.getNodeName());
            // }
            SpecsCheck.checkArgument(valueClass.isInstance(adaptedNode),
                    () -> "Expected id '" + nodeId + "' to be '" + valueClass.getSimpleName() + "', is "
                            + adaptedNode.getClass().getSimpleName());

            data.set(key, valueClass.cast(adaptedNode));

            // System.out.println("SETTING node " + nodeId + " for key " + key);
        };

        queuedActions.add(nodeToAdd);
    }

    public <T> void queueSetAction(DataClass<?> data, DataKey<T> key,
            Function<DataClass<?>, T> function) {

        Runnable nodeToAdd = () -> {

            // Calculate value
            var value = function.apply(data);

            // Set data
            data.set(key, value);
        };

        queuedActions.add(nodeToAdd);
    }

    private <T extends ClavaNode> ClavaNode adaptNode(ClavaNode node, Class<T> valueClass) {

        // Adapt NullNode to ValueClass, if needed
        // if (node instanceof NullNode && !valueClass.isInstance(node)) {
        // if (node instanceof NullNode) {
        // // System.out.println("VALUE CLASS: " + valueClass);
        // ClavaNode newNode = node.newInstance(false, valueClass, Collections.emptyList());
        // // System.out.println("NEW NODE CLASS: " + newNode.getClass());
        // return newNode;
        // }

        return node;
    }

    // private <T extends ClavaNode> ClavaNode getWithoutDummy(DataKey<T> key, String nodeId) {
    // ClavaNode node = get(nodeId);
    //
    // // If dummy node, create "empty" node with DummyNode information
    // if (node instanceof DummyNode) {
    // return node.newInstance(false, key.getValueClass(), node.getChildren());
    // }
    //
    // return node;
    // }

    // public <T extends ClavaNode> void queueSetOptionalNode(DataClass<?> dataClass, DataKey<Optional<T>> key,
    // String nodeId) {
    //
    // queueSetOptionalNode(dataClass.getData(), key, nodeId);
    // }

    public <T extends ClavaNode> void queueSetOptionalNode(DataClass<?> data, DataKey<Optional<T>> key,
            String nodeId) {

        Runnable nodeToAdd = () -> {

            // System.out.println("SETTING OPTIONAL KEY " + key.getName() + " in node " + data.get(ClavaNode.ID));
            // System.out.println("ID OF OPTIONAL VALUE " + nodeId);

            // if (get(nodeId) instanceof NullNode) {
            // System.out.println("NULL NODE:" + get(nodeId));
            // }
            Optional<T> value = isNullId(nodeId) ? Optional.empty()
                    // Optional<T> value = isNullId(nodeId) ? Optional.empty()
                    : key.getValueClass().cast(Optional.of(get(nodeId)));

            // System.out.println("VALUE: " + value);

            // if (value.isPresent() && value.get() instanceof NullNode) {
            // value = Optional.empty();
            // // System.out.println("ADSADASD");
            // }

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
            // if (!isNullId(nodeId)) {
            // System.out.println("SETTING OPTIONAL KEY " + key + " in node " + data.get(ClavaNode.ID));
            // System.out.println("Given id " + nodeId);
            // System.out.println("VALUE: " + value.get());
            // // System.out.println("SETTING OPTIONAL: " + nodeId);
            // // System.out.println("RETRIEVED NODE: " + get(nodeId));
            // }

            data.set(key, value);
        };

        queuedActions.add(nodeToAdd);
    }

    public <T extends ClavaNode> void queueSetNullableNode(DataClass<?> data, DataKey<T> key,
            String nodeId) {

        Runnable nodeToAdd = () -> {

            ClavaNode value = isNullId(nodeId) ? NullNodeAdapter.getNullNode(getNullNodeType(nodeId), factory)
                    : get(nodeId);

            data.set(key, key.getValueClass().cast(value));
        };

        queuedActions.add(nodeToAdd);
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

        queuedActions.add(nodeToAdd);
    }

    public void queueAction(Runnable runnable) {
        queuedActions.add(runnable);
    }
}
