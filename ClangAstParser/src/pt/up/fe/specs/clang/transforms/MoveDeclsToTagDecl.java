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
import pt.up.fe.specs.clava.ast.decl.DeclaratorDecl;
import pt.up.fe.specs.clava.ast.decl.TagDecl;
import pt.up.fe.specs.clava.ast.type.TagType;
import pt.up.fe.specs.clava.transform.SimplePreClavaRule;
import pt.up.fe.specs.util.treenode.transform.TransformQueue;

/**
 * When a struct/union declares a variable, Clang represents it as a VarDecl right after the RecordDecl. This
 * transformation moves the declaration to inside the RecordDecl.
 * 
 * <p>
 * This transformation must be called before CreateDeclStmts.
 * 
 * @author JoaoBispo
 *
 */
public class MoveDeclsToTagDecl implements SimplePreClavaRule {

    @Override
    public void applySimple(ClavaNode node, TransformQueue<ClavaNode> queue) {
        // System.out.println("PROCESSING NODE " + node.getClass());

        // Check if VarDecl
        if (!(node instanceof DeclaratorDecl)) {
            return;
        }

        // Check if decl kind is struct or union
        var declaratorDecl = (DeclaratorDecl) node;

        // System.out.println("PROCESSING DECL " + declaratorDecl.getDeclName());

        var tagDecl = getTagDecl(declaratorDecl);

        // If TagDecl could not be found, return
        if (tagDecl == null) {
            return;
        }

        // System.out.println("TAG DECL: " + tagDecl.getId());

        // Check if record is just above the node, ignoring other vardecls with the same tag decl
        if (!isTagDeclDirectlyAbove(declaratorDecl, tagDecl)) {
            return;
        }

        // Copy node to field of TagDecl
        // tagDecl.get(TagDecl.DECLS).add((DeclaratorDecl) declaratorDecl.copy(true));

        // Mark node as part of the tag
        declaratorDecl.set(DeclaratorDecl.IS_TAG_DECLARATION);

        // Add declarator as a child of TagDecl
        queue.addChild(tagDecl, declaratorDecl);

        // Delete node
        queue.delete(declaratorDecl);

        // System.out.println("VarDecl to join with tag: " + declaratorDecl.getDeclName());
        // System.out.println("Record decl: " + tagDecl.get(TagDecl.TAG_KIND));
        // record
    }

    private TagDecl getTagDecl(DeclaratorDecl decl) {
        var declType = decl.getType().desugarAll();

        // Type must be a RecordType
        if (!(declType instanceof TagType)) {
            return null;
        }

        var tagType = (TagType) declType;

        // Get record
        return tagType.get(TagType.DECL);
    }

    private boolean isTagDeclDirectlyAbove(DeclaratorDecl decl, TagDecl tagDecl) {
        var leftNodes = decl.getLeftSiblings();

        // Iterate from last to first, looking for the same TagDecl
        for (int i = leftNodes.size() - 1; i >= 0; i--) {
            var currentSibling = leftNodes.get(i);

            // Check if found the TagDecl
            if (currentSibling.equals(tagDecl)) {
                return true;
            }

            // Check if another variable declaration of the same TagDecl
            // If so, keep going backwards
            if (currentSibling instanceof DeclaratorDecl) {
                var siblingTagDecl = getTagDecl((DeclaratorDecl) currentSibling);
                if (siblingTagDecl != null && tagDecl.equals(siblingTagDecl)) {
                    continue;
                }
            }

            // TagDecl not found
            return false;
        }

        // No more siblings
        return false;
    }

}
