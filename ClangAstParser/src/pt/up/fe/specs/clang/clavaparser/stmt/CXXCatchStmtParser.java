/**
 * Copyright 2017 SPeCS.
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

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.decl.NullDecl;
import pt.up.fe.specs.clava.ast.extra.NullNodeOld;
import pt.up.fe.specs.clava.ast.stmt.CXXCatchStmt;
import pt.up.fe.specs.clava.ast.stmt.CompoundStmt;
import pt.up.fe.specs.util.SpecsCollections;
import pt.up.fe.specs.util.stringparser.StringParser;

public class CXXCatchStmtParser extends AClangNodeParser<CXXCatchStmt> {

    public CXXCatchStmtParser(ClangConverterTable converter) {
        super(converter, false);
    }

    @Override
    protected CXXCatchStmt parse(ClangNode node, StringParser parser) {

        // Children
        // VarDecl or NullNode (1)
        // CompoundStmt (1)

        List<ClavaNode> children = parseChildren(node);

        ClavaNode exceptionNode = SpecsCollections.popSingle(children, ClavaNode.class);
        Decl exceptionDecl = exceptionNode instanceof NullNodeOld ? NullDecl.create(exceptionNode) : (Decl) exceptionNode;

        CompoundStmt catchBody = SpecsCollections.popSingle(children, CompoundStmt.class);

        Preconditions.checkArgument(children.isEmpty());

        return ClavaNodeFactory.cxxCatchStmt(node.getInfo(), exceptionDecl, catchBody);
    }

}
