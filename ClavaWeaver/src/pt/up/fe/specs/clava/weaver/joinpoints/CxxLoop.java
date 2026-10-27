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

import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodes;
import pt.up.fe.specs.clava.ast.expr.BinaryOperator;
import pt.up.fe.specs.clava.ast.expr.enums.BinaryOperatorKind;
import pt.up.fe.specs.clava.ast.stmt.*;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.ast.type.enums.BuiltinKind;
import pt.up.fe.specs.clava.transform.loop.LoopAnalysisUtils;
import pt.up.fe.specs.clava.transform.loop.LoopInterchange;
import pt.up.fe.specs.clava.transform.loop.LoopTiling;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.*;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.enums.ALoopKindEnum;
import pt.up.fe.specs.clava.weaver.enums.Relation;
import pt.up.fe.specs.util.SpecsEnums;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.lazy.ThreadSafeLazy;

import java.util.*;

public class CxxLoop extends ALoop {

    private static final Lazy<Map<Class<? extends LoopStmt>, ALoopKindEnum>> LOOP_TYPE = new ThreadSafeLazy<>(
            () -> buildLoopTypeMap());

    private static Map<Class<? extends LoopStmt>, ALoopKindEnum> buildLoopTypeMap() {
        HashMap<Class<? extends LoopStmt>, ALoopKindEnum> loopTypes = new HashMap<>();

        loopTypes.put(ForStmt.class, ALoopKindEnum.FOR);
        loopTypes.put(WhileStmt.class, ALoopKindEnum.WHILE);
        loopTypes.put(DoStmt.class, ALoopKindEnum.DOWHILE);
        loopTypes.put(CXXForRangeStmt.class, ALoopKindEnum.FOREACH);

        return loopTypes;
    }

    private static final Set<BinaryOperatorKind> VALID_RELATION_OP_SETTER = EnumSet.of(BinaryOperatorKind.GT,
            BinaryOperatorKind.GE, BinaryOperatorKind.LT, BinaryOperatorKind.LE);

    private final LoopStmt loop;

    public CxxLoop(LoopStmt loop, CxxWeaver weaver) {
        super(new CxxStatement(loop, weaver), weaver);

        this.loop = loop;
    }

    @Override
    public String getKindImpl() {
        ALoopKindEnum loopType = LOOP_TYPE.get().get(loop.getClass());

        Objects.requireNonNull(loopType,
                () -> "Could not determine type of node '" + loop.getClass().getSimpleName() + "'");

        return loopType.name().toLowerCase();
    }

    @Override
    public Boolean getIsInnermostImpl() {
        // Loop is innermost if none of its descendants is a loop
        Optional<ClavaNode> anotherLoop = loop.getDescendantsStream()
                .filter(node -> node instanceof LoopStmt)
                .findFirst();

        return !anotherLoop.isPresent();
    }

    @Override
    public Boolean getIsOutermostImpl() {
        // Loop is outermost if none of its ancestors is a loop
        Optional<ClavaNode> anotherLoop = loop.getAscendantsStream()
                .filter(node -> node instanceof LoopStmt)
                .findFirst();

        return !anotherLoop.isPresent();
    }

    @Override
    public Integer getNestedLevelImpl() {
        // Go back and count how many Loops there are
        long parentLoops = loop.getAscendantsStream()
                .filter(node -> node instanceof LoopStmt)
                .count();

        return (int) parentLoops;
    }

