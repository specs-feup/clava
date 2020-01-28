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

package pt.up.fe.specs.clava.weaver.joinpoints;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.lara.interpreter.utils.DefMap;

import com.google.common.base.Preconditions;

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
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.enums.ALoopKindEnum;
import pt.up.fe.specs.clava.weaver.defs.CxxLoopDefs;
import pt.up.fe.specs.clava.weaver.enums.Relation;
import pt.up.fe.specs.util.SpecsEnums;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.lazy.ThreadSafeLazy;

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

    public CxxLoop(LoopStmt loop) {
        super(new CxxStatement(loop));

        this.loop = loop;
    }

    @Override
    public String getKindImpl() {
        ALoopKindEnum loopType = LOOP_TYPE.get().get(loop.getClass());

        Preconditions.checkNotNull(loopType,
                "Could not determine type of node '" + loop.getClass().getSimpleName() + "'");

        return loopType.name().toLowerCase();
    }

    @Override
    protected DefMap<?> getDefMap() {
        return CxxLoopDefs.getDefMap();
    }

    /*
    @Override
    public int getIncrementValue() {
        // Only supported for loops of type 'for'
        if (!(loop instanceof ForStmt)) {
            return 0;
        }
    
        ForStmt forLoop = (ForStmt) loop;
    
        Stmt inc = forLoop.getInc().orElse(null);
        if (inc == null) {
            return 0;
        }
    
        // TODO: Regular expression for <VAR_NAME>++; / <VAR_NAME>--;
        System.out.println("INC CODE:" + inc);
    
        return 0;
    }
    */

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
    public String getControlVarImpl() {

        // Only supported for loops of type 'for'
        if (!(loop instanceof ForStmt)) {
            return null;
        }

        ForStmt forStmt = (ForStmt) loop;

        List<String> controlVars = LoopAnalysisUtils.getControlVarNames(forStmt);

        if (controlVars.isEmpty()) {

            ClavaLog.info("Could not find control variable for loop in location: " + loop.getLocation());

            return null;
        }

        if (controlVars.size() > 1) {

            ClavaLog.info("Found more than one control variable (" + controlVars + ") for loop in location: "
                    + loop.getLocation());
        }

        return controlVars.get(0);

        // // 1. Find control var in the initialization
        // Stmt init = forStmt.getInit().orElse(null);
        // if (init != null) {
        //
        // // 1.1 When there is only initialization
        // DeclRefExpr expr = init.getFirstDescendantsAndSelf(DeclRefExpr.class).orElse(null);
        // if (expr != null) {
        // return expr.getRefName();
        // }
        //
        // // 1.2 When there is declaration and initialization
        // VarDecl decl = init.getFirstDescendantsAndSelf(VarDecl.class).orElse(null);
        // if (decl != null) {
        // return decl.getDeclName();
        // }
        //
        // }
        //
        // // 2. Find control var in the condition
        // Stmt cond = forStmt.getCond().orElse(null);
        // if (cond != null) {
        // DeclRefExpr expr = cond.getFirstDescendantsAndSelf(DeclRefExpr.class).orElse(null);
        // if (expr != null) {
        // return expr.getRefName();
        // }
        // }
        //
        // // 3. Find control var in the increment
        // Stmt inc = forStmt.getInc().orElse(null);
        // if (inc != null) {
        // DeclRefExpr expr = inc.getFirstDescendantsAndSelf(DeclRefExpr.class).orElse(null);
        // if (expr != null) {
        // return expr.getRefName();
        // }
        // }
        // return null;
    }

    @Override
    public List<? extends AStatement> selectInit() {
        if (!(loop instanceof ForStmt)) {
            return Collections.emptyList();
        }

        Stmt init = ((ForStmt) loop).getInit().orElse(null);
        if (init == null) {
            return Collections.emptyList();

        }

        return Arrays.asList(CxxJoinpoints.create(init, AStatement.class));
    }

    @Override
    public List<? extends AStatement> selectCond() {
        ClavaNode condition = loop.getStmtCondition().orElse(null);

        if (condition == null) {
            return Collections.emptyList();
        }

        return Arrays.asList(CxxJoinpoints.create(ClavaNodes.toStmt(condition), AStatement.class));
    }

    @Override
    public List<? extends AStatement> selectStep() {
        if (!(loop instanceof ForStmt)) {
            return Collections.emptyList();
        }

        Stmt inc = ((ForStmt) loop).getInc().orElse(null);
        if (inc == null) {
            return Collections.emptyList();

        }

        return Arrays.asList(CxxJoinpoints.create(inc, AStatement.class));
    }

    @Override
    public List<? extends AScope> selectBody() {
        // return loop.getBody()
        // .map(body -> Arrays.asList(CxxJoinpoints.create(body, this, AScope.class)))
        // .orElse(Collections.emptyList());
        return Arrays.asList(CxxJoinpoints.create(loop.getBody(), AScope.class));
    }

    @Override
    public LoopStmt getNode() {
        return loop;
    }

    @Override
    public Integer[] getRankArrayImpl() {
        return loop.getRank().toArray(new Integer[0]);
    }

    @Override
    public Boolean getIsParallelImpl() {
        return loop.isParallel();
        /*
        // Map<String, Consumer<? extends Object>> defMap = new HashMap<>();
        // defMap.put("qq", obj -> consumerString(obj));
        
        // Check if loop is annotated with pragma "parallel"
        List<Pragma> pragmas = ClavaNodes.getPragmas(getNode());
        
        boolean result = pragmas.stream()
                .filter(pragma -> pragma.getName().equals("clava"))
                .filter(clavaPragma -> clavaPragma.getContent().equals("parallel"))
                .findFirst()
                .isPresent();
        
        return result;
        */
    }

    // private void consumerString(String s) {
    //
    // }

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

    @Override
    public void changeKindImpl(String kind) {
        setKindImpl(kind);
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
            Stmt cond = ((ForStmt) loop).getCond().orElse(CxxWeaver.getFactory().nullStmt());
            WhileStmt whileStmt = CxxWeaver.getFactory().whileStmt(cond, loop.getBody());

            replaceWith(CxxJoinpoints.create(whileStmt));

            return;
        }

        throw new RuntimeException("Case not implemented:" + loop.getClass());

    }

    @Override
    public void defInitImpl(String value) {
        if (!(loop instanceof ForStmt)) {
            return; // TODO: warn user?
        }

        LiteralStmt literalStmt = getFactory().literalStmt(value + ";");

        ((ForStmt) loop).setInit(literalStmt);
    }

    @Override
    public void setInitImpl(String initCode) {
        defInitImpl(initCode);
        /*
        // ClavaLog.deprecated("action $loop.exec setInit is deprecated, please use setInitValue instead");
        // setInitValue(initCode);
        
        if (!(loop instanceof ForStmt)) {
            return; // TODO: warn user?
        }
        
        LiteralStmt literalStmt = ClavaNodeFactory.literalStmt(initCode + ";");
        
        ((ForStmt) loop).setInit(literalStmt);
        */
    }

    /*
    @Override
    public void setInitValueImpl(String initCode) {
        // if (!(loop instanceof ForStmt)) {
        // return; // TODO: warn user?
        // }
        //
        // LiteralStmt literalStmt = ClavaNodeFactory.literalStmt(initCode + ";");
        //
        // ((ForStmt) loop).setInit(literalStmt);
        defInitValueImpl(initCode);
    }
    
    @Override
    public void defInitValueImpl(String value) {
        if (!(loop instanceof ForStmt)) {
            return; // TODO: warn user?
        }
    
        LiteralStmt literalStmt = ClavaNodeFactory.literalStmt(value + ";");
    
        ((ForStmt) loop).setInit(literalStmt);
    }
    */

    @Override
    public void setInitValueImpl(String initCode) {
        defInitValueImpl(initCode);
    }

    @Override
    public void defInitValueImpl(String value) {
        if (!(loop instanceof ForStmt)) {
            return; // TODO: warn user?
        }

        Type intType = CxxWeaver.getFactory().builtinType(BuiltinKind.Int);

        ((ForStmt) loop).setInitValue(CxxWeaver.getFactory().literalExpr(value, intType));
    }

    @Override
    public void setEndValueImpl(String value) {
        if (!(loop instanceof ForStmt)) {
            return; // TODO: warn user?
        }

        Type intType = CxxWeaver.getFactory().builtinType(BuiltinKind.Int);

        ((ForStmt) loop).setConditionValue(CxxWeaver.getFactory().literalExpr(value, intType));
    }

    @Override
    public void setCondImpl(String condCode) {

        if (!(loop instanceof ForStmt)) {
            return; // TODO: warn user?
        }

        LiteralStmt literalStmt = getFactory().literalStmt(condCode + ";");

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
    public Relation getCondRelationImpl() {

        BinaryOperator condOp = getConditionOp();
        if (condOp == null) {
            return null;
        }

        return Relation.getHelper().fromNameTry(condOp.getOp().name()).orElse(null);
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
    public void defCondRelationImpl(Relation value) {
        if (value == Relation.EQ || value == Relation.NE) {
            ClavaLog.info(
                    "Relation not supported for 'def' of 'condRelation': " + value);
            return;
        }

        BinaryOperator condOp = getConditionOp();
        if (condOp == null) {
            return;
        }

        condOp.set(BinaryOperator.OP, getOpKind(value));

    }

    @Override
    public void defCondRelationImpl(String value) {
        BinaryOperatorKind kind = BinaryOperatorKind.getHelper().fromValueTry(value).orElse(null);
        // BinaryOperatorKind kind = SpecsEnums.valueOfTry(BinaryOperatorKind.class, value).orElse(null);

        if (kind == null) {
            ClavaLog.info("def 'condRelation': Invalid binary operator " + value);
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
    public void setCondRelationImpl(String operator) {
        defCondRelationImpl(operator);
    }

    private BinaryOperatorKind getOpKind(Relation relation) {
        switch (relation) {
        case EQ:
            return BinaryOperatorKind.EQ;
        case GE:
            return BinaryOperatorKind.GE;
        case GT:
            return BinaryOperatorKind.GT;
        case LE:
            return BinaryOperatorKind.LE;
        case LT:
            return BinaryOperatorKind.LT;
        case NE:
            return BinaryOperatorKind.NE;
        default:
            throw new NotImplementedException(relation);
        }
    }

    @Override
    public void setCondRelationImpl(Relation operator) {
        defCondRelationImpl(operator);
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
    public AStatement tileImpl(String blockSize, AStatement reference) {

        return tileImpl(blockSize, reference, true);
    }

    @Override
    public AStatement tileImpl(String blockSize, AStatement reference, Boolean useTernary) {

        LoopTiling loopTiling = new LoopTiling(CxxWeaver.getContex());

        boolean success = loopTiling.apply(loop, (Stmt) reference.getNode(),
                blockSize.toString(), useTernary);

        if (!success) {
            ClavaLog.info("Could not tile the loop: " + loop.getLocation());
        }

        if (loopTiling.getLastReferenceStmt() == null) {
            return reference;
        }

        return CxxJoinpoints.create(loopTiling.getLastReferenceStmt(), AStatement.class);

    }

    @Override
    public void defIsParallelImpl(Boolean value) {
        loop.setParallel(value);
    }

    @Override
    public void defIsParallelImpl(String value) {
        loop.setParallel(Boolean.parseBoolean(value));
    }

    @Override
    public void setIsParallelImpl(Boolean isParallel) {
        defIsParallelImpl(isParallel);
    }

    @Override
    public AExpression getIterationsExprImpl() {
        if (!(loop instanceof ForStmt)) {
            ClavaLog.warning(
                    "Not supported for loops of kind '" + getKindImpl() + "', only 'for' loops.");
            return null;
        }

        return ((ForStmt) loop).getIterationsExpr()
                .map(expr -> CxxJoinpoints.create(expr, AExpression.class))
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
        if (!(loop instanceof ForStmt)) {
            ClavaLog.warning(
                    "Not supported for loops of kind '" + getKindImpl() + "', only 'for' loops.");
            return null;
        }

        return ((ForStmt) loop).getInit().map(init -> CxxJoinpoints.create(init, AStatement.class)).orElse(null);

    }

    @Override
    public AScope getBodyImpl() {
        return (AScope) CxxJoinpoints.create(loop.getBody());
    }

    @Override
    public void defBodyImpl(AScope value) {
        loop.setBody((CompoundStmt) value.getNode());
    }

    @Override
    public void setBodyImpl(AScope body) {
        defBodyImpl(body);
    }
}
