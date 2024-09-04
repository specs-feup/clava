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

package eu.antarex.clang.parser.testers;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import eu.antarex.clang.parser.AClangAstTester;
import eu.antarex.clang.parser.ClangTester;

public class ClangCTester {

    @BeforeClass
    public static void setup() throws Exception {
        AClangAstTester.clear();
    }

    @After
    public void tearDown() throws Exception {
        AClangAstTester.clear();
    }

    @Test
    public void testCAttr() {
        new ClangTester("c/ast-dump-c-attr.c")
                .addFlags("-fdouble-square-bracket-attributes", "-Wno-deprecated-declarations")
                .test();
    }

    @Test
    public void testExpr() {
        new ClangTester("c/ast-dump-expr.c").addFlags("-Wno-unused-value", "-std=gnu11").test();
    }

    @Test
    public void testRecords() {
        new ClangTester("c/ast-dump-records.c").test();
    }

    @Test
    public void testStmt() {
        new ClangTester("c/ast-dump-stmt.c").addFlags("-std=gnu11").test();
    }

    @Test
    public void testBoolAsCBool() {
        new ClangTester("c/ast-print-bool.c").addFlags("-DDEF_BOOL_CBOOL").test();
    }

    @Test
    public void testBoolAsInt() {
        new ClangTester("c/ast-print-bool.c").addFlags("-DDEF_BOOL_INT").test();
    }

    @Test
    public void testEnumDecl() {
        new ClangTester("c/ast-print-enum-decl.c").test();
    }

    @Test
    public void testRecordDeclStruct() {
        new ClangTester("c/ast-print-record-decl.c").addFlags("-DKW=struct", "-DBASES=").test();
    }

    @Test
    public void testRecordDeclUnion() {
        new ClangTester("c/ast-print-record-decl.c").addFlags("-DKW=union", "-DBASES=").test();
    }

    @Test
    public void testAttrTarget() {
        new ClangTester("c/attr-target-ast.c").test();
    }

    @Test
    public void testCCasts() {
        new ClangTester("c/c-casts.c").test();
    }

    @Test
    public void testFixedPointToString() {
        new ClangTester("c/fixed_point_to_string.c").addFlags("-ffixed-point").test();
    }

    @Test
    public void testFixedPoint() {
        new ClangTester("c/fixed_point.c").addFlags("-ffixed-point").test();
    }

    @Test
    public void testImplicitCast() {
        new ClangTester("c/implicit-cast-dump.c").test();
    }

    @Test
    public void testMultistepExplicitCast() {
        new ClangTester("c/multistep-explicit-cast.c").test();
    }

    @Test
    public void testUnorderedComparePromote() {
        new ClangTester("c/rdr6094103-unordered-compare-promote.c").test();
    }

    @Test
    public void testVariadicPromotion() {
        new ClangTester("c/variadic-promotion.c").test();
    }

}
