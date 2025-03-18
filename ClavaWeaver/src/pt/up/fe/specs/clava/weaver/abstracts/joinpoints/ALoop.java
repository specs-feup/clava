package pt.up.fe.specs.clava.weaver.abstracts.joinpoints;

import org.lara.interpreter.exception.AttributeException;
import org.lara.interpreter.exception.ActionException;
import org.lara.interpreter.weaver.interf.JoinPoint;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.util.List;

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
     * Get value on attribute body
     * @return the attribute's value
     */
    public abstract AScope getBodyImpl();

    /**
     * Get value on attribute body
     * @return the attribute's value
     */
    public final Object getBody() {
        try {
        	AScope result = this.getBodyImpl();
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "body", e);
        }
    }

    /**
     * The statement of the loop condition
     */
    public abstract AStatement getCondImpl();

    /**
     * The statement of the loop condition
     */
    public final Object getCond() {
        try {
        	AStatement result = this.getCondImpl();
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "cond", e);
        }
    }

    /**
     * Get value on attribute condRelation
     * @return the attribute's value
     */
    public abstract String getCondRelationImpl();

    /**
     * Get value on attribute condRelation
     * @return the attribute's value
     */
    public final Object getCondRelation() {
        try {
        	String result = this.getCondRelationImpl();
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "condRelation", e);
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
        	String result = this.getControlVarImpl();
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "controlVar", e);
        }
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
        	String result = this.getEndValueImpl();
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "endValue", e);
        }
    }

    /**
     * True if the condition of the loop in the canonical format, and is one of: <, <=, >, >=
     */
    public abstract Boolean getHasCondRelationImpl();

    /**
     * True if the condition of the loop in the canonical format, and is one of: <, <=, >, >=
     */
    public final Object getHasCondRelation() {
        try {
        	Boolean result = this.getHasCondRelationImpl();
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "hasCondRelation", e);
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
        	String result = this.getIdImpl();
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "id", e);
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
        	AStatement result = this.getInitImpl();
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "init", e);
        }
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
        	String result = this.getInitValueImpl();
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "initValue", e);
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
        	Boolean result = this.getIsInnermostImpl();
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isInnermost", e);
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
        	Boolean result = this.isInterchangeableImpl(otherLoop);
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isInterchangeable", e);
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
        	Boolean result = this.getIsOutermostImpl();
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isOutermost", e);
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
        	Boolean result = this.getIsParallelImpl();
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isParallel", e);
        }
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
        	Integer result = this.getIterationsImpl();
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
        	AExpression result = this.getIterationsExprImpl();
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "iterationsExpr", e);
        }
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
        	String result = this.getKindImpl();
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "kind", e);
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
        	Integer result = this.getNestedLevelImpl();
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "nestedLevel", e);
        }
    }

    /**
     * Get value on attribute rank
     * @return the attribute's value
     */
    public abstract int[] getRankArrayImpl();

    /**
     * Get value on attribute rank
     * @return the attribute's value
     */
    public Object getRankImpl() {
        int[] intArrayImpl0 = getRankArrayImpl();
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(intArrayImpl0);
        return nativeArray0;
    }

    /**
     * Get value on attribute rank
     * @return the attribute's value
     */
    public final Object getRank() {
        try {
        	Object result = this.getRankImpl();
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "rank", e);
        }
    }

    /**
     * The statement of the loop step
     */
    public abstract AStatement getStepImpl();

    /**
     * The statement of the loop step
     */
    public final Object getStep() {
        try {
        	AStatement result = this.getStepImpl();
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "step", e);
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
        	String result = this.getStepValueImpl();
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "stepValue", e);
        }
    }

    /**
     * Interchanges two for loops, if possible
     * @param otherLoop 
     */
    public void interchangeImpl(ALoop otherLoop) {
        throw new UnsupportedOperationException(get_class()+": Action interchange not implemented ");
    }

    /**
     * Interchanges two for loops, if possible
     * @param otherLoop 
     */
    public final void interchange(ALoop otherLoop) {
        try {
        	this.interchangeImpl(otherLoop);
        } catch(Exception e) {
        	throw new ActionException(get_class(), "interchange", e);
        }
    }

    /**
     * Sets the body of the loop
     * @param body 
     */
    public void setBodyImpl(AScope body) {
        throw new UnsupportedOperationException(get_class()+": Action setBody not implemented ");
    }

    /**
     * Sets the body of the loop
     * @param body 
     */
    public final void setBody(AScope body) {
        try {
        	this.setBodyImpl(body);
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setBody", e);
        }
    }

    /**
     * Sets the conditional statement of the loop. Works with loops of kind 'for'
     * @param condCode 
     */
    public void setCondImpl(String condCode) {
        throw new UnsupportedOperationException(get_class()+": Action setCond not implemented ");
    }

    /**
     * Sets the conditional statement of the loop. Works with loops of kind 'for'
     * @param condCode 
     */
    public final void setCond(String condCode) {
        try {
        	this.setCondImpl(condCode);
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setCond", e);
        }
    }

    /**
     * Changes the operator of a canonical condition, if possible. Supported operators: lt, le, gt, ge
     * @param operator 
     */
    public void setCondRelationImpl(String operator) {
        throw new UnsupportedOperationException(get_class()+": Action setCondRelation not implemented ");
    }

    /**
     * Changes the operator of a canonical condition, if possible. Supported operators: lt, le, gt, ge
     * @param operator 
     */
    public final void setCondRelation(String operator) {
        try {
        	this.setCondRelationImpl(operator);
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setCondRelation", e);
        }
    }

    /**
     * Sets the end value of the loop. Works with loops of kind 'for'
     * @param initCode 
     */
    public void setEndValueImpl(String initCode) {
        throw new UnsupportedOperationException(get_class()+": Action setEndValue not implemented ");
    }

    /**
     * Sets the end value of the loop. Works with loops of kind 'for'
     * @param initCode 
     */
    public final void setEndValue(String initCode) {
        try {
        	this.setEndValueImpl(initCode);
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setEndValue", e);
        }
    }

    /**
     * Sets the init statement of the loop
     * @param initCode 
     */
    public void setInitImpl(String initCode) {
        throw new UnsupportedOperationException(get_class()+": Action setInit not implemented ");
    }

    /**
     * Sets the init statement of the loop
     * @param initCode 
     */
    public final void setInit(String initCode) {
        try {
        	this.setInitImpl(initCode);
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setInit", e);
        }
    }

    /**
     * Sets the init value of the loop. Works with loops of kind 'for'
     * @param initCode 
     */
    public void setInitValueImpl(String initCode) {
        throw new UnsupportedOperationException(get_class()+": Action setInitValue not implemented ");
    }

    /**
     * Sets the init value of the loop. Works with loops of kind 'for'
     * @param initCode 
     */
    public final void setInitValue(String initCode) {
        try {
        	this.setInitValueImpl(initCode);
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setInitValue", e);
        }
    }

    /**
     * Sets the attribute 'isParallel' of the loop
     * @param isParallel 
     */
    public void setIsParallelImpl(Boolean isParallel) {
        throw new UnsupportedOperationException(get_class()+": Action setIsParallel not implemented ");
    }

    /**
     * Sets the attribute 'isParallel' of the loop
     * @param isParallel 
     */
    public final void setIsParallel(Boolean isParallel) {
        try {
        	this.setIsParallelImpl(isParallel);
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setIsParallel", e);
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
        	this.setKindImpl(kind);
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setKind", e);
        }
    }

    /**
     * Sets the step statement of the loop. Works with loops of kind 'for'
     * @param stepCode 
     */
    public void setStepImpl(String stepCode) {
        throw new UnsupportedOperationException(get_class()+": Action setStep not implemented ");
    }

    /**
     * Sets the step statement of the loop. Works with loops of kind 'for'
     * @param stepCode 
     */
    public final void setStep(String stepCode) {
        try {
        	this.setStepImpl(stepCode);
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setStep", e);
        }
    }

    /**
     * Applies loop tiling to this loop.
     * @param blockSize 
     * @param reference 
     * @param useTernary 
     */
    public AStatement tileImpl(String blockSize, AStatement reference, Boolean useTernary) {
        throw new UnsupportedOperationException(get_class()+": Action tile not implemented ");
    }

    /**
     * Applies loop tiling to this loop.
     * @param blockSize 
     * @param reference 
     * @param useTernary 
     */
    public final Object tile(String blockSize, AStatement reference, Boolean useTernary) {
        try {
        	AStatement result = this.tileImpl(blockSize, reference, useTernary);
        	return result!=null?result:getUndefinedValue();
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
     * Get value on attribute ast
     * @return the attribute's value
     */
    @Override
    public String getAstImpl() {
        return this.aStatement.getAstImpl();
    }

    /**
     * Get value on attribute astChildrenArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] getAstChildrenArrayImpl() {
        return this.aStatement.getAstChildrenArrayImpl();
    }

    /**
     * Get value on attribute astId
     * @return the attribute's value
     */
    @Override
    public String getAstIdImpl() {
        return this.aStatement.getAstIdImpl();
    }

    /**
     * Get value on attribute astIsInstance
     * @return the attribute's value
     */
    @Override
    public Boolean astIsInstanceImpl(String className) {
        return this.aStatement.astIsInstanceImpl(className);
    }

    /**
     * Get value on attribute astName
     * @return the attribute's value
     */
    @Override
    public String getAstNameImpl() {
        return this.aStatement.getAstNameImpl();
    }

    /**
     * Get value on attribute astNumChildren
     * @return the attribute's value
     */
    @Override
    public Integer getAstNumChildrenImpl() {
        return this.aStatement.getAstNumChildrenImpl();
    }

    /**
     * Get value on attribute bitWidth
     * @return the attribute's value
     */
    @Override
    public Integer getBitWidthImpl() {
        return this.aStatement.getBitWidthImpl();
    }

    /**
     * Get value on attribute chainArrayImpl
     * @return the attribute's value
     */
    @Override
    public String[] getChainArrayImpl() {
        return this.aStatement.getChainArrayImpl();
    }

    /**
     * Get value on attribute childrenArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] getChildrenArrayImpl() {
        return this.aStatement.getChildrenArrayImpl();
    }

    /**
     * Get value on attribute code
     * @return the attribute's value
     */
    @Override
    public String getCodeImpl() {
        return this.aStatement.getCodeImpl();
    }

    /**
     * Get value on attribute column
     * @return the attribute's value
     */
    @Override
    public Integer getColumnImpl() {
        return this.aStatement.getColumnImpl();
    }

    /**
     * Get value on attribute contains
     * @return the attribute's value
     */
    @Override
    public Boolean containsImpl(AJoinPoint jp) {
        return this.aStatement.containsImpl(jp);
    }

    /**
     * Get value on attribute currentRegion
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getCurrentRegionImpl() {
        return this.aStatement.getCurrentRegionImpl();
    }

    /**
     * Get value on attribute data
     * @return the attribute's value
     */
    @Override
    public Object getDataImpl() {
        return this.aStatement.getDataImpl();
    }

    /**
     * Get value on attribute depth
     * @return the attribute's value
     */
    @Override
    public Integer getDepthImpl() {
        return this.aStatement.getDepthImpl();
    }

    /**
     * Get value on attribute descendantsArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] getDescendantsArrayImpl() {
        return this.aStatement.getDescendantsArrayImpl();
    }

    /**
     * Get value on attribute endColumn
     * @return the attribute's value
     */
    @Override
    public Integer getEndColumnImpl() {
        return this.aStatement.getEndColumnImpl();
    }

    /**
     * Get value on attribute endLine
     * @return the attribute's value
     */
    @Override
    public Integer getEndLineImpl() {
        return this.aStatement.getEndLineImpl();
    }

    /**
     * Get value on attribute filename
     * @return the attribute's value
     */
    @Override
    public String getFilenameImpl() {
        return this.aStatement.getFilenameImpl();
    }

    /**
     * Get value on attribute filepath
     * @return the attribute's value
     */
    @Override
    public String getFilepathImpl() {
        return this.aStatement.getFilepathImpl();
    }

    /**
     * Get value on attribute firstChild
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getFirstChildImpl() {
        return this.aStatement.getFirstChildImpl();
    }

    /**
     * Get value on attribute getAncestor
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getAncestorImpl(String type) {
        return this.aStatement.getAncestorImpl(type);
    }

    /**
     * Get value on attribute getAstAncestor
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getAstAncestorImpl(String type) {
        return this.aStatement.getAstAncestorImpl(type);
    }

    /**
     * Get value on attribute getAstChild
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getAstChildImpl(int index) {
        return this.aStatement.getAstChildImpl(index);
    }

    /**
     * Get value on attribute getChainAncestor
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getChainAncestorImpl(String type) {
        return this.aStatement.getChainAncestorImpl(type);
    }

    /**
     * Get value on attribute getChild
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getChildImpl(int index) {
        return this.aStatement.getChildImpl(index);
    }

    /**
     * Get value on attribute getDescendantsArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] getDescendantsArrayImpl(String type) {
        return this.aStatement.getDescendantsArrayImpl(type);
    }

    /**
     * Get value on attribute getDescendantsAndSelfArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] getDescendantsAndSelfArrayImpl(String type) {
        return this.aStatement.getDescendantsAndSelfArrayImpl(type);
    }

    /**
     * Get value on attribute getFirstJp
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getFirstJpImpl(String type) {
        return this.aStatement.getFirstJpImpl(type);
    }

    /**
     * Get value on attribute getJavaFieldType
     * @return the attribute's value
     */
    @Override
    public String getJavaFieldTypeImpl(String fieldName) {
        return this.aStatement.getJavaFieldTypeImpl(fieldName);
    }

    /**
     * Get value on attribute getKeyType
     * @return the attribute's value
     */
    @Override
    public Object getKeyTypeImpl(String key) {
        return this.aStatement.getKeyTypeImpl(key);
    }

    /**
     * Get value on attribute getUserField
     * @return the attribute's value
     */
    @Override
    public Object getUserFieldImpl(String fieldName) {
        return this.aStatement.getUserFieldImpl(fieldName);
    }

    /**
     * Get value on attribute getValue
     * @return the attribute's value
     */
    @Override
    public Object getValueImpl(String key) {
        return this.aStatement.getValueImpl(key);
    }

    /**
     * Get value on attribute hasChildren
     * @return the attribute's value
     */
    @Override
    public Boolean getHasChildrenImpl() {
        return this.aStatement.getHasChildrenImpl();
    }

    /**
     * Get value on attribute hasNode
     * @return the attribute's value
     */
    @Override
    public Boolean hasNodeImpl(Object nodeOrJp) {
        return this.aStatement.hasNodeImpl(nodeOrJp);
    }

    /**
     * Get value on attribute hasParent
     * @return the attribute's value
     */
    @Override
    public Boolean getHasParentImpl() {
        return this.aStatement.getHasParentImpl();
    }

    /**
     * Get value on attribute hasType
     * @return the attribute's value
     */
    @Override
    public Boolean getHasTypeImpl() {
        return this.aStatement.getHasTypeImpl();
    }

    /**
     * Get value on attribute inlineCommentsArrayImpl
     * @return the attribute's value
     */
    @Override
    public AComment[] getInlineCommentsArrayImpl() {
        return this.aStatement.getInlineCommentsArrayImpl();
    }

    /**
     * Get value on attribute isCilk
     * @return the attribute's value
     */
    @Override
    public Boolean getIsCilkImpl() {
        return this.aStatement.getIsCilkImpl();
    }

    /**
     * Get value on attribute isInSystemHeader
     * @return the attribute's value
     */
    @Override
    public Boolean getIsInSystemHeaderImpl() {
        return this.aStatement.getIsInSystemHeaderImpl();
    }

    /**
     * Get value on attribute isInsideHeader
     * @return the attribute's value
     */
    @Override
    public Boolean getIsInsideHeaderImpl() {
        return this.aStatement.getIsInsideHeaderImpl();
    }

    /**
     * Get value on attribute isInsideLoopHeader
     * @return the attribute's value
     */
    @Override
    public Boolean getIsInsideLoopHeaderImpl() {
        return this.aStatement.getIsInsideLoopHeaderImpl();
    }

    /**
     * Get value on attribute isMacro
     * @return the attribute's value
     */
    @Override
    public Boolean getIsMacroImpl() {
        return this.aStatement.getIsMacroImpl();
    }

    /**
     * Get value on attribute javaFieldsArrayImpl
     * @return the attribute's value
     */
    @Override
    public String[] getJavaFieldsArrayImpl() {
        return this.aStatement.getJavaFieldsArrayImpl();
    }

    /**
     * Get value on attribute jpId
     * @return the attribute's value
     */
    @Override
    public String getJpIdImpl() {
        return this.aStatement.getJpIdImpl();
    }

    /**
     * Get value on attribute keysArrayImpl
     * @return the attribute's value
     */
    @Override
    public String[] getKeysArrayImpl() {
        return this.aStatement.getKeysArrayImpl();
    }

    /**
     * Get value on attribute lastChild
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getLastChildImpl() {
        return this.aStatement.getLastChildImpl();
    }

    /**
     * Get value on attribute leftJp
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getLeftJpImpl() {
        return this.aStatement.getLeftJpImpl();
    }

    /**
     * Get value on attribute line
     * @return the attribute's value
     */
    @Override
    public Integer getLineImpl() {
        return this.aStatement.getLineImpl();
    }

    /**
     * Get value on attribute location
     * @return the attribute's value
     */
    @Override
    public String getLocationImpl() {
        return this.aStatement.getLocationImpl();
    }

    /**
     * Get value on attribute numChildren
     * @return the attribute's value
     */
    @Override
    public Integer getNumChildrenImpl() {
        return this.aStatement.getNumChildrenImpl();
    }

    /**
     * Get value on attribute originNode
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getOriginNodeImpl() {
        return this.aStatement.getOriginNodeImpl();
    }

    /**
     * Get value on attribute parent
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getParentImpl() {
        return this.aStatement.getParentImpl();
    }

    /**
     * Get value on attribute parentRegion
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getParentRegionImpl() {
        return this.aStatement.getParentRegionImpl();
    }

    /**
     * Get value on attribute pragmasArrayImpl
     * @return the attribute's value
     */
    @Override
    public APragma[] getPragmasArrayImpl() {
        return this.aStatement.getPragmasArrayImpl();
    }

    /**
     * Get value on attribute rightJp
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getRightJpImpl() {
        return this.aStatement.getRightJpImpl();
    }

    /**
     * Get value on attribute root
     * @return the attribute's value
     */
    @Override
    public AProgram getRootImpl() {
        return this.aStatement.getRootImpl();
    }

    /**
     * Get value on attribute scopeNodesArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] getScopeNodesArrayImpl() {
        return this.aStatement.getScopeNodesArrayImpl();
    }

    /**
     * Get value on attribute siblingsLeftArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] getSiblingsLeftArrayImpl() {
        return this.aStatement.getSiblingsLeftArrayImpl();
    }

    /**
     * Get value on attribute siblingsRightArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] getSiblingsRightArrayImpl() {
        return this.aStatement.getSiblingsRightArrayImpl();
    }

    /**
     * Get value on attribute stmt
     * @return the attribute's value
     */
    @Override
    public AStatement getStmtImpl() {
        return this.aStatement.getStmtImpl();
    }

    /**
     * Get value on attribute type
     * @return the attribute's value
     */
    @Override
    public AType getTypeImpl() {
        return this.aStatement.getTypeImpl();
    }

    /**
     * Performs a copy of the node and its children, but not of the nodes in its fields
     */
    @Override
    public AJoinPoint copyImpl() {
        return this.aStatement.copyImpl();
    }

    /**
     * Clears all properties from the .data object
     */
    @Override
    public void dataClearImpl() {
        this.aStatement.dataClearImpl();
    }

    /**
     * Performs a copy of the node and its children, including the nodes in their fields (only the first level of field nodes, this function is not recursive)
     */
    @Override
    public AJoinPoint deepCopyImpl() {
        return this.aStatement.deepCopyImpl();
    }

    /**
     * Removes the node associated to this joinpoint from the AST
     */
    @Override
    public AJoinPoint detachImpl() {
        return this.aStatement.detachImpl();
    }

    /**
     * 
     * @param position 
     * @param code 
     */
    @Override
    public AJoinPoint[] insertImpl(String position, String code) {
        return this.aStatement.insertImpl(position, code);
    }

    /**
     * 
     * @param position 
     * @param code 
     */
    @Override
    public AJoinPoint[] insertImpl(String position, JoinPoint code) {
        return this.aStatement.insertImpl(position, code);
    }

    /**
     * Inserts the given join point after this join point
     * @param node 
     */
    @Override
    public AJoinPoint insertAfterImpl(AJoinPoint node) {
        return this.aStatement.insertAfterImpl(node);
    }

    /**
     * Overload which accepts a string
     * @param code 
     */
    @Override
    public AJoinPoint insertAfterImpl(String code) {
        return this.aStatement.insertAfterImpl(code);
    }

    /**
     * Inserts the given join point before this join point
     * @param node 
     */
    @Override
    public AJoinPoint insertBeforeImpl(AJoinPoint node) {
        return this.aStatement.insertBeforeImpl(node);
    }

    /**
     * Overload which accepts a string
     * @param node 
     */
    @Override
    public AJoinPoint insertBeforeImpl(String node) {
        return this.aStatement.insertBeforeImpl(node);
    }

    /**
     * Adds a message that will be printed to the user after weaving finishes. Identical messages are removed
     * @param message 
     */
    @Override
    public void messageToUserImpl(String message) {
        this.aStatement.messageToUserImpl(message);
    }

    /**
     * Removes the children of this node
     */
    @Override
    public void removeChildrenImpl() {
        this.aStatement.removeChildrenImpl();
    }

    /**
     * Replaces this node with the given node
     * @param node 
     */
    @Override
    public AJoinPoint replaceWithImpl(AJoinPoint node) {
        return this.aStatement.replaceWithImpl(node);
    }

    /**
     * Overload which accepts a string
     * @param node 
     */
    @Override
    public AJoinPoint replaceWithImpl(String node) {
        return this.aStatement.replaceWithImpl(node);
    }

    /**
     * Overload which accepts a list of join points
     * @param node 
     */
    @Override
    public AJoinPoint replaceWithImpl(AJoinPoint[] node) {
        return this.aStatement.replaceWithImpl(node);
    }

    /**
     * Overload which accepts a list of strings
     * @param node 
     */
    @Override
    public AJoinPoint replaceWithStringsImpl(String[] node) {
        return this.aStatement.replaceWithStringsImpl(node);
    }

    /**
     * Setting data directly is not supported, this action just emits a warning and does nothing
     * @param source 
     */
    @Override
    public void setDataImpl(Object source) {
        this.aStatement.setDataImpl(source);
    }

    /**
     * Replaces the first child, or inserts the join point if no child is present. Returns the replaced child, or undefined if there was no child present.
     * @param node 
     */
    @Override
    public AJoinPoint setFirstChildImpl(AJoinPoint node) {
        return this.aStatement.setFirstChildImpl(node);
    }

    /**
     * Sets the commented that are embedded in a node
     * @param comments 
     */
    @Override
    public void setInlineCommentsImpl(String[] comments) {
        this.aStatement.setInlineCommentsImpl(comments);
    }

    /**
     * Sets the commented that are embedded in a node
     * @param comments 
     */
    @Override
    public void setInlineCommentsImpl(String comments) {
        this.aStatement.setInlineCommentsImpl(comments);
    }

    /**
     * Replaces the last child, or inserts the join point if no child is present. Returns the replaced child, or undefined if there was no child present.
     * @param node 
     */
    @Override
    public AJoinPoint setLastChildImpl(AJoinPoint node) {
        return this.aStatement.setLastChildImpl(node);
    }

    /**
     * Sets the type of a node, if it has a type
     * @param type 
     */
    @Override
    public void setTypeImpl(AType type) {
        this.aStatement.setTypeImpl(type);
    }

    /**
     * Associates arbitrary values to nodes of the AST
     * @param fieldName 
     * @param value 
     */
    @Override
    public Object setUserFieldImpl(String fieldName, Object value) {
        return this.aStatement.setUserFieldImpl(fieldName, value);
    }

    /**
     * Overload which accepts a map
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
     * Replaces this join point with a comment with the same contents as .code
     * @param prefix 
     * @param suffix 
     */
    @Override
    public AJoinPoint toCommentImpl(String prefix, String suffix) {
        return this.aStatement.toCommentImpl(prefix, suffix);
    }

    /**
     * 
     */
    @Override
    public Optional<? extends AStatement> getSuper() {
        return Optional.of(this.aStatement);
    }

    /**
     * Returns the join point type of this class
     * @return The join point type
     */
    @Override
    public String get_class() {
        return "loop";
    }

    /**
     * Defines if this joinpoint is an instanceof a given joinpoint class
     * @return True if this join point is an instanceof the given class
     */
    @Override
    public boolean instanceOf(String joinpointClass) {
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
        BODY("body"),
        COND("cond"),
        CONDRELATION("condRelation"),
        CONTROLVAR("controlVar"),
        ENDVALUE("endValue"),
        HASCONDRELATION("hasCondRelation"),
        ID("id"),
        INIT("init"),
        INITVALUE("initValue"),
        ISINNERMOST("isInnermost"),
        ISINTERCHANGEABLE("isInterchangeable"),
        ISOUTERMOST("isOutermost"),
        ISPARALLEL("isParallel"),
        ITERATIONS("iterations"),
        ITERATIONSEXPR("iterationsExpr"),
        KIND("kind"),
        NESTEDLEVEL("nestedLevel"),
        RANK("rank"),
        STEP("step"),
        STEPVALUE("stepValue"),
        ISFIRST("isFirst"),
        ISLAST("isLast"),
        AST("ast"),
        ASTCHILDREN("astChildren"),
        ASTID("astId"),
        ASTISINSTANCE("astIsInstance"),
        ASTNAME("astName"),
        ASTNUMCHILDREN("astNumChildren"),
        BITWIDTH("bitWidth"),
        CHAIN("chain"),
        CHILDREN("children"),
        CODE("code"),
        COLUMN("column"),
        CONTAINS("contains"),
        CURRENTREGION("currentRegion"),
        DATA("data"),
        DEPTH("depth"),
        DESCENDANTS("descendants"),
        ENDCOLUMN("endColumn"),
        ENDLINE("endLine"),
        FILENAME("filename"),
        FILEPATH("filepath"),
        FIRSTCHILD("firstChild"),
        GETANCESTOR("getAncestor"),
        GETASTANCESTOR("getAstAncestor"),
        GETASTCHILD("getAstChild"),
        GETCHAINANCESTOR("getChainAncestor"),
        GETCHILD("getChild"),
        GETDESCENDANTS("getDescendants"),
        GETDESCENDANTSANDSELF("getDescendantsAndSelf"),
        GETFIRSTJP("getFirstJp"),
        GETJAVAFIELDTYPE("getJavaFieldType"),
        GETKEYTYPE("getKeyType"),
        GETUSERFIELD("getUserField"),
        GETVALUE("getValue"),
        HASCHILDREN("hasChildren"),
        HASNODE("hasNode"),
        HASPARENT("hasParent"),
        HASTYPE("hasType"),
        INLINECOMMENTS("inlineComments"),
        ISCILK("isCilk"),
        ISINSYSTEMHEADER("isInSystemHeader"),
        ISINSIDEHEADER("isInsideHeader"),
        ISINSIDELOOPHEADER("isInsideLoopHeader"),
        ISMACRO("isMacro"),
        JAVAFIELDS("javaFields"),
        JPID("jpId"),
        KEYS("keys"),
        LASTCHILD("lastChild"),
        LEFTJP("leftJp"),
        LINE("line"),
        LOCATION("location"),
        NUMCHILDREN("numChildren"),
        ORIGINNODE("originNode"),
        PARENT("parent"),
        PARENTREGION("parentRegion"),
        PRAGMAS("pragmas"),
        RIGHTJP("rightJp"),
        ROOT("root"),
        SCOPENODES("scopeNodes"),
        SIBLINGSLEFT("siblingsLeft"),
        SIBLINGSRIGHT("siblingsRight"),
        STMT("stmt"),
        TYPE("type");
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
