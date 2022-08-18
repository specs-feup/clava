/**
 * Copyright 2020 SPeCS.
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

package pt.up.fe.specs.clava.weaver.utils;

import java.util.List;
import java.util.function.Function;

import org.lara.interpreter.weaver.ast.TreeNodeAstMethods;
import org.lara.interpreter.weaver.interf.JoinPoint;
import org.lara.interpreter.weaver.interf.WeaverEngine;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.CXXMethodDecl;
import pt.up.fe.specs.clava.ast.decl.CXXRecordDecl;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.decl.RecordDecl;
import pt.up.fe.specs.clava.utils.NullNode;
import pt.up.fe.specs.util.classmap.FunctionClassMap;

public class ClavaAstMethods extends TreeNodeAstMethods<ClavaNode> {

    private static final FunctionClassMap<ClavaNode, ClavaNode> CHILDREN_PROCESSORS;

    static {
        CHILDREN_PROCESSORS = new FunctionClassMap<ClavaNode, ClavaNode>();

        // Process first methods, and functions only after methods
        CHILDREN_PROCESSORS.put(CXXMethodDecl.class, ClavaAstMethods::processChild);
        CHILDREN_PROCESSORS.put(FunctionDecl.class, ClavaAstMethods::processChild);

        CHILDREN_PROCESSORS.put(CXXRecordDecl.class, ClavaAstMethods::processChild);
        CHILDREN_PROCESSORS.setDefaultFunction(child -> child);
    }

    public ClavaAstMethods(WeaverEngine engine, Class<ClavaNode> nodeClass,
            Function<ClavaNode, JoinPoint> toJoinPointFunction, Function<ClavaNode, String> toJoinPointNameFunction,
            Function<ClavaNode, List<ClavaNode>> scopeChildrenGetter) {

        super(engine, nodeClass, toJoinPointFunction, toJoinPointNameFunction, scopeChildrenGetter);
    }

    @Override
    protected Object[] getChildrenImpl(ClavaNode node) {

        var children = node.getChildren().stream()
                // Filter null nodes
                .filter(child -> !(child instanceof NullNode))
                // Process the child, it might be removed if it is transformed
                // into null
                .map(child -> CHILDREN_PROCESSORS.apply(child))
                // Some children might be removed after processChild,
                // this is identified by returning null
                .filter(child -> child != null)
                .toArray();

        return children;
    }

    /**
     * Because we are filtering null nodes, the number of children must be adjusted
     */
    @Override
    protected Integer getNumChildrenImpl(ClavaNode node) {
        return getChildrenImpl(node).length;
    }

    private static ClavaNode processChild(CXXMethodDecl method) {

        var record = method.getRecordDecl().orElse(null);

        // There is no RecordDecl
        // TODO: return it if canonical?
        if (record == null) {
            if (method.isCanonical()) {
                return method;
            } else {
                return null;
            }
        }

        var parentRecord = method.getAncestorTry(RecordDecl.class).orElse(null);

        // Method has a record and is not inside the class, ignore
        if (parentRecord == null) {
            return null;
        }

        // Method has a class ancestor, but is not its own class
        // TODO: Ignore it?
        if (!parentRecord.equals(record)) {
            return null;
        }

        // Return the canonical version
        return method.canonical();

    }

    private static ClavaNode processChild(FunctionDecl function) {
        // If definition, is always valid
        if (function.isDefinition()) {
            return function;
        }

        // It is a declaration, only valid if there is no definition
        // AND if it is the first declaration as returned by .getDefinition()
        if (function.getDefinition().isEmpty() && function.getDeclaration().get().equals(function)) {
            return function;
        }

        // In any other case, ignore the node
        return null;
    }

    private static ClavaNode processChild(CXXRecordDecl cxxRecordDecl) {

        // If definition, is always valid
        if (cxxRecordDecl.isCompleteDefinition()) {
            return cxxRecordDecl;
        }

        // It is a declaration, only valid if there is no definition
        // AND if it is the first declaration as returned by .getDefinition()
        if (cxxRecordDecl.getDefinition().isEmpty() && cxxRecordDecl.getDeclaration().get().equals(cxxRecordDecl)) {
            return cxxRecordDecl;
        }

        // In any other case, ignore the node
        return null;
    }

}
