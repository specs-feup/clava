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

package pt.up.fe.specs.clang.clavaparser;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeIterator;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.decl.DeclaratorDecl;
import pt.up.fe.specs.clava.ast.decl.RecordDecl;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.ast.type.ElaboratedType;
import pt.up.fe.specs.clava.ast.type.LiteralType;
import pt.up.fe.specs.util.treenode.NodeInsertUtils;

/**
 * Utility methods for post processing.
 * 
 * @author JoaoBispo
 *
 */
public class ClavaPostProcessing {

    /**
     * Currently applies the following passes:
     * 
     * <p>
     * 1) De-anonymize variable declarations;<br>
     * 
     * @param tUnit
     */
    public static void applyPostPasses(TranslationUnit tUnit) {
        // Find anon var decls and change the type with the previous RecordDecl
        // deanonymizeDecls(tUnit);

        // There can be nested structures
        // for (RecordDecl recordDecl : tUnit.getDescendants(RecordDecl.class)) {
        // deanonymizeDecls(recordDecl);
        // }

        tUnit.getDescendantsAndSelfStream()
                .forEach(child -> deanonymizeDecls(child));

    }

    public static void deanonymizeDecls(ClavaNode node) {
        if (!node.hasChildren()) {
            return;
        }

        ClavaNodeIterator iterator = node.getChildrenIterator();

        RecordDecl lastRecordDecl = null;
        while (iterator.hasNext()) {
            ClavaNode currentNode = iterator.next();

            if (currentNode instanceof RecordDecl) {
                lastRecordDecl = (RecordDecl) currentNode;
                continue;
            }

            if (!(currentNode instanceof DeclaratorDecl)) {
                continue;
            }

            DeclaratorDecl declaratorDecl = (DeclaratorDecl) currentNode;

            // Condition
            // assert varDecl.getType().get(0).isAnonynous() == varDecl.getType().get(0).getStringType().get()
            // .contains("(anonymous ");

            // If not anonymous type, continue
            // if (!(varDecl.getType() instanceof NullType)) {
            // continue;
            // }
            if (!declaratorDecl.getType().isAnonymous()) {
                continue;
            }

            // if (declaratorDecl.getDeclName().equals("lookuptable")) {
            // System.out.println("VARDECL ANON TYPE:" + declaratorDecl.getType());
            // }
            // Found variable declaration for anonymous type, set as type the name of the last RecordDecl
            assert lastRecordDecl != null;

            // System.out.println("ANON TYPE:" + declaratorDecl.getType().getCode());
            // String type = lastRecordDecl.getTagKind().getCode() + " " + lastRecordDecl.getDeclName();

            // varDecl.setTypes(Arrays.asList(TypeParser.parse(type, varDecl.getLocation())));
            // declaratorDecl.setType(ClavaNodeFactory.literalType(type));
            ElaboratedType elaboratedType = declaratorDecl.getType().getDescendantsAndSelfStream()
                    .filter(child -> child instanceof ElaboratedType)
                    .map(child -> (ElaboratedType) child)
                    .findFirst().get();

            LiteralType newElaboratedType = ClavaNodeFactory
                    .literalType(lastRecordDecl.getTagKind().getCode() + " " + lastRecordDecl.getDeclName());

            if (elaboratedType.hasParent()) {
                NodeInsertUtils.replace(elaboratedType, newElaboratedType);
            } else {
                // No parent means it is top-level type
                declaratorDecl.setType(newElaboratedType);
            }

            // System.out.println("NEW TYPE:" + declaratorDecl.getType());
        }

    }

}
