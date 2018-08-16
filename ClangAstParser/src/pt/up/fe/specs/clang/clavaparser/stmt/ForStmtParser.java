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
import pt.up.fe.specs.clava.ast.extra.NullNodeOld;
import pt.up.fe.specs.clava.ast.stmt.CompoundStmt;
import pt.up.fe.specs.clava.ast.stmt.ForStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.util.stringparser.StringParser;

public class ForStmtParser extends AClangNodeParser<ForStmt> {

    public ForStmtParser(ClangConverterTable converter) {
        super(converter, false);
    }

    @Override
    protected ForStmt parse(ClangNode node, StringParser parser) {

        List<ClavaNode> children = parseChildren(node);

        // ForStmt always has 5 children
        // All nodes except for the body are optional, and can be null
        checkNumChildren(children, 5);

        // 1st child is initialization
        Stmt init = toStmt(children.get(0));

        // 2nd child has always appeared as null node until now
        Preconditions.checkArgument(children.get(1) instanceof NullNodeOld, "Check what to do when this is not NullNode");

        // 3rd child is the condition
        Stmt cond = toStmt(children.get(2));

        // 4th child is the increment
        Stmt inc = toStmt(children.get(3));

        // 5th child is the body
        CompoundStmt body = toCompoundStmt(children.get(4));

        return ClavaNodeFactory.forStmt(node.getInfo(), init, cond, inc, body);
    }

}
