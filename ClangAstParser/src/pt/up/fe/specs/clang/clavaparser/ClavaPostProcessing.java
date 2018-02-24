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
import pt.up.fe.specs.clava.ast.decl.NamedDecl;
import pt.up.fe.specs.clava.ast.decl.RecordDecl;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.ast.type.ElaboratedType;
import pt.up.fe.specs.clava.ast.type.RecordType;

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

            // If last record decl is null, there is nothing that can be done
            if (lastRecordDecl == null) {
                continue;
            }

            NamedDecl anonymousDecl = getAnonymousDecl(currentNode, lastRecordDecl);
            if (anonymousDecl == null) {
                continue;
            }

            // if (!(currentNode instanceof NamedDecl)) {
            // continue;
            // }
            //
            // NamedDecl namedDecl = (NamedDecl) currentNode;
            // System.out.println("TYPE:" + namedDecl.getType());
            // System.out.println("TYPE CODE:" + namedDecl.getType().getCode());
            //
            // if (namedDecl instanceof TypedefDecl) {
            // System.out.println("DESUGARED:" + namedDecl.getType().desugar());
            // }

            // if (declaratorDecl.getDeclName().equals("lookuptable")) {
            // System.out.println("VARDECL ANON TYPE:" + declaratorDecl.getType());
            // }
            // Found variable declaration for anonymous type, set as type the name of the last RecordDecl
            assert lastRecordDecl != null;

            // System.out.println("ANON TYPE:" + declaratorDecl.getType().getCode());
            // String type = lastRecordDecl.getTagKind().getCode() + " " + lastRecordDecl.getDeclName();

            // varDecl.setTypes(Arrays.asList(TypeParser.parse(type, varDecl.getLocation())));
            // declaratorDecl.setType(ClavaNodeFactory.literalType(type));
            ElaboratedType elaboratedType = anonymousDecl.getType().getDescendantsAndSelfStream()
                    .filter(child -> child instanceof ElaboratedType)
                    .map(child -> (ElaboratedType) child)
                    .findFirst().get();

            String newBareType = lastRecordDecl.getTagKind().getCode() + " " + lastRecordDecl.getDeclName();
            elaboratedType.getTypeData().setBareType(newBareType);
            /*
            System.out.println("PREVIOUS ELABORATED:" + elaboratedType.getCode());
            LiteralType newElaboratedType = ClavaNodeFactory
                    .literalType(lastRecordDecl.getTagKind().getCode() + " " + lastRecordDecl.getDeclName());
            System.out.println("NEW ELABORATED:" + newElaboratedType.getCode());
            if (elaboratedType.hasParent()) {
                ClavaNode parent = elaboratedType.getParent();
                System.out.println("REPLACE BEFORE:" + parent);
                NodeInsertUtils.replace(elaboratedType, newElaboratedType);
                System.out.println("REPLACE AFTER:" + parent);
            } else {
                // No parent means it is top-level type
                anonymousDecl.setType(newElaboratedType);
                System.out.println("SET");
            }
            */

            // System.out.println("NEW TYPE:" + declaratorDecl.getType());
        }

    }

    private static NamedDecl getAnonymousDecl(ClavaNode node, RecordDecl lastRecordDecl) {
        if (!(node instanceof NamedDecl)) {
            return null;
        }

        NamedDecl namedDecl = (NamedDecl) node;

        // if (namedDecl instanceof DeclaratorDecl) {
        // DeclaratorDecl declaratorDecl = (DeclaratorDecl) namedDecl;
        // if (declaratorDecl.getType().isAnonymous()) {
        // return declaratorDecl;
        // }
        // }

        // If there is no last RecordDecl, there is nothing more that can be done
        // if (lastRecordDecl == null) {
        // return null;
        // }

        // System.out.println("TYPE:" + namedDecl.getType());
        // Check if type has a record type
        RecordType recordType = namedDecl.getType().to(RecordType.class).orElse(null);

        if (recordType != null // It has a RecordType
                && lastRecordDecl.getRecordDeclData().isAnonymous() // Last record is anonymous
                && recordType.getRecordName().equals(lastRecordDecl.getType().getCode())) { // They are the same type

            // System.out.println("RECORD TYPE:" + recordType.getRecordName());
            // System.out.println("LAST RECORD:" + lastRecordDecl.getType().getCode());

            return namedDecl;
        }

        // Condition
        // assert varDecl.getType().get(0).isAnonynous() == varDecl.getType().get(0).getStringType().get()
        // .contains("(anonymous ");

        // If not anonymous type, continue
        // if (!(varDecl.getType() instanceof NullType)) {
        // continue;
        // }
        // if (!declaratorDecl.getType().isAnonymous()) {
        return null;
    }

}
