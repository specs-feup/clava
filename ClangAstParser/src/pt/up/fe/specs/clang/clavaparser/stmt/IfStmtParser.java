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
import pt.up.fe.specs.clava.ast.extra.NullNodeOld;
import pt.up.fe.specs.clava.ast.stmt.CompoundStmt;
import pt.up.fe.specs.clava.ast.stmt.DeclStmt;
import pt.up.fe.specs.clava.ast.stmt.IfStmt;
import pt.up.fe.specs.util.stringparser.StringParser;

public class IfStmtParser extends AClangNodeParser<IfStmt> {

    public IfStmtParser(ClangConverterTable converter) {
        super(converter, false);
    }

    @Override
    protected IfStmt parse(ClangNode node, StringParser parser) {

        // The first child is optional and represents a condition that declares a variable
        // The second child is the condition without the declaration
        // The third child is the body of the then
        // The fourth child is optional and is the body of the else

        List<ClavaNode> children = parseChildren(node);

        // Always 4 children
        // System.out.println("CHILDREN:" + children);
        checkNumChildren(children, 4);
        // checkNumChildren(children, 5);

        ClavaNode declCondition = children.get(0) instanceof NullNodeOld ? null : children.get(0);
        if (declCondition instanceof DeclStmt) {
            declCondition = declCondition.getChild(0);
        }

        ClavaNode condition = declCondition != null ? declCondition : children.get(1);

        CompoundStmt thenStmt = toCompoundStmt(children.get(2));
        CompoundStmt elseStmt = toCompoundStmt(children.get(3));

        throw new RuntimeException("deprecated");

        // // System.out.println("ELSE NODE NAME:" + children.get(3).getNodeName());
        // // System.out.println("ELSE is NULL? " + (elseStmt == null));
        // if (elseStmt == null) {
        //
        // return ClavaNodeFactory.ifStmt(node.getInfo(), condition, thenStmt);
        // }
        //
        // // System.out.println("IF:" + thenStmt.getCode());
        // // System.out.println("ELSE:" + elseStmt.getCode());
        // return ClavaNodeFactory.ifStmt(node.getInfo(), condition, thenStmt, elseStmt);
    }

}
