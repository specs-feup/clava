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

import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.NullNodeType;
import pt.up.fe.specs.clava.context.ClavaFactory;
import pt.up.fe.specs.util.SpecsCheck;
import pt.up.fe.specs.util.SpecsSystem;
import pt.up.fe.specs.util.exceptions.CaseNotDefinedException;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

public class ClavaNodes {

    private static final String NULLPRT_DECL = "nullptr_decl";
    private static final String NULLPRT_STMT = "nullptr_stmt";
    private static final String NULLPRT_EXPR = "nullptr_expr";
    private static final String NULLPRT_TYPE = "nullptr_type";
    private static final String NULLPRT_ATTR = "nullptr_attr";

    private static final Set<String> NULL_IDS = new HashSet<>(
            Arrays.asList(NULLPRT_DECL, NULLPRT_STMT, NULLPRT_EXPR, NULLPRT_TYPE, NULLPRT_ATTR));

    private final Map<String, ClavaNode> clavaNodes;
    private final List<Runnable> queuedActions;
    private final ClavaFactory factory;

    public ClavaNodes(ClavaFactory factory) {
        this.clavaNodes = new HashMap<>();
        this.queuedActions = new ArrayList<>();
        this.factory = factory;
    }

    public Map<String, ClavaNode> getNodes() {
        return clavaNodes;
    }

    public List<Runnable> getQueuedActions() {
        return queuedActions;
    }

    public ClavaNode get(String nodeId) {
        ClavaNode clavaNode = clavaNodes.get(nodeId);

        Preconditions.checkNotNull(clavaNode, "Could not find ClavaNode with id '" + nodeId
                + "'. Check if node is being visited. If parsing of includes is enabled, check that the parsing level is sufficient.");

        return clavaNode;

    }

    public <T extends ClavaNode> ClavaNode get(String nodeId, Class<T> valueClass) {
        ClavaNode clavaNode = clavaNodes.get(nodeId);

        if (clavaNode == null) {
            ClavaLog.warning("Could not find ClavaNode with id '" + nodeId
                    + "', returning an empty instance. Check if node is being visited. If parsing of includes is enabled, check that the parsing level is sufficient.");

            clavaNode = SpecsSystem.newInstance(valueClass);
        }

        return clavaNode;
    }

    public <T extends ClavaNode> Optional<ClavaNode> getOptional(String nodeId) {
        ClavaNode clavaNode = clavaNodes.get(nodeId);

        if (clavaNode == null) {
            ClavaLog.warning("Could not find ClavaNode with id '" + nodeId
                    + "', returning an empty optional. Check if node is being visited. If parsing of includes is enabled, check that the parsing level is sufficient.");

            return Optional.empty();
        }

        return Optional.of(clavaNode);
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

    public <T extends ClavaNode> void queueSetNode(DataClass<?> data, DataKey<T> key, String nodeId) {
        Runnable nodeToAdd = () -> {

            // If null id, throw exception
            if (isNullId(nodeId)) {
                throw new RuntimeException(
                        "Null id for key '" + key + "' of node '" + data.get(ClavaNode.ID)
                                + "', if node can be null, use queueOptional instead. Node data:\n" + data);
            }

            // Get node
            ClavaNode node = get(nodeId);

            Class<T> valueClass = key.getValueClass();

            ClavaNode adaptedNode = adaptNode(node, valueClass);

            SpecsCheck.checkArgument(valueClass.isInstance(adaptedNode),
                    () -> "Expected id '" + nodeId + "' to be '" + valueClass.getSimpleName() + "', is "
                            + adaptedNode.getClass().getSimpleName());

            data.set(key, valueClass.cast(adaptedNode));
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

    public <T extends ClavaNode> void queueSetOptionalNode(DataClass<?> data, DataKey<Optional<T>> key,
            String nodeId) {

        Runnable nodeToAdd = () -> {

            Optional<T> value = isNullId(nodeId) ? Optional.empty()
                    : key.getValueClass().cast(getOptional(nodeId));

            data.set(key, value);
        };

        queuedActions.add(nodeToAdd);
    }

    public <T extends ClavaNode> void queueSetNullableNode(DataClass<?> data, DataKey<T> key,
            String nodeId) {

        Runnable nodeToAdd = () -> {

            ClavaNode value = isNullId(nodeId) ? getNullNodeType(nodeId).newNullNode(factory) : get(nodeId);

            data.set(key, key.getValueClass().cast(value));
        };

        queuedActions.add(nodeToAdd);
    }

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
