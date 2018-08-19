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

package pt.up.fe.specs.clang.clavaparser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.ast.genericnode.GenericClangNode;
import pt.up.fe.specs.clang.parsers.ClangParserData;
import pt.up.fe.specs.clang.parsers.ClavaNodes;
import pt.up.fe.specs.clang.utils.ChildrenAdapter;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.DummyNode;
import pt.up.fe.specs.clava.ast.LegacyToDataStore;
import pt.up.fe.specs.clava.ast.attr.AlignedExprAttr;
import pt.up.fe.specs.clava.ast.decl.CXXConstructorDecl;
import pt.up.fe.specs.clava.ast.decl.CXXRecordDecl;
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.decl.EnumConstantDecl;
import pt.up.fe.specs.clava.ast.decl.FieldDecl;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.decl.data.ctorinit.AnyMemberInit;
import pt.up.fe.specs.clava.ast.decl.data.ctorinit.BaseInit;
import pt.up.fe.specs.clava.ast.decl.data.ctorinit.CXXCtorInitializer;
import pt.up.fe.specs.clava.ast.decl.data.templates.TemplateArgument;
import pt.up.fe.specs.clava.ast.decl.data.templates.TemplateArgumentExpr;
import pt.up.fe.specs.clava.ast.decl.data.templates.TemplateArgumentType;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.InitListExpr;
import pt.up.fe.specs.clava.ast.extra.NullNodeOld;
import pt.up.fe.specs.clava.ast.extra.Undefined;
import pt.up.fe.specs.clava.ast.stmt.CXXCatchStmt;
import pt.up.fe.specs.clava.ast.stmt.CXXTryStmt;
import pt.up.fe.specs.clava.ast.stmt.CaseStmt;
import pt.up.fe.specs.clava.ast.stmt.CompoundStmt;
import pt.up.fe.specs.clava.ast.stmt.ForStmt;
import pt.up.fe.specs.clava.ast.stmt.IfStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.clava.ast.stmt.WhileStmt;
import pt.up.fe.specs.clava.ast.type.DependentSizedArrayType;
import pt.up.fe.specs.clava.ast.type.QualType;
import pt.up.fe.specs.clava.ast.type.TemplateSpecializationType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.ast.type.VariableArrayType;
import pt.up.fe.specs.clava.language.CXXCtorInitializerKind;
import pt.up.fe.specs.clava.utils.Typable;
import pt.up.fe.specs.util.SpecsCheck;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsSystem;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.stringparser.StringParser;
import pt.up.fe.specs.util.treenode.NodeInsertUtils;
import pt.up.fe.specs.util.treenode.transform.TransformQueue;

public class NewClavaNodeParser<T extends ClavaNode> extends AClangNodeParser<T> {

    // public static <T extends ClavaNode> Function<ClangConverterTable, ClangNodeParser<?>> newInstanceNoContent(
    // Class<T> nodeClass) {
    //
    // return (converter) -> new NewClavaNodeParser<>(converter, false, nodeClass);
    // }

    public static <T extends ClavaNode> Function<ClangConverterTable, ClangNodeParser<?>> newInstance(
            Class<T> nodeClass) {
        return (converter) -> new NewClavaNodeParser<>(converter, nodeClass);
    }

    private final Class<T> nodeClass;

    // public NewClavaNodeParser(ClangConverterTable converter, boolean hasContent, Class<T> nodeClass) {
    public NewClavaNodeParser(ClangConverterTable converter, Class<T> nodeClass) {
        super(converter, false, true);

        this.nodeClass = nodeClass;
    }

