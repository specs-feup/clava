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
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.util.SpecsCheck;

public class ClavaAstMethods extends TreeNodeAstMethods<ClavaNode> {

    public ClavaAstMethods(WeaverEngine engine, Class<ClavaNode> nodeClass,
            Function<ClavaNode, JoinPoint> toJoinPointFunction, Function<ClavaNode, String> toJoinPointNameFunction,
            Function<ClavaNode, List<ClavaNode>> scopeChildrenGetter) {
        super(engine, nodeClass, toJoinPointFunction, toJoinPointNameFunction, scopeChildrenGetter);
        // TODO Auto-generated constructor stub
    }

    @Override
    protected Object[] getChildrenImpl(ClavaNode node) {
        var children = node.getChildren().stream()
                // .filter(ClavaAstMethods::lclFilter)
                .toArray();
        return children;
    }

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

        /*
        // 	Classes
        if (node instanceof CXXRecordDecl) {
        
        	if (node.get(TagDecl.IS_COMPLETE_DEFINITION))
        		return true;
        
        	var definition = node.get(CXXRecordDecl.RECORD_DEFINITION);
        	if (definition.isPresent() && definition.get() == node)
        		return false;
        
        	else
        		return true;
        
        }
        */

        // No other cases
        return true;
    }

}
