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
 * Auto-Generated class for join point ACall
 * This class is overwritten by the Weaver Generator.
 * 
 * 
 * @author Lara Weaver Generator
 */
public abstract class ACall extends AExpression {

    protected AExpression aExpression;

    /**
     * 
     */
    public ACall(AExpression aExpression){
        this.aExpression = aExpression;
    }
    /**
     * Get value on attribute argList
     * @return the attribute's value
     */
    public abstract AExpression[] getArgListArrayImpl();

    /**
     * an alias for 'args'
     */
    public Object getArgListImpl() {
        AExpression[] aExpressionArrayImpl0 = getArgListArrayImpl();
        Object nativeArray0 = aExpressionArrayImpl0;
        return nativeArray0;
    }

    /**
     * an alias for 'args'
     */
    public final Object getArgList() {
        try {
        	Object result = this.getArgListImpl();
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "argList", e);
        }
    }

    /**
     * Get value on attribute args
     * @return the attribute's value
     */
    public abstract AExpression[] getArgsArrayImpl();

    /**
     * an array with the arguments of the call
     */
    public Object getArgsImpl() {
        AExpression[] aExpressionArrayImpl0 = getArgsArrayImpl();
        Object nativeArray0 = aExpressionArrayImpl0;
        return nativeArray0;
    }

    /**
     * an array with the arguments of the call
     */
    public final Object getArgs() {
        try {
        	Object result = this.getArgsImpl();
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "args", e);
        }
    }

    /**
     * a 'function' join point that represents the function of the call that was found, it can return either an implementation or a function prototype; 'undefined' if no declaration was found
     */
    public abstract AFunction getDeclarationImpl();

    /**
     * a 'function' join point that represents the function of the call that was found, it can return either an implementation or a function prototype; 'undefined' if no declaration was found
     */
    public final Object getDeclaration() {
        try {
        	AFunction result = this.getDeclarationImpl();
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "declaration", e);
        }
    }

    /**
     * a 'function' join point that represents the function definition of the call; 'undefined' if no definition was found
     */
    public abstract AFunction getDefinitionImpl();

    /**
     * a 'function' join point that represents the function definition of the call; 'undefined' if no definition was found
     */
    public final Object getDefinition() {
        try {
        	AFunction result = this.getDefinitionImpl();
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "definition", e);
        }
    }

    /**
     * a function join point that represents the 'raw' function of the call (e.g. if this is a call to a templated function, returns a declaration representing the template specialization, instead of the original function)
     */
    public abstract AFunction getDirectCalleeImpl();

    /**
     * a function join point that represents the 'raw' function of the call (e.g. if this is a call to a templated function, returns a declaration representing the template specialization, instead of the original function)
     */
    public final Object getDirectCallee() {
        try {
        	AFunction result = this.getDirectCalleeImpl();
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "directCallee", e);
        }
    }

    /**
     * a function join point associated with this call. If a definition is present, it is given priority over returning a declaration. If only declarations are present, returns a declaration
     */
    public abstract AFunction getFunctionImpl();

    /**
     * a function join point associated with this call. If a definition is present, it is given priority over returning a declaration. If only declarations are present, returns a declaration
     */
    public final Object getFunction() {
        try {
        	AFunction result = this.getFunctionImpl();
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "function", e);
        }
    }

    /**
     * the function type of the call, which includes the return type and the types of the parameters
     */
    public abstract AFunctionType getFunctionTypeImpl();

    /**
     * the function type of the call, which includes the return type and the types of the parameters
     */
    public final Object getFunctionType() {
        try {
        	AFunctionType result = this.getFunctionTypeImpl();
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "functionType", e);
        }
    }

    /**
     * 
     * @param index
     * @return 
     */
    public abstract AExpression getArgImpl(int index);

    /**
     * 
     * @param index
     * @return 
     */
    public final Object getArg(int index) {
        try {
        	AExpression result = this.getArgImpl(index);
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "getArg", e);
        }
    }

    /**
     * Get value on attribute isMemberAccess
     * @return the attribute's value
     */
    public abstract Boolean getIsMemberAccessImpl();

    /**
     * Get value on attribute isMemberAccess
     * @return the attribute's value
     */
    public final Object getIsMemberAccess() {
        try {
        	Boolean result = this.getIsMemberAccessImpl();
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isMemberAccess", e);
        }
    }

    /**
     * Get value on attribute isStmtCall
     * @return the attribute's value
     */
    public abstract Boolean getIsStmtCallImpl();

    /**
     * Get value on attribute isStmtCall
     * @return the attribute's value
     */
    public final Object getIsStmtCall() {
        try {
        	Boolean result = this.getIsStmtCallImpl();
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isStmtCall", e);
        }
    }

    /**
     * Get value on attribute memberAccess
     * @return the attribute's value
     */
    public abstract AMemberAccess getMemberAccessImpl();

    /**
     * Get value on attribute memberAccess
     * @return the attribute's value
     */
    public final Object getMemberAccess() {
        try {
        	AMemberAccess result = this.getMemberAccessImpl();
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "memberAccess", e);
        }
    }

    /**
     * Get value on attribute memberNames
     * @return the attribute's value
     */
    public abstract String[] getMemberNamesArrayImpl();

    /**
     * Get value on attribute memberNames
     * @return the attribute's value
     */
    public Object getMemberNamesImpl() {
        String[] stringArrayImpl0 = getMemberNamesArrayImpl();
        Object nativeArray0 = stringArrayImpl0;
        return nativeArray0;
    }

    /**
     * Get value on attribute memberNames
     * @return the attribute's value
     */
    public final Object getMemberNames() {
        try {
        	Object result = this.getMemberNamesImpl();
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "memberNames", e);
        }
    }

    /**
     * Get value on attribute name
     * @return the attribute's value
     */
    public abstract String getNameImpl();

    /**
     * Get value on attribute name
     * @return the attribute's value
     */
    public final Object getName() {
        try {
        	String result = this.getNameImpl();
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "name", e);
        }
    }

    /**
     * Get value on attribute numArgs
     * @return the attribute's value
     */
    public abstract Integer getNumArgsImpl();

    /**
     * Get value on attribute numArgs
     * @return the attribute's value
     */
    public final Object getNumArgs() {
        try {
        	Integer result = this.getNumArgsImpl();
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "numArgs", e);
        }
    }

    /**
     * the return type of the call
     */
    public abstract AType getReturnTypeImpl();

    /**
     * the return type of the call
     */
    public final Object getReturnType() {
        try {
        	AType result = this.getReturnTypeImpl();
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "returnType", e);
        }
    }

    /**
     * similar to $function.signature, but if no function decl could be found (e.g., function from system include), returns a signature based on just the name of the function
     */
    public abstract String getSignatureImpl();

    /**
     * similar to $function.signature, but if no function decl could be found (e.g., function from system include), returns a signature based on just the name of the function
     */
    public final Object getSignature() {
        try {
        	String result = this.getSignatureImpl();
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "signature", e);
        }
    }

    /**
     * Adds an argument at the end of the call, creating an expression using the given code and type. If a type is not provided, a dummy type is used
     * @param argCode 
     * @param type 
     */
    public void addArgImpl(String argCode, AType type) {
        throw new UnsupportedOperationException(get_class()+": Action addArg not implemented ");
    }

    /**
     * Adds an argument at the end of the call, creating an expression using the given code and type. If a type is not provided, a dummy type is used
     * @param argCode 
     * @param type 
     */
    public final void addArg(String argCode, AType type) {
        try {
        	this.addArgImpl(argCode, type);
        } catch(Exception e) {
        	throw new ActionException(get_class(), "addArg", e);
        }
    }

    /**
     * Adds an argument at the end of the call, creating a literal 'type' from the type string
     * @param arg 
     * @param type 
     */
    public void addArgImpl(String arg, String type) {
        throw new UnsupportedOperationException(get_class()+": Action addArg not implemented ");
    }

    /**
     * Adds an argument at the end of the call, creating a literal 'type' from the type string
     * @param arg 
     * @param type 
     */
    public final void addArg(String arg, String type) {
        try {
        	this.addArgImpl(arg, type);
        } catch(Exception e) {
        	throw new ActionException(get_class(), "addArg", e);
        }
    }

    /**
     * Tries to inline this call
     */
    public boolean inlineImpl() {
        throw new UnsupportedOperationException(get_class()+": Action inline not implemented ");
    }

    /**
     * Tries to inline this call
     */
    public final Object inline() {
        try {
        	boolean result = this.inlineImpl();
        	return result;
        } catch(Exception e) {
        	throw new ActionException(get_class(), "inline", e);
        }
    }

    /**
     * 
     * @param index 
     * @param expr 
     */
    public void setArgImpl(int index, AExpression expr) {
        throw new UnsupportedOperationException(get_class()+": Action setArg not implemented ");
    }

    /**
     * 
     * @param index 
     * @param expr 
     */
    public final void setArg(int index, AExpression expr) {
        try {
        	this.setArgImpl(index, expr);
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setArg", e);
        }
    }

    /**
     * 
     * @param index 
     * @param expr 
     */
    public void setArgFromStringImpl(int index, String expr) {
        throw new UnsupportedOperationException(get_class()+": Action setArgFromString not implemented ");
    }

    /**
     * 
     * @param index 
     * @param expr 
     */
    public final void setArgFromString(int index, String expr) {
        try {
        	this.setArgFromStringImpl(index, expr);
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setArgFromString", e);
        }
    }

    /**
     * Changes the name of the call
     * @param name 
     */
    public void setNameImpl(String name) {
        throw new UnsupportedOperationException(get_class()+": Action setName not implemented ");
    }

    /**
     * Changes the name of the call
     * @param name 
     */
    public final void setName(String name) {
        try {
        	this.setNameImpl(name);
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setName", e);
        }
    }

    /**
     * Wraps this call with a possibly new wrapping function
     * @param name 
     */
    public void wrapImpl(String name) {
        throw new UnsupportedOperationException(get_class()+": Action wrap not implemented ");
    }

    /**
     * Wraps this call with a possibly new wrapping function
     * @param name 
     */
    public final void wrap(String name) {
        try {
        	this.wrapImpl(name);
        } catch(Exception e) {
        	throw new ActionException(get_class(), "wrap", e);
        }
    }

    /**
     * Get value on attribute decl
     * @return the attribute's value
     */
    @Override
    public ADecl getDeclImpl() {
        return this.aExpression.getDeclImpl();
    }

    /**
     * Get value on attribute implicitCast
     * @return the attribute's value
     */
    @Override
    public ACast getImplicitCastImpl() {
        return this.aExpression.getImplicitCastImpl();
    }

    /**
     * Get value on attribute isFunctionArgument
     * @return the attribute's value
     */
    @Override
    public Boolean getIsFunctionArgumentImpl() {
        return this.aExpression.getIsFunctionArgumentImpl();
    }

    /**
     * Get value on attribute use
     * @return the attribute's value
     */
    @Override
    public String getUseImpl() {
        return this.aExpression.getUseImpl();
    }

    /**
     * Get value on attribute vardecl
     * @return the attribute's value
     */
    @Override
    public AVardecl getVardeclImpl() {
        return this.aExpression.getVardeclImpl();
    }

    /**
     * Get value on attribute ast
     * @return the attribute's value
     */
    @Override
    public String getAstImpl() {
        return this.aExpression.getAstImpl();
    }

    /**
     * Get value on attribute astChildrenArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] getAstChildrenArrayImpl() {
        return this.aExpression.getAstChildrenArrayImpl();
    }

    /**
     * Get value on attribute astId
     * @return the attribute's value
     */
    @Override
    public String getAstIdImpl() {
        return this.aExpression.getAstIdImpl();
    }

    /**
     * Get value on attribute astIsInstance
     * @return the attribute's value
     */
    @Override
    public Boolean astIsInstanceImpl(String className) {
        return this.aExpression.astIsInstanceImpl(className);
    }

    /**
     * Get value on attribute astName
     * @return the attribute's value
     */
    @Override
    public String getAstNameImpl() {
        return this.aExpression.getAstNameImpl();
    }

    /**
     * Get value on attribute astNumChildren
     * @return the attribute's value
     */
    @Override
    public Integer getAstNumChildrenImpl() {
        return this.aExpression.getAstNumChildrenImpl();
    }

    /**
     * Get value on attribute bitWidth
     * @return the attribute's value
     */
    @Override
    public Integer getBitWidthImpl() {
        return this.aExpression.getBitWidthImpl();
    }

    /**
     * Get value on attribute chainArrayImpl
     * @return the attribute's value
     */
    @Override
    public String[] getChainArrayImpl() {
        return this.aExpression.getChainArrayImpl();
    }

    /**
     * Get value on attribute childrenArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] getChildrenArrayImpl() {
        return this.aExpression.getChildrenArrayImpl();
    }

    /**
     * Get value on attribute code
     * @return the attribute's value
     */
    @Override
    public String getCodeImpl() {
        return this.aExpression.getCodeImpl();
    }

    /**
     * Get value on attribute column
     * @return the attribute's value
     */
    @Override
    public Integer getColumnImpl() {
        return this.aExpression.getColumnImpl();
    }

    /**
     * Get value on attribute contains
     * @return the attribute's value
     */
    @Override
    public Boolean containsImpl(AJoinPoint jp) {
        return this.aExpression.containsImpl(jp);
    }

    /**
     * Get value on attribute currentRegion
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getCurrentRegionImpl() {
        return this.aExpression.getCurrentRegionImpl();
    }

    /**
     * Get value on attribute data
     * @return the attribute's value
     */
    @Override
    public Object getDataImpl() {
        return this.aExpression.getDataImpl();
    }

    /**
     * Get value on attribute depth
     * @return the attribute's value
     */
    @Override
    public Integer getDepthImpl() {
        return this.aExpression.getDepthImpl();
    }

    /**
     * Get value on attribute descendantsArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] getDescendantsArrayImpl() {
        return this.aExpression.getDescendantsArrayImpl();
    }

    /**
     * Get value on attribute endColumn
     * @return the attribute's value
     */
    @Override
    public Integer getEndColumnImpl() {
        return this.aExpression.getEndColumnImpl();
    }

    /**
     * Get value on attribute endLine
     * @return the attribute's value
     */
    @Override
    public Integer getEndLineImpl() {
        return this.aExpression.getEndLineImpl();
    }

    /**
     * Get value on attribute filename
     * @return the attribute's value
     */
    @Override
    public String getFilenameImpl() {
        return this.aExpression.getFilenameImpl();
    }

    /**
     * Get value on attribute filepath
     * @return the attribute's value
     */
    @Override
    public String getFilepathImpl() {
        return this.aExpression.getFilepathImpl();
    }

    /**
     * Get value on attribute firstChild
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getFirstChildImpl() {
        return this.aExpression.getFirstChildImpl();
    }

    /**
     * Get value on attribute getAncestor
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getAncestorImpl(String type) {
        return this.aExpression.getAncestorImpl(type);
    }

    /**
     * Get value on attribute getAstAncestor
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getAstAncestorImpl(String type) {
        return this.aExpression.getAstAncestorImpl(type);
    }

    /**
     * Get value on attribute getAstChild
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getAstChildImpl(int index) {
        return this.aExpression.getAstChildImpl(index);
    }

    /**
     * Get value on attribute getChainAncestor
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getChainAncestorImpl(String type) {
        return this.aExpression.getChainAncestorImpl(type);
    }

    /**
     * Get value on attribute getChild
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getChildImpl(int index) {
        return this.aExpression.getChildImpl(index);
    }

    /**
     * Get value on attribute getDescendantsArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] getDescendantsArrayImpl(String type) {
        return this.aExpression.getDescendantsArrayImpl(type);
    }

    /**
     * Get value on attribute getDescendantsAndSelfArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] getDescendantsAndSelfArrayImpl(String type) {
        return this.aExpression.getDescendantsAndSelfArrayImpl(type);
    }

    /**
     * Get value on attribute getFirstJp
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getFirstJpImpl(String type) {
        return this.aExpression.getFirstJpImpl(type);
    }

    /**
     * Get value on attribute getJavaFieldType
     * @return the attribute's value
     */
    @Override
    public String getJavaFieldTypeImpl(String fieldName) {
        return this.aExpression.getJavaFieldTypeImpl(fieldName);
    }

    /**
     * Get value on attribute getKeyType
     * @return the attribute's value
     */
    @Override
    public Object getKeyTypeImpl(String key) {
        return this.aExpression.getKeyTypeImpl(key);
    }

    /**
     * Get value on attribute getUserField
     * @return the attribute's value
     */
    @Override
    public Object getUserFieldImpl(String fieldName) {
        return this.aExpression.getUserFieldImpl(fieldName);
    }

    /**
     * Get value on attribute getValue
     * @return the attribute's value
     */
    @Override
    public Object getValueImpl(String key) {
        return this.aExpression.getValueImpl(key);
    }

    /**
     * Get value on attribute hasChildren
     * @return the attribute's value
     */
    @Override
    public Boolean getHasChildrenImpl() {
        return this.aExpression.getHasChildrenImpl();
    }

    /**
     * Get value on attribute hasNode
     * @return the attribute's value
     */
    @Override
    public Boolean hasNodeImpl(Object nodeOrJp) {
        return this.aExpression.hasNodeImpl(nodeOrJp);
    }

    /**
     * Get value on attribute hasParent
     * @return the attribute's value
     */
    @Override
    public Boolean getHasParentImpl() {
        return this.aExpression.getHasParentImpl();
    }

    /**
     * Get value on attribute hasType
     * @return the attribute's value
     */
    @Override
    public Boolean getHasTypeImpl() {
        return this.aExpression.getHasTypeImpl();
    }

    /**
     * Get value on attribute inlineCommentsArrayImpl
     * @return the attribute's value
     */
    @Override
    public AComment[] getInlineCommentsArrayImpl() {
        return this.aExpression.getInlineCommentsArrayImpl();
    }

    /**
     * Get value on attribute isCilk
     * @return the attribute's value
     */
    @Override
    public Boolean getIsCilkImpl() {
        return this.aExpression.getIsCilkImpl();
    }

    /**
     * Get value on attribute isInSystemHeader
     * @return the attribute's value
     */
    @Override
    public Boolean getIsInSystemHeaderImpl() {
        return this.aExpression.getIsInSystemHeaderImpl();
    }

    /**
     * Get value on attribute isInsideHeader
     * @return the attribute's value
     */
    @Override
    public Boolean getIsInsideHeaderImpl() {
        return this.aExpression.getIsInsideHeaderImpl();
    }

    /**
     * Get value on attribute isInsideLoopHeader
     * @return the attribute's value
     */
    @Override
    public Boolean getIsInsideLoopHeaderImpl() {
        return this.aExpression.getIsInsideLoopHeaderImpl();
    }

    /**
     * Get value on attribute isMacro
     * @return the attribute's value
     */
    @Override
    public Boolean getIsMacroImpl() {
        return this.aExpression.getIsMacroImpl();
    }

    /**
     * Get value on attribute javaFieldsArrayImpl
     * @return the attribute's value
     */
    @Override
    public String[] getJavaFieldsArrayImpl() {
        return this.aExpression.getJavaFieldsArrayImpl();
    }

    /**
     * Get value on attribute jpId
     * @return the attribute's value
     */
    @Override
    public String getJpIdImpl() {
        return this.aExpression.getJpIdImpl();
    }

    /**
     * Get value on attribute keysArrayImpl
     * @return the attribute's value
     */
    @Override
    public String[] getKeysArrayImpl() {
        return this.aExpression.getKeysArrayImpl();
    }

    /**
     * Get value on attribute lastChild
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getLastChildImpl() {
        return this.aExpression.getLastChildImpl();
    }

    /**
     * Get value on attribute leftJp
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getLeftJpImpl() {
        return this.aExpression.getLeftJpImpl();
    }

    /**
     * Get value on attribute line
     * @return the attribute's value
     */
    @Override
    public Integer getLineImpl() {
        return this.aExpression.getLineImpl();
    }

    /**
     * Get value on attribute location
     * @return the attribute's value
     */
    @Override
    public String getLocationImpl() {
        return this.aExpression.getLocationImpl();
    }

    /**
     * Get value on attribute numChildren
     * @return the attribute's value
     */
    @Override
    public Integer getNumChildrenImpl() {
        return this.aExpression.getNumChildrenImpl();
    }

    /**
     * Get value on attribute originNode
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getOriginNodeImpl() {
        return this.aExpression.getOriginNodeImpl();
    }

    /**
     * Get value on attribute parent
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getParentImpl() {
        return this.aExpression.getParentImpl();
    }

    /**
     * Get value on attribute parentRegion
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getParentRegionImpl() {
        return this.aExpression.getParentRegionImpl();
    }

    /**
     * Get value on attribute pragmasArrayImpl
     * @return the attribute's value
     */
    @Override
    public APragma[] getPragmasArrayImpl() {
        return this.aExpression.getPragmasArrayImpl();
    }

    /**
     * Get value on attribute rightJp
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getRightJpImpl() {
        return this.aExpression.getRightJpImpl();
    }

    /**
     * Get value on attribute root
     * @return the attribute's value
     */
    @Override
    public AProgram getRootImpl() {
        return this.aExpression.getRootImpl();
    }

    /**
     * Get value on attribute scopeNodesArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] getScopeNodesArrayImpl() {
        return this.aExpression.getScopeNodesArrayImpl();
    }

    /**
     * Get value on attribute siblingsLeftArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] getSiblingsLeftArrayImpl() {
        return this.aExpression.getSiblingsLeftArrayImpl();
    }

    /**
     * Get value on attribute siblingsRightArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] getSiblingsRightArrayImpl() {
        return this.aExpression.getSiblingsRightArrayImpl();
    }

    /**
     * Get value on attribute stmt
     * @return the attribute's value
     */
    @Override
    public AStatement getStmtImpl() {
        return this.aExpression.getStmtImpl();
    }

    /**
     * Get value on attribute type
     * @return the attribute's value
     */
    @Override
    public AType getTypeImpl() {
        return this.aExpression.getTypeImpl();
    }

    /**
     * Performs a copy of the node and its children, but not of the nodes in its fields
     */
    @Override
    public AJoinPoint copyImpl() {
        return this.aExpression.copyImpl();
    }

    /**
     * Clears all properties from the .data object
     */
    @Override
    public void dataClearImpl() {
        this.aExpression.dataClearImpl();
    }

    /**
     * Performs a copy of the node and its children, including the nodes in their fields (only the first level of field nodes, this function is not recursive)
     */
    @Override
    public AJoinPoint deepCopyImpl() {
        return this.aExpression.deepCopyImpl();
    }

    /**
     * Removes the node associated to this joinpoint from the AST
     */
    @Override
    public AJoinPoint detachImpl() {
        return this.aExpression.detachImpl();
    }

    /**
     * 
     * @param position 
     * @param code 
     */
    @Override
    public AJoinPoint[] insertImpl(String position, String code) {
        return this.aExpression.insertImpl(position, code);
    }

    /**
     * 
     * @param position 
     * @param code 
     */
    @Override
    public AJoinPoint[] insertImpl(String position, JoinPoint code) {
        return this.aExpression.insertImpl(position, code);
    }

    /**
     * Inserts the given join point after this join point
     * @param node 
     */
    @Override
    public AJoinPoint insertAfterImpl(AJoinPoint node) {
        return this.aExpression.insertAfterImpl(node);
    }

    /**
     * Overload which accepts a string
     * @param code 
     */
    @Override
    public AJoinPoint insertAfterImpl(String code) {
        return this.aExpression.insertAfterImpl(code);
    }

    /**
     * Inserts the given join point before this join point
     * @param node 
     */
    @Override
    public AJoinPoint insertBeforeImpl(AJoinPoint node) {
        return this.aExpression.insertBeforeImpl(node);
    }

    /**
     * Overload which accepts a string
     * @param node 
     */
    @Override
    public AJoinPoint insertBeforeImpl(String node) {
        return this.aExpression.insertBeforeImpl(node);
    }

    /**
     * Adds a message that will be printed to the user after weaving finishes. Identical messages are removed
     * @param message 
     */
    @Override
    public void messageToUserImpl(String message) {
        this.aExpression.messageToUserImpl(message);
    }

    /**
     * Removes the children of this node
     */
    @Override
    public void removeChildrenImpl() {
        this.aExpression.removeChildrenImpl();
    }

    /**
     * Replaces this node with the given node
     * @param node 
     */
    @Override
    public AJoinPoint replaceWithImpl(AJoinPoint node) {
        return this.aExpression.replaceWithImpl(node);
    }

    /**
     * Overload which accepts a string
     * @param node 
     */
    @Override
    public AJoinPoint replaceWithImpl(String node) {
        return this.aExpression.replaceWithImpl(node);
    }

    /**
     * Overload which accepts a list of join points
     * @param node 
     */
    @Override
    public AJoinPoint replaceWithImpl(AJoinPoint[] node) {
        return this.aExpression.replaceWithImpl(node);
    }

    /**
     * Overload which accepts a list of strings
     * @param node 
     */
    @Override
    public AJoinPoint replaceWithStringsImpl(String[] node) {
        return this.aExpression.replaceWithStringsImpl(node);
    }

    /**
     * Setting data directly is not supported, this action just emits a warning and does nothing
     * @param source 
     */
    @Override
    public void setDataImpl(Object source) {
        this.aExpression.setDataImpl(source);
    }

    /**
     * Replaces the first child, or inserts the join point if no child is present. Returns the replaced child, or undefined if there was no child present.
     * @param node 
     */
    @Override
    public AJoinPoint setFirstChildImpl(AJoinPoint node) {
        return this.aExpression.setFirstChildImpl(node);
    }

    /**
     * Sets the commented that are embedded in a node
     * @param comments 
     */
    @Override
    public void setInlineCommentsImpl(String[] comments) {
        this.aExpression.setInlineCommentsImpl(comments);
    }

    /**
     * Sets the commented that are embedded in a node
     * @param comments 
     */
    @Override
    public void setInlineCommentsImpl(String comments) {
        this.aExpression.setInlineCommentsImpl(comments);
    }

    /**
     * Replaces the last child, or inserts the join point if no child is present. Returns the replaced child, or undefined if there was no child present.
     * @param node 
     */
    @Override
    public AJoinPoint setLastChildImpl(AJoinPoint node) {
        return this.aExpression.setLastChildImpl(node);
    }

    /**
     * Sets the type of a node, if it has a type
     * @param type 
     */
    @Override
    public void setTypeImpl(AType type) {
        this.aExpression.setTypeImpl(type);
    }

    /**
     * Associates arbitrary values to nodes of the AST
     * @param fieldName 
     * @param value 
     */
    @Override
    public Object setUserFieldImpl(String fieldName, Object value) {
        return this.aExpression.setUserFieldImpl(fieldName, value);
    }

    /**
     * Overload which accepts a map
     * @param fieldNameAndValue 
     */
    @Override
    public Object setUserFieldImpl(Map<?, ?> fieldNameAndValue) {
        return this.aExpression.setUserFieldImpl(fieldNameAndValue);
    }

    /**
     * Sets the value associated with the given property key
     * @param key 
     * @param value 
     */
    @Override
    public AJoinPoint setValueImpl(String key, Object value) {
        return this.aExpression.setValueImpl(key, value);
    }

    /**
     * Replaces this join point with a comment with the same contents as .code
     * @param prefix 
     * @param suffix 
     */
    @Override
    public AJoinPoint toCommentImpl(String prefix, String suffix) {
        return this.aExpression.toCommentImpl(prefix, suffix);
    }

    /**
     * 
     */
    @Override
    public Optional<? extends AExpression> getSuper() {
        return Optional.of(this.aExpression);
    }

    /**
     * Returns the join point type of this class
     * @return The join point type
     */
    @Override
    public String get_class() {
        return "call";
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
        return this.aExpression.instanceOf(joinpointClass);
    }
    /**
     * 
     */
    protected enum CallAttributes {
        ARGLIST("argList"),
        ARGS("args"),
        DECLARATION("declaration"),
        DEFINITION("definition"),
        DIRECTCALLEE("directCallee"),
        FUNCTION("function"),
        FUNCTIONTYPE("functionType"),
        GETARG("getArg"),
        ISMEMBERACCESS("isMemberAccess"),
        ISSTMTCALL("isStmtCall"),
        MEMBERACCESS("memberAccess"),
        MEMBERNAMES("memberNames"),
        NAME("name"),
        NUMARGS("numArgs"),
        RETURNTYPE("returnType"),
        SIGNATURE("signature"),
        DECL("decl"),
        IMPLICITCAST("implicitCast"),
        ISFUNCTIONARGUMENT("isFunctionArgument"),
        USE("use"),
        VARDECL("vardecl"),
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
        private CallAttributes(String name){
            this.name = name;
        }
        /**
         * Return an attribute enumeration item from a given attribute name
         */
        public static Optional<CallAttributes> fromString(String name) {
            return Arrays.asList(values()).stream().filter(attr -> attr.name.equals(name)).findAny();
        }

        /**
         * Return a list of attributes in String format
         */
        public static List<String> getNames() {
            return Arrays.asList(values()).stream().map(CallAttributes::name).collect(Collectors.toList());
        }

        /**
         * True if the enum contains the given attribute name, false otherwise.
         */
        public static boolean contains(String name) {
            return fromString(name).isPresent();
        }
    }
}
