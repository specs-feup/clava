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

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeIterator;
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.decl.RecordDecl;
import pt.up.fe.specs.clava.ast.stmt.DeclStmt;
import pt.up.fe.specs.clava.ast.type.ElaboratedType;
import pt.up.fe.specs.clava.ast.type.RecordType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.transform.SimplePostClavaRule;
import pt.up.fe.specs.clava.utils.Typable;
import pt.up.fe.specs.util.treenode.NodeInsertUtils;
import pt.up.fe.specs.util.treenode.transform.TransformQueue;

/**
 * Creates names for anonymous decls.
 * 
 * @author JoaoBispo
 *
 */
public class DenanonymizeDecls implements SimplePostClavaRule {

    @Override
    public void applySimple(ClavaNode node, TransformQueue<ClavaNode> queue) {
        // if (!(node instanceof TranslationUnit)) {
        // return;
        // }
        //
        // ((TranslationUnit) node).getDescendantsAndSelfStream()
        // .forEach(child -> deanonymizeDecls(child, queue));
        // }
        //
        // public void deanonymizeDecls(ClavaNode node, TransformQueue<ClavaNode> queue) {

        if (!node.hasChildren()) {
            return;
        }

        ClavaNodeIterator iterator = node.getChildrenIterator();

        RecordDecl lastRecordDecl = null;
        while (iterator.hasNext()) {

            ClavaNode currentNode = iterator.next();

            // Convert to Decl
            if (currentNode instanceof DeclStmt) {
                currentNode = ((DeclStmt) currentNode).getDecls().get(0);
            }

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

            // Found variable declaration for anonymous type, set as type the name of the last RecordDecl
            assert lastRecordDecl != null;

            ElaboratedType elaboratedType = anonymousDecl.getType().getDescendantsAndSelfStream()
                    .filter(child -> child instanceof ElaboratedType)
                    .map(child -> (ElaboratedType) child)
                    .findFirst().get();

            String newBareType = lastRecordDecl.getTagKind().getCode() + " " + lastRecordDecl.getDeclName();
            Type denanonymizedElaboratedType = elaboratedType.setBareType(newBareType);

            // If elaboratedType does not have a parent, set new type
            if (!elaboratedType.hasParent()) {
                Preconditions.checkArgument(elaboratedType == anonymousDecl.getType(),
                        "Expected elaborated type to be the type of the node");
                anonymousDecl.setType(denanonymizedElaboratedType);
            }
            // Otherwise, replace type
            else {
                NodeInsertUtils.replace(elaboratedType, denanonymizedElaboratedType);
            }

            // Special case: directly changing the Data object, since we want this change to reflect across
            // all nodes
            // TODO: Alternatively, this should be implemented as a ClavaRule and apply as a bottom-up change
            // if (elaboratedType.hasData()) {
            // elaboratedType.getData().setTypeAsString(newBareType);
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
                && lastRecordDecl.get(RecordDecl.IS_ANONYMOUS) // Last record is anonymous
                && recordType.getRecordName().equals(lastRecordDecl.getTypeCode())) { // They are the same type

            // return namedDecl;
            return typableDecl;
        }

        return null;
    }

    /*
        if (!(node instanceof CXXFunctionalCastExpr)) {
            return;
        }
    
        CXXFunctionalCastExpr castExpr = (CXXFunctionalCastExpr) node;
    
        // if (!castExpr.getTargetType().equals("_Bool")) {
        if (!castExpr.getType().getCode().equals("_Bool")) {
            return;
        }
    
        // Get sub-expression
        Expr subExpr = castExpr.getSubExpr();
    
        // Remove parent to avoid copying of the subtree
        subExpr.detach();
    
        // CXXFunctionalCastExpr newCastExpr = ClavaNodeFactory.cxxFunctionalCastExpr("bool",
        // castExpr.getCastKind(), castExpr.getExprData(), castExpr.getInfo(), subExpr);
        CXXFunctionalCastExpr newCastExpr = CXXFunctionalCastExpr.newInstance("bool", castExpr, subExpr);
    
        // Expr newSubExpr = newCastExpr.getSubExpr();
        // System.out.println("old sub expr == new sub expr? " + (subExpr == newSubExpr));
    
        queue.replace(node, newCastExpr);
    }
    */

}
