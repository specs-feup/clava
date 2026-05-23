/**
 * Copyright 2016 SPeCS.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clava.weaver.joinpoints;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodes;
import pt.up.fe.specs.clava.ast.expr.BinaryOperator;
import pt.up.fe.specs.clava.ast.expr.enums.BinaryOperatorKind;
import pt.up.fe.specs.clava.ast.stmt.CXXForRangeStmt;
import pt.up.fe.specs.clava.ast.stmt.CompoundStmt;
import pt.up.fe.specs.clava.ast.stmt.DoStmt;
import pt.up.fe.specs.clava.ast.stmt.ForStmt;
import pt.up.fe.specs.clava.ast.stmt.LiteralStmt;
import pt.up.fe.specs.clava.ast.stmt.LoopStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.clava.ast.stmt.WhileStmt;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.ast.type.enums.BuiltinKind;
import pt.up.fe.specs.clava.transform.loop.LoopAnalysisUtils;
import pt.up.fe.specs.clava.transform.loop.LoopInterchange;
import pt.up.fe.specs.clava.transform.loop.LoopTiling;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AExpression;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ALoop;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AScope;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AStatement;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AVarref;
import pt.up.fe.specs.clava.weaver.enums.LoopKind;
import pt.up.fe.specs.clava.weaver.enums.Relation;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.lazy.ThreadSafeLazy;

public class CxxLoop<Self extends CxxLoop<Self>> extends ALoop<Self> {

    private static final Lazy<Map<Class<? extends LoopStmt>, LoopKind>> LOOP_TYPE = new ThreadSafeLazy<>(
            () -> buildLoopTypeMap());

    private static Map<Class<? extends LoopStmt>, LoopKind> buildLoopTypeMap() {
        HashMap<Class<? extends LoopStmt>, LoopKind> loopTypes = new HashMap<>();

        loopTypes.put(ForStmt.class, LoopKind.FOR);
        loopTypes.put(WhileStmt.class, LoopKind.WHILE);
        loopTypes.put(DoStmt.class, LoopKind.DOWHILE);
        loopTypes.put(CXXForRangeStmt.class, LoopKind.FOREACH);

        return loopTypes;
    }

    private static final Set<BinaryOperatorKind> VALID_RELATION_OP_SETTER = EnumSet.of(BinaryOperatorKind.GT,
            BinaryOperatorKind.GE, BinaryOperatorKind.LT, BinaryOperatorKind.LE);

    public CxxLoop(LoopStmt loop, CxxWeaver weaver) {
        super(loop, weaver);
    }

    @Override
    public LoopStmt getNodeImpl() {
        return (LoopStmt) super.getNodeImpl();
    }

    @Override
    public LoopKind getKindImpl() {
        LoopKind loopType = LOOP_TYPE.get().get(this.getNodeImpl().getClass());

        Objects.requireNonNull(loopType,
                () -> "Could not determine type of node '" + this.getNodeImpl().getClass().getSimpleName() + "'");

        return loopType;
    }

    @Override
    public boolean getIsInnermostImpl() {
        // Loop is innermost if none of its descendants is a loop
        Optional<ClavaNode> anotherLoop = this.getNodeImpl().getDescendantsStream()
                .filter(node -> node instanceof LoopStmt)
                .findFirst();

        return !anotherLoop.isPresent();
    }

    @Override
    public boolean getIsOutermostImpl() {
        // Loop is outermost if none of its ancestors is a loop
        Optional<ClavaNode> anotherLoop = this.getNodeImpl().getAscendantsStream()
                .filter(node -> node instanceof LoopStmt)
                .findFirst();

        return !anotherLoop.isPresent();
    }

    @Override
    public int getNestedLevelImpl() {
        // Go back and count how many Loops there are
        long parentLoops = this.getNodeImpl().getAscendantsStream()
                .filter(node -> node instanceof LoopStmt)
                .count();

        return (int) parentLoops;
    }

    @Override
    public AVarref<?> getControlVarrefImpl() {

        // Only supported for loops of type 'for'
        if (!(this.getNodeImpl() instanceof ForStmt forStmt)) {
            return null;
        }

        var controlVars = LoopAnalysisUtils.getControlVars(forStmt);

        if (controlVars.isEmpty()) {

            ClavaLog.info("Could not find control variable for loop in location: " + this.getNodeImpl().getLocation());

            return null;
        }

        if (controlVars.size() > 1) {

            ClavaLog.info("Found more than one control variable (" + controlVars + ") for loop in location: "
                    + this.getNodeImpl().getLocation());
        }

        return CxxJoinpoints.create(controlVars.get(0), getWeaverEngine(), AVarref.class);

    }

    @Override
    public String getControlVarImpl() {

        var controlVarref = getControlVarrefImpl();

        if (controlVarref == null) {
            return null;
        }

        return controlVarref.getNameImpl();
    }

    @Override
    public AStatement<?> getCondImpl() {
        ClavaNode condition = this.getNodeImpl().getStmtCondition().orElse(null);

        if (condition == null) {
            return null;
        }

        return CxxJoinpoints.create(ClavaNodes.toStmt(condition), getWeaverEngine(), AStatement.class);
    }

    @Override
    public AStatement<?> getStepImpl() {
        if (!(this.getNodeImpl() instanceof ForStmt)) {
            return null;
        }

        Stmt inc = ((ForStmt) this.getNodeImpl()).getInc().orElse(null);
        if (inc == null) {
            return null;

        }

        return CxxJoinpoints.create(inc, getWeaverEngine(), AStatement.class);
    }

    @Override
    public int[] getRankImpl() {
        var rank = this.getNodeImpl().getRank();
        return rank.stream().mapToInt(Integer::intValue).toArray();
    }

    @Override
    public boolean getIsParallelImpl() {
        return this.getNodeImpl().isParallel();
    }

    @Override
    public Integer getIterationsImpl() {
        return this.getNodeImpl().getIterations();
    }

    @Override
    public void setKindImpl(LoopKind kind) {
        if (kind == null) {
            ClavaLog.warning("Unsupported loop kind:" + kind);
            return;
        }

        switch (kind) {
            case WHILE:
                convertToWhile();
                break;
            default:
                throw new RuntimeException("Not implemented: " + kind);
        }

    }

    private void convertToWhile() {
        if (this.getNodeImpl() instanceof WhileStmt) {
            return;
        }

        if (this.getNodeImpl() instanceof ForStmt) {
            Stmt cond = ((ForStmt) this.getNodeImpl()).getCond().orElse(getWeaverEngine().getFactory().nullStmt());
            WhileStmt whileStmt = getWeaverEngine().getFactory().whileStmt(cond, this.getNodeImpl().getBody());

            replaceWith(CxxJoinpoints.create(whileStmt, getWeaverEngine()));
            return;
        }

        throw new RuntimeException("Case not implemented:" + this.getNodeImpl().getClass());
    }

    @Override
    public void setInitImpl(String initCode) {
        if (!(this.getNodeImpl() instanceof ForStmt)) {
            return; // TODO: warn user?
        }

        var suffix = initCode.strip().endsWith(";") ? "" : ";";
        LiteralStmt literalStmt = getFactory().literalStmt(initCode + suffix);

        ((ForStmt) this.getNodeImpl()).setInit(literalStmt);
    }

    @Override
    public void setInitValueImpl(String initCode) {
        if (!(this.getNodeImpl() instanceof ForStmt)) {
            return; // TODO: warn user?
        }

        Type intType = getWeaverEngine().getFactory().builtinType(BuiltinKind.Int);

        ((ForStmt) this.getNodeImpl()).setInitValue(getWeaverEngine().getFactory().literalExpr(initCode, intType));
    }

    @Override
    public void setEndValueImpl(String value) {
        if (!(this.getNodeImpl() instanceof ForStmt)) {
            return; // TODO: warn user?
        }

        Type intType = getWeaverEngine().getFactory().builtinType(BuiltinKind.Int);

        ((ForStmt) this.getNodeImpl()).setConditionValue(getWeaverEngine().getFactory().literalExpr(value, intType));
    }

    @Override
    public void setCondImpl(String condCode) {

        if (!(this.getNodeImpl() instanceof ForStmt)) {
            return; // TODO: warn user?
        }

        var suffix = condCode.strip().endsWith(";") ? "" : ";";
        LiteralStmt literalStmt = getFactory().literalStmt(condCode + suffix);

        ((ForStmt) this.getNodeImpl()).setCond(literalStmt);
    }

    @Override
    public void setStepImpl(String stepCode) {

        if (!(this.getNodeImpl() instanceof ForStmt)) {
            return; // TODO: warn user?
        }

        LiteralStmt literalStmt = getFactory().literalStmt(stepCode);

        ((ForStmt) this.getNodeImpl()).setInc(literalStmt);
    }

    @Override
    public String getInitValueImpl() {

        if (!(this.getNodeImpl() instanceof ForStmt)) {
            ClavaLog.info(
                    "$loop.initValue: Not supported for loops of kind '" + getKindImpl() + "', only 'for' loops.");
            return null;
        }

        String initValue = ((ForStmt) this.getNodeImpl()).getInitValueExpr()
                .map(ClavaNode::getCode)
                .orElse(null);

        if (initValue == null) {
            ClavaLog.info(
                    "$loop.initValue: Could not determine the initial value of the loop. The init statement should be a variable declaration with initialization or assignment.");
        }

        return initValue;
    }

    @Override
    public String getEndValueImpl() {

        if (!(this.getNodeImpl() instanceof ForStmt)) {
            ClavaLog.info("Not supported for loops of kind '" + getKindImpl() + "', only 'for' loops ("
                    + getLocationImpl() + ").");
            return null;
        }

        ForStmt forLoop = (ForStmt) this.getNodeImpl();
        String endValue = forLoop.getConditionValueExpr()
                .map(ClavaNode::getCode)
                .orElse(null);

        if (endValue == null) {
            ClavaLog.debug(
                    "Could not determine the end value of the loop at '" + getLocationImpl()
                            + "'. The condition statement should be a Canonical Loop Form test expression, as defined by the OpenMP standard.");
            return null;
        }

        return endValue;
    }

    @Override
    public Relation getCondRelationImpl() {

        BinaryOperator condOp = getConditionOp();
        if (condOp == null) {
            return null;
        }

        // Relation enum constants use the same uppercase names as BinaryOperatorKind
        var opName = condOp.getOp().name();

        // Get Relation with the same name as the operator
        Relation relation = null;
        try {
            relation = Relation.valueOf(opName);
        } catch (IllegalArgumentException e) {
            var supportedNames = Arrays.stream(Relation.values())
                    .map(Relation::name)
                    .collect(Collectors.joining(", "));
            ClavaLog.warning("Could not map operation with name '" + opName
                    + "' to a Relation. Supported names: " + supportedNames);
        }

        return relation;
    }

    @Override
    public boolean getHasCondRelationImpl() {
        return getConditionOp(false) != null;
    }

    private BinaryOperator getConditionOp() {
        return getConditionOp(true);
    }

    private BinaryOperator getConditionOp(boolean showWarnings) {
        if (!(this.getNodeImpl() instanceof ForStmt)) {
            if (showWarnings) {
                ClavaLog.info(
                        "Not supported for loops of kind '" + getKindImpl() + "', only 'for' loops.");
            }

            return null;
        }

        ForStmt forLoop = (ForStmt) this.getNodeImpl();
        BinaryOperator binOp = forLoop.getCondOperator().orElse(null);

        if (binOp == null) {
            if (showWarnings) {
                ClavaLog.info(
                        "Could not obtain the condition operator for the expression '"
                                + forLoop.getCond().map(ClavaNode::getCode).orElse("") + "'");
            }

            return null;
        }

        return binOp;
    }

    @Override
    public void setCondRelationImpl(Relation operator) {
        BinaryOperatorKind kind = BinaryOperatorKind.getHelper().fromValueTry(operator.toString()).orElse(null);

        if (kind == null) {
            ClavaLog.info("def 'condRelation': Invalid binary operator " + operator);
            return;
        }

        // Verify kind
        if (!VALID_RELATION_OP_SETTER.contains(kind)) {
            ClavaLog.info("def 'condRelation': Invalid relation operator for def " + kind);
            return;
        }

        BinaryOperator condOp = getConditionOp();
        if (condOp == null) {
            return;
        }

        condOp.set(BinaryOperator.OP, kind);
    }

    @Override
    public String getIdImpl() {
        return this.getNodeImpl().getLoopId();
    }

    @Override
    public void interchangeImpl(ALoop<?> otherLoop) {

        Optional<LoopInterchange> loopInterchange = LoopInterchange.newInstance(this.getNodeImpl(), (LoopStmt) otherLoop.getNodeImpl());
        if (!loopInterchange.isPresent()) {
            ClavaLog.info("Could not interchange loops");
            return;
        }

        loopInterchange.get().apply();
    }

    @Override
    public boolean getIsInterchangeableImpl(ALoop<?> otherLoop) {
        return LoopInterchange.test(this.getNodeImpl(), (LoopStmt) otherLoop.getNodeImpl());
    }

    @Override
    public AStatement<?> tileImpl(String blockSize, AStatement<?> reference, boolean useTernary) {

        LoopTiling loopTiling = new LoopTiling(getWeaverEngine().getContex());

        boolean success = loopTiling.apply(this.getNodeImpl(), (Stmt) reference.getNodeImpl(),
                blockSize.toString(), useTernary);

        if (!success) {
            ClavaLog.info("Could not tile the loop: " + this.getNodeImpl().getLocation());
        }

        if (loopTiling.getLastReferenceStmt() == null) {
            return reference;
        }

        return CxxJoinpoints.create(loopTiling.getLastReferenceStmt(), getWeaverEngine(), AStatement.class);

    }

    @Override
    public void setIsParallelImpl(boolean isParallel) {
        this.getNodeImpl().setParallel(isParallel);
    }

    @Override
    public AExpression<?> getIterationsExprImpl() {
        if (!(this.getNodeImpl() instanceof ForStmt)) {
            ClavaLog.warning(
                    "Not supported for loops of kind '" + getKindImpl() + "', only 'for' loops.");
            return null;
        }

        return ((ForStmt) this.getNodeImpl()).getIterationsExpr()
                .map(expr -> CxxJoinpoints.create(expr,
                        getWeaverEngine(), AExpression.class))
                .orElse(null);
    }

    @Override
    public String getStepValueImpl() {
        if (!(this.getNodeImpl() instanceof ForStmt)) {
            ClavaLog.warning(
                    "Not supported for loops of kind '" + getKindImpl() + "', only 'for' loops.");
            return null;
        }

        String stepValue = ((ForStmt) this.getNodeImpl()).getStepValueExpr()
                .map(ClavaNode::getCode)
                .orElse(null);

        if (stepValue == null) {
            ClavaLog.warning(
                    "Could not determine the step value of the loop. The step statement should be a Canonical Loop Form increment expression, as defined by the OpenMP standard.");
        }

        return stepValue;
    }

    @Override
    public AStatement<?> getInitImpl() {

        if (this.getNodeImpl() instanceof ForStmt) {
            return ((ForStmt) this.getNodeImpl()).getInit()
                    .map(init -> CxxJoinpoints.create(init,
                            getWeaverEngine(), AStatement.class))
                    .orElse(null);
        }

        // If range stmt, return begin
        if (this.getNodeImpl() instanceof CXXForRangeStmt) {
            return ((CXXForRangeStmt) this.getNodeImpl()).getBegin()
                    .map(init -> CxxJoinpoints.create(init,
                            getWeaverEngine(), AStatement.class))
                    .orElse(null);
        }

        return null;

    }

    @Override
    public AScope<?> getBodyImpl() {
        return CxxJoinpoints.create(this.getNodeImpl().getBody(), getWeaverEngine(), AScope.class);
    }

    @Override
    public void setBodyImpl(AScope<?> body) {
        this.getNodeImpl().setBody((CompoundStmt) body.getNodeImpl());
    }

}
