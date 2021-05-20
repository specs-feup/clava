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

package pt.up.fe.specs.clava.ast.stmt;

import java.util.Collection;
import java.util.Optional;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodes;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.utils.StmtWithCondition;

public class IfStmt extends Stmt implements StmtWithCondition {

    public IfStmt(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);

    }

    // public IfStmt(ClavaNodeInfo info, ClavaNode condition, CompoundStmt thenStmt) {
    // this(info, Arrays.asList(condition, thenStmt));
    // }
    //
    // public IfStmt(ClavaNodeInfo info, ClavaNode condition, CompoundStmt thenStmt, CompoundStmt elseStmt) {
    // this(info, Arrays.asList(condition, thenStmt, elseStmt));
    // }
    //
    // private IfStmt(ClavaNodeInfo info, Collection<? extends ClavaNode> children) {
    // super(info, children);
    // }
    //
    // @Override
    // protected ClavaNode copyPrivate() {
    // return new IfStmt(getInfo(), Collections.emptyList());
    // }

    // public boolean hasDeclCondition() {
    // return getChild(0) instanceof DeclStmt;
    // }

    // private int getConditionIndex() {
    // return hasDeclCondition() ? 1 : 0;
    // }

    public Optional<VarDecl> getDeclCondition() {
        return getOptionalChild(VarDecl.class, 0);
    }

    // public ClavaNode getCondition() {
    public Expr getCondition() {

        // return getChild(0);
        // return ClavaNodes.getChild(this, 0);
        // return ClavaNodes.getChild(this, getConditionIndex());
        return getChild(Expr.class, 1);

    }

    public void setCondition(Expr condition) {
        setChild(1, condition);
    }

    /**
     * The 'then' block.
     * 
     * <p>
     * In C++ the IfStmt can have no then (e.g., "if(true);").
     * 
     * @return
     */
    public Optional<CompoundStmt> getThen() {
        return getOptionalChild(CompoundStmt.class, 2);
    }

    public void setThen(Stmt thenBody) {
        var thenNode = thenBody != null ? ClavaNodes.toCompoundStmt(thenBody) : getFactory().nullStmt();
        setChild(2, thenNode);
    }

    public Optional<CompoundStmt> getElse() {
        return getOptionalChild(CompoundStmt.class, 3);
        // Optional<Stmt> stmt = getOptionalChild(Stmt.class, 3);
        // if (!stmt.isPresent()) {
        // return Optional.empty();
        // }
        //
        // if (stmt.get() instanceof CompoundStmt) {
        // return Optional.of((CompoundStmt) stmt.get());
        // }
        // System.out.println("IfStmt: Else not being normalized as a CompoundStmt: " + stmt.get());
        // return stmt.map(ClavaNodes::toCompoundStmt);
    }

    public void setElse(Stmt elseBody) {
        var elseNode = elseBody != null ? ClavaNodes.toCompoundStmt(elseBody) : getFactory().nullStmt();
        setChild(3, elseNode);
    }

    @Override
    public String getCode() {

        StringBuilder code = new StringBuilder();

        String conditionCode = getDeclCondition().map(VarDecl::getCode).orElse(getCondition().getCode());

        String thenCode = getThen().map(this::getThenCode).orElse(";\n");

        // If then does not end with newline, add one
        thenCode = thenCode.endsWith(ln()) ? thenCode : thenCode + ln();

        code.append("if(").append(conditionCode).append(")").append(thenCode);
        // System.out.println("THEN: '" + thenCode + "'");
        // CompoundStmt takes care of prefixing a space before the statement code
        getElse().ifPresent(elseStmt -> code.append(getElseCode(elseStmt)));

        return code.toString();
    }

    private String getThenCode(CompoundStmt thenStmt) {
        boolean isNaked = thenStmt.isNaked();
        var code = thenStmt.getCode(isNaked);

        if (isNaked) {
            code = " " + code.stripLeading();
        }

        return code;
    }

    private String getElseCode(CompoundStmt elseStmt) {
        var elseCode = elseStmt.getCode();

        // If else stmt is a naked block with a single if, remove whitespace in the beginning?

        // if (elseStmt.isNaked() && elseStmt.getStatements().size() == 1
        // && elseStmt.getStatements().get(0) instanceof IfStmt) {
        if (elseStmt.isNaked()) {
            elseCode = " " + elseCode.stripLeading();
        }

        // System.out.println("ELSE CODE:'" + elseStmt.getCode(elseS) + "'");

        // String prefixNewline = elseStmt.isNaked() ? "\n" : "";
        // boolean inline = elseStmt.isNaked();
        return "else" + elseCode;
    }

    /*
    private String getThenCode(CompoundStmt stmt) {
        if (stmt instanceof CompoundStmt) {
            return stmt.getCode();
        }
    
        // Not a compound statement, manually create braces
        return "{" + ln() + getTab() + stmt.getCode() + ln() + "}" + ln();
    }
    */

    @Override
    public Optional<ClavaNode> getStmtCondition() {
        return getDeclCondition().map(ClavaNode.class::cast);
        // return Optional.ofNullable(hasDeclCondition() ? getChild(0) : null);
        // return Optional.of(getCondition());
    }

    /*
    @Override
    public List<Stmt> toStatements() {
    
        List<Stmt> then = getThen().toStatements();
    
        if (!getElse().isPresent()) {
            return then;
        }
    
        return SpecsCollections.concat(then, getElse().get().toStatements());
    }
    */

}
