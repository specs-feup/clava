package pt.up.fe.specs.clava.weaver.abstracts.joinpoints;

import org.lara.interpreter.weaver.interf.events.Stage;
import java.util.Optional;
import org.lara.interpreter.exception.AttributeException;
import java.util.List;
import org.lara.interpreter.weaver.interf.SelectOp;
import org.lara.interpreter.exception.ActionException;
import java.util.Map;
import org.lara.interpreter.weaver.interf.JoinPoint;
import java.util.stream.Collectors;
import java.util.Arrays;

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
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "name", Optional.empty());
        	}
        	String result = this.getNameImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "name", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "name", e);
        }
    }

    /**
     * 
     */
    public void defNameImpl(String value) {
        throw new UnsupportedOperationException("Join point "+get_class()+": Action def name with type String not implemented ");
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
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "numArgs", Optional.empty());
        	}
        	Integer result = this.getNumArgsImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "numArgs", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "numArgs", e);
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
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(stringArrayImpl0);
        return nativeArray0;
    }

    /**
     * Get value on attribute memberNames
     * @return the attribute's value
     */
    public final Object getMemberNames() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "memberNames", Optional.empty());
        	}
        	Object result = this.getMemberNamesImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "memberNames", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "memberNames", e);
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
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "declaration", Optional.empty());
        	}
        	AFunction result = this.getDeclarationImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "declaration", Optional.ofNullable(result));
        	}
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
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "definition", Optional.empty());
        	}
        	AFunction result = this.getDefinitionImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "definition", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "definition", e);
        }
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
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(aExpressionArrayImpl0);
        return nativeArray0;
    }

    /**
     * an alias for 'args'
     */
    public final Object getArgList() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "argList", Optional.empty());
        	}
        	Object result = this.getArgListImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "argList", Optional.ofNullable(result));
        	}
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
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(aExpressionArrayImpl0);
        return nativeArray0;
    }

    /**
     * an array with the arguments of the call
     */
    public final Object getArgs() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "args", Optional.empty());
        	}
        	Object result = this.getArgsImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "args", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "args", e);
        }
    }

    /**
     * 
     * @param index
     * @return 
     */
    public abstract AExpression argImpl(int index);

    /**
     * 
     * @param index
     * @return 
     */
    public final Object arg(int index) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "arg", Optional.empty(), index);
        	}
        	AExpression result = this.argImpl(index);
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "arg", Optional.ofNullable(result), index);
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "arg", e);
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
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "returnType", Optional.empty());
        	}
        	AType result = this.getReturnTypeImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "returnType", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "returnType", e);
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
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "functionType", Optional.empty());
        	}
        	AFunctionType result = this.getFunctionTypeImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "functionType", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "functionType", e);
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
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "isMemberAccess", Optional.empty());
        	}
        	Boolean result = this.getIsMemberAccessImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "isMemberAccess", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isMemberAccess", e);
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
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "memberAccess", Optional.empty());
        	}
        	AMemberAccess result = this.getMemberAccessImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "memberAccess", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "memberAccess", e);
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
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "isStmtCall", Optional.empty());
        	}
        	Boolean result = this.getIsStmtCallImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "isStmtCall", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isStmtCall", e);
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
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "function", Optional.empty());
        	}
        	AFunction result = this.getFunctionImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "function", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "function", e);
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
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "signature", Optional.empty());
        	}
        	String result = this.getSignatureImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "signature", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "signature", e);
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
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "directCallee", Optional.empty());
        	}
        	AFunction result = this.getDirectCalleeImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "directCallee", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "directCallee", e);
        }
    }

    /**
     * Default implementation of the method used by the lara interpreter to select callees
     * @return 
     */
    public List<? extends AExpression> selectCallee() {
        return select(pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AExpression.class, SelectOp.DESCENDANTS);
    }

    /**
     * Default implementation of the method used by the lara interpreter to select args
     * @return 
     */
    public List<? extends AExpression> selectArg() {
        return select(pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AExpression.class, SelectOp.DESCENDANTS);
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
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setName", this, Optional.empty(), name);
        	}
        	this.setNameImpl(name);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setName", this, Optional.empty(), name);
        	}
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
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "wrap", this, Optional.empty(), name);
        	}
        	this.wrapImpl(name);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "wrap", this, Optional.empty(), name);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "wrap", e);
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
    public final boolean inline() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "inline", this, Optional.empty());
        	}
        	boolean result = this.inlineImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "inline", this, Optional.ofNullable(result));
        	}
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
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setArgFromString", this, Optional.empty(), index, expr);
        	}
        	this.setArgFromStringImpl(index, expr);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setArgFromString", this, Optional.empty(), index, expr);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setArgFromString", e);
        }
    }

    /**
     * 
     * @param index 
     * @param expr 
     */
    public void setArgImpl(Integer index, AExpression expr) {
        throw new UnsupportedOperationException(get_class()+": Action setArg not implemented ");
    }

    /**
     * 
     * @param index 
     * @param expr 
     */
    public final void setArg(Integer index, AExpression expr) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setArg", this, Optional.empty(), index, expr);
        	}
        	this.setArgImpl(index, expr);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setArg", this, Optional.empty(), index, expr);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setArg", e);
        }
    }

    /**
     * Adds an argument at the end of the call, creating an expression using the given code and type
     * @param argCode 
     * @param type 
     */
    public void addArgImpl(String argCode, AType type) {
        throw new UnsupportedOperationException(get_class()+": Action addArg not implemented ");
    }

    /**
     * Adds an argument at the end of the call, creating an expression using the given code and type
     * @param argCode 
     * @param type 
     */
    public final void addArg(String argCode, AType type) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "addArg", this, Optional.empty(), argCode, type);
        	}
        	this.addArgImpl(argCode, type);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "addArg", this, Optional.empty(), argCode, type);
        	}
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
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "addArg", this, Optional.empty(), arg, type);
        	}
        	this.addArgImpl(arg, type);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "addArg", this, Optional.empty(), arg, type);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "addArg", e);
        }
    }

    /**
     * Adds an argument at the end of the call, creating an expression using a dummy type
     * @param argCode 
     */
    public void addArgImpl(String argCode) {
        throw new UnsupportedOperationException(get_class()+": Action addArg not implemented ");
    }

    /**
     * Adds an argument at the end of the call, creating an expression using a dummy type
     * @param argCode 
     */
    public final void addArg(String argCode) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "addArg", this, Optional.empty(), argCode);
        	}
        	this.addArgImpl(argCode);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "addArg", this, Optional.empty(), argCode);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "addArg", e);
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
     * Get value on attribute vardecl
     * @return the attribute's value
     */
    @Override
    public AVardecl getVardeclImpl() {
        return this.aExpression.getVardeclImpl();
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
     * Get value on attribute isFunctionArgument
     * @return the attribute's value
     */
    @Override
    public Boolean getIsFunctionArgumentImpl() {
        return this.aExpression.getIsFunctionArgumentImpl();
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
     * Method used by the lara interpreter to select vardecls
     * @return 
     */
    @Override
    public List<? extends AVardecl> selectVardecl() {
        return this.aExpression.selectVardecl();
    }

    /**
     * Replaces this join point with the given join
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
     * Removes the node associated to this joinpoint from the AST
     */
    @Override
    public AJoinPoint detachImpl() {
        return this.aExpression.detachImpl();
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
     * Performs a copy of the node and its children, but not of the nodes in its fields
     */
    @Override
    public AJoinPoint copyImpl() {
        return this.aExpression.copyImpl();
    }

    /**
     * Performs a copy of the node and its children, including the nodes in their fields (only the first level of field nodes, this function is not recursive)
     */
    @Override
    public AJoinPoint deepCopyImpl() {
        return this.aExpression.deepCopyImpl();
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
     * Replaces the first child, or inserts the join point if no child is present
     * @param node 
     */
    @Override
    public void setFirstChildImpl(AJoinPoint node) {
        this.aExpression.setFirstChildImpl(node);
    }

    /**
     * Replaces the last child, or inserts the join point if no child is present
     * @param node 
     */
    @Override
    public void setLastChildImpl(AJoinPoint node) {
        this.aExpression.setLastChildImpl(node);
    }

    /**
     * Replaces this join point with a comment with the same contents as .code
     */
    @Override
    public AJoinPoint toCommentImpl() {
        return this.aExpression.toCommentImpl();
    }

    /**
     * Replaces this join point with a comment with the same contents as .code
     * @param prefix 
     */
    @Override
    public AJoinPoint toCommentImpl(String prefix) {
        return this.aExpression.toCommentImpl(prefix);
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
     * Setting data directly is not supported, this action just emits a warning and does nothing
     * @param source 
     */
    @Override
    public void setDataImpl(Object source) {
        this.aExpression.setDataImpl(source);
    }

    /**
     * Copies all enumerable own properties from the source object to the .data object
     * @param source 
     */
    @Override
    public void dataAssignImpl(Object source) {
        this.aExpression.dataAssignImpl(source);
    }

    /**
     * Clears all properties from the .data object
     */
    @Override
    public void dataClearImpl() {
        this.aExpression.dataClearImpl();
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
     * 
     */
    @Override
    public Optional<? extends AExpression> getSuper() {
        return Optional.of(this.aExpression);
    }

    /**
     * 
     */
    @Override
    public List<? extends JoinPoint> select(String selectName) {
        List<? extends JoinPoint> joinPointList;
        switch(selectName) {
        	case "callee": 
        		joinPointList = selectCallee();
        		break;
        	case "arg": 
        		joinPointList = selectArg();
        		break;
        	case "vardecl": 
        		joinPointList = selectVardecl();
        		break;
        	default:
        		joinPointList = this.aExpression.select(selectName);
        		break;
        }
        return joinPointList;
    }

    /**
     * 
     */
    @Override
    public void defImpl(String attribute, Object value) {
        switch(attribute){
        case "data": {
        	if(value instanceof Object){
        		this.defDataImpl((Object)value);
        		return;
        	}
        	this.unsupportedTypeForDef(attribute, value);
        }
        case "type": {
        	if(value instanceof AType){
        		this.defTypeImpl((AType)value);
        		return;
        	}
        	this.unsupportedTypeForDef(attribute, value);
        }
        case "firstChild": {
        	if(value instanceof AJoinPoint){
        		this.defFirstChildImpl((AJoinPoint)value);
        		return;
        	}
        	this.unsupportedTypeForDef(attribute, value);
        }
        case "inlineComments": {
        	if(value instanceof String[]){
        		this.defInlineCommentsImpl((String[])value);
        		return;
        	}
        	if(value instanceof String){
        		this.defInlineCommentsImpl((String)value);
        		return;
        	}
        	this.unsupportedTypeForDef(attribute, value);
        }
        case "lastChild": {
        	if(value instanceof AJoinPoint){
        		this.defLastChildImpl((AJoinPoint)value);
        		return;
        	}
        	this.unsupportedTypeForDef(attribute, value);
        }
        case "name": {
        	if(value instanceof String){
        		this.defNameImpl((String)value);
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
    protected void fillWithAttributes(List<String> attributes) {
        this.aExpression.fillWithAttributes(attributes);
        attributes.add("name");
        attributes.add("numArgs");
        attributes.add("memberNames");
        attributes.add("declaration");
        attributes.add("definition");
        attributes.add("argList");
        attributes.add("args");
        attributes.add("arg");
        attributes.add("returnType");
        attributes.add("functionType");
        attributes.add("isMemberAccess");
        attributes.add("memberAccess");
        attributes.add("isStmtCall");
        attributes.add("function");
        attributes.add("signature");
        attributes.add("directCallee");
    }

    /**
     * 
     */
    @Override
    protected void fillWithSelects(List<String> selects) {
        this.aExpression.fillWithSelects(selects);
        selects.add("callee");
        selects.add("arg");
    }

    /**
     * 
     */
    @Override
    protected void fillWithActions(List<String> actions) {
        this.aExpression.fillWithActions(actions);
        actions.add("void setName(string)");
        actions.add("void wrap(string)");
        actions.add("boolean inline()");
        actions.add("void setArgFromString(int, string)");
        actions.add("void setArg(Integer, expression)");
        actions.add("void addArg(String, type)");
        actions.add("void addArg(String, String)");
        actions.add("void addArg(String)");
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
        NAME("name"),
        NUMARGS("numArgs"),
        MEMBERNAMES("memberNames"),
        DECLARATION("declaration"),
        DEFINITION("definition"),
        ARGLIST("argList"),
        ARGS("args"),
        ARG("arg"),
        RETURNTYPE("returnType"),
        FUNCTIONTYPE("functionType"),
        ISMEMBERACCESS("isMemberAccess"),
        MEMBERACCESS("memberAccess"),
        ISSTMTCALL("isStmtCall"),
        FUNCTION("function"),
        SIGNATURE("signature"),
        DIRECTCALLEE("directCallee"),
        DECL("decl"),
        VARDECL("vardecl"),
        USE("use"),
        ISFUNCTIONARGUMENT("isFunctionArgument"),
        IMPLICITCAST("implicitCast"),
        PARENT("parent"),
        ASTANCESTOR("astAncestor"),
        AST("ast"),
        SIBLINGSLEFT("siblingsLeft"),
        DATA("data"),
        HASCHILDREN("hasChildren"),
        DESCENDANTSANDSELF("descendantsAndSelf"),
        TYPE("type"),
        SIBLINGSRIGHT("siblingsRight"),
        RIGHTJP("rightJp"),
        ISCILK("isCilk"),
        FILEPATH("filepath"),
        SCOPENODES("scopeNodes"),
        LARADESCENDANTS("laraDescendants"),
        CHILDREN("children"),
        FIRSTCHILD("firstChild"),
        NUMCHILDREN("numChildren"),
        ANCESTOR("ancestor"),
        LEFTJP("leftJp"),
        INLINECOMMENTS("inlineComments"),
        ASTCHILD("astChild"),
        ASTNAME("astName"),
        JPID("jpId"),
        ASTID("astId"),
        CONTAINS("contains"),
        ASTISINSTANCE("astIsInstance"),
        FILENAME("filename"),
        JAVAFIELDS("javaFields"),
        ISINSYSTEMHEADER("isInSystemHeader"),
        ASTPARENT("astParent"),
        BITWIDTH("bitWidth"),
        USERFIELD("userField"),
        HASNODE("hasNode"),
        CHILD("child"),
        ENDLINE("endLine"),
        ENDCOLUMN("endColumn"),
        CODE("code"),
        ISINSIDELOOPHEADER("isInsideLoopHeader"),
        LINE("line"),
        KEYS("keys"),
        ISINSIDEHEADER("isInsideHeader"),
        ASTNUMCHILDREN("astNumChildren"),
        DESCENDANTS("descendants"),
        ASTCHILDREN("astChildren"),
        ISMACRO("isMacro"),
        LASTCHILD("lastChild"),
        ROOT("root"),
        JAVAVALUE("javaValue"),
        KEYTYPE("keyType"),
        CHAINANCESTOR("chainAncestor"),
        CHAIN("chain"),
        JOINPOINTTYPE("joinpointType"),
        CURRENTREGION("currentRegion"),
        HASASTPARENT("hasAstParent"),
        COLUMN("column"),
        PARENTREGION("parentRegion"),
        GETVALUE("getValue"),
        FIRSTJP("firstJp"),
        DEPTH("depth"),
        JAVAFIELDTYPE("javaFieldType"),
        LOCATION("location"),
        GETUSERFIELD("getUserField"),
        HASTYPE("hasType"),
        PRAGMAS("pragmas"),
        STMT("stmt"),
        HASPARENT("hasParent");
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
