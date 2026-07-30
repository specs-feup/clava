/**
 * Copyright 2026 SPeCS.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clang.parser.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import pt.up.fe.specs.clang.codeparser.CodeParser;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.decl.enums.InitializationStyle;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsSystem;

public class InitializationStyleTest {

    @TempDir
    Path tempFolder;

    @Test
    public void parenthesizedListInitializationKeepsAllArguments() {
        SpecsSystem.programStandardInit();

        File sourceFile = SpecsIo.resourceCopy("cxx/paren_list_initialization.cpp", tempFolder.toFile(), false, true);
        App app = CodeParser.newInstance().parse(List.of(sourceFile), List.of("-std=c++2a"));

        VarDecl point = app.getDescendants(VarDecl.class).stream()
                .filter(varDecl -> varDecl.getDeclName().equals("point"))
                .findFirst()
                .orElseThrow();

        assertEquals("Point point(1, 2)", point.getCode());
    }

    @Test
    public void javascriptInitializationStyleNamesRemainCompatible() {
        assertEquals("callinit", InitializationStyle.CALL_INIT.getString());
        assertEquals("listinit", InitializationStyle.LIST_INIT.getString());
    }
}
