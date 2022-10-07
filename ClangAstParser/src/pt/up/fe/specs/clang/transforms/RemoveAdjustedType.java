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
import pt.up.fe.specs.clava.ast.type.AdjustedType;
import pt.up.fe.specs.clava.transform.SimplePreClavaRule;
import pt.up.fe.specs.clava.utils.Typable;
import pt.up.fe.specs.util.treenode.transform.TransformQueue;

/**
 * Replaces AdjustedType nodes, which are inserted when there are semantic conversions, with the original type nodes.
 * 
 * <p>
 * TODO: This transformation breaks CTest.testInline, because it breaks getting to the function implementation from the
 * function declaration
 * 
 * @author JoaoBispo
 *
 */
public class RemoveAdjustedType implements SimplePreClavaRule {

    @Override
    public void applySimple(ClavaNode node, TransformQueue<ClavaNode> queue) {

        if (!(node instanceof Typable)) {
            return;
        }

        var typableNode = ((Typable) node);

        var type = typableNode.getType();

        if (!(type instanceof AdjustedType)) {
            return;
        }

        var adjustedType = (AdjustedType) type;
        // System.out.println("ADJUSTED TYPE: " + adjustedType.getCode());
        var originalType = adjustedType.getOriginalType();
        // System.out.println("ORIGINAL TYPE: " + originalType.getCode());
        // Set the type of the current node as being the original type
        typableNode.setType(originalType);

        // Set the adjusted type
        typableNode.setAdjustedType(adjustedType);
    }

}
