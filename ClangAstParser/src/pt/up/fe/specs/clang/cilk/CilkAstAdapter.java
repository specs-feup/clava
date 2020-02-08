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

package pt.up.fe.specs.clang.cilk;

import java.io.File;
import java.util.Collections;
import java.util.stream.Stream;

import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.cilk.CilkFor;
import pt.up.fe.specs.clava.ast.cilk.CilkSpawn;
import pt.up.fe.specs.clava.ast.cilk.CilkSync;
import pt.up.fe.specs.clava.ast.comment.MultiLineComment;
import pt.up.fe.specs.clava.ast.expr.CallExpr;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.ast.pragma.Pragma;
import pt.up.fe.specs.clava.ast.stmt.ForStmt;
import pt.up.fe.specs.clava.ast.stmt.WrapperStmt;
import pt.up.fe.specs.clava.transform.SimplePreClavaRule;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.treenode.transform.TransformQueue;

/**
 * Adds Cilk nodes after code parsing.
 * 
 * @author JoaoBispo
 *
 */
public class CilkAstAdapter implements SimplePreClavaRule {

    @Override
    public void applySimple(ClavaNode node, TransformQueue<ClavaNode> queue) {

        // Change name of the translation unit
        if (node instanceof TranslationUnit) {
            var tunit = (TranslationUnit) node;
            var filename = tunit.getFilename();

            // Remove first extension
            var originalExtension = SpecsIo.getExtension(filename);
            var withoutExtension = SpecsIo.removeExtension(filename);

            var cilkExtension = SpecsIo.getExtension(withoutExtension);
            if (!cilkExtension.equals(CilkParser.getCilkExtension())) {
                return;
            }

            // Build new filename
            var cilkTempFile = tunit.getFile();
            var newFilename = SpecsIo.removeExtension(withoutExtension) + "." + originalExtension;
            var newFile = new File(cilkTempFile.getParentFile(), newFilename);
            tunit.setFile(newFile);

            // If temporary Clava file exists, delete it
            SpecsIo.delete(cilkTempFile);

            var oldFilepath = SpecsIo.getCanonicalPath(cilkTempFile);
            var newFilepath = SpecsIo.getCanonicalPath(newFile);

            // Change the file location of all nodes
            tunit.getDescendantsAndSelfStream()
                    .map(aNode -> aNode.getLocation())
                    .flatMap(sourceRange -> Stream.of(sourceRange.getStart(), sourceRange.getEnd()))
                    .filter(sourceLoc -> oldFilepath.equals(sourceLoc.getFilepath()))
                    // .filter(sourceRange -> sourceRange.getFilenameTry().orElse("").equals(oldFilename))
                    .forEach(sourceLoc -> sourceLoc.setFilepath(newFilepath));

            return;
        }

        // Three transformations: cilk_spawn, cilk_for and cilk_sync

        if (node instanceof Pragma) {
            var pragmaName = ((Pragma) node).getName();

            if (pragmaName.equals(CilkParser.getClavaCilkSync())) {
                addCilkSync((Pragma) node, queue);
            }

            if (pragmaName.equals(CilkParser.getClavaCilkFor())) {
                addCilkFor((Pragma) node, queue);
            }

            return;
        }

        if (node instanceof MultiLineComment) {
            // var wrapperStmt = (WrapperStmt) node;
            // System.out.println("MULTILINE: " + wrapperStmt.getWrappedNode());
            var comment = (MultiLineComment) node;
            var text = comment.getText();

            if (text.equals(CilkParser.getClavaCilkSpawn()) && node.getLocation().getStartLine() > 0) {
                addCilkSpawn((MultiLineComment) node, queue);
            }
        }

    }

    private void addCilkSpawn(MultiLineComment node, TransformQueue<ClavaNode> queue) {
        // Discover call that is related to this comment
        var parent = node.getParent();
        var indexOfSelf = node.indexOfSelf();

        if (parent instanceof WrapperStmt) {
            indexOfSelf = parent.indexOfSelf();
            parent = parent.getParent();
        }

        int spawnLine = node.getLocation().getStartLine();

        var functionCall = getCilkSpawnCall(parent, indexOfSelf, spawnLine);

        if (functionCall == null) {
            ClavaLog.info("Could not process cilk_spawn around line " + spawnLine);
            return;
        }

        // Replace function call with cilk_spawn
        queue.replace(functionCall, functionCall.newInstance(false, CilkSpawn.class, functionCall.getChildren()));

        // Remove comment
        delete(node, queue);
    }

    private CallExpr getCilkSpawnCall(ClavaNode parent, int indexOfSelf, int spawnLine) {
        // Check upper node. If upper node, multi-comment is after the code in the parsed tree
        if (indexOfSelf > 0) {

            var candidate = parent.getChildren().get(indexOfSelf - 1);

            // If spawn line plus one is inside candidate range, return first call in that line
            if (candidate.getLocation().isLineInside(spawnLine)) {
                return getCilkSpawnCall(candidate, spawnLine + 1);
            }
        }

        // Check lower node. If lower, multi-comment is just before the code in the parsed tree
        if (indexOfSelf < parent.getNumChildren() - 1) {

            var candidate = parent.getChildren().get(indexOfSelf + 1);

            // If spawn line inside candidate range, return first call in the same line
            if (candidate.getLocation().isLineInside(spawnLine + 1)) {
                return getCilkSpawnCall(candidate, spawnLine + 1);
            }
        }

        return null;
    }

    private CallExpr getCilkSpawnCall(ClavaNode candidate, int spawnLine) {

        // Find the first CallExpr that starts in the same line
        return candidate.getDescendantsAndSelfStream()
                .filter(node -> node instanceof CallExpr)
                .map(CallExpr.class::cast)
                .filter(call -> call.getLocation().getStartLine() == spawnLine)
                .findFirst()
                .orElse(null);
    }

    private void addCilkSync(Pragma cilkSyncPragma, TransformQueue<ClavaNode> queue) {

        boolean isCilkSync = cilkSyncPragma.getTarget()
                .map(target -> target.getCode().equals(CilkParser.getClavaCilkNop()))
                .orElse(false);
        if (!isCilkSync) {
            ClavaLog.debug(
                    "Found cilk_sync pragma, but target is not what was expected: " + cilkSyncPragma.getTarget());
            return;
        }

        var target = cilkSyncPragma.getTarget().get();

        // Replace pragma target with CilkSync node
        queue.replace(target, target.newInstance(false, CilkSync.class, Collections.emptyList()));

        // Remove pragma
        delete(cilkSyncPragma, queue);
    }

    private void addCilkFor(Pragma cilkForPragma, TransformQueue<ClavaNode> queue) {

        boolean isCilkFor = cilkForPragma.getTarget()
                .map(target -> target instanceof ForStmt)
                .orElse(false);
        if (!isCilkFor) {
            ClavaLog.debug(
                    "Found cilk_for pragma, but target is not what was expected: " + cilkForPragma.getTarget());
            return;
        }

        ForStmt target = (ForStmt) cilkForPragma.getTarget().get();

        // Replace pragma target with CilkFor node
        queue.replace(target, target.newInstance(false, CilkFor.class, target.getChildren()));

        // Remove pragma
        delete(cilkForPragma, queue);
    }

}
