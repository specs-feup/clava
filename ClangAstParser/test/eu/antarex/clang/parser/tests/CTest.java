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
import eu.antarex.clang.parser.CTester;
import pt.up.fe.specs.lang.SpecsPlatforms;

public class CTest {

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
        new CTester("boolean.c", "boolean2.c").test();
    }

    @Test
    public void testDecl() {
        new CTester("decl.c").test();
    }

    @Test
    public void testEnum() {
        new CTester("enum.c", "enum.h").test();
    }

    @Test
    public void testOffset() {
        new CTester("offset.c").test();
    }

    @Test
    public void testStruct() {
        new CTester("struct.c").test();
    }

    @Test
    public void testVariadic() {
        new CTester("variadic.c").test();
    }

    @Test
    public void testTimerWindows() {
        if (!SpecsPlatforms.isWindows()) {
            return;
        }

        new CTester("timer_windows.c").test();
    }

    @Test
    public void testArrayFiller() {
        new CTester("array_filler.c").test();
    }

    @Test
    public void testGnuStmtExpr() {
        new CTester("gnu_stmt_expr.c").test();
    }

    @Test
    public void testBuiltinTypesCl() {
        new CTester("builtin_types.cl").addFlags("-x", "cl").test();
    }

    @Test
    public void testCompoundLiteral() {
        new CTester("compound_literal.c").test();
    }

}