    @Override
    protected T parse(ClangNode node, StringParser parser) {

        // System.out.println("PARSING " + node.getExtendedId());
        // Discard parser contents
        parser.clear();

        // Get already parsed new ClavaNode
        // Using parsed ClavaNodes instead of simply ClavaData to avoid manually applying
        // post-processing and other complications
        ClavaNode clavaNode = getClangRootData().getNewParsedNodes().get(node.getId());

        Preconditions.checkNotNull(clavaNode, "Could not find new ClavaNode for id '" + node.getId() + "'");
        // System.out.println("NODE:" + node.getContent());
        // System.out.println("CLAVA DATA:" + clavaNode.getData().getClass());
        // System.out.println("CLAVA NODE:" + clavaNode);

        boolean isType = clavaNode instanceof Type;
        // Parse children
        // List<ClavaNode> children = parseChildren(node);

        // childrenId.stream().map(getClangRootData().getAllClangNodes()::get)
        // .collect(Collectors.toList());

        List<ClangNode> childrenClangNodes = getChildrenClangNodes(clavaNode, node);
        // List<ClavaNode> children = parseChildren(node.getChildrenStream(), getClass().getSimpleName(), isType);
        List<ClavaNode> children = parseChildren(childrenClangNodes.stream(), getClass().getSimpleName(), isType);

        /*
        if (clavaNode instanceof DeclRefExpr) {
            System.out.println(
                    "VISITED CHILDREN:" + getStdErr().get(ClangParserKeys.VISITED_CHILDREN).get(node.getExtendedId()));
        
            for (String childId : getStdErr().get(ClangParserKeys.VISITED_CHILDREN).get(node.getExtendedId())) {
                ClangNode childNode = getClangRootData().getAllClangNodes().get(childId);
        
                if (childId == null) {
                    throw new RuntimeException("No ClangNode for id " + childNode);
                }
            }
        
        }
        */
        // .map(nodeId -> getParsedNode(nodeId))

        // getStdErr().get(ClangParserKeys.VISITED_CHILDREN).get(node.getExtendedId()).stream()
        // .map(nodeId -> getParsedNode(nodeId))
        // List<ClavaNode> children = parseChildren(
        // getStdErr().get(ClangParserKeys.VISITED_CHILDREN).get(node.getExtendedId()).stream(),
        // getClass().getSimpleName(),
        // isType);

        // if (clavaNode instanceof DeclRefExpr) {
        // System.out.println("DeclRefExpr CHILDREN:" + children);
        // }

        children = pruneChildren(clavaNode, children);

        // getConfig().get(ClavaNode.CONTEXT)
        // .get(ClavaContext.FACTORY)
        // .newNode()
        /*
        if (!clavaNode.getClass().equals(nodeClass)) {
            System.out.println("CLAVA NODE CLASS:" + clavaNode.getClass());
            System.out.println("NEW CLAVA NODE CLASS:" + nodeClass);
        }
        
        clavaNode.setChildren(children);
        
        // Additional processing on the node itself
        processNodeCopy(clavaNode);
        
        return nodeClass.cast(clavaNode);
        */

        // return (T) clavaNode;

        // System.out.println("NODE CLASS:" + nodeClass);
        // Share data, in order to avoid divergence in nodes that are supposed to be the same
        // T clavaNodeCopy = clavaNode.newInstance(true, true, nodeClass, children);

        // T clavaNodeCopy = withoutCopy(clavaNode, children);
        children = new ChildrenAdapter(LegacyToDataStore.CLAVA_CONTEXT.get()).adaptChildren(clavaNode, children);
        clavaNode.setChildren(children);
        return (T) clavaNode;
        // T clavaNodeCopy = withoutCopy(clavaNode, children);
        // return clavaNodeCopy;

        // clavaNode.setChildren(children);
        // T clavaNodeCopy = (T) clavaNode;

        // Additional processing on the node itself
        // processNodeCopy(clavaNodeCopy);

        // return newClavaNode(nodeClass, clavaNode.getDataI(), children);
        /*
        if (clavaNode.hasDataI()) {
            return newClavaNode(nodeClass, clavaNode.getDataI(), children);
        }
        
        return newClavaNode(nodeClass, clavaNode.getData(), children);
        */
        /*
        // Check if children has any dummy type
        if (children.stream().filter(DummyNode.class::isInstance)
                .findFirst().isPresent()) {
            System.out.println("NODE " + clavaNode);
            System.out.println("HAS DUMMY TYPES:" + children);
        }
        */
    }

