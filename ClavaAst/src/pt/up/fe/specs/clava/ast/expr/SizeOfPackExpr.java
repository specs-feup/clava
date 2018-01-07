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
import java.util.Collections;
import java.util.List;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.decl.NamedDecl;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.clava.ast.extra.TemplateArgument;

/**
 * Represents an expression that computes the length of a parameter pack.
 * 
 * @author JoaoBispo
 *
 */
public class SizeOfPackExpr extends Expr {

    private final String packId;
    private final String packName;

    private NamedDecl pack;

    public SizeOfPackExpr(String packId, String packName, ExprData exprData, ClavaNodeInfo info,
            List<TemplateArgument> partialArguments) {

        this(packId, packName, exprData, info, (Collection<? extends ClavaNode>) partialArguments);
    }

    private SizeOfPackExpr(String packId, String packName, ExprData exprData, ClavaNodeInfo info,
            Collection<? extends ClavaNode> children) {

        super(exprData, info, children);

        this.packId = packId;
        this.packName = packName;

        pack = null;
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new SizeOfPackExpr(packId, packName, getExprData(), getInfo(), Collections.emptyList());
    }

    public NamedDecl getPack() {
        if (pack == null) {
            // Retrive pack
            pack = (NamedDecl) getApp().getNode(packId);
        }

        return pack;
    }

    public boolean isPartiallySubstituted() {
        // Is it possible to be partially substituted and not have template arguments?
        return hasChildren();
    }

    public List<TemplateArgument> getPartialArguments() {
        return getChildren(TemplateArgument.class);
    }

    @Override
    public String getCode() {
        return "sizeof...(" + packName + ")";
    }
}
