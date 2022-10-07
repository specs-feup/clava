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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.NamedDecl;
import pt.up.fe.specs.clava.ast.decl.ValueDecl;
import pt.up.fe.specs.clava.language.AccessSpecifier;
import pt.up.fe.specs.util.SpecsCollections;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.collections.SpecsList;

/**
 * Represents members of a structure/union.
 * 
 * @author JoaoBispo
 *
 */
public class MemberExpr extends Expr {

    /// DATAKEYS BEGIN

    public final static DataKey<String> MEMBER_NAME = KeyFactory.string("memberName");

    public final static DataKey<Boolean> IS_ARROW = KeyFactory.bool("isArrow");

    public final static DataKey<ValueDecl> MEMBER_DECL = KeyFactory.object("memberDecl", ValueDecl.class);

    public final static DataKey<NamedDecl> FOUND_DECL = KeyFactory.object("foundDecl", NamedDecl.class);

    public final static DataKey<AccessSpecifier> FOUND_DECL_ACCESS_SPECIFIER = KeyFactory
            .enumeration("foundDeclAccessSpecifier", AccessSpecifier.class);

    /// DATAKEYS END

    public MemberExpr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public String getMemberName() {
        return get(MEMBER_NAME);
    }

    public void setMemberName(String memberName) {
        set(MEMBER_NAME, memberName);
    }

    public boolean isArrow() {
        return get(IS_ARROW);
    }

    public Expr getBase() {
        return getChild(Expr.class, 0);
    }

    public ValueDecl getMemberDecl() {
        return get(MEMBER_DECL);
    }

    @Override
    public String getCode() {

        String baseCode = getBase().getCode();
        boolean addPar = getMemberName().startsWith("operator ") && !baseCode.startsWith("(");

        StringBuilder code = new StringBuilder();

        if (addPar) {
            baseCode = "(" + baseCode + ")";
        }

        code.append(baseCode)
                .append(getSeparatorCode())
                .append(getMemberName());

        return code.toString();
    }

    protected String getSeparatorCode() {
        return isArrow() ? "->" : ".";
    }

    public List<String> getChain() {
        return getChain(this);
    }

    private List<String> getChain(Expr expr) {
        return getExprChain(expr).stream()
                .map(this::extractMemberName)
                .collect(Collectors.toList());
    }

    private String extractMemberName(Expr expr) {
        if (expr instanceof DeclRefExpr) {
            return ((DeclRefExpr) expr).getRefName();
        }

        if (expr instanceof MemberExpr) {
            return ((MemberExpr) expr).getMemberName();
        }

        throw new RuntimeException("Case not implemented for class " + expr.getClass());
    }

    public List<Expr> getExprChain() {
        return getExprChain(this);
    }

    private List<Expr> getExprChain(Expr expr) {
        // TODO: Concatenating the list is inefficient, if chain is big, although this is not expected?

        // DeclRefExpr is the end of the chain
        if (expr instanceof DeclRefExpr) {
            return Arrays.asList(expr);
        }

        if (expr instanceof MemberExpr) {
            MemberExpr memberExpr = (MemberExpr) expr;
            // Get base that interest us
            Expr chainBase = toChainBase(memberExpr.getBase());
            // Could not treat this case, abort
            if (chainBase == null) {
                return Collections.emptyList();
            }

            List<Expr> baseString = getExprChain(chainBase);

            // If empty, something happened
            if (baseString.isEmpty()) {
                return Collections.emptyList();
            }

            return SpecsCollections.concat(baseString, memberExpr);
        }

        SpecsLogs.warn("Expr should be a DeclRefExpr or a MemberExpr, is:" + expr);
        return Collections.emptyList();
    }

    /**
     * 
     * @return either a MemberExpr or a DeclRefExpr
     */
    private Expr toChainBase(Expr candidateBase) {
        if (candidateBase instanceof DeclRefExpr || candidateBase instanceof MemberExpr) {
            return candidateBase;
        }

        if (candidateBase.getNumChildren() == 1) {
            return toChainBase((Expr) candidateBase.getChild(0));
        }

        SpecsLogs.msgLib("MemberExpr.toChainBase: case not defined, " + candidateBase);
        return null;
    }

    @Override
    public SpecsList<DataKey<?>> getSignatureKeys() {
        return super.getSignatureKeys().andAdd(MEMBER_NAME);
    }

}
