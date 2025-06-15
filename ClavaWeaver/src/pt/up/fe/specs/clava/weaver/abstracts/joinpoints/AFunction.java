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
        Object nativeArray0 = aCallArrayImpl0;
        return nativeArray0;
    }

    /**
     * Get value on attribute calls
     * @return the attribute's value
     */
    public final Object getCalls() {
        try {
        	Object result = this.getCallsImpl();
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "calls", e);
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
        	AFunction result = this.getCanonicalImpl();
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "canonical", e);
        }
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
        	AFunction result = this.getDeclarationJpImpl();
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
        Object nativeArray0 = aFunctionArrayImpl0;
        return nativeArray0;
    }

    /**
     * Returns the prototypes of this function that are present in the code. If there are none, returns an empty array
     */
    public final Object getDeclarationJps() {
        try {
        	Object result = this.getDeclarationJpsImpl();
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
        	AFunction result = this.getDefinitionJpImpl();
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "definitionJp", e);
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
        	AFunctionType result = this.getFunctionTypeImpl();
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "functionType", e);
        }
    }

    /**
     * 
     * @param withReturnType
     * @return 
     */
    public abstract String getDeclarationImpl(Boolean withReturnType);

    /**
     * 
     * @param withReturnType
     * @return 
     */
    public final Object getDeclaration(Boolean withReturnType) {
        try {
        	String result = this.getDeclarationImpl(withReturnType);
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "getDeclaration", e);
        }
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
        	Boolean result = this.getHasDefinitionImpl();
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "hasDefinition", e);
        }
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
        	String result = this.getIdImpl();
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "id", e);
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
        	Boolean result = this.getIsCanonicalImpl();
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isCanonical", e);
        }
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
        	Boolean result = this.getIsCudaKernelImpl();
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isCudaKernel", e);
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
        	Boolean result = this.getIsDeleteImpl();
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isDelete", e);
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
        	Boolean result = this.getIsImplementationImpl();
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isImplementation", e);
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
        	Boolean result = this.getIsInlineImpl();
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isInline", e);
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
        	Boolean result = this.getIsModulePrivateImpl();
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isModulePrivate", e);
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
        	Boolean result = this.getIsPrototypeImpl();
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isPrototype", e);
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
        	Boolean result = this.getIsPureImpl();
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isPure", e);
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
        	Boolean result = this.getIsVirtualImpl();
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isVirtual", e);
        }
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
        Object nativeArray0 = stringArrayImpl0;
        return nativeArray0;
    }

    /**
     * Get value on attribute paramNames
     * @return the attribute's value
     */
    public final Object getParamNames() {
        try {
        	Object result = this.getParamNamesImpl();
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
        Object nativeArray0 = aParamArrayImpl0;
        return nativeArray0;
    }

    /**
     * Get value on attribute params
     * @return the attribute's value
     */
    public final Object getParams() {
        try {
        	Object result = this.getParamsImpl();
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "params", e);
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
        	AType result = this.getReturnTypeImpl();
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "returnType", e);
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
        	String result = this.getSignatureImpl();
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "signature", e);
        }
    }

    /**
     * The storage class of this function (i.e., one of NONE, EXTERN, PRIVATE_EXTERN or STATIC)
     */
    public abstract String getStorageClassImpl();

    /**
     * The storage class of this function (i.e., one of NONE, EXTERN, PRIVATE_EXTERN or STATIC)
     */
    public final Object getStorageClass() {
        try {
        	String result = this.getStorageClassImpl();
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "storageClass", e);
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
        	this.addParamImpl(param);
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
        	this.addParamImpl(name, type);
        } catch(Exception e) {
        	throw new ActionException(get_class(), "addParam", e);
        }
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
    public final Object clone(String newName, Boolean insert) {
        try {
        	AFunction result = this.cloneImpl(newName, insert);
        	return result!=null?result:getUndefinedValue();
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
    public final Object cloneOnFile(String newName, String fileName) {
        try {
        	AFunction result = this.cloneOnFileImpl(newName, fileName);
        	return result!=null?result:getUndefinedValue();
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
    public final Object cloneOnFile(String newName, AFile fileName) {
        try {
        	AFunction result = this.cloneOnFileImpl(newName, fileName);
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new ActionException(get_class(), "cloneOnFile", e);
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
    public final Object insertReturn(AJoinPoint code) {
        try {
        	AJoinPoint result = this.insertReturnImpl(code);
        	return result!=null?result:getUndefinedValue();
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
    public final Object insertReturn(String code) {
        try {
        	AJoinPoint result = this.insertReturnImpl(code);
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new ActionException(get_class(), "insertReturn", e);
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
    public final Object newCall(Object[] args) {
        try {
        	ACall result = this.newCallImpl(pt.up.fe.specs.util.SpecsCollections.cast(args, AJoinPoint.class));
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new ActionException(get_class(), "newCall", e);
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
        	this.setBodyImpl(body);
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setBody", e);
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
        	this.setFunctionTypeImpl(functionType);
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setFunctionType", e);
        }
    }

    /**
     * Sets the parameter of the function at the given position
     * @param index 
     * @param param 
     */
    public void setParamImpl(int index, AParam param) {
        throw new UnsupportedOperationException(get_class()+": Action setParam not implemented ");
    }

    /**
     * Sets the parameter of the function at the given position
     * @param index 
     * @param param 
     */
    public final void setParam(int index, AParam param) {
        try {
        	this.setParamImpl(index, param);
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
    public void setParamImpl(int index, String name, AType type) {
        throw new UnsupportedOperationException(get_class()+": Action setParam not implemented ");
    }

    /**
     * Sets the parameter of the function at the given position
     * @param index 
     * @param name 
     * @param type 
     */
    public final void setParam(int index, String name, AType type) {
        try {
        	this.setParamImpl(index, name, type);
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setParam", e);
        }
    }

    /**
     * Sets the type of a parameter of the function
     * @param index 
     * @param newType 
     */
    public void setParamTypeImpl(int index, AType newType) {
        throw new UnsupportedOperationException(get_class()+": Action setParamType not implemented ");
    }

    /**
     * Sets the type of a parameter of the function
     * @param index 
     * @param newType 
     */
    public final void setParamType(int index, AType newType) {
        try {
        	this.setParamTypeImpl(index, newType);
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setParamType", e);
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
    public final void setParams(Object[] params) {
        try {
        	this.setParamsImpl(pt.up.fe.specs.util.SpecsCollections.cast(params, AParam.class));
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
    public final void setParamsFromStrings(Object[] params) {
        try {
        	this.setParamsFromStringsImpl(pt.up.fe.specs.util.SpecsCollections.cast(params, String.class));
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setParamsFromStrings", e);
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
        	this.setReturnTypeImpl(returnType);
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setReturnType", e);
        }
    }

    /**
     * Sets the storage class of this specific function decl. AUTO and REGISTER are not allowed for functions, and EXTERN is not allowed in function implementations, or function declarations that are in the same file as the implementation. Returns true if the storage class changed, false otherwise.
     * @param storageClass 
     */
    public boolean setStorageClassImpl(String storageClass) {
        throw new UnsupportedOperationException(get_class()+": Action setStorageClass not implemented ");
    }

    /**
     * Sets the storage class of this specific function decl. AUTO and REGISTER are not allowed for functions, and EXTERN is not allowed in function implementations, or function declarations that are in the same file as the implementation. Returns true if the storage class changed, false otherwise.
     * @param storageClass 
     */
    public final Object setStorageClass(String storageClass) {
        try {
        	boolean result = this.setStorageClassImpl(storageClass);
        	return result;
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setStorageClass", e);
        }
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
     * Get value on attribute name
     * @return the attribute's value
     */
    @Override
    public String getNameImpl() {
        return this.aDeclarator.getNameImpl();
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
     * Get value on attribute qualifiedPrefix
     * @return the attribute's value
     */
    @Override
    public String getQualifiedPrefixImpl() {
        return this.aDeclarator.getQualifiedPrefixImpl();
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
     * Get value on attribute ast
     * @return the attribute's value
     */
    @Override
    public String getAstImpl() {
        return this.aDeclarator.getAstImpl();
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
     * Get value on attribute astId
     * @return the attribute's value
     */
    @Override
    public String getAstIdImpl() {
        return this.aDeclarator.getAstIdImpl();
    }

    /**
     * Get value on attribute astIsInstance
     * @return the attribute's value
     */
    @Override
    public Boolean astIsInstanceImpl(String className) {
        return this.aDeclarator.astIsInstanceImpl(className);
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
     * Get value on attribute astNumChildren
     * @return the attribute's value
     */
    @Override
    public Integer getAstNumChildrenImpl() {
        return this.aDeclarator.getAstNumChildrenImpl();
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
     * Get value on attribute chainArrayImpl
     * @return the attribute's value
     */
    @Override
    public String[] getChainArrayImpl() {
        return this.aDeclarator.getChainArrayImpl();
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
     * Get value on attribute code
     * @return the attribute's value
     */
    @Override
    public String getCodeImpl() {
        return this.aDeclarator.getCodeImpl();
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
     * Get value on attribute contains
     * @return the attribute's value
     */
    @Override
    public Boolean containsImpl(AJoinPoint jp) {
        return this.aDeclarator.containsImpl(jp);
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
     * Get value on attribute data
     * @return the attribute's value
     */
    @Override
    public Object getDataImpl() {
        return this.aDeclarator.getDataImpl();
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
     * Get value on attribute descendantsArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] getDescendantsArrayImpl() {
        return this.aDeclarator.getDescendantsArrayImpl();
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
     * Get value on attribute endLine
     * @return the attribute's value
     */
    @Override
    public Integer getEndLineImpl() {
        return this.aDeclarator.getEndLineImpl();
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
     * Get value on attribute filepath
     * @return the attribute's value
     */
    @Override
    public String getFilepathImpl() {
        return this.aDeclarator.getFilepathImpl();
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
     * Get value on attribute getAncestor
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getAncestorImpl(String type) {
        return this.aDeclarator.getAncestorImpl(type);
    }

    /**
     * Get value on attribute getAstAncestor
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getAstAncestorImpl(String type) {
        return this.aDeclarator.getAstAncestorImpl(type);
    }

    /**
     * Get value on attribute getAstChild
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getAstChildImpl(int index) {
        return this.aDeclarator.getAstChildImpl(index);
    }

    /**
     * Get value on attribute getChainAncestor
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getChainAncestorImpl(String type) {
        return this.aDeclarator.getChainAncestorImpl(type);
    }

    /**
     * Get value on attribute getChild
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getChildImpl(int index) {
        return this.aDeclarator.getChildImpl(index);
    }

    /**
     * Get value on attribute getDescendantsArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] getDescendantsArrayImpl(String type) {
        return this.aDeclarator.getDescendantsArrayImpl(type);
    }

    /**
     * Get value on attribute getDescendantsAndSelfArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] getDescendantsAndSelfArrayImpl(String type) {
        return this.aDeclarator.getDescendantsAndSelfArrayImpl(type);
    }

    /**
     * Get value on attribute getFirstJp
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getFirstJpImpl(String type) {
        return this.aDeclarator.getFirstJpImpl(type);
    }

    /**
     * Get value on attribute getJavaFieldType
     * @return the attribute's value
     */
    @Override
    public String getJavaFieldTypeImpl(String fieldName) {
        return this.aDeclarator.getJavaFieldTypeImpl(fieldName);
    }

    /**
     * Get value on attribute getKeyType
     * @return the attribute's value
     */
    @Override
    public Object getKeyTypeImpl(String key) {
        return this.aDeclarator.getKeyTypeImpl(key);
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
     * Get value on attribute getValue
     * @return the attribute's value
     */
    @Override
    public Object getValueImpl(String key) {
        return this.aDeclarator.getValueImpl(key);
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
     * Get value on attribute hasNode
     * @return the attribute's value
     */
    @Override
    public Boolean hasNodeImpl(Object nodeOrJp) {
        return this.aDeclarator.hasNodeImpl(nodeOrJp);
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
     * Get value on attribute hasType
     * @return the attribute's value
     */
    @Override
    public Boolean getHasTypeImpl() {
        return this.aDeclarator.getHasTypeImpl();
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
     * Get value on attribute isCilk
     * @return the attribute's value
     */
    @Override
    public Boolean getIsCilkImpl() {
        return this.aDeclarator.getIsCilkImpl();
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
     * Get value on attribute isInsideHeader
     * @return the attribute's value
     */
    @Override
    public Boolean getIsInsideHeaderImpl() {
        return this.aDeclarator.getIsInsideHeaderImpl();
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
     * Get value on attribute isMacro
     * @return the attribute's value
     */
    @Override
    public Boolean getIsMacroImpl() {
        return this.aDeclarator.getIsMacroImpl();
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
     * Get value on attribute jpId
     * @return the attribute's value
     */
    @Override
    public String getJpIdImpl() {
        return this.aDeclarator.getJpIdImpl();
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
     * Get value on attribute lastChild
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getLastChildImpl() {
        return this.aDeclarator.getLastChildImpl();
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
     * Get value on attribute line
     * @return the attribute's value
     */
    @Override
    public Integer getLineImpl() {
        return this.aDeclarator.getLineImpl();
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
     * Get value on attribute numChildren
     * @return the attribute's value
     */
    @Override
    public Integer getNumChildrenImpl() {
        return this.aDeclarator.getNumChildrenImpl();
    }

    /**
     * Get value on attribute originNode
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getOriginNodeImpl() {
        return this.aDeclarator.getOriginNodeImpl();
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
     * Get value on attribute parentRegion
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getParentRegionImpl() {
        return this.aDeclarator.getParentRegionImpl();
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
     * Get value on attribute rightJp
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getRightJpImpl() {
        return this.aDeclarator.getRightJpImpl();
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
     * Get value on attribute scopeNodesArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] getScopeNodesArrayImpl() {
        return this.aDeclarator.getScopeNodesArrayImpl();
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
     * Get value on attribute siblingsRightArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] getSiblingsRightArrayImpl() {
        return this.aDeclarator.getSiblingsRightArrayImpl();
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
     * Get value on attribute type
     * @return the attribute's value
     */
    @Override
    public AType getTypeImpl() {
        return this.aDeclarator.getTypeImpl();
    }

    /**
     * Performs a copy of the node and its children, but not of the nodes in its fields
     */
    @Override
    public AJoinPoint copyImpl() {
        return this.aDeclarator.copyImpl();
    }

    /**
     * Clears all properties from the .data object
     */
    @Override
    public void dataClearImpl() {
        this.aDeclarator.dataClearImpl();
    }

    /**
     * Performs a copy of the node and its children, including the nodes in their fields (only the first level of field nodes, this function is not recursive)
     */
    @Override
    public AJoinPoint deepCopyImpl() {
        return this.aDeclarator.deepCopyImpl();
    }

    /**
     * Removes the node associated to this joinpoint from the AST
     */
    @Override
    public AJoinPoint detachImpl() {
        return this.aDeclarator.detachImpl();
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
     * Setting data directly is not supported, this action just emits a warning and does nothing
     * @param source 
     */
    @Override
    public void setDataImpl(Object source) {
        this.aDeclarator.setDataImpl(source);
    }

    /**
     * Replaces the first child, or inserts the join point if no child is present. Returns the replaced child, or undefined if there was no child present.
     * @param node 
     */
    @Override
    public AJoinPoint setFirstChildImpl(AJoinPoint node) {
        return this.aDeclarator.setFirstChildImpl(node);
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
     * Replaces the last child, or inserts the join point if no child is present. Returns the replaced child, or undefined if there was no child present.
     * @param node 
     */
    @Override
    public AJoinPoint setLastChildImpl(AJoinPoint node) {
        return this.aDeclarator.setLastChildImpl(node);
    }

    /**
     * Sets the name of this namedDecl
     * @param name 
     */
    @Override
    public void setNameImpl(String name) {
        this.aDeclarator.setNameImpl(name);
    }

    /**
     * Sets the qualified name of this namedDecl (changes both the name and qualified prefix)
     * @param name 
     */
    @Override
    public void setQualifiedNameImpl(String name) {
        this.aDeclarator.setQualifiedNameImpl(name);
    }

    /**
     * Sets the qualified prefix of this namedDecl
     * @param qualifiedPrefix 
     */
    @Override
    public void setQualifiedPrefixImpl(String qualifiedPrefix) {
        this.aDeclarator.setQualifiedPrefixImpl(qualifiedPrefix);
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
     * Replaces this join point with a comment with the same contents as .code
     * @param prefix 
     * @param suffix 
     */
    @Override
    public AJoinPoint toCommentImpl(String prefix, String suffix) {
        return this.aDeclarator.toCommentImpl(prefix, suffix);
    }

    /**
     * 
     */
    @Override
    public Optional<? extends ADeclarator> getSuper() {
        return Optional.of(this.aDeclarator);
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
        BODY("body"),
        CALLS("calls"),
        CANONICAL("canonical"),
        DECLARATIONJP("declarationJp"),
        DECLARATIONJPS("declarationJps"),
        DEFINITIONJP("definitionJp"),
        FUNCTIONTYPE("functionType"),
        GETDECLARATION("getDeclaration"),
        HASDEFINITION("hasDefinition"),
        ID("id"),
        ISCANONICAL("isCanonical"),
        ISCUDAKERNEL("isCudaKernel"),
        ISDELETE("isDelete"),
        ISIMPLEMENTATION("isImplementation"),
        ISINLINE("isInline"),
        ISMODULEPRIVATE("isModulePrivate"),
        ISPROTOTYPE("isPrototype"),
        ISPURE("isPure"),
        ISVIRTUAL("isVirtual"),
        PARAMNAMES("paramNames"),
        PARAMS("params"),
        RETURNTYPE("returnType"),
        SIGNATURE("signature"),
        STORAGECLASS("storageClass"),
        ISPUBLIC("isPublic"),
        NAME("name"),
        QUALIFIEDNAME("qualifiedName"),
        QUALIFIEDPREFIX("qualifiedPrefix"),
        ATTRS("attrs"),
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
