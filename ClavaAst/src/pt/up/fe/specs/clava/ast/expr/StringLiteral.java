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

package pt.up.fe.specs.clava.ast.expr;

import java.util.Collection;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;

public class StringLiteral extends Literal {

    /// DATAKEY BEGIN

    // public final static DataKey<String> STRING = KeyFactory.string("string");

    /// DATAKEY END

    // private final String string;

    public StringLiteral(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    // public StringLiteral(String string, ExprData exprData, ClavaNodeInfo info) {
    // // According to CPP reference, string literals are lvalues
    // // http://en.cppreference.com/w/cpp/language/value_category
    // super(exprData, info, Collections.emptyList());
    //
    // this.string = string;
    // }

    // @Override
    // protected ClavaNode copyPrivate() {
    // return new StringLiteral(string, getExprData(), getInfo());
    // }

    public String getString() {
        return getLiteral();
        // return string;
    }

    // public String getStringContents() {
    // // return get(STRING);
    // String literalString = getString();
    // return literalString.substring(1, literalString.length() - 1);
    // String stringContents = string;
    // if (stringContents.startsWith("\"")) {
    // stringContents = stringContents.substring(1);
    // }
    // if (stringContents.endsWith("\"")) {
    //
    // }
    //
    // return string;
    // }

    @Override
    public String getCode() {
        return getLiteral();
        // System.out.println("STRING CONTENTS:" + getStringContents());
        // System.out.println("LITERAL:" + getLiteral());
        // System.out.println("ESCAPED:" + SpecsStrings.escapeJson(getStringContents()));
        // // return getString();
        // return "\"" + SpecsStrings.escapeJson(getStringContents()) + "\"";
    }

    // @Override
    // public String toContentString() {
    // return ClavaNode.toContentString(super.toContentString(), "string:" + string);
    // // return super.toContentString() + "string:" + string;
    // }

    // @Override
    // public String getLiteral() {
    // return string;
    // }

    // StringKind getKind()

}
