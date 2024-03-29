/**
 * Copyright 2022 SPeCS.
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

package pt.up.fe.specs.clava.extra;

import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.Test;

import pt.up.fe.specs.clang.utils.ExpressionParser;
import pt.up.fe.specs.util.SpecsSystem;

public class ClavaMathExtraTest {

    @BeforeClass
    public static void init() {
        SpecsSystem.programStandardInit();
    }

    @Test
    public void test() {

        var expr = "2 + a + b * c";

        var exprNode = new ExpressionParser(Arrays.asList("a", "b", "c")).parse(expr);

        System.out.println("Clava original AST: " + exprNode.toTree());

        ClavaMathExtra.simplify(exprNode);
    }

}
