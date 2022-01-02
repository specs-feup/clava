package pt.up.fe.specs.clava.weaver.abstracts.joinpoints;

import org.lara.interpreter.weaver.interf.events.Stage;
import java.util.Optional;
import org.lara.interpreter.exception.AttributeException;
import org.lara.interpreter.exception.ActionException;
import pt.up.fe.specs.clava.weaver.enums.StorageClass;
import java.util.List;
import java.util.Map;
import org.lara.interpreter.weaver.interf.JoinPoint;
import java.util.stream.Collectors;
import java.util.Arrays;

/**
 * Auto-Generated class for join point AMethod
 * This class is overwritten by the Weaver Generator.
 * 
 * Represents a C++ class method declaration or definition
 * @author Lara Weaver Generator
 */
public abstract class AMethod extends AFunction {

    protected AFunction aFunction;

    /**
     * 
     */
    public AMethod(AFunction aFunction){
        super(aFunction);
        this.aFunction = aFunction;
    }
    /**
     * Get value on attribute record
     * @return the attribute's value
     */
    public abstract AClass getRecordImpl();

    /**
     * Get value on attribute record
     * @return the attribute's value
     */
    public final Object getRecord() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "record", Optional.empty());
        	}
        	AClass result = this.getRecordImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "record", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "record", e);
        }
    }

    /**
     * Removes the of the method
     */
    public void removeRecordImpl() {
        throw new UnsupportedOperationException(get_class()+": Action removeRecord not implemented ");
    }

    /**
     * Removes the of the method
     */
    public final void removeRecord() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "removeRecord", this, Optional.empty());
        	}
        	this.removeRecordImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "removeRecord", this, Optional.empty());
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "removeRecord", e);
        }
    }

    /**
     * Get value on attribute hasDefinition
     * @return the attribute's value
     */
    @Override
    public Boolean getHasDefinitionImpl() {
        return this.aFunction.getHasDefinitionImpl();
    }

    /**
     * Get value on attribute isImplementation
     * @return the attribute's value
     */
    @Override
    public Boolean getIsImplementationImpl() {
        return this.aFunction.getIsImplementationImpl();
    }

    /**
     * Get value on attribute isPrototype
     * @return the attribute's value
     */
    @Override
    public Boolean getIsPrototypeImpl() {
        return this.aFunction.getIsPrototypeImpl();
    }

    /**
     * Get value on attribute functionType
     * @return the attribute's value
     */
    @Override
    public AFunctionType getFunctionTypeImpl() {
        return this.aFunction.getFunctionTypeImpl();
    }

    /**
     * Get value on attribute declarationJp
     * @return the attribute's value
     */
    @Override
    public AFunction getDeclarationJpImpl() {
        return this.aFunction.getDeclarationJpImpl();
    }

    /**
     * Get value on attribute declarationJpsArrayImpl
     * @return the attribute's value
     */
    @Override
    public AFunction[] getDeclarationJpsArrayImpl() {
        return this.aFunction.getDeclarationJpsArrayImpl();
    }

    /**
     * Get value on attribute definitionJp
     * @return the attribute's value
     */
    @Override
    public AFunction getDefinitionJpImpl() {
        return this.aFunction.getDefinitionJpImpl();
    }

    /**
     * Get value on attribute declaration
     * @return the attribute's value
     */
    @Override
    public String declarationImpl(Boolean withReturnType) {
        return this.aFunction.declarationImpl(withReturnType);
    }

    /**
     * Get value on attribute body
     * @return the attribute's value
     */
    @Override
    public AScope getBodyImpl() {
        return this.aFunction.getBodyImpl();
    }

    /**
     * Get value on attribute paramNamesArrayImpl
     * @return the attribute's value
     */
    @Override
    public String[] getParamNamesArrayImpl() {
        return this.aFunction.getParamNamesArrayImpl();
    }

    /**
     * Get value on attribute paramsArrayImpl
     * @return the attribute's value
     */
    @Override
    public AParam[] getParamsArrayImpl() {
        return this.aFunction.getParamsArrayImpl();
    }

    /**
     * Get value on attribute id
     * @return the attribute's value
     */
    @Override
    public String getIdImpl() {
        return this.aFunction.getIdImpl();
    }

    /**
     * Get value on attribute isInline
     * @return the attribute's value
     */
    @Override
    public Boolean getIsInlineImpl() {
        return this.aFunction.getIsInlineImpl();
    }

    /**
     * Get value on attribute isVirtual
     * @return the attribute's value
     */
    @Override
    public Boolean getIsVirtualImpl() {
        return this.aFunction.getIsVirtualImpl();
    }

    /**
     * Get value on attribute isModulePrivate
     * @return the attribute's value
     */
    @Override
    public Boolean getIsModulePrivateImpl() {
        return this.aFunction.getIsModulePrivateImpl();
    }

    /**
     * Get value on attribute isPure
     * @return the attribute's value
     */
    @Override
    public Boolean getIsPureImpl() {
        return this.aFunction.getIsPureImpl();
    }

    /**
     * Get value on attribute isDelete
     * @return the attribute's value
     */
    @Override
    public Boolean getIsDeleteImpl() {
        return this.aFunction.getIsDeleteImpl();
    }

    /**
     * Get value on attribute storageClass
     * @return the attribute's value
     */
    @Override
    public StorageClass getStorageClassImpl() {
        return this.aFunction.getStorageClassImpl();
    }

    /**
     * Get value on attribute callsArrayImpl
     * @return the attribute's value
     */
    @Override
    public ACall[] getCallsArrayImpl() {
        return this.aFunction.getCallsArrayImpl();
    }

    /**
     * Get value on attribute signature
     * @return the attribute's value
     */
    @Override
    public String getSignatureImpl() {
        return this.aFunction.getSignatureImpl();
    }

    /**
     * Get value on attribute returnType
     * @return the attribute's value
     */
    @Override
    public AType getReturnTypeImpl() {
        return this.aFunction.getReturnTypeImpl();
    }

    /**
     * Get value on attribute isCudaKernel
     * @return the attribute's value
     */
    @Override
    public Boolean getIsCudaKernelImpl() {
        return this.aFunction.getIsCudaKernelImpl();
    }

    /**
     * Method used by the lara interpreter to select bodys
     * @return 
     */
    @Override
    public List<? extends ABody> selectBody() {
        return this.aFunction.selectBody();
    }

    /**
     * Method used by the lara interpreter to select params
     * @return 
     */
    @Override
    public List<? extends AParam> selectParam() {
        return this.aFunction.selectParam();
    }

    /**
     * Method used by the lara interpreter to select decls
     * @return 
     */
    @Override
    public List<? extends ADecl> selectDecl() {
        return this.aFunction.selectDecl();
    }

    /**
     * Get value on attribute name
     * @return the attribute's value
     */
    @Override
    public String getNameImpl() {
        return this.aFunction.getNameImpl();
    }

    /**
     * Get value on attribute isPublic
     * @return the attribute's value
     */
    @Override
    public Boolean getIsPublicImpl() {
        return this.aFunction.getIsPublicImpl();
    }

    /**
     * Get value on attribute qualifiedPrefix
     * @return the attribute's value
     */
    @Override
    public String getQualifiedPrefixImpl() {
        return this.aFunction.getQualifiedPrefixImpl();
    }

    /**
     * Get value on attribute qualifiedName
     * @return the attribute's value
     */
    @Override
    public String getQualifiedNameImpl() {
        return this.aFunction.getQualifiedNameImpl();
    }

    /**
     * Get value on attribute attrsArrayImpl
     * @return the attribute's value
     */
    @Override
    public AAttribute[] getAttrsArrayImpl() {
        return this.aFunction.getAttrsArrayImpl();
    }

    /**
     * 
     */
    public void defNameImpl(String value) {
        this.aFunction.defNameImpl(value);
    }

    /**
     * 
     */
    public void defQualifiedPrefixImpl(String value) {
        this.aFunction.defQualifiedPrefixImpl(value);
    }

    /**
     * 
     */
    public void defQualifiedNameImpl(String value) {
        this.aFunction.defQualifiedNameImpl(value);
    }

    /**
     * 
     */
    public void defFunctionTypeImpl(AFunctionType value) {
        this.aFunction.defFunctionTypeImpl(value);
    }

    /**
     * 
     */
    public void defBodyImpl(AScope value) {
        this.aFunction.defBodyImpl(value);
    }

    /**
     * 
     */
    public void defParamsImpl(AParam[] value) {
        this.aFunction.defParamsImpl(value);
    }

    /**
     * 
     */
    public void defParamsImpl(String[] value) {
        this.aFunction.defParamsImpl(value);
    }

    /**
     * 
     */
    public void defReturnTypeImpl(AType value) {
        this.aFunction.defReturnTypeImpl(value);
    }

    /**
     * Replaces this join point with the given join
     * @param node 
     */
    @Override
    public AJoinPoint replaceWithImpl(AJoinPoint node) {
        return this.aFunction.replaceWithImpl(node);
    }

    /**
     * Overload which accepts a string
     * @param node 
     */
    @Override
    public AJoinPoint replaceWithImpl(String node) {
        return this.aFunction.replaceWithImpl(node);
    }

    /**
     * Inserts the given join point before this join point
     * @param node 
     */
    @Override
    public AJoinPoint insertBeforeImpl(AJoinPoint node) {
        return this.aFunction.insertBeforeImpl(node);
    }

    /**
     * Overload which accepts a string
     * @param node 
     */
    @Override
    public AJoinPoint insertBeforeImpl(String node) {
        return this.aFunction.insertBeforeImpl(node);
    }

    /**
     * Inserts the given join point after this join point
     * @param node 
     */
    @Override
    public AJoinPoint insertAfterImpl(AJoinPoint node) {
        return this.aFunction.insertAfterImpl(node);
    }

    /**
     * Overload which accepts a string
     * @param code 
     */
    @Override
    public AJoinPoint insertAfterImpl(String code) {
        return this.aFunction.insertAfterImpl(code);
    }

    /**
     * Removes the node associated to this joinpoint from the AST
     */
    @Override
    public void detachImpl() {
        this.aFunction.detachImpl();
    }

    /**
     * Sets the type of a node, if it has a type
     * @param type 
     */
    @Override
    public void setTypeImpl(AType type) {
        this.aFunction.setTypeImpl(type);
    }

    /**
     * Performs a copy of the node and its children, but not of the nodes in its fields
     */
    @Override
    public AJoinPoint copyImpl() {
        return this.aFunction.copyImpl();
    }

    /**
     * Performs a copy of the node and its children, including the nodes in their fields (only the first level of field nodes, this function is not recursive)
     */
    @Override
    public AJoinPoint deepCopyImpl() {
        return this.aFunction.deepCopyImpl();
    }

    /**
     * Associates arbitrary values to nodes of the AST
     * @param fieldName 
     * @param value 
     */
    @Override
    public Object setUserFieldImpl(String fieldName, Object value) {
        return this.aFunction.setUserFieldImpl(fieldName, value);
    }

    /**
     * Overload which accepts a map
     * @param fieldNameAndValue 
     */
    @Override
    public Object setUserFieldImpl(Map<?, ?> fieldNameAndValue) {
        return this.aFunction.setUserFieldImpl(fieldNameAndValue);
    }

    /**
     * Sets the value associated with the given property key
     * @param key 
     * @param value 
     */
    @Override
    public AJoinPoint setValueImpl(String key, Object value) {
        return this.aFunction.setValueImpl(key, value);
    }

    /**
     * Adds a message that will be printed to the user after weaving finishes. Identical messages are removed
     * @param message 
     */
    @Override
    public void messageToUserImpl(String message) {
        this.aFunction.messageToUserImpl(message);
    }

    /**
     * Removes the children of this node
     */
    @Override
    public void removeChildrenImpl() {
        this.aFunction.removeChildrenImpl();
    }

    /**
     * Replaces the first child, or inserts the join point if no child is present
     * @param node 
     */
    @Override
    public void setFirstChildImpl(AJoinPoint node) {
        this.aFunction.setFirstChildImpl(node);
    }

    /**
     * Replaces the last child, or inserts the join point if no child is present
     * @param node 
     */
    @Override
    public void setLastChildImpl(AJoinPoint node) {
        this.aFunction.setLastChildImpl(node);
    }

    /**
     * Replaces this join point with a comment with the same contents as .code
     */
    @Override
    public AJoinPoint toCommentImpl() {
        return this.aFunction.toCommentImpl();
    }

    /**
     * Replaces this join point with a comment with the same contents as .code
     * @param prefix 
     */
    @Override
    public AJoinPoint toCommentImpl(String prefix) {
        return this.aFunction.toCommentImpl(prefix);
    }

    /**
     * Replaces this join point with a comment with the same contents as .code
     * @param prefix 
     * @param suffix 
     */
    @Override
    public AJoinPoint toCommentImpl(String prefix, String suffix) {
        return this.aFunction.toCommentImpl(prefix, suffix);
    }

    /**
     * Sets the commented that are embedded in a node
     * @param comments 
     */
    @Override
    public void setInlineCommentsImpl(String[] comments) {
        this.aFunction.setInlineCommentsImpl(comments);
    }

    /**
     * Sets the commented that are embedded in a node
     * @param comments 
     */
    @Override
    public void setInlineCommentsImpl(String comments) {
        this.aFunction.setInlineCommentsImpl(comments);
    }

    /**
     * Setting data directly is not supported, this action just emits a warning and does nothing
     * @param source 
     */
    @Override
    public void setDataImpl(Object source) {
        this.aFunction.setDataImpl(source);
    }

    /**
     * Copies all enumerable own properties from the source object to the .data object
     * @param source 
     */
    @Override
    public void dataAssignImpl(Object source) {
        this.aFunction.dataAssignImpl(source);
    }

    /**
     * Clears all properties from the .data object
     */
    @Override
    public void dataClearImpl() {
        this.aFunction.dataClearImpl();
    }

    /**
     * Clones this function assigning it a new name, inserts the cloned function after the original function. If the name is the same and the original method, automatically removes the cloned method from the class
     * @param newName 
     * @param insert 
     */
    @Override
    public AFunction cloneImpl(String newName, Boolean insert) {
        return this.aFunction.cloneImpl(newName, insert);
    }

    /**
     * Overload which inserts the cloned function by default
     * @param newName 
     */
    @Override
    public AFunction cloneImpl(String newName) {
        return this.aFunction.cloneImpl(newName);
    }

    /**
     * Generates a clone of the provided function on a new file (with a weaver-generated name).
     * @param newName 
     */
    @Override
    public AFunction cloneOnFileImpl(String newName) {
        return this.aFunction.cloneOnFileImpl(newName);
    }

    /**
     * Generates a clone of the provided function on a new file (with the provided name).
     * @param newName 
     * @param fileName 
     */
    @Override
    public AFunction cloneOnFileImpl(String newName, String fileName) {
        return this.aFunction.cloneOnFileImpl(newName, fileName);
    }

    /**
     * Generates a clone of the provided function on a new file (with the provided join point).
     * @param newName 
     * @param fileName 
     */
    @Override
    public AFunction cloneOnFileImpl(String newName, AFile fileName) {
        return this.aFunction.cloneOnFileImpl(newName, fileName);
    }

    /**
     * Inserts the joinpoint before the return points of the function (return statements and implicitly, at the end of the function). Returns the last inserted node
     * @param code 
     */
    @Override
    public AJoinPoint insertReturnImpl(AJoinPoint code) {
        return this.aFunction.insertReturnImpl(code);
    }

    /**
     * Inserts code as a literal statement before the return points of the function (return statements and implicitly, at the end of the function). Returns the last inserted node
     * @param code 
     */
    @Override
    public AJoinPoint insertReturnImpl(String code) {
        return this.aFunction.insertReturnImpl(code);
    }

    /**
     * Sets the parameters of the function
     * @param params 
     */
    @Override
    public void setParamsImpl(AParam[] params) {
        this.aFunction.setParamsImpl(params);
    }

    /**
     * Overload that accepts strings that represent type-varname pairs (e.g., int param1)
     * @param params 
     */
    @Override
    public void setParamsFromStringsImpl(String[] params) {
        this.aFunction.setParamsFromStringsImpl(params);
    }

    /**
     * Sets the parameter of the function at the given position
     * @param index 
     * @param param 
     */
    @Override
    public void setParamImpl(Integer index, AParam param) {
        this.aFunction.setParamImpl(index, param);
    }

    /**
     * Sets the parameter of the function at the given position (overload that accepts a String)
     * @param index 
     * @param param 
     */
    @Override
    public void setParamImpl(Integer index, String param) {
        this.aFunction.setParamImpl(index, param);
    }

    /**
     * Sets the parameter of the function at the given position (overload that accepts a String and a Type)
     * @param index 
     * @param name 
     * @param type 
     */
    @Override
    public void setParamImpl(Integer index, String name, AType type) {
        this.aFunction.setParamImpl(index, name, type);
    }

    /**
     * Sets the body of the function
     * @param body 
     */
    @Override
    public void setBodyImpl(AScope body) {
        this.aFunction.setBodyImpl(body);
    }

    /**
     * Creates a new call to this function
     * @param args 
     */
    @Override
    public ACall newCallImpl(AJoinPoint[] args) {
        return this.aFunction.newCallImpl(args);
    }

    /**
     * Sets the type of the function
     * @param functionType 
     */
    @Override
    public void setFunctionTypeImpl(AFunctionType functionType) {
        this.aFunction.setFunctionTypeImpl(functionType);
    }

    /**
     * Sets the return type of the function
     * @param returnType 
     */
    @Override
    public void setReturnTypeImpl(AType returnType) {
        this.aFunction.setReturnTypeImpl(returnType);
    }

    /**
     * Sets the type of a parameter of the function
     * @param index 
     * @param newType 
     */
    @Override
    public void setParamTypeImpl(Integer index, AType newType) {
        this.aFunction.setParamTypeImpl(index, newType);
    }

    /**
     * Adds a new parameter to the function
     * @param param 
     */
    @Override
    public void addParamImpl(String param) {
        this.aFunction.addParamImpl(param);
    }

    /**
     * Adds a new parameter to the function
     * @param param 
     */
    @Override
    public void addParamImpl(AParam param) {
        this.aFunction.addParamImpl(param);
    }

    /**
     * Adds a new parameter to the function
     * @param name 
     * @param type 
     */
    @Override
    public void addParamImpl(String name, AType type) {
        this.aFunction.addParamImpl(name, type);
    }

    /**
     * 
     * @param position 
     * @param code 
     */
    @Override
    public AJoinPoint[] insertImpl(String position, String code) {
        return this.aFunction.insertImpl(position, code);
    }

    /**
     * 
     * @param position 
     * @param code 
     */
    @Override
    public AJoinPoint[] insertImpl(String position, JoinPoint code) {
        return this.aFunction.insertImpl(position, code);
    }

    /**
     * 
     */
    @Override
    public Optional<? extends AFunction> getSuper() {
        return Optional.of(this.aFunction);
    }

    /**
     * 
     */
    @Override
    public final List<? extends JoinPoint> select(String selectName) {
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
        		joinPointList = this.aFunction.select(selectName);
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
    protected final void fillWithAttributes(List<String> attributes) {
        this.aFunction.fillWithAttributes(attributes);
        attributes.add("record");
    }

    /**
     * 
     */
    @Override
    protected final void fillWithSelects(List<String> selects) {
        this.aFunction.fillWithSelects(selects);
    }

    /**
     * 
     */
    @Override
    protected final void fillWithActions(List<String> actions) {
        this.aFunction.fillWithActions(actions);
        actions.add("void removeRecord()");
    }

    /**
     * Returns the join point type of this class
     * @return The join point type
     */
    @Override
    public final String get_class() {
        return "method";
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
        return this.aFunction.instanceOf(joinpointClass);
    }
    /**
     * 
     */
    protected enum MethodAttributes {
        RECORD("record"),
        HASDEFINITION("hasDefinition"),
        ISIMPLEMENTATION("isImplementation"),
        ISPROTOTYPE("isPrototype"),
        FUNCTIONTYPE("functionType"),
        DECLARATIONJP("declarationJp"),
        DECLARATIONJPS("declarationJps"),
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
        RETURNTYPE("returnType"),
        ISCUDAKERNEL("isCudaKernel"),
        NAME("name"),
        ISPUBLIC("isPublic"),
        QUALIFIEDPREFIX("qualifiedPrefix"),
        QUALIFIEDNAME("qualifiedName"),
        ATTRS("attrs"),
        PARENT("parent"),
        ASTANCESTOR("astAncestor"),
        AST("ast"),
        SIBLINGSLEFT("siblingsLeft"),
        DATA("data"),
        HASCHILDREN("hasChildren"),
        DESCENDANTSANDSELF("descendantsAndSelf"),
        TYPE("type"),
        SIBLINGSRIGHT("siblingsRight"),
        ISCILK("isCilk"),
        FILEPATH("filepath"),
        SCOPENODES("scopeNodes"),
        CHILDREN("children"),
        FIRSTCHILD("firstChild"),
        NUMCHILDREN("numChildren"),
        ANCESTOR("ancestor"),
        INLINECOMMENTS("inlineComments"),
        ASTCHILD("astChild"),
        ASTNAME("astName"),
        JPID("jpId"),
        ASTID("astId"),
        CONTAINS("contains"),
        ASTISINSTANCE("astIsInstance"),
        FILENAME("filename"),
        JAVAFIELDS("javaFields"),
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
        PRAGMAS("pragmas"),
        STMT("stmt"),
        HASPARENT("hasParent");
        private String name;

        /**
         * 
         */
        private MethodAttributes(String name){
            this.name = name;
        }
        /**
         * Return an attribute enumeration item from a given attribute name
         */
        public static Optional<MethodAttributes> fromString(String name) {
            return Arrays.asList(values()).stream().filter(attr -> attr.name.equals(name)).findAny();
        }

        /**
         * Return a list of attributes in String format
         */
        public static List<String> getNames() {
            return Arrays.asList(values()).stream().map(MethodAttributes::name).collect(Collectors.toList());
        }

        /**
         * True if the enum contains the given attribute name, false otherwise.
         */
        public static boolean contains(String name) {
            return fromString(name).isPresent();
        }
    }
}
