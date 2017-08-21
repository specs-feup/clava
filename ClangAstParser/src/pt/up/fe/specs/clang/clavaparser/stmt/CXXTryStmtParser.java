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
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.stmt.CXXCatchStmt;
import pt.up.fe.specs.clava.ast.stmt.CXXTryStmt;
import pt.up.fe.specs.clava.ast.stmt.CompoundStmt;
import pt.up.fe.specs.util.SpecsCollections;
import pt.up.fe.specs.util.stringparser.StringParser;

public class CXXTryStmtParser extends AClangNodeParser<CXXTryStmt> {

    public CXXTryStmtParser(ClangConverterTable converter) {
        super(converter, false);
    }

    @Override
    protected CXXTryStmt parse(ClangNode node, StringParser parser) {

        // Children
        // CompoundStmt (1)
        // CXXCatchStmt (1 or more)

        List<ClavaNode> children = parseChildren(node);

        CompoundStmt body = SpecsCollections.popSingle(children, CompoundStmt.class);
        List<CXXCatchStmt> handlers = SpecsCollections.cast(children, CXXCatchStmt.class);

        return ClavaNodeFactory.cxxTryStmt(node.getInfo(), body, handlers);
    }

}
