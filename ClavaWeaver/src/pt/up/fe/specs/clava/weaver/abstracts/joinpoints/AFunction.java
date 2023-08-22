package pt.up.fe.specs.clava.weaver.abstracts.joinpoints;

import org.lara.interpreter.weaver.interf.events.Stage;
import java.util.Optional;
import org.lara.interpreter.exception.AttributeException;
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
 * Represents a function declaration or definition
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
     * [DEPRECATED: Use .isImplementation instead] True if this particular function join point has a body, false otherwise
     */
    public abstract Boolean getHasDefinitionImpl();

    /**
     * [DEPRECATED: Use .isImplementation instead] True if this particular function join point has a body, false otherwise
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
     * true if this particular function join point is an implementation (i.e. has a body), false otherwise
     */
    public abstract Boolean getIsImplementationImpl();

    /**
     * true if this particular function join point is an implementation (i.e. has a body), false otherwise
     */
    public final Object getIsImplementation() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "isImplementation", Optional.empty());
        	}
        	Boolean result = this.getIsImplementationImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "isImplementation", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isImplementation", e);
        }
    }

    /**
     * true if this particular function join point is a prototype (i.e. does not have a body), false otherwise
     */
    public abstract Boolean getIsPrototypeImpl();

    /**
     * true if this particular function join point is a prototype (i.e. does not have a body), false otherwise
     */
    public final Object getIsPrototype() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "isPrototype", Optional.empty());
        	}
        	Boolean result = this.getIsPrototypeImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "isPrototype", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isPrototype", e);
        }
    }

    /**
     * the type of the call, which includes the return type and the types of the parameters
     */
    public abstract AFunctionType getFunctionTypeImpl();

    /**
     * the type of the call, which includes the return type and the types of the parameters
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
     * 
     */
    public void defFunctionTypeImpl(AFunctionType value) {
        throw new UnsupportedOperationException("Join point "+get_class()+": Action def functionType with type AFunctionType not implemented ");
    }

    /**
     * Returns the first prototype of this function that could be found, or undefined if there is none
     */
    public abstract AFunction getDeclarationJpImpl();

    /**
     * Returns the first prototype of this function that could be found, or undefined if there is none
     */
    public final Object getDeclarationJp() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "declarationJp", Optional.empty());
        	}
        	AFunction result = this.getDeclarationJpImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "declarationJp", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "declarationJp", e);
        }
    }

    /**
     * Get value on attribute declarationJps
     * @return the attribute's value
     */
    public abstract AFunction[] getDeclarationJpsArrayImpl();

    /**
     * Returns the prototypes of this function that are present in the code. If there are none, returns an empty array
     */
    public Object getDeclarationJpsImpl() {
        AFunction[] aFunctionArrayImpl0 = getDeclarationJpsArrayImpl();
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(aFunctionArrayImpl0);
        return nativeArray0;
    }

    /**
     * Returns the prototypes of this function that are present in the code. If there are none, returns an empty array
     */
    public final Object getDeclarationJps() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "declarationJps", Optional.empty());
        	}
        	Object result = this.getDeclarationJpsImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "declarationJps", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "declarationJps", e);
        }
    }

    /**
     * Returns the implementation of this function if there is one, or undefined otherwise
     */
    public abstract AFunction getDefinitionJpImpl();

    /**
     * Returns the implementation of this function if there is one, or undefined otherwise
     */
    public final Object getDefinitionJp() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "definitionJp", Optional.empty());
        	}
        	AFunction result = this.getDefinitionJpImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "definitionJp", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "definitionJp", e);
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
    public Object getParamNamesImpl() {
        String[] stringArrayImpl0 = getParamNamesArrayImpl();
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(stringArrayImpl0);
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
        	Object result = this.getParamNamesImpl();
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
    public Object getParamsImpl() {
        AParam[] aParamArrayImpl0 = getParamsArrayImpl();
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(aParamArrayImpl0);
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
        	Object result = this.getParamsImpl();
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
    public Object getCallsImpl() {
        ACall[] aCallArrayImpl0 = getCallsArrayImpl();
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(aCallArrayImpl0);
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
        	Object result = this.getCallsImpl();
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
     * Get value on attribute returnType
     * @return the attribute's value
     */
    public abstract AType getReturnTypeImpl();

    /**
     * Get value on attribute returnType
     * @return the attribute's value
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
     * 
     */
    public void defReturnTypeImpl(AType value) {
        throw new UnsupportedOperationException("Join point "+get_class()+": Action def returnType with type AType not implemented ");
    }

    /**
     * Get value on attribute isCudaKernel
     * @return the attribute's value
     */
    public abstract Boolean getIsCudaKernelImpl();

    /**
     * Get value on attribute isCudaKernel
     * @return the attribute's value
     */
    public final Object getIsCudaKernel() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "isCudaKernel", Optional.empty());
        	}
        	Boolean result = this.getIsCudaKernelImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "isCudaKernel", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isCudaKernel", e);
        }
    }

    /**
     * Function join points can either represent declarations or definitions, returns the definition of this function, if present, or the first declaration, if only declarations are present
     */
    public abstract AFunction getCanonicalImpl();

    /**
     * Function join points can either represent declarations or definitions, returns the definition of this function, if present, or the first declaration, if only declarations are present
     */
    public final Object getCanonical() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "canonical", Optional.empty());
        	}
        	AFunction result = this.getCanonicalImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "canonical", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "canonical", e);
        }
    }

    /**
     * true, if this is the function returned by the 'canonical' attribute
     */
    public abstract Boolean getIsCanonicalImpl();

    /**
     * true, if this is the function returned by the 'canonical' attribute
     */
    public final Object getIsCanonical() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "isCanonical", Optional.empty());
        	}
        	Boolean result = this.getIsCanonicalImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "isCanonical", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isCanonical", e);
        }
    }

    /**
     * Default implementation of the method used by the lara interpreter to select bodys
     * @return 
     */
    public List<? extends ABody> selectBody() {
        return select(pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ABody.class, SelectOp.DESCENDANTS);
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
     * Clones this function assigning it a new name, inserts the cloned function after the original function. If the name is the same and the original method, automatically removes the cloned method from the class
     * @param newName 
     * @param insert 
     */
    public AFunction cloneImpl(String newName, Boolean insert) {
        throw new UnsupportedOperationException(get_class()+": Action clone not implemented ");
    }

    /**
     * Clones this function assigning it a new name, inserts the cloned function after the original function. If the name is the same and the original method, automatically removes the cloned method from the class
     * @param newName 
     * @param insert 
     */
    public final AFunction clone(String newName, Boolean insert) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "clone", this, Optional.empty(), newName, insert);
        	}
        	AFunction result = this.cloneImpl(newName, insert);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "clone", this, Optional.ofNullable(result), newName, insert);
        	}
        	return result;
        } catch(Exception e) {
        	throw new ActionException(get_class(), "clone", e);
        }
    }

    /**
     * Generates a clone of the provided function on a new file with the provided name (or with a weaver-generated name if one is not provided).
     * @param newName 
     * @param fileName 
     */
    public AFunction cloneOnFileImpl(String newName, String fileName) {
        throw new UnsupportedOperationException(get_class()+": Action cloneOnFile not implemented ");
    }

    /**
     * Generates a clone of the provided function on a new file with the provided name (or with a weaver-generated name if one is not provided).
     * @param newName 
     * @param fileName 
     */
    public final AFunction cloneOnFile(String newName, String fileName) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "cloneOnFile", this, Optional.empty(), newName, fileName);
        	}
        	AFunction result = this.cloneOnFileImpl(newName, fileName);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "cloneOnFile", this, Optional.ofNullable(result), newName, fileName);
        	}
        	return result;
        } catch(Exception e) {
        	throw new ActionException(get_class(), "cloneOnFile", e);
        }
    }

    /**
     * Generates a clone of the provided function on a new file (with the provided join point).
     * @param newName 
     * @param fileName 
     */
    public AFunction cloneOnFileImpl(String newName, AFile fileName) {
        throw new UnsupportedOperationException(get_class()+": Action cloneOnFile not implemented ");
    }

    /**
     * Generates a clone of the provided function on a new file (with the provided join point).
     * @param newName 
     * @param fileName 
     */
    public final AFunction cloneOnFile(String newName, AFile fileName) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "cloneOnFile", this, Optional.empty(), newName, fileName);
        	}
        	AFunction result = this.cloneOnFileImpl(newName, fileName);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "cloneOnFile", this, Optional.ofNullable(result), newName, fileName);
        	}
        	return result;
        } catch(Exception e) {
        	throw new ActionException(get_class(), "cloneOnFile", e);
        }
    }

    /**
     * 
     * @param withReturnType 
     */
    public String getDeclarationImpl(Boolean withReturnType) {
        throw new UnsupportedOperationException(get_class()+": Action getDeclaration not implemented ");
    }

    /**
     * 
     * @param withReturnType 
     */
    public final String getDeclaration(Boolean withReturnType) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "getDeclaration", this, Optional.empty(), withReturnType);
        	}
        	String result = this.getDeclarationImpl(withReturnType);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "getDeclaration", this, Optional.ofNullable(result), withReturnType);
        	}
        	return result;
        } catch(Exception e) {
        	throw new ActionException(get_class(), "getDeclaration", e);
        }
    }

    /**
     * Inserts the joinpoint before the return points of the function (return statements and implicitly, at the end of the function). Returns the last inserted node
     * @param code 
     */
    public AJoinPoint insertReturnImpl(AJoinPoint code) {
        throw new UnsupportedOperationException(get_class()+": Action insertReturn not implemented ");
    }

    /**
     * Inserts the joinpoint before the return points of the function (return statements and implicitly, at the end of the function). Returns the last inserted node
     * @param code 
     */
    public final AJoinPoint insertReturn(AJoinPoint code) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "insertReturn", this, Optional.empty(), code);
        	}
        	AJoinPoint result = this.insertReturnImpl(code);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "insertReturn", this, Optional.ofNullable(result), code);
        	}
        	return result;
        } catch(Exception e) {
        	throw new ActionException(get_class(), "insertReturn", e);
        }
    }

    /**
     * Inserts code as a literal statement before the return points of the function (return statements and implicitly, at the end of the function). Returns the last inserted node
     * @param code 
     */
    public AJoinPoint insertReturnImpl(String code) {
        throw new UnsupportedOperationException(get_class()+": Action insertReturn not implemented ");
    }

    /**
     * Inserts code as a literal statement before the return points of the function (return statements and implicitly, at the end of the function). Returns the last inserted node
     * @param code 
     */
    public final AJoinPoint insertReturn(String code) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "insertReturn", this, Optional.empty(), code);
        	}
        	AJoinPoint result = this.insertReturnImpl(code);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "insertReturn", this, Optional.ofNullable(result), code);
        	}
        	return result;
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
        		eventTrigger().triggerAction(Stage.BEGIN, "setParams", this, Optional.empty(), new Object[] { params});
        	}
        	this.setParamsImpl(params);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setParams", this, Optional.empty(), new Object[] { params});
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
        		eventTrigger().triggerAction(Stage.BEGIN, "setParamsFromStrings", this, Optional.empty(), new Object[] { params});
        	}
        	this.setParamsFromStringsImpl(params);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setParamsFromStrings", this, Optional.empty(), new Object[] { params});
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setParamsFromStrings", e);
        }
    }

    /**
     * Sets the parameter of the function at the given position
     * @param index 
     * @param param 
     */
    public void setParamImpl(Integer index, AParam param) {
        throw new UnsupportedOperationException(get_class()+": Action setParam not implemented ");
    }

    /**
     * Sets the parameter of the function at the given position
     * @param index 
     * @param param 
     */
    public final void setParam(Integer index, AParam param) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setParam", this, Optional.empty(), index, param);
        	}
        	this.setParamImpl(index, param);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setParam", this, Optional.empty(), index, param);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setParam", e);
        }
    }

    /**
     * Sets the parameter of the function at the given position
     * @param index 
     * @param name 
     * @param type 
     */
    public void setParamImpl(Integer index, String name, AType type) {
        throw new UnsupportedOperationException(get_class()+": Action setParam not implemented ");
    }

    /**
     * Sets the parameter of the function at the given position
     * @param index 
     * @param name 
     * @param type 
     */
    public final void setParam(Integer index, String name, AType type) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setParam", this, Optional.empty(), index, name, type);
        	}
        	this.setParamImpl(index, name, type);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setParam", this, Optional.empty(), index, name, type);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setParam", e);
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
        		eventTrigger().triggerAction(Stage.BEGIN, "newCall", this, Optional.empty(), new Object[] { args});
        	}
        	ACall result = this.newCallImpl(args);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "newCall", this, Optional.ofNullable(result), new Object[] { args});
        	}
        	return result;
        } catch(Exception e) {
        	throw new ActionException(get_class(), "newCall", e);
        }
    }

    /**
     * Sets the type of the function
     * @param functionType 
     */
    public void setFunctionTypeImpl(AFunctionType functionType) {
        throw new UnsupportedOperationException(get_class()+": Action setFunctionType not implemented ");
    }

    /**
     * Sets the type of the function
     * @param functionType 
     */
    public final void setFunctionType(AFunctionType functionType) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setFunctionType", this, Optional.empty(), functionType);
        	}
        	this.setFunctionTypeImpl(functionType);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setFunctionType", this, Optional.empty(), functionType);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setFunctionType", e);
        }
    }

    /**
     * Sets the return type of the function
     * @param returnType 
     */
    public void setReturnTypeImpl(AType returnType) {
        throw new UnsupportedOperationException(get_class()+": Action setReturnType not implemented ");
    }

    /**
     * Sets the return type of the function
     * @param returnType 
     */
    public final void setReturnType(AType returnType) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setReturnType", this, Optional.empty(), returnType);
        	}
        	this.setReturnTypeImpl(returnType);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setReturnType", this, Optional.empty(), returnType);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setReturnType", e);
        }
    }

    /**
     * Sets the type of a parameter of the function
     * @param index 
     * @param newType 
     */
    public void setParamTypeImpl(Integer index, AType newType) {
        throw new UnsupportedOperationException(get_class()+": Action setParamType not implemented ");
    }

    /**
     * Sets the type of a parameter of the function
     * @param index 
     * @param newType 
     */
    public final void setParamType(Integer index, AType newType) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setParamType", this, Optional.empty(), index, newType);
        	}
        	this.setParamTypeImpl(index, newType);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setParamType", this, Optional.empty(), index, newType);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setParamType", e);
        }
    }

    /**
     * Adds a new parameter to the function
     * @param param 
     */
    public void addParamImpl(AParam param) {
        throw new UnsupportedOperationException(get_class()+": Action addParam not implemented ");
    }

    /**
     * Adds a new parameter to the function
     * @param param 
     */
    public final void addParam(AParam param) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "addParam", this, Optional.empty(), param);
        	}
        	this.addParamImpl(param);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "addParam", this, Optional.empty(), param);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "addParam", e);
        }
    }

    /**
     * Adds a new parameter to the function
     * @param name 
     * @param type 
     */
    public void addParamImpl(String name, AType type) {
        throw new UnsupportedOperationException(get_class()+": Action addParam not implemented ");
    }

    /**
     * Adds a new parameter to the function
     * @param name 
     * @param type 
     */
    public final void addParam(String name, AType type) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "addParam", this, Optional.empty(), name, type);
        	}
        	this.addParamImpl(name, type);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "addParam", this, Optional.empty(), name, type);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "addParam", e);
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
     * Get value on attribute attrsArrayImpl
     * @return the attribute's value
     */
    @Override
    public AAttribute[] getAttrsArrayImpl() {
        return this.aDeclarator.getAttrsArrayImpl();
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
     * Get value on attribute parent
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getParentImpl() {
        return this.aDeclarator.getParentImpl();
    }

    /**
     * Get value on attribute ast
     * @return the attribute's value
     */
    @Override
    public String getAstImpl() {
        return this.aDeclarator.getAstImpl();
    }

    /**
     * Get value on attribute siblingsLeftArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] getSiblingsLeftArrayImpl() {
        return this.aDeclarator.getSiblingsLeftArrayImpl();
    }

    /**
     * Get value on attribute data
     * @return the attribute's value
     */
    @Override
    public Object getDataImpl() {
        return this.aDeclarator.getDataImpl();
    }

    /**
     * Get value on attribute hasChildren
     * @return the attribute's value
     */
    @Override
    public Boolean getHasChildrenImpl() {
        return this.aDeclarator.getHasChildrenImpl();
    }

    /**
     * Get value on attribute type
     * @return the attribute's value
     */
    @Override
    public AType getTypeImpl() {
        return this.aDeclarator.getTypeImpl();
    }

    /**
     * Get value on attribute siblingsRightArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] getSiblingsRightArrayImpl() {
        return this.aDeclarator.getSiblingsRightArrayImpl();
    }

    /**
     * Get value on attribute rightJp
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getRightJpImpl() {
        return this.aDeclarator.getRightJpImpl();
    }

    /**
     * Get value on attribute isCilk
     * @return the attribute's value
     */
    @Override
    public Boolean getIsCilkImpl() {
        return this.aDeclarator.getIsCilkImpl();
    }

    /**
     * Get value on attribute filepath
     * @return the attribute's value
     */
    @Override
    public String getFilepathImpl() {
        return this.aDeclarator.getFilepathImpl();
    }

    /**
     * Get value on attribute scopeNodesArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] getScopeNodesArrayImpl() {
        return this.aDeclarator.getScopeNodesArrayImpl();
    }

    /**
     * Get value on attribute childrenArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] getChildrenArrayImpl() {
        return this.aDeclarator.getChildrenArrayImpl();
    }

    /**
     * Get value on attribute firstChild
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getFirstChildImpl() {
        return this.aDeclarator.getFirstChildImpl();
    }

    /**
     * Get value on attribute numChildren
     * @return the attribute's value
     */
    @Override
    public Integer getNumChildrenImpl() {
        return this.aDeclarator.getNumChildrenImpl();
    }

    /**
     * Get value on attribute leftJp
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getLeftJpImpl() {
        return this.aDeclarator.getLeftJpImpl();
    }

    /**
     * Get value on attribute inlineCommentsArrayImpl
     * @return the attribute's value
     */
    @Override
    public AComment[] getInlineCommentsArrayImpl() {
        return this.aDeclarator.getInlineCommentsArrayImpl();
    }

    /**
     * Get value on attribute astName
     * @return the attribute's value
     */
    @Override
    public String getAstNameImpl() {
        return this.aDeclarator.getAstNameImpl();
    }

    /**
     * Get value on attribute jpId
     * @return the attribute's value
     */
    @Override
    public String getJpIdImpl() {
        return this.aDeclarator.getJpIdImpl();
    }

    /**
     * Get value on attribute astId
     * @return the attribute's value
     */
    @Override
    public String getAstIdImpl() {
        return this.aDeclarator.getAstIdImpl();
    }

    /**
     * Get value on attribute contains
     * @return the attribute's value
     */
    @Override
    public Boolean containsImpl(AJoinPoint jp) {
        return this.aDeclarator.containsImpl(jp);
    }

    /**
     * Get value on attribute filename
     * @return the attribute's value
     */
    @Override
    public String getFilenameImpl() {
        return this.aDeclarator.getFilenameImpl();
    }

    /**
     * Get value on attribute javaFieldsArrayImpl
     * @return the attribute's value
     */
    @Override
    public String[] getJavaFieldsArrayImpl() {
        return this.aDeclarator.getJavaFieldsArrayImpl();
    }

    /**
     * Get value on attribute isInSystemHeader
     * @return the attribute's value
     */
    @Override
    public Boolean getIsInSystemHeaderImpl() {
        return this.aDeclarator.getIsInSystemHeaderImpl();
    }

    /**
     * Get value on attribute bitWidth
     * @return the attribute's value
     */
    @Override
    public Integer getBitWidthImpl() {
        return this.aDeclarator.getBitWidthImpl();
    }

    /**
     * Get value on attribute userField
     * @return the attribute's value
     */
    @Override
    public Object userFieldImpl(String fieldName) {
        return this.aDeclarator.userFieldImpl(fieldName);
    }

    /**
     * Get value on attribute hasNode
     * @return the attribute's value
     */
    @Override
    public Boolean hasNodeImpl(Object nodeOrJp) {
        return this.aDeclarator.hasNodeImpl(nodeOrJp);
    }

    /**
     * Get value on attribute endLine
     * @return the attribute's value
     */
    @Override
    public Integer getEndLineImpl() {
        return this.aDeclarator.getEndLineImpl();
    }

    /**
     * Get value on attribute endColumn
     * @return the attribute's value
     */
    @Override
    public Integer getEndColumnImpl() {
        return this.aDeclarator.getEndColumnImpl();
    }

    /**
     * Get value on attribute code
     * @return the attribute's value
     */
    @Override
    public String getCodeImpl() {
        return this.aDeclarator.getCodeImpl();
    }

    /**
     * Get value on attribute isInsideLoopHeader
     * @return the attribute's value
     */
    @Override
    public Boolean getIsInsideLoopHeaderImpl() {
        return this.aDeclarator.getIsInsideLoopHeaderImpl();
    }

    /**
     * Get value on attribute line
     * @return the attribute's value
     */
    @Override
    public Integer getLineImpl() {
        return this.aDeclarator.getLineImpl();
    }

    /**
     * Get value on attribute keysArrayImpl
     * @return the attribute's value
     */
    @Override
    public String[] getKeysArrayImpl() {
        return this.aDeclarator.getKeysArrayImpl();
    }

    /**
     * Get value on attribute isInsideHeader
     * @return the attribute's value
     */
    @Override
    public Boolean getIsInsideHeaderImpl() {
        return this.aDeclarator.getIsInsideHeaderImpl();
    }

    /**
     * Get value on attribute astNumChildren
     * @return the attribute's value
     */
    @Override
    public Integer getAstNumChildrenImpl() {
        return this.aDeclarator.getAstNumChildrenImpl();
    }

    /**
     * Get value on attribute descendantsArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] getDescendantsArrayImpl() {
        return this.aDeclarator.getDescendantsArrayImpl();
    }

    /**
     * Get value on attribute astChildrenArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] getAstChildrenArrayImpl() {
        return this.aDeclarator.getAstChildrenArrayImpl();
    }

    /**
     * Get value on attribute isMacro
     * @return the attribute's value
     */
    @Override
    public Boolean getIsMacroImpl() {
        return this.aDeclarator.getIsMacroImpl();
    }

    /**
     * Get value on attribute lastChild
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getLastChildImpl() {
        return this.aDeclarator.getLastChildImpl();
    }

    /**
     * Get value on attribute root
     * @return the attribute's value
     */
    @Override
    public AProgram getRootImpl() {
        return this.aDeclarator.getRootImpl();
    }

    /**
     * Get value on attribute javaValue
     * @return the attribute's value
     */
    @Override
    public Object javaValueImpl(String fieldName) {
        return this.aDeclarator.javaValueImpl(fieldName);
    }

    /**
     * Get value on attribute keyType
     * @return the attribute's value
     */
    @Override
    public Object keyTypeImpl(String key) {
        return this.aDeclarator.keyTypeImpl(key);
    }

    /**
     * Get value on attribute chainAncestor
     * @return the attribute's value
     */
    @Override
    public AJoinPoint chainAncestorImpl(String type) {
        return this.aDeclarator.chainAncestorImpl(type);
    }

    /**
     * Get value on attribute chainArrayImpl
     * @return the attribute's value
     */
    @Override
    public String[] getChainArrayImpl() {
        return this.aDeclarator.getChainArrayImpl();
    }

    /**
     * Get value on attribute joinpointType
     * @return the attribute's value
     */
    @Override
    public String getJoinpointTypeImpl() {
        return this.aDeclarator.getJoinpointTypeImpl();
    }

    /**
     * Get value on attribute currentRegion
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getCurrentRegionImpl() {
        return this.aDeclarator.getCurrentRegionImpl();
    }

    /**
     * Get value on attribute column
     * @return the attribute's value
     */
    @Override
    public Integer getColumnImpl() {
        return this.aDeclarator.getColumnImpl();
    }

    /**
     * Get value on attribute parentRegion
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getParentRegionImpl() {
        return this.aDeclarator.getParentRegionImpl();
    }

    /**
     * Get value on attribute getValue
     * @return the attribute's value
     */
    @Override
    public Object getValueImpl(String key) {
        return this.aDeclarator.getValueImpl(key);
    }

    /**
     * Get value on attribute depth
     * @return the attribute's value
     */
    @Override
    public Integer getDepthImpl() {
        return this.aDeclarator.getDepthImpl();
    }

    /**
     * Get value on attribute javaFieldType
     * @return the attribute's value
     */
    @Override
    public String javaFieldTypeImpl(String fieldName) {
        return this.aDeclarator.javaFieldTypeImpl(fieldName);
    }

    /**
     * Get value on attribute location
     * @return the attribute's value
     */
    @Override
    public String getLocationImpl() {
        return this.aDeclarator.getLocationImpl();
    }

    /**
     * Get value on attribute getUserField
     * @return the attribute's value
     */
    @Override
    public Object getUserFieldImpl(String fieldName) {
        return this.aDeclarator.getUserFieldImpl(fieldName);
    }

    /**
     * Get value on attribute hasType
     * @return the attribute's value
     */
    @Override
    public Boolean getHasTypeImpl() {
        return this.aDeclarator.getHasTypeImpl();
    }

    /**
     * Get value on attribute pragmasArrayImpl
     * @return the attribute's value
     */
    @Override
    public APragma[] getPragmasArrayImpl() {
        return this.aDeclarator.getPragmasArrayImpl();
    }

    /**
     * Get value on attribute stmt
     * @return the attribute's value
     */
    @Override
    public AStatement getStmtImpl() {
        return this.aDeclarator.getStmtImpl();
    }

    /**
     * Get value on attribute hasParent
     * @return the attribute's value
     */
    @Override
    public Boolean getHasParentImpl() {
        return this.aDeclarator.getHasParentImpl();
    }

    /**
     * true, if this node is a Java instance of the given name, which corresponds to a simple Java class name of an AST node. For an equivalent function for join point names, use 'instanceOf(joinPointName)'
     * @param className 
     */
    @Override
    public Boolean astIsInstanceImpl(String className) {
        return this.aDeclarator.astIsInstanceImpl(className);
    }

    /**
     * Looks in the descendants for the first node of the given type
     * @param type 
     */
    @Override
    public AJoinPoint getFirstJpImpl(String type) {
        return this.aDeclarator.getFirstJpImpl(type);
    }

    /**
     * Returns the child of the node at the given index, ignoring null nodes
     * @param index 
     */
    @Override
    public AJoinPoint getChildImpl(Integer index) {
        return this.aDeclarator.getChildImpl(index);
    }

    /**
     * Returns the child of the node at the given index, considering null nodes
     * @param index 
     */
    @Override
    public AJoinPoint getAstChildImpl(Integer index) {
        return this.aDeclarator.getAstChildImpl(index);
    }

    /**
     * Looks for an ancestor joinpoint name, walking back on the AST
     * @param type 
     */
    @Override
    public AJoinPoint getAncestorImpl(String type) {
        return this.aDeclarator.getAncestorImpl(type);
    }

    /**
     * Retrieves the descendants of the given type
     * @param type 
     */
    @Override
    public AJoinPoint[] getDescendantsImpl(String type) {
        return this.aDeclarator.getDescendantsImpl(type);
    }

    /**
     * Retrieves the descendants of the given type, including the node itself
     * @param type 
     */
    @Override
    public AJoinPoint[] getDescendantsAndSelfImpl(String type) {
        return this.aDeclarator.getDescendantsAndSelfImpl(type);
    }

    /**
     * Replaces this node with the given node
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
     * Overload which accepts a list of join points
     * @param node 
     */
    @Override
    public AJoinPoint replaceWithImpl(AJoinPoint[] node) {
        return this.aDeclarator.replaceWithImpl(node);
    }

    /**
     * Overload which accepts a list of strings
     * @param node 
     */
    @Override
    public AJoinPoint replaceWithStringsImpl(String[] node) {
        return this.aDeclarator.replaceWithStringsImpl(node);
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
    public AJoinPoint detachImpl() {
        return this.aDeclarator.detachImpl();
    }

    /**
     * Sets the type of a node, if it has a type
     * @param type 
     */
    @Override
    public void setTypeImpl(AType type) {
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
     * Removes the children of this node
     */
    @Override
    public void removeChildrenImpl() {
        this.aDeclarator.removeChildrenImpl();
    }

    /**
     * Replaces the first child, or inserts the join point if no child is present
     * @param node 
     */
    @Override
    public void setFirstChildImpl(AJoinPoint node) {
        this.aDeclarator.setFirstChildImpl(node);
    }

    /**
     * Replaces the last child, or inserts the join point if no child is present
     * @param node 
     */
    @Override
    public void setLastChildImpl(AJoinPoint node) {
        this.aDeclarator.setLastChildImpl(node);
    }

    /**
     * Replaces this join point with a comment with the same contents as .code
     * @param prefix 
     * @param suffix 
     */
    @Override
    public AJoinPoint toCommentImpl(String prefix, String suffix) {
        return this.aDeclarator.toCommentImpl(prefix, suffix);
    }

    /**
     * Sets the commented that are embedded in a node
     * @param comments 
     */
    @Override
    public void setInlineCommentsImpl(String[] comments) {
        this.aDeclarator.setInlineCommentsImpl(comments);
    }

    /**
     * Sets the commented that are embedded in a node
     * @param comments 
     */
    @Override
    public void setInlineCommentsImpl(String comments) {
        this.aDeclarator.setInlineCommentsImpl(comments);
    }

    /**
     * Setting data directly is not supported, this action just emits a warning and does nothing
     * @param source 
     */
    @Override
    public void setDataImpl(Object source) {
        this.aDeclarator.setDataImpl(source);
    }

    /**
     * Copies all enumerable own properties from the source object to the .data object
     * @param source 
     */
    @Override
    public void dataAssignImpl(Object source) {
        this.aDeclarator.dataAssignImpl(source);
    }

    /**
     * Clears all properties from the .data object
     */
    @Override
    public void dataClearImpl() {
        this.aDeclarator.dataClearImpl();
    }

    /**
     * 
     * @param position 
     * @param code 
     */
    @Override
    public AJoinPoint[] insertImpl(String position, String code) {
        return this.aDeclarator.insertImpl(position, code);
    }

    /**
     * 
     * @param position 
     * @param code 
     */
    @Override
    public AJoinPoint[] insertImpl(String position, JoinPoint code) {
        return this.aDeclarator.insertImpl(position, code);
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
        case "functionType": {
        	if(value instanceof AFunctionType){
        		this.defFunctionTypeImpl((AFunctionType)value);
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
        case "returnType": {
        	if(value instanceof AType){
        		this.defReturnTypeImpl((AType)value);
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
        attributes.add("isImplementation");
        attributes.add("isPrototype");
        attributes.add("functionType");
        attributes.add("declarationJp");
        attributes.add("declarationJps");
        attributes.add("definitionJp");
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
        attributes.add("returnType");
        attributes.add("isCudaKernel");
        attributes.add("canonical");
        attributes.add("isCanonical");
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
        actions.add("function clone(String, Boolean)");
        actions.add("function cloneOnFile(String, String)");
        actions.add("function cloneOnFile(String, file)");
        actions.add("String getDeclaration(Boolean)");
        actions.add("joinpoint insertReturn(joinpoint)");
        actions.add("joinpoint insertReturn(String)");
        actions.add("void setParams(param[])");
        actions.add("void setParamsFromStrings(String[])");
        actions.add("void setParam(Integer, param)");
        actions.add("void setParam(Integer, String, type)");
        actions.add("void setBody(scope)");
        actions.add("call newCall(joinpoint[])");
        actions.add("void setFunctionType(functionType)");
        actions.add("void setReturnType(type)");
        actions.add("void setParamType(Integer, type)");
        actions.add("void addParam(param)");
        actions.add("void addParam(String, type)");
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
        ISIMPLEMENTATION("isImplementation"),
        ISPROTOTYPE("isPrototype"),
        FUNCTIONTYPE("functionType"),
        DECLARATIONJP("declarationJp"),
        DECLARATIONJPS("declarationJps"),
        DEFINITIONJP("definitionJp"),
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
        RETURNTYPE("returnType"),
        ISCUDAKERNEL("isCudaKernel"),
        CANONICAL("canonical"),
        ISCANONICAL("isCanonical"),
        NAME("name"),
        ISPUBLIC("isPublic"),
        QUALIFIEDPREFIX("qualifiedPrefix"),
        QUALIFIEDNAME("qualifiedName"),
        ATTRS("attrs"),
        PARENT("parent"),
        AST("ast"),
        SIBLINGSLEFT("siblingsLeft"),
        DATA("data"),
        HASCHILDREN("hasChildren"),
        TYPE("type"),
        SIBLINGSRIGHT("siblingsRight"),
        RIGHTJP("rightJp"),
        ISCILK("isCilk"),
        FILEPATH("filepath"),
        SCOPENODES("scopeNodes"),
        CHILDREN("children"),
        FIRSTCHILD("firstChild"),
        NUMCHILDREN("numChildren"),
        LEFTJP("leftJp"),
        INLINECOMMENTS("inlineComments"),
        ASTNAME("astName"),
        JPID("jpId"),
        ASTID("astId"),
        CONTAINS("contains"),
        FILENAME("filename"),
        JAVAFIELDS("javaFields"),
        ISINSYSTEMHEADER("isInSystemHeader"),
        BITWIDTH("bitWidth"),
        USERFIELD("userField"),
        HASNODE("hasNode"),
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
        COLUMN("column"),
        PARENTREGION("parentRegion"),
        GETVALUE("getValue"),
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
