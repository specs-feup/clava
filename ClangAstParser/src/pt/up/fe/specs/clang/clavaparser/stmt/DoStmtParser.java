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

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.stmt.CompoundStmt;
import pt.up.fe.specs.clava.ast.stmt.DoStmt;
import pt.up.fe.specs.util.stringparser.StringParser;

public class DoStmtParser extends AClangNodeParser<DoStmt> {

    public DoStmtParser(ClangConverterTable converter) {
        super(converter, false);
    }

    @Override
    protected DoStmt parse(ClangNode node, StringParser parser) {

        List<ClavaNode> children = parseChildren(node);
        checkNumChildren(children, 2);

        CompoundStmt body = toCompoundStmt(children.get(0));
        Expr condition = toExpr(children.get(1));
        throw new RuntimeException("deprecated");
        // return ClavaNodeFactory.doStmt(node.getInfo(), body, condition);
    }

}