    private T withoutCopy(ClavaNode clavaNode, List<ClavaNode> children) {

        // Check if children need a Stmt wrapper
        if (clavaNode instanceof CompoundStmt) {
            for (int i = 0; i < children.size(); i++) {
                ClavaNode child = children.get(i);
                if (child instanceof Stmt) {
                    continue;
                }

                if (child instanceof Expr) {
                    children.set(i, LegacyToDataStore.getFactory().exprStmt((Expr) child));
                    continue;
                }

                throw new RuntimeException("Case not defined: " + child.getClass());
            }
        }

        clavaNode.setChildren(children);

        return (T) clavaNode;
    }

    public void processNodeCopy(ClavaNode clavaNodeCopy) {

        // if (true) {
        // return;
        // }

        // In case node has a type, replace with old node type if the type or any of its descendants it a dummy node
        if (clavaNodeCopy instanceof Typable) {
            // if (clavaNodeCopy instanceof DeclRefExpr) {
            // System.out.println("DECL REF EXPR TYPE BEFORE:" + ((Typable) clavaNodeCopy).getType());
            // }

            Typable typable = (Typable) clavaNodeCopy;

            boolean replaceType = checkReplaceType(clavaNodeCopy);

            if (replaceType) {
                Type oldParsingType = getTypesMap().get(clavaNodeCopy.getId());

                typable.setType(oldParsingType);
                // System.out.println("FOUND DUMMY TYPE!");
                // System.out.println("COPY ID:" + clavaNodeCopy.getId());
                // System.out.println("TYPES MAP:" + getTypesMap());
                // System.out.println("ORIGINAL TYPE:" + getOriginalTypes().get(typable.getType().getId()));
                // System.out.println("TYPE:" + getTypesMap().get(clavaNodeCopy.getId()));
                // System.out.println("NODE CLASS:" + clavaNodeCopy.getClass());
            }

            // if (clavaNodeCopy instanceof DeclRefExpr) {
            // System.out.println("DECL REF EXPR TYPE AFTER:" + ((Typable) clavaNodeCopy).getType());
            // }

        }

        // If CXXConstructorDecl, replace dummy types in initializers
        if (clavaNodeCopy instanceof CXXConstructorDecl) {
            for (CXXCtorInitializer init : clavaNodeCopy.get(CXXConstructorDecl.CONSTRUCTOR_INITS)) {
                Expr initExpr = init.get(CXXCtorInitializer.INIT_EXPR);

                if (initExpr instanceof DummyNode) {
                    ClavaNode nonDummyExpr = getOldClavaNode(initExpr);
                    // System.out.println("NON DUMMY EXPR:" + nonDummyExpr.toTree());
                    // NodeInsertUtils.replace(initExpr, nonDummyExpr, true);
                    init.set(CXXCtorInitializer.INIT_EXPR, (Expr) nonDummyExpr);

                    CXXCtorInitializerKind kind = init.get(CXXCtorInitializer.INIT_KIND);
                    switch (kind) {
                    case ANY_MEMBER_INITIALIZER:
                        Decl anyMemberDecl = init.get(AnyMemberInit.ANY_MEMBER_DECL);
                        if (anyMemberDecl instanceof DummyNode) {
                            Decl newAnyMemberDecl = (Decl) getOldClavaNode(anyMemberDecl);
                            // NodeInsertUtils.replace(anyMemberDecl, newAnyMemberDecl, true);
                            init.set(AnyMemberInit.ANY_MEMBER_DECL, newAnyMemberDecl);
                        }
                        break;

                    case BASE_INITIALIZER:
                        Type baseClass = init.get(BaseInit.BASE_CLASS);
                        if (baseClass instanceof DummyNode) {
                            Type newType = (Type) getOldClavaNode(baseClass);
                            // NodeInsertUtils.replace(baseClass, newType, true);
                            init.set(BaseInit.BASE_CLASS, newType);
                        }
                        break;

                    default:
                        throw new NotImplementedException(kind);
                    }
                    // List<ClavaNode> children = parseChildren(childrenClangNodes.stream(), getClass().getSimpleName(),
                    // isType);

                }
                // if(init.get(AnyMemberInit.INIT_EXPR))
                /*
                switch (init.get(CXXCtorInitializer.INIT_KIND)) {
                case ANY_MEMBER_INITIALIZER:
                
                default:
                    continue;
                }
                */
            }
        }

        // If type and as sugar, replace dummy unsugared type
        if (clavaNodeCopy instanceof Type) {
            Type type = (Type) clavaNodeCopy;

            Type currentType = type;

            while (currentType.hasSugar()) {
                // If has dummy desugared
                if (currentType.desugar() instanceof DummyNode) {
                    Type newType = (Type) getOldClavaNode(currentType.desugar());
                    // NodeInsertUtils.replace(currentType.desugar(), newType, true);
                    currentType.set(Type.UNQUALIFIED_DESUGARED_TYPE, Optional.of(newType));
                }

                currentType = currentType.get(Type.UNQUALIFIED_DESUGARED_TYPE).get();
            }

            if (type instanceof TemplateSpecializationType) {
                TemplateSpecializationType tsType = (TemplateSpecializationType) type;
                int numArgs = tsType.getTemplateArguments().size();
                for (int i = 0; i < numArgs; i++) {
                    TemplateArgument arg = tsType.getTemplateArgument(i);
                    switch (arg.get(TemplateArgument.TEMPLATE_ARGUMENT_KIND)) {
                    case Expression:
                        if (arg.get(TemplateArgumentExpr.EXPR) instanceof DummyNode) {
                            Expr newExpr = (Expr) getOldClavaNode(arg.get(TemplateArgumentExpr.EXPR));
                            // NodeInsertUtils.replace(arg.get(TemplateArgumentExpr.EXPR), newExpr);
                            TemplateArgumentExpr newArgExpr = new TemplateArgumentExpr(newExpr);
                            tsType.setTemplateArgument(i, newArgExpr);
                        }
                        break;

                    case Type:
                        if (arg.get(TemplateArgumentType.TYPE) instanceof DummyNode) {
                            Type newType = (Type) getOldClavaNode(arg.get(TemplateArgumentType.TYPE));
                            // NodeInsertUtils.replace(arg.get(TemplateArgumentType.TYPE), newType);

                            TemplateArgumentType newArgType = new TemplateArgumentType(newType);
                            // TemplateArgumentType newArgType = new TemplateArgumentType(
                            // (Type) getOldClavaNode(arg.get(TemplateArgumentType.TYPE)));
                            tsType.setTemplateArgument(i, newArgType);
                        }
                        break;

                    default:
                        throw new NotImplementedException(arg.get(TemplateArgument.TEMPLATE_ARGUMENT_KIND));
                    }
                }
            }
        }

        if (clavaNodeCopy instanceof Typable) {
            Typable typable = (Typable) clavaNodeCopy;

            // typable.getType().getDescendantsStream()
            // .filter(DummyNode.class::isInstance)
            // .forEach(dummyNode -> System.out.println("DUMMY NODE ID:" + dummyNode.get(ClavaNode.ID)));

            TransformQueue<ClavaNode> queue = new TransformQueue<>("Dummy Types Replacement");

            typable.getType().getDescendantsStream()
                    .filter(DummyNode.class::isInstance)
                    .filter(dummyNode -> getOldClavaNode(dummyNode, false) != null)
                    .filter(dummyNode -> dummyNode.hasParent())
                    .forEach(dummyNode -> queue.swap(dummyNode, getOldClavaNode(dummyNode), false));

            queue.apply();

            // if (typable.getType().getDescendantsStream().filter(DummyNode.class::isInstance).findFirst()
            // .isPresent()) {
            //
            // throw new RuntimeException("STOP");
            // }
        }

        // if (clavaNodeCopy instanceof CXXMethodDecl) {
        // System.out.println("HAS DUMMY?" + clavaNodeCopy.getDescendantsAndSelfStream()
        // .filter(DummyNode.class::isInstance)
        // .findFirst()
        // .isPresent());
        // }

        // if (clavaNodeCopy instanceof TagType) {
        // Decl tagDecl = clavaNodeCopy.get(TagType.DECL);
        //
        // if (tagDecl instanceof DummyNode) {
        // ClavaNode nonDummyDecl = getOldClavaNode(tagDecl);
        // clavaNodeCopy.set(TagType.DECL, (Decl) nonDummyDecl);
        // }
        //
        // }

    }

