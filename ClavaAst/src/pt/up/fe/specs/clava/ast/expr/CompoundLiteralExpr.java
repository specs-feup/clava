/**
 * Copyright 2018 SPeCS.
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

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;

public class CompoundLiteralExpr extends Literal {

    /// DATAKEY BEGIN

    public final static DataKey<Boolean> IS_FILE_SCOPE = KeyFactory.bool("isFileScope");

    /// DATAKEY END

    public CompoundLiteralExpr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public InitListExpr getInitializer() {
        return getChild(InitListExpr.class, 0);
    }

    // TODO: Implement getCode() after other nodes are implemented

    @Override
    public String getCode() {
        // System.out.println("IS MACRO:" + get(ClavaNode.IS_MACRO));
        // System.out.println("IS INITIALIZER MACRO:" + getInitializer().get(ClavaNode.IS_MACRO));
        // System.out.println("CODE:" + getLiteral());

        // if (get(ClavaNode.IS_MACRO)) {
        // return "(" + getTypeCode() + ")" + getInitializer().getCode();
        // }

        // System.out.println("INITIALIZER:" + getInitializer().toTree());
        // System.out.println("INITIALIZER CHILDREN:" + getInitializer().getChildren().size());
        // System.out.println("INITIALIZER FILLER:" + getInitializer().get(InitListExpr.ARRAY_FILLER));
        //
        // System.out.println("INITIALIZER CODE:" + getInitializer().getCode());
        // return "(" + getTypeCode() + ")" + getInitializer().getCode();
        // System.out.println("COMPOUND CODE: " + getLiteral());
        // System.out.println("COMPOUND CODE V2: " + "(" + getTypeCode() + ")" + getChild(0).getCode());
        // return getLiteral();

        return "(" + getTypeCode() + ")" + getChild(0).getCode();

        // System.out.println("TYPE:" + getTypeCode());
        // System.out.println("COMPOUND AST:" + toTree());
        // System.out.println("INIT LIST CODE:" + getChild(0).getCode());
        // return getChild(0).getCode();

    }

}
