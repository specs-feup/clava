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
import java.util.List;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;

/**
 * Describes an explicit type conversion that uses functional notion but could not be resolved because one or more
 * arguments are type-dependent.
 * 
 * @author JoaoBispo
 *
 */
// public class CXXUnresolvedConstructExpr extends Expr implements Nameable {
public class CXXUnresolvedConstructExpr extends Expr {

    public CXXUnresolvedConstructExpr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    // private final Type typeAsWritten;
    //
    // public CXXUnresolvedConstructExpr(Type typeAsWritten, ExprData exprData, ClavaNodeInfo info, List<Expr>
    // arguments) {
    // this(typeAsWritten, exprData, info, (Collection<? extends ClavaNode>) arguments);
    // }
    //
    // private CXXUnresolvedConstructExpr(Type typeAsWritten, ExprData exprData, ClavaNodeInfo info,
    // Collection<? extends ClavaNode> children) {
    // super(exprData, info, children);
    //
    // this.typeAsWritten = typeAsWritten;
    // }
    //
    // @Override
    // protected ClavaNode copyPrivate() {
    // return new CXXUnresolvedConstructExpr((Type) typeAsWritten.copy(), getExprData(), getInfo(),
    // Collections.emptyList());
    // }

    public List<Expr> getArguments() {
        return getChildren(Expr.class);
    }

    // public Type getTypeAsWritten() {
    // return typeAsWritten;
    // }

    @Override
    public String getCode() {
        // return getTypeAsWritten().getCode(this) + "("
        return get(TYPE).get().getCode(this) + "("
                + getArguments().stream().map(ClavaNode::getCode).collect(Collectors.joining(", ")) + ")";
    }

    /*
    @Override
    public String getName() {
        System.out.println("CXXUNRESOLVED:" + typeAsWritten.getCode());
        return typeAsWritten.getCode();
    }
    
    @Override
    public void setName(String name) {
        SpecsLogs.msgInfo("setName() not supported for node '" + getClass().getSimpleName() + "'");
    }
    */

}
