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
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.decl.RecordDecl;
import pt.up.fe.specs.clava.utils.NullNode;

public class ClavaAstMethods extends TreeNodeAstMethods<ClavaNode> {

    // private static final ClassMap<ClavaNode, ClavaNode> CHILDREN_PROCESSORS;
    //
    // static {
    // CHILDREN_PROCESSORS = new ClassMap<>();
    // }

    public ClavaAstMethods(WeaverEngine engine, Class<ClavaNode> nodeClass,
            Function<ClavaNode, JoinPoint> toJoinPointFunction, Function<ClavaNode, String> toJoinPointNameFunction,
            Function<ClavaNode, List<ClavaNode>> scopeChildrenGetter) {
        super(engine, nodeClass, toJoinPointFunction, toJoinPointNameFunction, scopeChildrenGetter);
        // TODO Auto-generated constructor stub
    }

    @Override
    protected Object[] getChildrenImpl(ClavaNode node) {

        // final List<Object> childrenList = Arrays.asList(node.getChildren());

        var children = node.getChildren().stream()
                // Filter null nodes
                .filter(child -> !(child instanceof NullNode))
                // Process the child, it might be removed if it is transformed
                // into null
                .map(ClavaAstMethods::processChild)
                // Some children might be removed after processChild,
                // this is identified by returning null
                .filter(child -> child != null)
                // .filter(ClavaAstMethods::lclFilter)
                // .map(child -> replaceMethodImplementation(child, childrenList))
                .toArray();

        // If node is a RecordDecl, make sure present functions are implementations and not declarations,
        // in case an implementation is present
        // if (node instanceof RecordDecl) {
        // System.out.println("Class: " + node.get(NamedDecl.DECL_NAME));
        // System.out.println("Children: "
        // + node.getChildren().stream().map(child -> child.getNodeName()).collect(Collectors.toList()));
        // }

        /* */

        // This is already being handled in Selector._addJps
        /*
        if (node instanceof CXXRecordDecl) {
            replaceForMethodImplementations(children);
        }
        */

        /* */

        return children;
    }

    /**
     * Because we are filtering null nodes, the number of children must be adjusted
     */
    @Override
    protected Integer getNumChildrenImpl(ClavaNode node) {
        return getChildrenImpl(node).length;
    }

    /*
    protected static boolean lclFilter(ClavaNode node) {
        // Function
        if (node instanceof FunctionDecl) {
            var functionDecl = (FunctionDecl) node;
            var declaration = functionDecl.getPrototypes();
            var definition = functionDecl.getImplementation();
    
            SpecsCheck.checkArgument(!declaration.isEmpty() || definition.isPresent(),
                    () -> "Expected at least one of them to be present");
    
            // XOR, if only one of them is present, current node must be one of them
            if (declaration.isEmpty() ^ definition.isEmpty()) {
                return true;
            }
    
            // Both are present, only return current node if it is the definition
            // Using == since they must be the same object
            return definition.get() == node;
        }
    
      
    
        // No other cases
        return true;
    }
    */
    // protected static Object replaceMethodImplementation(Object child, List<Object> childrenList) {
    //
    // // check if CXXMethodDecl
    // if (!(child instanceof CXXMethodDecl))
    // return child;
    //
    // CXXMethodDecl method = (CXXMethodDecl) child;
    //
    // // if has body, do nothing
    // if (method.hasBody())
    // return child;
    //
    // // if implementation is equal, do nothing
    // Optional<FunctionDecl> implementationOptional = method.getImplementation();
    // if (!implementationOptional.isPresent() || method.equals(implementationOptional.get()))
    // return child;
    //
    // FunctionDecl implementation = implementationOptional.get();
    //
    // boolean isAlreadyInMethodsList = childrenList.stream()
    // // filter non-methods
    // .filter(obj -> obj instanceof CXXMethodDecl)
    // .map(obj -> (CXXMethodDecl) obj)
    // // filter its own
    // .filter(m -> !m.equals(method))
    // // map to implementation
    // .map(m -> m.getImplementation())
    // // filter those which do not have implementation
    // .filter(impl -> impl.isPresent())
    // // map to code
    // .map(impl -> impl.get().getCode())
    // .anyMatch(code -> code.equals(implementation.getCode()));
    //
    // if (isAlreadyInMethodsList)
    // return child;
    //
    // // System.out.println("replacing impl for => " + node.getDeclName() + "." + method.getDeclName());
    //
    // return implementation;
    // }

    // protected static Object[] replaceForMethodImplementations(Object[] children) {
    //
    // List<Object> childrenList = Arrays.asList(children);
    //
    // // with index in order to replace at position
    // for (int i = 0; i < children.length; i++) {
    // Object child = children[i];
    //
    // children[i] = replaceMethodImplementation(child, childrenList);
    //
    // /*
    // if (!(child instanceof CXXMethodDecl)) continue;
    //
    // CXXMethodDecl method = (CXXMethodDecl) child;
    //
    // // if has body, do nothing
    // if (method.hasBody()) continue;
    //
    // // if implementation is equal, do nothing
    // Optional<FunctionDecl> implementationOptional = method.getImplementation();
    // if (!implementationOptional.isPresent() || method.equals(implementationOptional.get())) continue;
    //
    // FunctionDecl implementation = implementationOptional.get();
    //
    // boolean isAlreadyInMethodsList = childrenList.stream()
    // // filter non-methods
    // .filter(obj -> obj instanceof CXXMethodDecl)
    // .map(obj -> (CXXMethodDecl) obj)
    // // filter its own
    // .filter(m -> !m.equals(method))
    // // map to implementation
    // .map(m -> m.getImplementation())
    // // filter those which do not have implementation
    // .filter(impl -> impl.isPresent())
    // // map to code
    // .map(impl -> impl.get().getCode())
    // .anyMatch(code -> code.equals(implementation.getCode()));
    //
    // if (isAlreadyInMethodsList) continue;
    //
    // children[i] = implementation;
    //
    // // System.out.println("replacing impl for => " + node.getDeclName() + "." + method.getDeclName());
    // */
    // }
    //
    // return children;
    // }

    protected static ClavaNode processChild(ClavaNode child) {

        // When class method, only return if part of the class, and transform into canonical form
        // This way, we avoid returning duplicated methods, as they usually appear in C++,
        // once as a declaration inside the class, and again as an implementation in another file
        // with the possibility of appearing more than one declaration
        if (child instanceof CXXMethodDecl) {
            var method = (CXXMethodDecl) child;

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
            // return ((CXXMethodDecl) child).canonical();
        }

        if (child instanceof FunctionDecl) {
            var function = (FunctionDecl) child;

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

        return child;
    }

}
