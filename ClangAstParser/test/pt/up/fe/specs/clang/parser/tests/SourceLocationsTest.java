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
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import pt.up.fe.specs.clang.codeparser.CodeParser;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.SourceRange;
import pt.up.fe.specs.clava.ast.decl.TemplateTypeParmDecl;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsSystem;

public class SourceLocationsTest {

    private static final String RESOURCE = "cxx/source_locations.cpp";

    @TempDir
    Path tempFolder;

    @Test
    public void sourceLocationsUseRealFileCoordinates() {
        SpecsSystem.programStandardInit();

        File sourceFile = SpecsIo.resourceCopy(RESOURCE, tempFolder.toFile(), false, true);
        App app = CodeParser.newInstance().parse(List.of(sourceFile), List.of("-std=c++11"));

        VarDecl ordinary = find(app.getDescendants(VarDecl.class), varDecl -> varDecl.getDeclName().equals("ordinary"));
        assertRange(ordinary, false, 7, 1, 7, 16);

        VarDecl macro = find(app.getDescendants(VarDecl.class), varDecl -> varDecl.getDeclName().equals("macro_value"));
        assertRange(macro, true, 6, 1, 6, 24);
        assertRange(macro.getInit().orElseThrow(), true, 6, 1, 6, 24);

        VarDecl pastedReference = find(app.getDescendants(VarDecl.class),
                varDecl -> varDecl.getDeclName().equals("pasted_reference"));
        assertRange(pastedReference.getInit().orElseThrow(), true, 9, 24, 9, 36);

        TemplateTypeParmDecl templateParameter = find(app.getDescendantsAndFields(TemplateTypeParmDecl.class),
                parameter -> parameter.getDeclName().equals("BinaryType"));
        assertRange(templateParameter, false, 16, 11, 16, 54);
    }

    private static <T> T find(List<T> nodes, Predicate<T> predicate) {
        return nodes.stream()
                .filter(predicate)
                .findFirst()
                .orElseThrow();
    }

    private static void assertRange(ClavaNode node, boolean isMacro, int startLine, int startColumn, int endLine,
            int endColumn) {

        SourceRange location = node.getLocation();

        assertEquals("source_locations.cpp", location.getFilename());
        assertEquals("source_locations.cpp", Path.of(location.getStartFilepath()).getFileName().toString());
        assertEquals("source_locations.cpp", Path.of(location.getEndFilepath()).getFileName().toString());
        assertEquals(startLine, location.getStartLine());
        assertEquals(startColumn, location.getStartCol());
        assertEquals(endLine, location.getEndLine());
        assertEquals(endColumn, location.getEndCol());
        assertEquals(isMacro, node.get(ClavaNode.IS_MACRO));
        assertFalse(location.toString().contains("<scratch space>"));
    }
}