    @Override
    public AVarref getControlVarrefImpl() {

        // Only supported for loops of type 'for'
        if (!(loop instanceof ForStmt forStmt)) {
            return null;
        }

        var controlVars = LoopAnalysisUtils.getControlVars(forStmt);

        if (controlVars.isEmpty()) {

            ClavaLog.info("Could not find control variable for loop in location: " + loop.getLocation());

            return null;
        }

        if (controlVars.size() > 1) {

            ClavaLog.info("Found more than one control variable (" + controlVars + ") for loop in location: "
                    + loop.getLocation());
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
    public AStatement getCondImpl() {
        ClavaNode condition = loop.getStmtCondition().orElse(null);

        if (condition == null) {
            return null;
        }

        return CxxJoinpoints.create(ClavaNodes.toStmt(condition), getWeaverEngine(), AStatement.class);
    }

    @Override
    public AStatement getStepImpl() {
        if (!(loop instanceof ForStmt)) {
            return null;
        }

        Stmt inc = ((ForStmt) loop).getInc().orElse(null);
        if (inc == null) {
            return null;

        }

        return CxxJoinpoints.create(inc, getWeaverEngine(), AStatement.class);
    }

    @Override
    public LoopStmt getNode() {
        return loop;
    }

    @Override
    public int[] getRankArrayImpl() {
        var rank = loop.getRank();
        return rank.stream().mapToInt(Integer::intValue).toArray();
    }

    @Override
    public Boolean getIsParallelImpl() {
        return loop.isParallel();
    }

    @Override
    public Integer getIterationsImpl() {
        return loop.getIterations();
    }

    @Override
    public void setKindImpl(String kind) {
        ALoopKindEnum loopKind = SpecsEnums.valueOf(ALoopKindEnum.class, kind.toUpperCase());

        if (loopKind == null) {
            ClavaLog.warning("Unsupported loop kind:" + kind);
            return;
        }

        switch (loopKind) {
            case WHILE:
                convertToWhile();
                break;
            default:
                throw new RuntimeException("Not implemented: " + loopKind);
        }

    }

    private void convertToWhile() {
        if (loop instanceof WhileStmt) {
            return;
        }

        if (loop instanceof ForStmt) {

            // WhileStmt whileStmt = ClavaNodeFactory.whileStmt(loop.getInfo(), ((ForStmt) loop).getCond().orElse(null),
            // loop.getBody().orElse(null));

            // WhileStmt whileStmt = ClavaNodeFactory.whileStmt(loop.getInfo(), ((ForStmt) loop).getCond().orElse(null),
            // loop.getBody());
            Stmt cond = ((ForStmt) loop).getCond().orElse(getWeaverEngine().getFactory().nullStmt());
            WhileStmt whileStmt = getWeaverEngine().getFactory().whileStmt(cond, loop.getBody());

            replaceWith(CxxJoinpoints.create(whileStmt, getWeaverEngine()));

            return;
        }

        throw new RuntimeException("Case not implemented:" + loop.getClass());

    }

    @Override
    public void setInitImpl(String initCode) {
        if (!(loop instanceof ForStmt)) {
            return; // TODO: warn user?
        }

        var suffix = initCode.strip().endsWith(";") ? "" : ";";
        LiteralStmt literalStmt = getFactory().literalStmt(initCode + suffix);

        ((ForStmt) loop).setInit(literalStmt);
    }

    @Override
    public void setInitValueImpl(String initCode) {
        if (!(loop instanceof ForStmt)) {
            return; // TODO: warn user?
        }

        Type intType = getWeaverEngine().getFactory().builtinType(BuiltinKind.Int);

        ((ForStmt) loop).setInitValue(getWeaverEngine().getFactory().literalExpr(initCode, intType));
    }

    @Override
    public void setEndValueImpl(String value) {
        if (!(loop instanceof ForStmt)) {
            return; // TODO: warn user?
        }

        Type intType = getWeaverEngine().getFactory().builtinType(BuiltinKind.Int);

        ((ForStmt) loop).setConditionValue(getWeaverEngine().getFactory().literalExpr(value, intType));
    }

    @Override
    public void setCondImpl(String condCode) {

        if (!(loop instanceof ForStmt)) {
            return; // TODO: warn user?
        }

        var suffix = condCode.strip().endsWith(";") ? "" : ";";
        LiteralStmt literalStmt = getFactory().literalStmt(condCode + suffix);

        ((ForStmt) loop).setCond(literalStmt);
    }

    @Override
    public void setStepImpl(String stepCode) {

        if (!(loop instanceof ForStmt)) {
            return; // TODO: warn user?
        }

        LiteralStmt literalStmt = getFactory().literalStmt(stepCode);

        ((ForStmt) loop).setInc(literalStmt);
    }

    @Override
    public String getInitValueImpl() {

        if (!(loop instanceof ForStmt)) {
            ClavaLog.info(
                    "$loop.initValue: Not supported for loops of kind '" + getKindImpl() + "', only 'for' loops.");
            return null;
        }

        String initValue = ((ForStmt) loop).getInitValueExpr()
                .map(ClavaNode::getCode)
                .orElse(null);

        if (initValue == null) {
            ClavaLog.info(
                    "$loop.initValue: Could not determine the initial value of the loop. The init statement should be a variable declaration with initialization or assignment.");
        }

        return initValue;
        /*
        Optional<Stmt> initOpt = ((ForStmt) loop).getInit();
        
        if (initOpt.isPresent()) {
        
            Stmt init = initOpt.get();
        
            ClavaNode child = init.getChild(0);
        
            if (child instanceof VarDecl) {
        
                VarDecl decl = (VarDecl) child;
        
                Optional<Expr> declInitOpt = decl.getInit();
                if (declInitOpt.isPresent()) {
        
                    return declInitOpt.get().getCode();
                }
            } else if (child instanceof BinaryOperator) {
        
                BinaryOperator binOp = (BinaryOperator) child;
                if (binOp.getOp() == BinaryOperatorKind.ASSIGN) {
        
                    return binOp.getRhs().getCode();
                }
            }
        }
        
        ClavaLog.warning(
                "Could not determine the initial value of the loop. The init statement should be a variable declaration with initialization or assignment.");
        return null;
        */
    }

    @Override
    public String getEndValueImpl() {

        // Set<BinaryOperatorKind> ops = new HashSet<>();
        // ops.add(BinaryOperatorKind.LE);
        // ops.add(BinaryOperatorKind.LT);
        // ops.add(BinaryOperatorKind.GE);
        // ops.add(BinaryOperatorKind.GT);
        // ops.add(BinaryOperatorKind.NE);

        if (!(loop instanceof ForStmt)) {
            ClavaLog.info("Not supported for loops of kind '" + getKindImpl() + "', only 'for' loops ("
                    + getLocationImpl() + ").");
            return null;
        }

        ForStmt forLoop = (ForStmt) loop;
        String endValue = forLoop.getConditionValueExpr()
                .map(ClavaNode::getCode)
                .orElse(null);
        // String endValue = forLoop.getCondOperator()
        // .filter(binOp -> ops.contains(binOp.getOp()))
        // .map(binOp -> binOp.getRhs().getCode())
        // .orElse(null);

        if (endValue == null) {
            ClavaLog.debug(
                    "Could not determine the end value of the loop at '" + getLocationImpl()
                            + "'. The condition statement should be a Canonical Loop Form test expression, as defined by the OpenMP standard.");
            return null;
        }

        return endValue;
    }

    @Override
    public String getCondRelationImpl() {

        BinaryOperator condOp = getConditionOp();
        if (condOp == null) {
            return null;
        }

        // Relation requires lowercase names
        var opName = condOp.getOp().name().toLowerCase();

        var relation = Relation.getHelper().fromNameTry(opName).map(Relation::getString).orElse(null);

        if (relation == null) {
            ClavaLog.warning("Could not map operation with name '" + opName + "' to a Relation. Supported names: " + Relation.getHelper().names());
        }

        return relation;
    }

    @Override
    public Boolean getHasCondRelationImpl() {
        return getConditionOp(false) != null;
    }

    private BinaryOperator getConditionOp() {
        return getConditionOp(true);
    }

    private BinaryOperator getConditionOp(boolean showWarnings) {
        if (!(loop instanceof ForStmt)) {
            if (showWarnings) {
                ClavaLog.info(
                        "Not supported for loops of kind '" + getKindImpl() + "', only 'for' loops.");
            }

            return null;
        }

        ForStmt forLoop = (ForStmt) loop;
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
    public void setCondRelationImpl(String operator) {
        BinaryOperatorKind kind = BinaryOperatorKind.getHelper().fromValueTry(operator).orElse(null);

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
        return loop.getLoopId();
    }

    @Override
    public void interchangeImpl(ALoop otherLoop) {

        Optional<LoopInterchange> loopInterchange = LoopInterchange.newInstance(loop, (LoopStmt) otherLoop.getNode());
        if (!loopInterchange.isPresent()) {
            ClavaLog.info("Could not interchange loops");
            return;
        }

        loopInterchange.get().apply();
    }

    @Override
    public Boolean isInterchangeableImpl(ALoop otherLoop) {
        return LoopInterchange.test(loop, (LoopStmt) otherLoop.getNode());
    }

    @Override
    public AStatement tileImpl(String blockSize, AStatement reference, Boolean useTernary) {

        LoopTiling loopTiling = new LoopTiling(getWeaverEngine().getContex());

        boolean success = loopTiling.apply(loop, (Stmt) reference.getNode(),
                blockSize.toString(), useTernary);

        if (!success) {
            ClavaLog.info("Could not tile the loop: " + loop.getLocation());
        }

        if (loopTiling.getLastReferenceStmt() == null) {
            return reference;
        }

        return CxxJoinpoints.create(loopTiling.getLastReferenceStmt(), getWeaverEngine(), AStatement.class);

    }

    @Override
    public void setIsParallelImpl(Boolean isParallel) {
        loop.setParallel(isParallel);
    }

    @Override
    public AExpression getIterationsExprImpl() {
        if (!(loop instanceof ForStmt)) {
            ClavaLog.warning(
                    "Not supported for loops of kind '" + getKindImpl() + "', only 'for' loops.");
            return null;
        }

        return ((ForStmt) loop).getIterationsExpr()
                .map(expr -> CxxJoinpoints.create(expr,
                        getWeaverEngine(), AExpression.class))
                .orElse(null);
    }

    @Override
    public String getStepValueImpl() {
        if (!(loop instanceof ForStmt)) {
            ClavaLog.warning(
                    "Not supported for loops of kind '" + getKindImpl() + "', only 'for' loops.");
            return null;
        }

        String stepValue = ((ForStmt) loop).getStepValueExpr()
                .map(ClavaNode::getCode)
                .orElse(null);

        if (stepValue == null) {
            ClavaLog.warning(
                    "Could not determine the step value of the loop. The step statement should be a Canonical Loop Form increment expression, as defined by the OpenMP standard.");
        }

        return stepValue;
    }

    @Override
    public AStatement getInitImpl() {

        if (loop instanceof ForStmt) {
            return ((ForStmt) loop).getInit()
                    .map(init -> CxxJoinpoints.create(init,
                            getWeaverEngine(), AStatement.class))
                    .orElse(null);
        }

        // If range stmt, return begin
        if (loop instanceof CXXForRangeStmt) {
            return ((CXXForRangeStmt) loop).getBegin()
                    .map(init -> CxxJoinpoints.create(init,
                            getWeaverEngine(), AStatement.class))
                    .orElse(null);
        }

        return null;

    }

    @Override
    public AScope getBodyImpl() {
        return CxxJoinpoints.create(loop.getBody(), getWeaverEngine(), AScope.class);
    }

    @Override
    public void setBodyImpl(AScope body) {
        loop.setBody((CompoundStmt) body.getNode());
    }

}
