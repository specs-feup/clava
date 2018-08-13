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

package eu.antarex.clang.parser.tests;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import eu.antarex.clang.parser.AClangAstTester;
import eu.antarex.clang.parser.CxxTester;

public class CxxTest {

    @BeforeClass
    public static void setup() throws Exception {
        AClangAstTester.clear();
    }

    @After
    public void tearDown() throws Exception {
        AClangAstTester.clear();
    }

    @Test
    public void testBoolean() {
        new CxxTester("boolean.cpp").test();
    }

    @Test
    public void testCharacter() {
        new CxxTester("character.cpp").test();
    }

    @Test
    public void testComment() {
        new CxxTester("comment.cpp").test();
    }

    @Test
    public void testConstructor() {
        new CxxTester("constructor.cpp", "constructor.h").test();
    }

    @Test
    public void testDestructor() {
        new CxxTester("destructor.cpp").test();
    }

    @Test
    public void testDecl() {
        new CxxTester("decl.cpp").test();
    }

    @Test
    public void testEnum() {
        new CxxTester("enum.cpp", "enum.hpp").test();
    }

    @Test
    public void testExceptions() {
        new CxxTester("exceptions.cpp").test();
    }

    @Test
    public void testFor() {
        new CxxTester("for.cpp").test();
    }

    @Test
    public void testFunctions() {
        new CxxTester("functions.cpp").test();
    }

    @Test
    public void testIfs() {
        new CxxTester("if.cpp").test();
    }

    @Test
    public void testLiterals() {
        new CxxTester("literals.cpp")
                .addFlags("-std=c++14")
                .test();
    }

    @Test
    public void testNamespaceAlias() {
        new CxxTester("namespacealias.cpp").test();
    }

    @Test
    public void testNew() {
        new CxxTester("new.cpp").onePass().test();
    }

    @Test
    public void testOffset() {
        new CxxTester("offset.cpp").test();
    }

    @Test
    public void testMultipleClausesOmpPragmas() {
        new CxxTester("multiple_clauses_omp_pragmas.cpp").test();
    }

    @Test
    public void testOmpPragmas() {
        new CxxTester("omp_pragmas.cpp").test();
    }

    @Test
    public void testPragmas() {
        new CxxTester("pragmas.cpp").test();
    }

    @Test
    public void testQualifiers() {
        new CxxTester("qualifiers.cpp").test();
    }

    @Test
    public void testScope() {
        new CxxTester("scope.cpp")
                .addFlags("-Wno-empty-body")
                .test();
    }

    @Test
    public void testTemplates() {
        new CxxTester("templates.cpp", "templates.h").test();
    }

    @Test
    public void testThrow() {
        new CxxTester("throw.cpp").test();
    }

    @Test
    public void testWhile() {
        new CxxTester("while.cpp").test();
    }

    @Test
    public void testClassTemplate() {
        new CxxTester("class_template.cpp", "class_template.h").test();
    }

    @Test
    public void testBuiltinTypes() {
        new CxxTester("builtin_types.cpp").test();
    }

    @Test
    public void testLambda() {
        new CxxTester("lambda.cpp").addFlags("-std=c++14").test();
    }

    @Test
    public void testDblmax() {
        new CxxTester("dbl_max.cpp").test();
    }

    @Test
    public void testGnuExtensions() {
        new CxxTester("gnu_extensions.cpp").addFlags("-std=gnu++11").test();
    }

    @Test
    public void testSizeof() {
        new CxxTester("sizeof.cpp").test();
    }

    @Test
    public void testIncludes() {
        new CxxTester("includes.cpp", "includes.h", "includes2.h", "includes2.cpp").test();
    }

    @Test
    public void testAttribute() {
        new CxxTester("attribute.cpp").test();
    }

    @Test
    public void testTypes() {
        new CxxTester("types.cpp").test();
    }

}
