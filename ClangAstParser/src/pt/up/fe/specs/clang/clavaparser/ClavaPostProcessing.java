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
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.decl.RecordDecl;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.ast.type.ElaboratedType;
import pt.up.fe.specs.clava.ast.type.RecordType;
import pt.up.fe.specs.clava.utils.Typable;

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

            // NamedDecl anonymousDecl = getAnonymousDecl(currentNode, lastRecordDecl);
            Typable anonymousDecl = getAnonymousDecl(currentNode, lastRecordDecl);
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

            // Special case: directly changing the Data object, since we want this change to reflect across
            // all nodes
            // TODO: Alternatively, this should be implemented as a ClavaRule and apply as a bottom-up change

            elaboratedType.setTypeAsString(newBareType);
            // if (elaboratedType.hasDataI()) {
            // // elaboratedType.getDataI().set(ElaboratedType.TYPE_AS_STRING, newBareType);
            // elaboratedType.setTypeAsString(newBareType);
            // } else {
            // elaboratedType.getTypeData().setBareType(newBareType);
            // }

        }

    }

    // private static NamedDecl getAnonymousDecl(ClavaNode node, RecordDecl lastRecordDecl) {
    private static Typable getAnonymousDecl(ClavaNode node, RecordDecl lastRecordDecl) {
        // if (!(node instanceof NamedDecl)) {
        // return null;
        // }

        if (!(node instanceof Decl)) {
            return null;
        }

        if (!(node instanceof Typable)) {
            return null;
        }

        // NamedDecl namedDecl = (NamedDecl) node;
        Typable typableDecl = (Typable) node;

        // Check if type has a record type
        // RecordType recordType = namedDecl.getType().toTry(RecordType.class).orElse(null);
        RecordType recordType = typableDecl.getType().toTry(RecordType.class).orElse(null);

        if (recordType != null // It has a RecordType
                // && lastRecordDecl.getRecordDeclData().isAnonymous() // Last record is anonymous
                && lastRecordDecl.get(RecordDecl.IS_ANONYMOUS) // Last record is anonymous
                && recordType.getRecordName().equals(lastRecordDecl.getTypeCode())) { // They are the same type

            // return namedDecl;
            return typableDecl;
        }

        return null;
    }

}
