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
import pt.up.fe.specs.clava.ast.extra.NullNode;
import pt.up.fe.specs.clava.ast.stmt.CompoundStmt;
import pt.up.fe.specs.clava.ast.stmt.DeclStmt;
import pt.up.fe.specs.clava.ast.stmt.WhileStmt;
import pt.up.fe.specs.util.stringparser.StringParser;

public class WhileStmtParser extends AClangNodeParser<WhileStmt> {

    public WhileStmtParser(ClangConverterTable converter) {
        super(converter, false);
    }

    @Override
    protected WhileStmt parse(ClangNode node, StringParser parser) {

        List<ClavaNode> children = parseChildren(node);

        // The first child is optional and represents a condition that declares a variable
        // The second child is the condition without the declaration
        // The third child is the body of the while

        // Always 3 children
        checkNumChildren(children, 3);

        ClavaNode declCondition = children.get(0) instanceof NullNode ? null : children.get(0);
        if (declCondition instanceof DeclStmt) {
            declCondition = declCondition.getChild(0);
        }

        ClavaNode condition = declCondition != null ? declCondition : children.get(1);

        CompoundStmt thenStmt = toCompoundStmt(children.get(2));

        return ClavaNodeFactory.whileStmt(node.getInfo(), condition, thenStmt);
    }

}
