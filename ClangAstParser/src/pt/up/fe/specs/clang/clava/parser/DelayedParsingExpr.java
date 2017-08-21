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

package pt.up.fe.specs.clang.clava.parser;

import java.util.Collections;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clang.CppParsing;
import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;

/**
 * Represents an Expr that has been found during Type Parsing phase, and can only be parsed after Type Parsing finishes.
 * 
 * @author JoaoBispo
 *
 */
public class DelayedParsingExpr extends Expr implements DelayedParsing {

    private final ClangNode clangNode;

    public DelayedParsingExpr(ClangNode clangNode) {
        super(ExprData.empty(), clangNode.getInfo(), Collections.emptyList());

        this.clangNode = clangNode;

        Preconditions.checkArgument(CppParsing.isExprNodeName(clangNode.getName()),
                "Expected node to be an Expr, it is '" + clangNode.getName() + "'");
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new DelayedParsingExpr(clangNode);
    }

    @Override
    public ClangNode getClangNode() {
        return clangNode;
    }

}
