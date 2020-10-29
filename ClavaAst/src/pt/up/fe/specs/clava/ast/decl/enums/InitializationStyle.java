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

package pt.up.fe.specs.clava.ast.decl.enums;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.expr.CXXConstructExpr;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.util.enums.EnumHelperWithValue;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.providers.StringProvider;

public enum InitializationStyle implements StringProvider {
    NO_INIT,
    CINIT, // C-style initialization with assignment
    CALL_INIT("callinit"), // Call-style initialization (C++98)
    LIST_INIT("listinit"); // Direct list-initialization (C++11)

    private static Lazy<EnumHelperWithValue<InitializationStyle>> ENUM_HELPER = EnumHelperWithValue
            .newLazyHelperWithValue(InitializationStyle.class, NO_INIT);

    public static EnumHelperWithValue<InitializationStyle> getHelper() {
        return ENUM_HELPER.get();
    }

    private final String string;

    private InitializationStyle() {
        this.string = name().toLowerCase();
    }

    private InitializationStyle(String string) {
        this.string = string;
    }

    @Override
    public String getString() {
        return string;
    }

    public String getCode(VarDecl node) {
        // Write code according to type of declaration
        switch (this) {
        case CINIT:
            return cinitCode(node);
        case NO_INIT:
            return "";
        case CALL_INIT:
            return callInitCode(node);
        case LIST_INIT:
            return listInitCode(node);
        default:
            throw new RuntimeException("Case not defined:" + this);
        }
    }

    /**
     * Example: footype foo = <child_code>;
     * 
     * @return
     */
    private String cinitCode(VarDecl node) {
        String prefix = " = ";

        // return " = " + getChild(0).getCode();

        Expr initExpr = node.getInit().get();

        return prefix + initExpr.getCode();
    }

    private String callInitCode(VarDecl node) {
        Preconditions.checkArgument(node.getNumChildren() == 1, "Expected one child");
        ClavaNode init = node.getChild(0);

        // If CXXConstructorExpr, use args code only
        if (init instanceof CXXConstructExpr) {
            return ((CXXConstructExpr) init).getArgsCode();
        }

        // If expression, just return the code
        Preconditions.checkArgument(init instanceof Expr,
                "Expected an Expr, got '" + init.getClass().getSimpleName() + "'");

        return "(" + init.getCode() + ")";
    }

    private String listInitCode(VarDecl node) {
        // Must be present
        Expr initList = node.getInit().get();

        // SpecsCheck.checkArgument(initList instanceof InitListExpr,
        // () -> "Expected argument to be an instance of " + InitListExpr.class + ", it is a "
        // + initList.getClass());

        return initList.getCode();
        // return getInit().map(ClavaNode::getCode)
        // .orElseThrow(() -> new RuntimeException());
    }

}