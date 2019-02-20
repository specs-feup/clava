package pt.up.fe.specs.clava.weaver.abstracts.joinpoints;

import org.lara.interpreter.weaver.interf.events.Stage;
import java.util.Optional;
import org.lara.interpreter.exception.AttributeException;
import javax.script.Bindings;
import pt.up.fe.specs.clava.weaver.enums.StorageClass;
import java.util.List;
import org.lara.interpreter.weaver.interf.SelectOp;
import org.lara.interpreter.exception.ActionException;
import java.util.Map;
import org.lara.interpreter.weaver.interf.JoinPoint;
import java.util.stream.Collectors;
import java.util.Arrays;

/**
 * Auto-Generated class for join point AFunction
 * This class is overwritten by the Weaver Generator.
 * 
 * 
 * @author Lara Weaver Generator
 */
public abstract class AFunction extends ADeclarator {

    protected ADeclarator aDeclarator;

    /**
     * 
     */
    public AFunction(ADeclarator aDeclarator){
        super(aDeclarator);
        this.aDeclarator = aDeclarator;
    }
    /**
     * Get value on attribute hasDefinition
     * @return the attribute's value
     */
    public abstract Boolean getHasDefinitionImpl();

    /**
     * Get value on attribute hasDefinition
     * @return the attribute's value
     */
    public final Object getHasDefinition() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "hasDefinition", Optional.empty());
        	}
        	Boolean result = this.getHasDefinitionImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "hasDefinition", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "hasDefinition", e);
        }
    }

    /**
     * Get value on attribute functionType
     * @return the attribute's value
     */
    public abstract AFunctionType getFunctionTypeImpl();

    /**
     * Get value on attribute functionType
     * @return the attribute's value
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
     * Get value on attribute declarationJp
     * @return the attribute's value
     */
    public abstract AJoinPoint getDeclarationJpImpl();

    /**
     * Get value on attribute declarationJp
     * @return the attribute's value
     */
    public final Object getDeclarationJp() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "declarationJp", Optional.empty());
        	}
        	AJoinPoint result = this.getDeclarationJpImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "declarationJp", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "declarationJp", e);
        }
    }

    /**
     * Get value on attribute definitionJp
     * @return the attribute's value
     */
    public abstract AJoinPoint getDefinitionJpImpl();

    /**
     * Get value on attribute definitionJp
     * @return the attribute's value
     */
    public final Object getDefinitionJp() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "definitionJp", Optional.empty());
        	}
        	AJoinPoint result = this.getDefinitionJpImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "definitionJp", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "definitionJp", e);
        }
    }

    /**
     * 
     * @param withReturnType
     * @return 
     */
    public abstract String declarationImpl(Boolean withReturnType);

    /**
     * 
     * @param withReturnType
     * @return 
     */
    public final Object declaration(Boolean withReturnType) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "declaration", Optional.empty(), withReturnType);
        	}
        	String result = this.declarationImpl(withReturnType);
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "declaration", Optional.ofNullable(result), withReturnType);
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "declaration", e);
        }
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
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "body", Optional.empty());
        	}
        	AScope result = this.getBodyImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "body", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "body", e);
        }
    }

    /**
     * 
     */
    public void defBodyImpl(AScope value) {
        throw new UnsupportedOperationException("Join point "+get_class()+": Action def body with type AScope not implemented ");
    }

    /**
     * Get value on attribute paramNames
     * @return the attribute's value
     */
    public abstract String[] getParamNamesArrayImpl();

    /**
     * Get value on attribute paramNames
     * @return the attribute's value
     */
    public Bindings getParamNamesImpl() {
        String[] stringArrayImpl0 = getParamNamesArrayImpl();
        Bindings nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(stringArrayImpl0);
        return nativeArray0;
    }

    /**
     * Get value on attribute paramNames
     * @return the attribute's value
     */
    public final Object getParamNames() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "paramNames", Optional.empty());
        	}
        	Bindings result = this.getParamNamesImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "paramNames", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "paramNames", e);
        }
    }

    /**
     * Get value on attribute params
     * @return the attribute's value
     */
    public abstract AParam[] getParamsArrayImpl();

    /**
     * Get value on attribute params
     * @return the attribute's value
     */
    public Bindings getParamsImpl() {
        AParam[] aParamArrayImpl0 = getParamsArrayImpl();
        Bindings nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(aParamArrayImpl0);
        return nativeArray0;
    }

    /**
     * Get value on attribute params
     * @return the attribute's value
     */
    public final Object getParams() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "params", Optional.empty());
        	}
        	Bindings result = this.getParamsImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "params", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "params", e);
        }
    }

    /**
     * 
     */
    public void defParamsImpl(AParam[] value) {
        throw new UnsupportedOperationException("Join point "+get_class()+": Action def params with type AParam not implemented ");
    }

    /**
     * 
     */
    public void defParamsImpl(String[] value) {
        throw new UnsupportedOperationException("Join point "+get_class()+": Action def params with type String not implemented ");
    }

    /**
     * Get value on attribute id
     * @return the attribute's value
     */
    public abstract String getIdImpl();

    /**
     * Get value on attribute id
     * @return the attribute's value
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
     * Get value on attribute isInline
     * @return the attribute's value
     */
    public abstract Boolean getIsInlineImpl();

    /**
     * Get value on attribute isInline
     * @return the attribute's value
     */
    public final Object getIsInline() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "isInline", Optional.empty());
        	}
        	Boolean result = this.getIsInlineImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "isInline", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isInline", e);
        }
    }

    /**
     * Get value on attribute isVirtual
     * @return the attribute's value
     */
    public abstract Boolean getIsVirtualImpl();

    /**
     * Get value on attribute isVirtual
     * @return the attribute's value
     */
    public final Object getIsVirtual() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "isVirtual", Optional.empty());
        	}
        	Boolean result = this.getIsVirtualImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "isVirtual", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isVirtual", e);
        }
    }

    /**
     * Get value on attribute isModulePrivate
     * @return the attribute's value
     */
    public abstract Boolean getIsModulePrivateImpl();

    /**
     * Get value on attribute isModulePrivate
     * @return the attribute's value
     */
    public final Object getIsModulePrivate() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "isModulePrivate", Optional.empty());
        	}
        	Boolean result = this.getIsModulePrivateImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "isModulePrivate", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isModulePrivate", e);
        }
    }

    /**
     * Get value on attribute isPure
     * @return the attribute's value
     */
    public abstract Boolean getIsPureImpl();

    /**
     * Get value on attribute isPure
     * @return the attribute's value
     */
    public final Object getIsPure() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "isPure", Optional.empty());
        	}
        	Boolean result = this.getIsPureImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "isPure", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isPure", e);
        }
    }

    /**
     * Get value on attribute isDelete
     * @return the attribute's value
     */
    public abstract Boolean getIsDeleteImpl();

    /**
     * Get value on attribute isDelete
     * @return the attribute's value
     */
    public final Object getIsDelete() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "isDelete", Optional.empty());
        	}
        	Boolean result = this.getIsDeleteImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "isDelete", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isDelete", e);
        }
    }

    /**
     * Get value on attribute storageClass
     * @return the attribute's value
     */
    public abstract StorageClass getStorageClassImpl();

    /**
     * Get value on attribute storageClass
     * @return the attribute's value
     */
    public final Object getStorageClass() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "storageClass", Optional.empty());
        	}
        	StorageClass result = this.getStorageClassImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "storageClass", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "storageClass", e);
        }
    }

    /**
     * Get value on attribute calls
     * @return the attribute's value
     */
    public abstract ACall[] getCallsArrayImpl();

    /**
     * Get value on attribute calls
     * @return the attribute's value
     */
    public Bindings getCallsImpl() {
        ACall[] aCallArrayImpl0 = getCallsArrayImpl();
        Bindings nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(aCallArrayImpl0);
        return nativeArray0;
    }

    /**
     * Get value on attribute calls
     * @return the attribute's value
     */
    public final Object getCalls() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "calls", Optional.empty());
        	}
        	Bindings result = this.getCallsImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "calls", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "calls", e);
        }
    }

    /**
     * a string with the signature of this function (e.g., name of the function, plus the parameters types)
     */
    public abstract String getSignatureImpl();

    /**
     * a string with the signature of this function (e.g., name of the function, plus the parameters types)
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
     * Default implementation of the method used by the lara interpreter to select bodys
     * @return 
     */
    public List<? extends AScope> selectBody() {
        return select(pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AScope.class, SelectOp.DESCENDANTS);
    }

    /**
     * Default implementation of the method used by the lara interpreter to select params
     * @return 
     */
    public List<? extends AParam> selectParam() {
        return select(pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AParam.class, SelectOp.DESCENDANTS);
    }

    /**
     * Default implementation of the method used by the lara interpreter to select decls
     * @return 
     */
    public List<? extends ADecl> selectDecl() {
        return select(pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ADecl.class, SelectOp.DESCENDANTS);
    }

    /**
     * Clones this function assigning it a new name, inserts the cloned function after the original function
     * @param newName 
     */
    public AFunction cloneImpl(String newName) {
        throw new UnsupportedOperationException(get_class()+": Action clone not implemented ");
    }

    /**
     * Clones this function assigning it a new name, inserts the cloned function after the original function
     * @param newName 
     */
    public final AFunction clone(String newName) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "clone", this, Optional.empty(), newName);
        	}
        	AFunction result = this.cloneImpl(newName);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "clone", this, Optional.ofNullable(result), newName);
        	}
        	return result;
        } catch(Exception e) {
        	throw new ActionException(get_class(), "clone", e);
        }
    }

    /**
     * Generates a clone of the provided function on a new file (with a weaver-generated name).
     * @param newName 
     */
    public String cloneOnFileImpl(String newName) {
        throw new UnsupportedOperationException(get_class()+": Action cloneOnFile not implemented ");
    }

    /**
     * Generates a clone of the provided function on a new file (with a weaver-generated name).
     * @param newName 
     */
    public final String cloneOnFile(String newName) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "cloneOnFile", this, Optional.empty(), newName);
        	}
        	String result = this.cloneOnFileImpl(newName);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "cloneOnFile", this, Optional.ofNullable(result), newName);
        	}
        	return result;
        } catch(Exception e) {
        	throw new ActionException(get_class(), "cloneOnFile", e);
        }
    }

    /**
     * Generates a clone of the provided function on a new file (with the provided name).
     * @param newName 
     * @param fileName 
     */
    public String cloneOnFileImpl(String newName, String fileName) {
        throw new UnsupportedOperationException(get_class()+": Action cloneOnFile not implemented ");
    }

    /**
     * Generates a clone of the provided function on a new file (with the provided name).
     * @param newName 
     * @param fileName 
     */
    public final String cloneOnFile(String newName, String fileName) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "cloneOnFile", this, Optional.empty(), newName, fileName);
        	}
        	String result = this.cloneOnFileImpl(newName, fileName);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "cloneOnFile", this, Optional.ofNullable(result), newName, fileName);
        	}
        	return result;
        } catch(Exception e) {
        	throw new ActionException(get_class(), "cloneOnFile", e);
        }
    }

    /**
     * Inserts the joinpoint before the return points of the function (return statements and implicitly, at the end of the function)
     * @param code 
     */
    public void insertReturnImpl(AJoinPoint code) {
        throw new UnsupportedOperationException(get_class()+": Action insertReturn not implemented ");
    }

    /**
     * Inserts the joinpoint before the return points of the function (return statements and implicitly, at the end of the function)
     * @param code 
     */
    public final void insertReturn(AJoinPoint code) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "insertReturn", this, Optional.empty(), code);
        	}
        	this.insertReturnImpl(code);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "insertReturn", this, Optional.empty(), code);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "insertReturn", e);
        }
    }

    /**
     * Inserts code as a literal statement before the return points of the function (return statements and implicitly, at the end of the function)
     * @param code 
     */
    public void insertReturnImpl(String code) {
        throw new UnsupportedOperationException(get_class()+": Action insertReturn not implemented ");
    }

    /**
     * Inserts code as a literal statement before the return points of the function (return statements and implicitly, at the end of the function)
     * @param code 
     */
    public final void insertReturn(String code) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "insertReturn", this, Optional.empty(), code);
        	}
        	this.insertReturnImpl(code);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "insertReturn", this, Optional.empty(), code);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "insertReturn", e);
        }
    }

    /**
     * Sets the parameters of the function
     * @param params 
     */
    public void setParamsImpl(AParam[] params) {
        throw new UnsupportedOperationException(get_class()+": Action setParams not implemented ");
    }

    /**
     * Sets the parameters of the function
     * @param params 
     */
    public final void setParams(AParam[] params) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setParams", this, Optional.empty(), params);
        	}
        	this.setParamsImpl(params);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setParams", this, Optional.empty(), params);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setParams", e);
        }
    }

    /**
     * Overload that accepts strings that represent type-varname pairs (e.g., int param1)
     * @param params 
     */
    public void setParamsFromStringsImpl(String[] params) {
        throw new UnsupportedOperationException(get_class()+": Action setParamsFromStrings not implemented ");
    }

    /**
     * Overload that accepts strings that represent type-varname pairs (e.g., int param1)
     * @param params 
     */
    public final void setParamsFromStrings(String[] params) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setParamsFromStrings", this, Optional.empty(), params);
        	}
        	this.setParamsFromStringsImpl(params);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setParamsFromStrings", this, Optional.empty(), params);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setParamsFromStrings", e);
        }
    }

    /**
     * Sets the body of the function
     * @param body 
     */
    public void setBodyImpl(AScope body) {
        throw new UnsupportedOperationException(get_class()+": Action setBody not implemented ");
    }

    /**
     * Sets the body of the function
     * @param body 
     */
    public final void setBody(AScope body) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setBody", this, Optional.empty(), body);
        	}
        	this.setBodyImpl(body);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setBody", this, Optional.empty(), body);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setBody", e);
        }
    }

    /**
     * Creates a new call to this function
     * @param args 
     */
    public ACall newCallImpl(AJoinPoint[] args) {
        throw new UnsupportedOperationException(get_class()+": Action newCall not implemented ");
    }

    /**
     * Creates a new call to this function
     * @param args 
     */
    public final ACall newCall(AJoinPoint[] args) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "newCall", this, Optional.empty(), args);
        	}
        	ACall result = this.newCallImpl(args);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "newCall", this, Optional.ofNullable(result), args);
        	}
        	return result;
        } catch(Exception e) {
        	throw new ActionException(get_class(), "newCall", e);
        }
    }

    /**
     * Get value on attribute name
     * @return the attribute's value
     */
    @Override
    public String getNameImpl() {
        return this.aDeclarator.getNameImpl();
    }

    /**
     * Get value on attribute isPublic
     * @return the attribute's value
     */
    @Override
    public Boolean getIsPublicImpl() {
        return this.aDeclarator.getIsPublicImpl();
    }

    /**
     * Get value on attribute qualifiedPrefix
     * @return the attribute's value
     */
    @Override
    public String getQualifiedPrefixImpl() {
        return this.aDeclarator.getQualifiedPrefixImpl();
    }

    /**
     * Get value on attribute qualifiedName
     * @return the attribute's value
     */
    @Override
    public String getQualifiedNameImpl() {
        return this.aDeclarator.getQualifiedNameImpl();
    }

    /**
     * 
     */
    public void defNameImpl(String value) {
        this.aDeclarator.defNameImpl(value);
    }

    /**
     * 
     */
    public void defQualifiedPrefixImpl(String value) {
        this.aDeclarator.defQualifiedPrefixImpl(value);
    }

    /**
     * 
     */
    public void defQualifiedNameImpl(String value) {
        this.aDeclarator.defQualifiedNameImpl(value);
    }

    /**
     * Replaces this join point with the given join
     * @param node 
     */
    @Override
    public AJoinPoint replaceWithImpl(AJoinPoint node) {
        return this.aDeclarator.replaceWithImpl(node);
    }

    /**
     * Overload which accepts a string
     * @param node 
     */
    @Override
    public AJoinPoint replaceWithImpl(String node) {
        return this.aDeclarator.replaceWithImpl(node);
    }

    /**
     * Inserts the given join point before this join point
     * @param node 
     */
    @Override
    public AJoinPoint insertBeforeImpl(AJoinPoint node) {
        return this.aDeclarator.insertBeforeImpl(node);
    }

    /**
     * Overload which accepts a string
     * @param node 
     */
    @Override
    public AJoinPoint insertBeforeImpl(String node) {
        return this.aDeclarator.insertBeforeImpl(node);
    }

    /**
     * Inserts the given join point after this join point
     * @param node 
     */
    @Override
    public AJoinPoint insertAfterImpl(AJoinPoint node) {
        return this.aDeclarator.insertAfterImpl(node);
    }

    /**
     * Overload which accepts a string
     * @param code 
     */
    @Override
    public AJoinPoint insertAfterImpl(String code) {
        return this.aDeclarator.insertAfterImpl(code);
    }

    /**
     * Removes the node associated to this joinpoint from the AST
     */
    @Override
    public void detachImpl() {
        this.aDeclarator.detachImpl();
    }

    /**
     * Sets the type of a node, if it has a type
     * @param type 
     */
    @Override
    public void setTypeImpl(AJoinPoint type) {
        this.aDeclarator.setTypeImpl(type);
    }

    /**
     * Performs a copy of the node and its children, but not of the nodes in its fields
     */
    @Override
    public AJoinPoint copyImpl() {
        return this.aDeclarator.copyImpl();
    }

    /**
     * Performs a copy of the node and its children, including the nodes in their fields (only the first level of field nodes, this function is not recursive)
     */
    @Override
    public AJoinPoint deepCopyImpl() {
        return this.aDeclarator.deepCopyImpl();
    }

    /**
     * Associates arbitrary values to nodes of the AST
     * @param fieldName 
     * @param value 
     */
    @Override
    public Object setUserFieldImpl(String fieldName, Object value) {
        return this.aDeclarator.setUserFieldImpl(fieldName, value);
    }

    /**
     * Overload which accepts a map
     * @param fieldNameAndValue 
     */
    @Override
    public Object setUserFieldImpl(Map<?, ?> fieldNameAndValue) {
        return this.aDeclarator.setUserFieldImpl(fieldNameAndValue);
    }

    /**
     * Sets the value associated with the given property key
     * @param key 
     * @param value 
     */
    @Override
    public AJoinPoint setValueImpl(String key, Object value) {
        return this.aDeclarator.setValueImpl(key, value);
    }

    /**
     * Adds a message that will be printed to the user after weaving finishes. Identical messages are removed
     * @param message 
     */
    @Override
    public void messageToUserImpl(String message) {
        this.aDeclarator.messageToUserImpl(message);
    }

    /**
     * 
     * @param position 
     * @param code 
     */
    @Override
    public void insertImpl(String position, String code) {
        this.aDeclarator.insertImpl(position, code);
    }

    /**
     * 
     */
    @Override
    public String toString() {
        return this.aDeclarator.toString();
    }

    /**
     * 
     */
    @Override
    public Optional<? extends ADeclarator> getSuper() {
        return Optional.of(this.aDeclarator);
    }

    /**
     * 
     */
    @Override
    public List<? extends JoinPoint> select(String selectName) {
        List<? extends JoinPoint> joinPointList;
        switch(selectName) {
        	case "body": 
        		joinPointList = selectBody();
        		break;
        	case "param": 
        		joinPointList = selectParam();
        		break;
        	case "decl": 
        		joinPointList = selectDecl();
        		break;
        	default:
        		joinPointList = this.aDeclarator.select(selectName);
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
        case "type": {
        	if(value instanceof AJoinPoint){
        		this.defTypeImpl((AJoinPoint)value);
        		return;
        	}
        	this.unsupportedTypeForDef(attribute, value);
        }
        case "body": {
        	if(value instanceof AScope){
        		this.defBodyImpl((AScope)value);
        		return;
        	}
        	this.unsupportedTypeForDef(attribute, value);
        }
        case "params": {
        	if(value instanceof AParam[]){
        		this.defParamsImpl((AParam[])value);
        		return;
        	}
        	if(value instanceof String[]){
        		this.defParamsImpl((String[])value);
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
        case "qualifiedPrefix": {
        	if(value instanceof String){
        		this.defQualifiedPrefixImpl((String)value);
        		return;
        	}
        	this.unsupportedTypeForDef(attribute, value);
        }
        case "qualifiedName": {
        	if(value instanceof String){
        		this.defQualifiedNameImpl((String)value);
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
        this.aDeclarator.fillWithAttributes(attributes);
        attributes.add("hasDefinition");
        attributes.add("functionType");
        attributes.add("declarationJp");
        attributes.add("definitionJp");
        attributes.add("declaration");
        attributes.add("body");
        attributes.add("paramNames");
        attributes.add("params");
        attributes.add("id");
        attributes.add("isInline");
        attributes.add("isVirtual");
        attributes.add("isModulePrivate");
        attributes.add("isPure");
        attributes.add("isDelete");
        attributes.add("storageClass");
        attributes.add("calls");
        attributes.add("signature");
    }

    /**
     * 
     */
    @Override
    protected void fillWithSelects(List<String> selects) {
        this.aDeclarator.fillWithSelects(selects);
        selects.add("body");
        selects.add("param");
        selects.add("decl");
    }

    /**
     * 
     */
    @Override
    protected void fillWithActions(List<String> actions) {
        this.aDeclarator.fillWithActions(actions);
        actions.add("function clone(String)");
        actions.add("String cloneOnFile(String)");
        actions.add("String cloneOnFile(String, String)");
        actions.add("void insertReturn(joinpoint)");
        actions.add("void insertReturn(String)");
        actions.add("void setParams(param[])");
        actions.add("void setParamsFromStrings(String[])");
        actions.add("void setBody(scope)");
        actions.add("call newCall(joinpoint[])");
    }

    /**
     * Returns the join point type of this class
     * @return The join point type
     */
    @Override
    public String get_class() {
        return "function";
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
        return this.aDeclarator.instanceOf(joinpointClass);
    }
    /**
     * 
     */
    protected enum FunctionAttributes {
        HASDEFINITION("hasDefinition"),
        FUNCTIONTYPE("functionType"),
        DECLARATIONJP("declarationJp"),
        DEFINITIONJP("definitionJp"),
        DECLARATION("declaration"),
        BODY("body"),
        PARAMNAMES("paramNames"),
        PARAMS("params"),
        ID("id"),
        ISINLINE("isInline"),
        ISVIRTUAL("isVirtual"),
        ISMODULEPRIVATE("isModulePrivate"),
        ISPURE("isPure"),
        ISDELETE("isDelete"),
        STORAGECLASS("storageClass"),
        CALLS("calls"),
        SIGNATURE("signature"),
        NAME("name"),
        ISPUBLIC("isPublic"),
        QUALIFIEDPREFIX("qualifiedPrefix"),
        QUALIFIEDNAME("qualifiedName"),
        ENDLINE("endLine"),
        PARENT("parent"),
        ENDCOLUMN("endColumn"),
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
        ISMACRO("isMacro"),
        CHILDREN("children"),
        ROOT("root"),
        NUMCHILDREN("numChildren"),
        JAVAVALUE("javaValue"),
        KEYTYPE("keyType"),
        CHAINANCESTOR("chainAncestor"),
        CHAIN("chain"),
        JOINPOINTTYPE("joinpointType"),
        CURRENTREGION("currentRegion"),
        ANCESTOR("ancestor"),
        HASASTPARENT("hasAstParent"),
        COLUMN("column"),
        ASTCHILD("astChild"),
        PARENTREGION("parentRegion"),
        ASTNAME("astName"),
        ASTID("astId"),
        GETVALUE("getValue"),
        CONTAINS("contains"),
        FIRSTJP("firstJp"),
        ASTISINSTANCE("astIsInstance"),
        FILENAME("filename"),
        JAVAFIELDS("javaFields"),
        ASTPARENT("astParent"),
        JAVAFIELDTYPE("javaFieldType"),
        USERFIELD("userField"),
        LOCATION("location"),
        HASNODE("hasNode"),
        GETUSERFIELD("getUserField"),
        PRAGMAS("pragmas"),
        HASPARENT("hasParent"),
        CHILD("child");
        private String name;

        /**
         * 
         */
        private FunctionAttributes(String name){
            this.name = name;
        }
        /**
         * Return an attribute enumeration item from a given attribute name
         */
        public static Optional<FunctionAttributes> fromString(String name) {
            return Arrays.asList(values()).stream().filter(attr -> attr.name.equals(name)).findAny();
        }

        /**
         * Return a list of attributes in String format
         */
        public static List<String> getNames() {
            return Arrays.asList(values()).stream().map(FunctionAttributes::name).collect(Collectors.toList());
        }

        /**
         * True if the enum contains the given attribute name, false otherwise.
         */
        public static boolean contains(String name) {
            return fromString(name).isPresent();
        }
    }
}