    private ClavaNode getOldClavaNode(ClavaNode newNode) {
        return getOldClavaNode(newNode, true);
    }

    private ClavaNode getOldClavaNode(ClavaNode newNode, boolean checkNull) {
        String id = newNode.get(ClavaNode.ID);
        ClangNode clangNode = getClangRootData().getAllClangNodes().get(id);

        if (checkNull) {
            SpecsCheck.checkNotNull(clangNode, () -> "Could not find old Clava node for node '" + newNode + "'");
        } else {
            if (clangNode == null) {
                return null;
            }
        }
        // System.out.println("NODE:" + newNode);
        // System.out.println("IS TYPE:" + (newNode instanceof Type));
        ClavaNode oldNode = parseChild(clangNode, newNode instanceof Type);

        // Put children in a new list, so that they can be detached
        List<ClavaNode> newChildren = newNode.getChildrenStream()
                .collect(Collectors.toList());

        // Replace children
        newChildren.stream().forEach(ClavaNode::detach);
        oldNode.setChildren(newChildren);

        // If new node has parent, replace it
        if (newNode.hasParent()) {
            NodeInsertUtils.replace(newNode, oldNode);
        }

        return oldNode;
    }

    private boolean checkReplaceType(ClavaNode node) {
        Typable typable = (Typable) node;

        // If types map is not yet initialized, return false
        // if (!getConverter().isTypesMapInitialized()) {
        // return false;
        // }

        Type oldParsingType = getTypesMap().get(node.getId());

        // If no old parsing type, do not replace
        if (oldParsingType == null) {
            // SpecsLogs.msgWarn("Old parsing type is null, is this correct?"); // Maybe, if we are visiting nodes with
            // the
            // new nodes that are not visited by the
            // old nodes.

            return false;
        }

        // Verify if the type of the node is according to the expected type from old parsing
        // System.out.println("GETTING TYPE FROM " + node.get(ClavaNode.ID));
        // System.out.println("NODE " + node);
        Type nodeType = getOriginalTypes().get(typable.getType().getId());
        if (nodeType == null) {
            // ClavaLog.debug("NodeType is null. Type id: " + typable.getType().getId());
            return false;
        }

        if (oldParsingType != null && nodeType != oldParsingType) {
            if (SpecsSystem.isDebug()) {
                SpecsLogs.msgWarn("Current node type different from expected type:\nExpected type:" + oldParsingType
                        + "\nCurrent type:" + nodeType);
            }

            return true;
        }

        boolean hasDummyNodes = typable.getType().getDescendantsAndSelfStream()
                .filter(DummyNode.class::isInstance)
                .findAny()
                .isPresent();

        if (hasDummyNodes) {
            return true;
        }

        return false;
    }

