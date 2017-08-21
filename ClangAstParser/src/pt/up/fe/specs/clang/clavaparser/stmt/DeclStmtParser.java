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

package pt.up.fe.specs.clang.clavaparser.stmt;

import java.util.List;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.decl.NamedDecl;
import pt.up.fe.specs.clava.ast.decl.RecordDecl;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.stmt.DeclStmt;
import pt.up.fe.specs.util.SpecsCollections;
import pt.up.fe.specs.util.stringparser.StringParser;

public class DeclStmtParser extends AClangNodeParser<DeclStmt> {

    public DeclStmtParser(ClangConverterTable converter) {
        super(converter, false);
    }

    @Override
    protected DeclStmt parse(ClangNode node, StringParser parser) {
        List<ClavaNode> children = parseChildren(node);

        ClavaNode firstChild = children.get(0);

        // Check if first child is a RecordDecl
        if (firstChild instanceof RecordDecl) {
            RecordDecl record = (RecordDecl) firstChild;
            List<VarDecl> varDecls = SpecsCollections.cast(SpecsCollections.subList(children, 1), VarDecl.class);

            return ClavaNodeFactory.declStmt(info(node), record, varDecls);
        }
        // if (children.size() == 1) {
        // ClavaNode child = children.get(0);
        // if (children.get(0) instanceof RecordDecl) {
        // LoggingUtils.msgInfo("RECORD DECL IN DECL STMT:" + node.getLocation());
        // System.out.println("CHILDREN:\n" + children);
        // }
        // }

        // Check if all children are VarDecls
        long numVarDecls = children.stream()
                .filter(child -> child instanceof NamedDecl)
                .count();

        if (numVarDecls == children.size()) {
            List<NamedDecl> decls = SpecsCollections.cast(children, NamedDecl.class);
            return ClavaNodeFactory.declStmt(info(node), decls);
        }

        throw new RuntimeException("Case not defined for the children:\n" + children);

        // List<VarDecl> decls = children.stream()
        // // .filter(child -> !(child instanceof NullNode))
        // .map(child -> (VarDecl) toDecl(child))
        // .collect(Collectors.toList());

        // return ClavaNodeFactory.declStmt(info(node), decls);
    }

}
