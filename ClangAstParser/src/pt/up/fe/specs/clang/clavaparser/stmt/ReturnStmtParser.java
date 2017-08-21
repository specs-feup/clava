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

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.stmt.ReturnStmt;
import pt.up.fe.specs.util.stringparser.StringParser;

public class ReturnStmtParser extends AClangNodeParser<ReturnStmt> {

    public ReturnStmtParser(ClangConverterTable converter) {
        super(converter, false);
    }

    @Override
    public ReturnStmt parse(ClangNode node, StringParser parser) {

        List<ClavaNode> children = parseChildren(node);

        Preconditions.checkArgument(children.size() < 2, "Expected 0 or 1 child:\n" + children);

        if (children.size() == 0) {
            return ClavaNodeFactory.returnStmt(info(node));
        }

        Expr retValue = toExpr(children.get(0));

        return ClavaNodeFactory.returnStmt(info(node), retValue);
    }

}