    private List<ClangNode> getChildrenClangNodes(ClavaNode clavaNode, ClangNode node) {
        // In certain cases, use new nodes children
        if (useNewNodesChildren(clavaNode)) {
            // Get ClangNodes of the children visited by the new node
            List<String> childrenId = getStdErr().get(ClangParserData.VISITED_CHILDREN).get(node.getExtendedId());
            // if (clavaNode instanceof ElaboratedType) {
            // System.out.println("ELABORATED CHILDREN:" + childrenId);
            // }

            List<ClangNode> childrenClangNodes = new ArrayList<>();
            for (String childId : childrenId) {
                ClangNode clangNode = getClangRootData().getAllClangNodes().get(childId);

                if (clangNode != null) {
                    childrenClangNodes.add(clangNode);
                    continue;
                }

                if (ClavaNodes.isNullId(childId)) {
                    childrenClangNodes.add(new pt.up.fe.specs.clang.ast.genericnode.NullNode());
                    continue;
                }

                //
                // if (clangNode != null) {
                // System.out.println("NAME:" + clangNode.getName());
                // System.out.println("ID:" + clangNode.getId());
                // }

                // If null, check if system header node
                // if (clangNode == null) {
                // // System.out.println("SYSTEM HEADER IDS:"
                // // + getStdErr().get(ClangParserKeys.SYSTEM_HEADERS_CLANG_NODES).keySet());
                // // System.out.println("SYSTEM HEADER ID:" + childId);
                // clangNode = getStdErr().get(ClangParserData.SYSTEM_HEADERS_CLANG_NODES).get(childId);
                // // System.out.println("CLANG NODE:" + clangNode);
                // }

                // System.out.println("CLANG NODES:" + getClangRootData().getAllClangNodes().keySet());
                // System.out.println("REQUIRED ID:" + childId);
                // if (true) {
                // throw new RuntimeException("Stop");
                // }

                // Create ClangNode
                ClavaNode childNode = getStdErr().get(ClangParserData.CLAVA_NODES).get(childId);

                if (childNode == null) {
                    throw new RuntimeException("Could not find child with id '" + childId + "' for node " + clavaNode);
                }

                // Create ClangNode
                clangNode = new GenericClangNode(childNode.getClass().getSimpleName(), childId, true);
                // System.out.println("CLANG NODE: " + clangNode);
                /*
                if (clangNode == null) {
                    System.out.println("Null clang node for child id " + childId);
                    continue;
                }
                */
                childrenClangNodes.add(clangNode);

            }

            return childrenClangNodes;
        }

        // AST dumper children
        return node.getChildren();
    }

