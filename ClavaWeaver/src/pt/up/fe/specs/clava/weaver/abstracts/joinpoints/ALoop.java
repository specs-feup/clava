package pt.up.fe.specs.clava.weaver.abstracts.joinpoints;

import org.lara.interpreter.weaver.interf.events.Stage;
import java.util.Optional;
import org.lara.interpreter.exception.AttributeException;
import javax.script.Bindings;
import pt.up.fe.specs.clava.weaver.enums.Relation;
import java.util.List;
import org.lara.interpreter.exception.ActionException;
import java.util.Map;
import org.lara.interpreter.weaver.interf.JoinPoint;
import java.util.stream.Collectors;
import java.util.Arrays;

/**
 * Auto-Generated class for join point ALoop
 * This class is overwritten by the Weaver Generator.
 * 
 * 
 * @author Lara Weaver Generator
 */
public abstract class ALoop extends AStatement {

    protected AStatement aStatement;

    /**
     * 
     */
    public ALoop(AStatement aStatement){
        this.aStatement = aStatement;
    }
    /**
     * Get value on attribute kind
     * @return the attribute's value
     */
    public abstract String getKindImpl();

    /**
     * Get value on attribute kind
     * @return the attribute's value
     */
    public final Object getKind() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "kind", Optional.empty());
        	}
        	String result = this.getKindImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "kind", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "kind", e);
        }
    }

    /**
     * Uniquely identifies the loop inside the program
     */
    public abstract String getIdImpl();

    /**
     * Uniquely identifies the loop inside the program
     */
    public final Object getId() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "id", Optional.empty());
        	}
        	String result = this.getIdImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "id", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "id", e);
        }
    }

    /**
     * Get value on attribute isInnermost
     * @return the attribute's value
     */
    public abstract Boolean getIsInnermostImpl();

    /**
     * Get value on attribute isInnermost
     * @return the attribute's value
     */
    public final Object getIsInnermost() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "isInnermost", Optional.empty());
        	}
        	Boolean result = this.getIsInnermostImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "isInnermost", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isInnermost", e);
        }
    }

    /**
     * Get value on attribute isOutermost
     * @return the attribute's value
     */
    public abstract Boolean getIsOutermostImpl();

    /**
     * Get value on attribute isOutermost
     * @return the attribute's value
     */
    public final Object getIsOutermost() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "isOutermost", Optional.empty());
        	}
        	Boolean result = this.getIsOutermostImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "isOutermost", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isOutermost", e);
        }
    }

    /**
     * Get value on attribute nestedLevel
     * @return the attribute's value
     */
    public abstract Integer getNestedLevelImpl();

    /**
     * Get value on attribute nestedLevel
     * @return the attribute's value
     */
    public final Object getNestedLevel() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "nestedLevel", Optional.empty());
        	}
        	Integer result = this.getNestedLevelImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "nestedLevel", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "nestedLevel", e);
        }
    }

    /**
     * Get value on attribute controlVar
     * @return the attribute's value
     */
    public abstract String getControlVarImpl();

    /**
     * Get value on attribute controlVar
     * @return the attribute's value
     */
    public final Object getControlVar() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "controlVar", Optional.empty());
        	}
        	String result = this.getControlVarImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "controlVar", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "controlVar", e);
        }
    }

    /**
     * Get value on attribute rank
     * @return the attribute's value
     */
    public abstract Integer[] getRankArrayImpl();

    /**
     * Get value on attribute rank
     * @return the attribute's value
     */
    public Bindings getRankImpl() {
        Integer[] integerArrayImpl0 = getRankArrayImpl();
        Bindings nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(integerArrayImpl0);
        return nativeArray0;
    }

    /**
     * Get value on attribute rank
     * @return the attribute's value
     */
    public final Object getRank() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "rank", Optional.empty());
        	}
        	Bindings result = this.getRankImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "rank", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "rank", e);
        }
    }

    /**
     * Get value on attribute isParallel
     * @return the attribute's value
     */
    public abstract Boolean getIsParallelImpl();

    /**
     * Get value on attribute isParallel
     * @return the attribute's value
     */
    public final Object getIsParallel() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "isParallel", Optional.empty());
        	}
        	Boolean result = this.getIsParallelImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "isParallel", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isParallel", e);
        }
    }

    /**
     * 
     */
    public void defIsParallelImpl(Boolean value) {
        throw new UnsupportedOperationException("Join point "+get_class()+": Action def isParallel with type Boolean not implemented ");
    }

    /**
     * 
     */
    public void defIsParallelImpl(String value) {
        throw new UnsupportedOperationException("Join point "+get_class()+": Action def isParallel with type String not implemented ");
    }

    /**
     * Get value on attribute iterations
     * @return the attribute's value
     */
    public abstract Integer getIterationsImpl();

    /**
     * Get value on attribute iterations
     * @return the attribute's value
     */
    public final Object getIterations() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "iterations", Optional.empty());
        	}
        	Integer result = this.getIterationsImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "iterations", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "iterations", e);
        }
    }

    /**
     * Get value on attribute iterationsExpr
     * @return the attribute's value
     */
    public abstract AExpression getIterationsExprImpl();

    /**
     * Get value on attribute iterationsExpr
     * @return the attribute's value
     */
    public final Object getIterationsExpr() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "iterationsExpr", Optional.empty());
        	}
        	AExpression result = this.getIterationsExprImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "iterationsExpr", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "iterationsExpr", e);
        }
    }

    /**
     * 
     * @param otherLoop
     * @return 
     */
    public abstract Boolean isInterchangeableImpl(ALoop otherLoop);

    /**
     * 
     * @param otherLoop
     * @return 
     */
    public final Object isInterchangeable(ALoop otherLoop) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "isInterchangeable", Optional.empty(), otherLoop);
        	}
        	Boolean result = this.isInterchangeableImpl(otherLoop);
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "isInterchangeable", Optional.ofNullable(result), otherLoop);
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isInterchangeable", e);
        }
    }

    /**
     * The statement of the loop initialization
     */
    public abstract AStatement getInitImpl();

    /**
     * The statement of the loop initialization
     */
    public final Object getInit() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "init", Optional.empty());
        	}
        	AStatement result = this.getInitImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "init", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "init", e);
        }
    }

    /**
     * 
     */
    public void defInitImpl(String value) {
        throw new UnsupportedOperationException("Join point "+get_class()+": Action def init with type String not implemented ");
    }

    /**
     * The expression of the first value of the control variable (e.g. '0' in 'size_t i = 0;')
     */
    public abstract String getInitValueImpl();

    /**
     * The expression of the first value of the control variable (e.g. '0' in 'size_t i = 0;')
     */
    public final Object getInitValue() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "initValue", Optional.empty());
        	}
        	String result = this.getInitValueImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "initValue", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "initValue", e);
        }
    }

    /**
     * 
     */
    public void defInitValueImpl(String value) {
        throw new UnsupportedOperationException("Join point "+get_class()+": Action def initValue with type String not implemented ");
    }

    /**
     * The expression of the last value of the control variable (e.g. 'length' in 'i < length;')
     */
    public abstract String getEndValueImpl();

    /**
     * The expression of the last value of the control variable (e.g. 'length' in 'i < length;')
     */
    public final Object getEndValue() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "endValue", Optional.empty());
        	}
        	String result = this.getEndValueImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "endValue", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "endValue", e);
        }
    }

    /**
     * The expression of the iteration step
     */
    public abstract String getStepValueImpl();

    /**
     * The expression of the iteration step
     */
    public final Object getStepValue() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "stepValue", Optional.empty());
        	}
        	String result = this.getStepValueImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "stepValue", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "stepValue", e);
        }
    }

    /**
     * Get value on attribute condRelation
     * @return the attribute's value
     */
    public abstract Relation getCondRelationImpl();

    /**
     * Get value on attribute condRelation
     * @return the attribute's value
     */
    public final Object getCondRelation() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "condRelation", Optional.empty());
        	}
        	Relation result = this.getCondRelationImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "condRelation", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "condRelation", e);
        }
    }

    /**
     * Method used by the lara interpreter to select inits
     * @return 
     */
    public abstract List<? extends AStatement> selectInit();

    /**
     * Method used by the lara interpreter to select conds
     * @return 
     */
    public abstract List<? extends AStatement> selectCond();

    /**
     * Method used by the lara interpreter to select steps
     * @return 
     */
    public abstract List<? extends AStatement> selectStep();

    /**
     * Method used by the lara interpreter to select bodys
     * @return 
     */
    public abstract List<? extends AScope> selectBody();

    /**
     * DEPRECATED: use 'setKind' instead
     * @param kind 
     */
    public void changeKindImpl(String kind) {
        throw new UnsupportedOperationException(get_class()+": Action changeKind not implemented ");
    }

    /**
     * DEPRECATED: use 'setKind' instead
     * @param kind 
     */
    public final void changeKind(String kind) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "changeKind", this, Optional.empty(), kind);
        	}
        	this.changeKindImpl(kind);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "changeKind", this, Optional.empty(), kind);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "changeKind", e);
        }
    }

    /**
     * Sets the kind of the loop
     * @param kind 
     */
    public void setKindImpl(String kind) {
        throw new UnsupportedOperationException(get_class()+": Action setKind not implemented ");
    }

    /**
     * Sets the kind of the loop
     * @param kind 
     */
    public final void setKind(String kind) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setKind", this, Optional.empty(), kind);
        	}
        	this.setKindImpl(kind);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setKind", this, Optional.empty(), kind);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setKind", e);
        }
    }

    /**
     * Sets the init statement of the loop.
     * @param initCode 
     */
    public void setInitImpl(String initCode) {
        throw new UnsupportedOperationException(get_class()+": Action setInit not implemented ");
    }

    /**
     * Sets the init statement of the loop.
     * @param initCode 
     */
    public final void setInit(String initCode) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setInit", this, Optional.empty(), initCode);
        	}
        	this.setInitImpl(initCode);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setInit", this, Optional.empty(), initCode);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setInit", e);
        }
    }

    /**
     * Sets the init value of the loop. Works with loops of kind 'for'.
     * @param initCode 
     */
    public void setInitValueImpl(String initCode) {
        throw new UnsupportedOperationException(get_class()+": Action setInitValue not implemented ");
    }

    /**
     * Sets the init value of the loop. Works with loops of kind 'for'.
     * @param initCode 
     */
    public final void setInitValue(String initCode) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setInitValue", this, Optional.empty(), initCode);
        	}
        	this.setInitValueImpl(initCode);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setInitValue", this, Optional.empty(), initCode);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setInitValue", e);
        }
    }

    /**
     * Sets the end value of the loop. Works with loops of kind 'for'.
     * @param initCode 
     */
    public void setEndValueImpl(String initCode) {
        throw new UnsupportedOperationException(get_class()+": Action setEndValue not implemented ");
    }

    /**
     * Sets the end value of the loop. Works with loops of kind 'for'.
     * @param initCode 
     */
    public final void setEndValue(String initCode) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setEndValue", this, Optional.empty(), initCode);
        	}
        	this.setEndValueImpl(initCode);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setEndValue", this, Optional.empty(), initCode);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setEndValue", e);
        }
    }

    /**
     * Sets the conditional statement of the loop. Works with loops of kind 'for'.
     * @param condCode 
     */
    public void setCondImpl(String condCode) {
        throw new UnsupportedOperationException(get_class()+": Action setCond not implemented ");
    }

    /**
     * Sets the conditional statement of the loop. Works with loops of kind 'for'.
     * @param condCode 
     */
    public final void setCond(String condCode) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setCond", this, Optional.empty(), condCode);
        	}
        	this.setCondImpl(condCode);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setCond", this, Optional.empty(), condCode);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setCond", e);
        }
    }

    /**
     * Sets the step statement of the loop. Works with loops of kind 'for'.
     * @param stepCode 
     */
    public void setStepImpl(String stepCode) {
        throw new UnsupportedOperationException(get_class()+": Action setStep not implemented ");
    }

    /**
     * Sets the step statement of the loop. Works with loops of kind 'for'.
     * @param stepCode 
     */
    public final void setStep(String stepCode) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setStep", this, Optional.empty(), stepCode);
        	}
        	this.setStepImpl(stepCode);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setStep", this, Optional.empty(), stepCode);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setStep", e);
        }
    }

    /**
     * Sets the attribute 'isParallel' of the loop.
     * @param isParallel 
     */
    public void setIsParallelImpl(Boolean isParallel) {
        throw new UnsupportedOperationException(get_class()+": Action setIsParallel not implemented ");
    }

    /**
     * Sets the attribute 'isParallel' of the loop.
     * @param isParallel 
     */
    public final void setIsParallel(Boolean isParallel) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setIsParallel", this, Optional.empty(), isParallel);
        	}
        	this.setIsParallelImpl(isParallel);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setIsParallel", this, Optional.empty(), isParallel);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setIsParallel", e);
        }
    }

    /**
     * Interchanges two for loops, if possible.
     * @param otherLoop 
     */
    public void interchangeImpl(ALoop otherLoop) {
        throw new UnsupportedOperationException(get_class()+": Action interchange not implemented ");
    }

    /**
     * Interchanges two for loops, if possible.
     * @param otherLoop 
     */
    public final void interchange(ALoop otherLoop) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "interchange", this, Optional.empty(), otherLoop);
        	}
        	this.interchangeImpl(otherLoop);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "interchange", this, Optional.empty(), otherLoop);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "interchange", e);
        }
    }

    /**
     * Applies loop tiling to target loop.
     * @param blockSize 
     * @param reference 
     */
    public AStatement tileImpl(String blockSize, AStatement reference) {
        throw new UnsupportedOperationException(get_class()+": Action tile not implemented ");
    }

    /**
     * Applies loop tiling to target loop.
     * @param blockSize 
     * @param reference 
     */
    public final AStatement tile(String blockSize, AStatement reference) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "tile", this, Optional.empty(), blockSize, reference);
        	}
        	AStatement result = this.tileImpl(blockSize, reference);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "tile", this, Optional.ofNullable(result), blockSize, reference);
        	}
        	return result;
        } catch(Exception e) {
        	throw new ActionException(get_class(), "tile", e);
        }
    }

    /**
     * Applies loop tiling to target loop.
     * @param blockSize 
     * @param reference 
     * @param useTernary 
     */
    public AStatement tileImpl(String blockSize, AStatement reference, Boolean useTernary) {
        throw new UnsupportedOperationException(get_class()+": Action tile not implemented ");
    }

    /**
     * Applies loop tiling to target loop.
     * @param blockSize 
     * @param reference 
     * @param useTernary 
     */
    public final AStatement tile(String blockSize, AStatement reference, Boolean useTernary) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "tile", this, Optional.empty(), blockSize, reference, useTernary);
        	}
        	AStatement result = this.tileImpl(blockSize, reference, useTernary);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "tile", this, Optional.ofNullable(result), blockSize, reference, useTernary);
        	}
        	return result;
        } catch(Exception e) {
        	throw new ActionException(get_class(), "tile", e);
        }
    }

    /**
     * Get value on attribute isFirst
     * @return the attribute's value
     */
    @Override
    public Boolean getIsFirstImpl() {
        return this.aStatement.getIsFirstImpl();
    }

    /**
     * Get value on attribute isLast
     * @return the attribute's value
     */
    @Override
    public Boolean getIsLastImpl() {
        return this.aStatement.getIsLastImpl();
    }

    /**
     * Method used by the lara interpreter to select exprs
     * @return 
     */
    @Override
    public List<? extends AExpression> selectExpr() {
        return this.aStatement.selectExpr();
    }

    /**
     * Method used by the lara interpreter to select childExprs
     * @return 
     */
    @Override
    public List<? extends AExpression> selectChildExpr() {
        return this.aStatement.selectChildExpr();
    }

    /**
     * Method used by the lara interpreter to select calls
     * @return 
     */
    @Override
    public List<? extends ACall> selectCall() {
        return this.aStatement.selectCall();
    }

    /**
     * Method used by the lara interpreter to select stmtCalls
     * @return 
     */
    @Override
    public List<? extends ACall> selectStmtCall() {
        return this.aStatement.selectStmtCall();
    }

    /**
     * Method used by the lara interpreter to select memberCalls
     * @return 
     */
    @Override
    public List<? extends AMemberCall> selectMemberCall() {
        return this.aStatement.selectMemberCall();
    }

    /**
     * Method used by the lara interpreter to select memberAccesss
     * @return 
     */
    @Override
    public List<? extends AMemberAccess> selectMemberAccess() {
        return this.aStatement.selectMemberAccess();
    }

    /**
     * Method used by the lara interpreter to select arrayAccesss
     * @return 
     */
    @Override
    public List<? extends AArrayAccess> selectArrayAccess() {
        return this.aStatement.selectArrayAccess();
    }

    /**
     * Method used by the lara interpreter to select vardecls
     * @return 
     */
    @Override
    public List<? extends AVardecl> selectVardecl() {
        return this.aStatement.selectVardecl();
    }

    /**
     * Method used by the lara interpreter to select varrefs
     * @return 
     */
    @Override
    public List<? extends AVarref> selectVarref() {
        return this.aStatement.selectVarref();
    }

    /**
     * Method used by the lara interpreter to select binaryOps
     * @return 
     */
    @Override
    public List<? extends ABinaryOp> selectBinaryOp() {
        return this.aStatement.selectBinaryOp();
    }

    /**
     * Method used by the lara interpreter to select unaryOps
     * @return 
     */
    @Override
    public List<? extends AUnaryOp> selectUnaryOp() {
        return this.aStatement.selectUnaryOp();
    }

    /**
     * Method used by the lara interpreter to select newExprs
     * @return 
     */
    @Override
    public List<? extends ANewExpr> selectNewExpr() {
        return this.aStatement.selectNewExpr();
    }

    /**
     * Method used by the lara interpreter to select deleteExprs
     * @return 
     */
    @Override
    public List<? extends ADeleteExpr> selectDeleteExpr() {
        return this.aStatement.selectDeleteExpr();
    }

    /**
     * 
     * @param node 
     */
    @Override
    public AJoinPoint replaceWithImpl(AJoinPoint node) {
        return this.aStatement.replaceWithImpl(node);
    }

    /**
     * 
     * @param node 
     */
    @Override
    public AJoinPoint replaceWithImpl(String node) {
        return this.aStatement.replaceWithImpl(node);
    }

    /**
     * 
     * @param node 
     */
    @Override
    public AJoinPoint insertBeforeImpl(AJoinPoint node) {
        return this.aStatement.insertBeforeImpl(node);
    }

    /**
     * 
     * @param node 
     */
    @Override
    public AJoinPoint insertBeforeImpl(String node) {
        return this.aStatement.insertBeforeImpl(node);
    }

    /**
     * 
     * @param node 
     */
    @Override
    public AJoinPoint insertAfterImpl(AJoinPoint node) {
        return this.aStatement.insertAfterImpl(node);
    }

    /**
     * 
     * @param code 
     */
    @Override
    public AJoinPoint insertAfterImpl(String code) {
        return this.aStatement.insertAfterImpl(code);
    }

    /**
     * 
     */
    @Override
    public void detachImpl() {
        this.aStatement.detachImpl();
    }

    /**
     * 
     * @param type 
     */
    @Override
    public void setTypeImpl(AJoinPoint type) {
        this.aStatement.setTypeImpl(type);
    }

    /**
     * 
     */
    @Override
    public AJoinPoint copyImpl() {
        return this.aStatement.copyImpl();
    }

    /**
     * 
     * @param fieldName 
     * @param value 
     */
    @Override
    public Object setUserFieldImpl(String fieldName, Object value) {
        return this.aStatement.setUserFieldImpl(fieldName, value);
    }

    /**
     * 
     * @param fieldNameAndValue 
     */
    @Override
    public Object setUserFieldImpl(Map<?, ?> fieldNameAndValue) {
        return this.aStatement.setUserFieldImpl(fieldNameAndValue);
    }

    /**
     * Sets the value associated with the given property key
     * @param key 
     * @param value 
     */
    @Override
    public AJoinPoint setValueImpl(String key, Object value) {
        return this.aStatement.setValueImpl(key, value);
    }

    /**
     * 
     * @param message 
     */
    @Override
    public void messageToUserImpl(String message) {
        this.aStatement.messageToUserImpl(message);
    }

    /**
     * 
     * @param position 
     * @param code 
     */
    @Override
    public void insertImpl(String position, String code) {
        this.aStatement.insertImpl(position, code);
    }

    /**
     * 
     */
    @Override
    public String toString() {
        return this.aStatement.toString();
    }

    /**
     * 
     */
    @Override
    public Optional<? extends AStatement> getSuper() {
        return Optional.of(this.aStatement);
    }

    /**
     * 
     */
    @Override
    public final List<? extends JoinPoint> select(String selectName) {
        List<? extends JoinPoint> joinPointList;
        switch(selectName) {
        	case "init": 
        		joinPointList = selectInit();
        		break;
        	case "cond": 
        		joinPointList = selectCond();
        		break;
        	case "step": 
        		joinPointList = selectStep();
        		break;
        	case "body": 
        		joinPointList = selectBody();
        		break;
        	case "expr": 
        		joinPointList = selectExpr();
        		break;
        	case "childExpr": 
        		joinPointList = selectChildExpr();
        		break;
        	case "call": 
        		joinPointList = selectCall();
        		break;
        	case "stmtCall": 
        		joinPointList = selectStmtCall();
        		break;
        	case "memberCall": 
        		joinPointList = selectMemberCall();
        		break;
        	case "memberAccess": 
        		joinPointList = selectMemberAccess();
        		break;
        	case "arrayAccess": 
        		joinPointList = selectArrayAccess();
        		break;
        	case "vardecl": 
        		joinPointList = selectVardecl();
        		break;
        	case "varref": 
        		joinPointList = selectVarref();
        		break;
        	case "binaryOp": 
        		joinPointList = selectBinaryOp();
        		break;
        	case "unaryOp": 
        		joinPointList = selectUnaryOp();
        		break;
        	case "newExpr": 
        		joinPointList = selectNewExpr();
        		break;
        	case "deleteExpr": 
        		joinPointList = selectDeleteExpr();
        		break;
        	default:
        		joinPointList = this.aStatement.select(selectName);
        		break;
        }
        return joinPointList;
    }

    /**
     * 
     */
    @Override
    public final void defImpl(String attribute, Object value) {
        switch(attribute){
        case "type": {
        	if(value instanceof AJoinPoint){
        		this.defTypeImpl((AJoinPoint)value);
        		return;
        	}
        	this.unsupportedTypeForDef(attribute, value);
        }
        case "isParallel": {
        	if(value instanceof Boolean){
        		this.defIsParallelImpl((Boolean)value);
        		return;
        	}
        	if(value instanceof String){
        		this.defIsParallelImpl((String)value);
        		return;
        	}
        	this.unsupportedTypeForDef(attribute, value);
        }
        case "init": {
        	if(value instanceof String){
        		this.defInitImpl((String)value);
        		return;
        	}
        	this.unsupportedTypeForDef(attribute, value);
        }
        case "initValue": {
        	if(value instanceof String){
        		this.defInitValueImpl((String)value);
        		return;
        	}
        	this.unsupportedTypeForDef(attribute, value);
        }
        default: throw new UnsupportedOperationException("Join point "+get_class()+": attribute '"+attribute+"' cannot be defined");
        }
    }

    /**
     * 
     */
    @Override
    protected final void fillWithAttributes(List<String> attributes) {
        this.aStatement.fillWithAttributes(attributes);
        attributes.add("kind");
        attributes.add("id");
        attributes.add("isInnermost");
        attributes.add("isOutermost");
        attributes.add("nestedLevel");
        attributes.add("controlVar");
        attributes.add("rank");
        attributes.add("isParallel");
        attributes.add("iterations");
        attributes.add("iterationsExpr");
        attributes.add("isInterchangeable");
        attributes.add("init");
        attributes.add("initValue");
        attributes.add("endValue");
        attributes.add("stepValue");
        attributes.add("condRelation");
    }

    /**
     * 
     */
    @Override
    protected final void fillWithSelects(List<String> selects) {
        this.aStatement.fillWithSelects(selects);
        selects.add("init");
        selects.add("cond");
        selects.add("step");
        selects.add("body");
    }

    /**
     * 
     */
    @Override
    protected final void fillWithActions(List<String> actions) {
        this.aStatement.fillWithActions(actions);
        actions.add("void changeKind(String)");
        actions.add("void setKind(String)");
        actions.add("void setInit(String)");
        actions.add("void setInitValue(String)");
        actions.add("void setEndValue(String)");
        actions.add("void setCond(String)");
        actions.add("void setStep(String)");
        actions.add("void setIsParallel(Boolean)");
        actions.add("void interchange(loop)");
        actions.add("statement tile(String, statement)");
        actions.add("statement tile(String, statement, Boolean)");
    }

    /**
     * Returns the join point type of this class
     * @return The join point type
     */
    @Override
    public final String get_class() {
        return "loop";
    }

    /**
     * Defines if this joinpoint is an instanceof a given joinpoint class
     * @return True if this join point is an instanceof the given class
     */
    @Override
    public final boolean instanceOf(String joinpointClass) {
        boolean isInstance = get_class().equals(joinpointClass);
        if(isInstance) {
        	return true;
        }
        return this.aStatement.instanceOf(joinpointClass);
    }
    /**
     * 
     */
    protected enum LoopAttributes {
        KIND("kind"),
        ID("id"),
        ISINNERMOST("isInnermost"),
        ISOUTERMOST("isOutermost"),
        NESTEDLEVEL("nestedLevel"),
        CONTROLVAR("controlVar"),
        RANK("rank"),
        ISPARALLEL("isParallel"),
        ITERATIONS("iterations"),
        ITERATIONSEXPR("iterationsExpr"),
        ISINTERCHANGEABLE("isInterchangeable"),
        INIT("init"),
        INITVALUE("initValue"),
        ENDVALUE("endValue"),
        STEPVALUE("stepValue"),
        CONDRELATION("condRelation"),
        ISFIRST("isFirst"),
        ISLAST("isLast"),
        PARENT("parent"),
        ASTANCESTOR("astAncestor"),
        AST("ast"),
        CODE("code"),
        DATA("data"),
        ISINSIDELOOPHEADER("isInsideLoopHeader"),
        LINE("line"),
        KEYS("keys"),
        DESCENDANTSANDSELF("descendantsAndSelf"),
        ASTNUMCHILDREN("astNumChildren"),
        TYPE("type"),
        DESCENDANTS("descendants"),
        ASTCHILDREN("astChildren"),
        ROOT("root"),
        JAVAVALUE("javaValue"),
        CHAINANCESTOR("chainAncestor"),
        CHAIN("chain"),
        JOINPOINTTYPE("joinpointType"),
        CURRENTREGION("currentRegion"),
        ANCESTOR("ancestor"),
        HASASTPARENT("hasAstParent"),
        ASTCHILD("astChild"),
        PARENTREGION("parentRegion"),
        ASTNAME("astName"),
        ASTID("astId"),
        GETVALUE("getValue"),
        CONTAINS("contains"),
        ASTISINSTANCE("astIsInstance"),
        JAVAFIELDS("javaFields"),
        ASTPARENT("astParent"),
        JAVAFIELDTYPE("javaFieldType"),
        USERFIELD("userField"),
        LOCATION("location"),
        HASNODE("hasNode"),
        GETUSERFIELD("getUserField"),
        PRAGMAS("pragmas"),
        HASPARENT("hasParent");
        private String name;

        /**
         * 
         */
        private LoopAttributes(String name){
            this.name = name;
        }
        /**
         * Return an attribute enumeration item from a given attribute name
         */
        public static Optional<LoopAttributes> fromString(String name) {
            return Arrays.asList(values()).stream().filter(attr -> attr.name.equals(name)).findAny();
        }

        /**
         * Return a list of attributes in String format
         */
        public static List<String> getNames() {
            return Arrays.asList(values()).stream().map(LoopAttributes::name).collect(Collectors.toList());
        }

        /**
         * True if the enum contains the given attribute name, false otherwise.
         */
        public static boolean contains(String name) {
            return fromString(name).isPresent();
        }
    }
}
