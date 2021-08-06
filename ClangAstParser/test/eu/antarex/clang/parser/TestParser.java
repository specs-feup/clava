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

package eu.antarex.clang.parser;

import java.util.List;

import org.junit.Test;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.astlineparser.AstParser;

public class TestParser {

    public static final String EXAMPLE = "FunctionDecl 0x3328978 <C:\\Users\\JoaoBispo\\Desktop\\clang_ast\\test.cpp:3:1, line:5:1> line:3:8 foo 'double (void)'\r\n"
            +
            "`-CompoundStmt 0x3328ab0 <col:14, line:5:1>\r\n" +
            "  `-ReturnStmt 0x3328a98 <line:4:2, col:9>\r\n" +
            "    `-FloatingLiteral 0x3328a78 <col:9> 'double' 2.000000e+00\r\n" +
            "FunctionDecl 0x3328b28 <C:\\Users\\JoaoBispo\\Desktop\\clang_ast\\test.cpp:7:1, line:11:1> line:7:5 main 'int (void)'\r\n"
            +
            "`-CompoundStmt 0x3328e60 <col:12, line:11:1>\r\n" +
            "  |-DeclStmt 0x3328ca8 <line:8:2, col:11>\r\n" +
            "  | `-VarDecl 0x3328c20 <col:2, line:1:11> line:8:6 used a 'int' cinit\r\n" +
            "  |   `-IntegerLiteral 0x3328c88 <line:1:11> 'int' 2\r\n" +
            "  `-ReturnStmt 0x3328e48 <line:10:2, col:17>\r\n" +
            "    `-ImplicitCastExpr 0x3328e30 <col:9, col:17> 'int' <FloatingToIntegral>\r\n" +
            "      `-BinaryOperator 0x3328e08 <col:9, col:17> 'double' '+'\r\n" +
            "        |-ImplicitCastExpr 0x3328df0 <col:9> 'double' <IntegralToFloating>\r\n" +
            "        | `-ImplicitCastExpr 0x3328dd8 <col:9> 'int' <LValueToRValue>\r\n" +
            "        |   `-DeclRefExpr 0x3328cc0 <col:9> 'int' lvalue Var 0x3328c20 'a' 'int'\r\n" +
            "        `-CallExpr 0x3328db0 <col:13, col:17> 'double'\r\n" +
            "          `-ImplicitCastExpr 0x3328d98 <col:13> 'double (*)(void)' <FunctionToPointerDecay>\r\n" +
            "            `-DeclRefExpr 0x3328d40 <col:13> 'double (void)' lvalue Function 0x3328978 'foo' 'double (void)'";

    public static final String EXAMPLE2 = "FunctionDecl 0x1397198 <C:\\Users\\Joao Bispo\\Desktop\\clang_ast\\test2.cpp:3:1, line:19:1> line:3:5 main 'int (void)'\n"
            +
            "`-CompoundStmt 0x1410df8 <col:12, line:19:1>\n" +
            "  |-DeclStmt 0x1397360 <line:7:2, col:11>\n" +
            "  | `-VarDecl 0x13972d8 <col:2, col:10> col:6 used a 'int' cinit\n" +
            "  |   `-IntegerLiteral 0x1397340 <col:10> 'int' 2\n" +
            "  |-DeclStmt 0x13973f8 <line:8:2, col:7>\n" +
            "  | `-VarDecl 0x1397390 <col:2, col:6> col:6 used b 'int'\n" +
            "  |-CompoundStmt 0x13976d8 <line:9:2, line:13:2>\n" +
            "  | |-DeclStmt 0x13974b0 <line:10:3, col:12>\n" +
            "  | | `-VarDecl 0x1397428 <col:3, col:11> col:7 used a 'int' cinit\n" +
            "  | |   `-IntegerLiteral 0x1397490 <col:11> 'int' 3\n" +
            "  | `-BinaryOperator 0x1397578 <line:11:9, col:15> 'int' lvalue '='\n" +
            "  |   |-DeclRefExpr 0x13974c8 <col:9> 'int' lvalue Var 0x1397390 'b' 'int'\n" +
            "  |   `-BinaryOperator 0x1397550 <col:13, col:15> 'int' '*'\n" +
            "  |     |-IntegerLiteral 0x13974f0 <col:13> 'int' 3\n" +
            "  |     `-ImplicitCastExpr 0x1397538 <col:15> 'int' <LValueToRValue>\n" +
            "  |       `-DeclRefExpr 0x1397510 <col:15> 'int' lvalue Var 0x1397428 'a' 'int'\n" +
            "  |-BinaryOperator 0x1410cd0 <line:15:2, col:8> 'int' lvalue '='\n" +
            "  | |-DeclRefExpr 0x1397700 <col:2> 'int' lvalue Var 0x1397390 'b' 'int'\n" +
            "  | `-BinaryOperator 0x1397788 <col:6, col:8> 'int' '*'\n" +
            "  |   |-IntegerLiteral 0x1397728 <col:6> 'int' 2\n" +
            "  |   `-ImplicitCastExpr 0x1397770 <col:8> 'int' <LValueToRValue>\n" +
            "  |     `-DeclRefExpr 0x1397748 <col:8> 'int' lvalue Var 0x13972d8 'a' 'int'\n" +
            "  `-ReturnStmt 0x1410de0 <line:18:2, col:9>\n" +
            "    `-ImplicitCastExpr 0x1410dc8 <col:9> 'int' <LValueToRValue>\n" +
            "      `-DeclRefExpr 0x1410da0 <col:9> 'int' lvalue Var 0x1397390 'b' 'int'";

    @Test
    public void test() {
        List<ClangNode> nodes = new AstParser().parse(TestParser.EXAMPLE2);
        System.out.println("NODES:" + nodes);
    }

}