    private boolean useNewNodesChildren(ClavaNode clavaNode) {

        // Bypass
        if (true) {
            return true;
        }

        // For now, only use children from Clang AST dump
        // Decl children replace BareDecl
        // if (clavaNode instanceof DeclRefExpr) {
        // return true;
        // }

        if (clavaNode instanceof FunctionDecl
                || clavaNode instanceof CXXRecordDecl
                || clavaNode instanceof VarDecl
                || clavaNode instanceof QualType) {
            // SpecsLogs.debug("FUNCTION DECL CHILDREN: " + clavaNode.getChildren());
            return true;
        }

        return false;
    }

    private List<ClavaNode> pruneChildren(ClavaNode clavaNode, List<ClavaNode> children) {
        // Check if it has a filler node
        if (clavaNode instanceof InitListExpr) {
            if (children.size() > 0
                    && (children.get(0) instanceof Undefined)) {

                String content = children.get(0).toContentString();
                Preconditions.checkArgument(content.equals("array - filler"),
                        "Content of node is not 'array - filler:'" + content);

                return children.subList(1, children.size());
            }
        }

        // Check if any of the children is a NullNode
        boolean hasNullNode = children.stream()
                .filter(child -> child instanceof NullNodeOld)
                .findFirst()
                .isPresent();

        if (hasNullNode) {
            processNullNodes(clavaNode, children);
        }

        return children;
    }

