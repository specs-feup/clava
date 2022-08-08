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

import java.util.Optional;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.DeclaratorDecl;
import pt.up.fe.specs.clava.ast.decl.NamedDecl;
import pt.up.fe.specs.clava.ast.decl.TagDecl;
import pt.up.fe.specs.clava.ast.decl.TypedefDecl;
import pt.up.fe.specs.clava.ast.type.TagType;
import pt.up.fe.specs.clava.transform.SimplePreClavaRule;
import pt.up.fe.specs.clava.utils.Typable;
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

        // Check if NamedDecl
        var maybeNamedDecl = getNamedDeclWithType(node);

        if (maybeNamedDecl.isEmpty()) {
            return;
        }

        // if (!(node instanceof DeclaratorDecl)) {
        // return;
        // }

        // Check if decl kind is struct or union
        // var declaratorDecl = (DeclaratorDecl) node;
        var namedDecl = maybeNamedDecl.get();

        var tagDecl = getTagDecl(namedDecl);

        // If TagDecl could not be found, return
        if (tagDecl == null) {
            return;
        }

        // Check if record is just above the node, ignoring other vardecls with the same tag decl
        if (!isTagDeclDirectlyAbove(namedDecl, tagDecl)) {
            return;
        }

        // Add declarator as a child of tagDeclVars
        var tagDeclVars = tagDecl.getTagDeclVars();

        // First delete node, so that the node itself is inserted, instead of a copy
        queue.delete(namedDecl);
        queue.addChild(tagDeclVars, namedDecl);

        // In Clang AST, typedef in a struct is moved to the var declaration. When moving the var declaration inside the
        // struct, move the typedef also.
        // if (node instanceof TypedefDecl) {
        // tagDecl.set(TagDecl.HAS_TYPEDEF);
        // }
    }

    private Optional<NamedDecl> getNamedDeclWithType(ClavaNode node) {
        if (!(node instanceof Typable)) {
            return Optional.empty();
        }

        if (node instanceof DeclaratorDecl) {
            return Optional.of((DeclaratorDecl) node);
        }

        if (node instanceof TypedefDecl) {
            return Optional.of((TypedefDecl) node);
        }

        return Optional.empty();
    }

    private TagDecl getTagDecl(NamedDecl decl) {
        var declType = ((Typable) decl).getType().desugarAll();

        // Type must be a RecordType
        if (!(declType instanceof TagType)) {
            return null;
        }

        var tagType = (TagType) declType;

        // Get record
        return tagType.get(TagType.DECL);
    }

    private boolean isTagDeclDirectlyAbove(NamedDecl decl, TagDecl tagDecl) {
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

            if (getNamedDeclWithType(currentSibling).isPresent()) {
                var siblingTagDecl = getTagDecl((NamedDecl) currentSibling);
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
