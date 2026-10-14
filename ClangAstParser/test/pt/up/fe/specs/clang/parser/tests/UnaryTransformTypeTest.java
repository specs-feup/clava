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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import pt.up.fe.specs.clang.codeparser.CodeParser;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.ast.type.UnaryTransformType;
import pt.up.fe.specs.clava.ast.type.enums.UnaryTransformTypeKind;
import pt.up.fe.specs.util.SpecsSystem;

public class UnaryTransformTypeTest {

    private static final String SOURCE = """
            template <typename T>
            struct Dependent {
                using type = __underlying_type(T);
            };

            enum Resolved { Value };
            using ResolvedType = __underlying_type(Resolved);
            """;

    @TempDir
    Path tempFolder;

    @Test
    public void dependentTransformsMayHaveNoUnderlyingType() throws IOException {
        SpecsSystem.programStandardInit();

        File sourceFile = tempFolder.resolve("unary_transform_type.cpp").toFile();
        Files.writeString(sourceFile.toPath(), SOURCE);

        App app = CodeParser.newInstance().parse(List.of(sourceFile), List.of("-std=c++11"));

        List<UnaryTransformType> transforms = app.getDescendantsAndFields(UnaryTransformType.class).stream()
                .filter(transform -> transform.get(UnaryTransformType.KIND) == UnaryTransformTypeKind.EnumUnderlyingType)
                .toList();

        assertEquals(2, transforms.size());

        UnaryTransformType dependentTransform = transforms.stream()
                .filter(transform -> transform.getUnderlyingType().isEmpty())
                .findFirst()
                .orElseThrow();

        assertNotNull(dependentTransform.getBaseType());
        assertEquals(1, dependentTransform.getNodeFields().size());
        assertTrue(dependentTransform.getNodeFields().contains(dependentTransform.getBaseType()));

        UnaryTransformType resolvedTransform = transforms.stream()
                .filter(transform -> transform.getUnderlyingType().isPresent())
                .findFirst()
                .orElseThrow();

        Type resolvedUnderlyingType = resolvedTransform.getUnderlyingType().orElseThrow();
        assertNotNull(resolvedTransform.getBaseType());
        assertTrue(resolvedTransform.getNodeFields().contains(resolvedTransform.getBaseType()));
        assertTrue(resolvedTransform.getNodeFields().contains(resolvedUnderlyingType));
    }
}