    private void processNullNodes(ClavaNode clavaNode, List<ClavaNode> children) {
        if (clavaNode instanceof DependentSizedArrayType) {
            // Only second child can be null
            Preconditions
                    .checkArgument(
                            !(children.get(0) instanceof NullNodeOld) && (children.get(1) instanceof NullNodeOld));

            // Replace with NullExpr
            children.set(1, LegacyToDataStore.getFactory().nullExpr());
            return;
        }

        if (clavaNode instanceof AlignedExprAttr) {
            Preconditions.checkArgument(children.size() == 1);
            children.set(0, LegacyToDataStore.getFactory().nullExpr());
            return;
        }

        if (clavaNode instanceof VariableArrayType) {
            Preconditions.checkArgument(children.size() > 1);
            if (children.get(1) instanceof NullNodeOld) {
                children.set(1, LegacyToDataStore.getFactory().nullExpr());
            }
            return;
        }

        if (clavaNode instanceof FieldDecl) {
            Preconditions.checkArgument(children.size() > 1);
            if (children.get(0) instanceof NullNodeOld) {
                children.set(0, LegacyToDataStore.getFactory().nullExpr());
            }
            if (children.get(1) instanceof NullNodeOld) {
                children.set(1, LegacyToDataStore.getFactory().nullExpr());
            }
            return;
        }

        if (clavaNode instanceof EnumConstantDecl) {
            Preconditions.checkArgument(children.size() == 1);
            if (children.get(0) instanceof NullNodeOld) {
                children.set(0, LegacyToDataStore.getFactory().nullExpr());
            }

            return;
        }

        if (clavaNode instanceof IfStmt) {
            Preconditions.checkArgument(children.size() == 4);
            if (children.get(2) instanceof NullNodeOld) {
                children.set(2, LegacyToDataStore.getFactory().nullStmt());
            }
            if (children.get(3) instanceof NullNodeOld) {
                children.set(3, LegacyToDataStore.getFactory().nullStmt());
            }

            return;
        }

        if (clavaNode instanceof ForStmt) {
            Preconditions.checkArgument(children.size() == 4);
            if (children.get(0) instanceof NullNodeOld) {
                children.set(0, LegacyToDataStore.getFactory().nullStmt());
            }
            if (children.get(1) instanceof NullNodeOld) {
                children.set(1, LegacyToDataStore.getFactory().nullStmt());
            }
            if (children.get(2) instanceof NullNodeOld) {
                children.set(2, LegacyToDataStore.getFactory().nullStmt());
            }
            if (children.get(3) instanceof NullNodeOld) {
                children.set(3, LegacyToDataStore.getFactory().nullStmt());
            }

            return;
        }

        if (clavaNode instanceof WhileStmt) {
            Preconditions.checkArgument(children.size() == 3);

            if (children.get(0) instanceof NullNodeOld) {
                children.set(0, LegacyToDataStore.getFactory().nullDecl());
            }
            if (children.get(1) instanceof NullNodeOld) {
                children.set(1, LegacyToDataStore.getFactory().nullStmt());
            }
            if (children.get(2) instanceof NullNodeOld) {
                children.set(2, LegacyToDataStore.getFactory().nullStmt());
            }

            return;
        }

        if (clavaNode instanceof CXXCatchStmt) {
            Preconditions.checkArgument(children.size() == 2);

            if (children.get(0) instanceof NullNodeOld) {
                children.set(0, LegacyToDataStore.getFactory().nullDecl());
            }
            if (children.get(1) instanceof NullNodeOld) {
                children.set(1, LegacyToDataStore.getFactory().nullStmt());
            }

            return;
        }

        if (clavaNode instanceof CXXTryStmt) {
            Preconditions.checkArgument(children.size() > 0);

            if (children.get(1) instanceof NullNodeOld) {
                children.set(1, LegacyToDataStore.getFactory().nullStmt());
            }

            return;
        }

        if (clavaNode instanceof CaseStmt) {
            Preconditions.checkArgument(children.size() > 0);

            if (children.get(1) instanceof NullNodeOld) {
                children.set(1, LegacyToDataStore.getFactory().nullExpr());
            }

            return;
        }

        throw new RuntimeException("NullNodeOld not being handled in class " + clavaNode.getClass());
    }

    @Override
    protected boolean isLegacyParser() {
        return false;
    }

}
