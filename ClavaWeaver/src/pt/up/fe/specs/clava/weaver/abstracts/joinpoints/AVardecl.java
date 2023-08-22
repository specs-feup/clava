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
 * Auto-Generated class for join point AVardecl
 * This class is overwritten by the Weaver Generator.
 * 
 * Represents a variable declaration or definition
 * @author Lara Weaver Generator
 */
public abstract class AVardecl extends ADeclarator {

    protected ADeclarator aDeclarator;

    /**
     * 
     */
    public AVardecl(ADeclarator aDeclarator){
        super(aDeclarator);
        this.aDeclarator = aDeclarator;
    }
    /**
     * true, if vardecl has an initialization value
     */
    public abstract Boolean getHasInitImpl();

    /**
     * true, if vardecl has an initialization value
     */
    public final Object getHasInit() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "hasInit", Optional.empty());
        	}
        	Boolean result = this.getHasInitImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "hasInit", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "hasInit", e);
        }
    }

    /**
     * If vardecl has an initialization value, returns an expression with that value
     */
    public abstract AExpression getInitImpl();

    /**
     * If vardecl has an initialization value, returns an expression with that value
     */
    public final Object getInit() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "init", Optional.empty());
        	}
        	AExpression result = this.getInitImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "init", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "init", e);
        }
    }

    /**
     * The initialization style of this vardecl, which can be no_init, cinit, callinit, listinit
     */
    public abstract String getInitStyleImpl();

    /**
     * The initialization style of this vardecl, which can be no_init, cinit, callinit, listinit
     */
    public final Object getInitStyle() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "initStyle", Optional.empty());
        	}
        	String result = this.getInitStyleImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "initStyle", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "initStyle", e);
        }
    }

    /**
     * true, if vardecl is a function parameter
     */
    public abstract Boolean getIsParamImpl();

    /**
     * true, if vardecl is a function parameter
     */
    public final Object getIsParam() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "isParam", Optional.empty());
        	}
        	Boolean result = this.getIsParamImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "isParam", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isParam", e);
        }
    }

    /**
     * Storage class specifier, which can be none, extern, static, __private_extern__, auto, register
     */
    public abstract String getStorageClassImpl();

    /**
     * Storage class specifier, which can be none, extern, static, __private_extern__, auto, register
     */
    public final Object getStorageClass() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "storageClass", Optional.empty());
        	}
        	String result = this.getStorageClassImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "storageClass", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "storageClass", e);
        }
    }

    /**
     * 
     */
    public void defStorageClassImpl(String value) {
        throw new UnsupportedOperationException("Join point "+get_class()+": Action def storageClass with type String not implemented ");
    }

    /**
     * true, if this variable does not have local storage. This includes all global variables as well as static variables declared within a function.
     */
    public abstract Boolean getIsGlobalImpl();

    /**
     * true, if this variable does not have local storage. This includes all global variables as well as static variables declared within a function.
     */
    public final Object getIsGlobal() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "isGlobal", Optional.empty());
        	}
        	Boolean result = this.getIsGlobalImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "isGlobal", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isGlobal", e);
        }
    }

    /**
     * The vardecl corresponding to the actual definition. For global variables, returns the vardecl of the file where it is actually defined (instead of the vardecl that defines an external link to the variable)
     */
    public abstract AVardecl getDefinitionImpl();

    /**
     * The vardecl corresponding to the actual definition. For global variables, returns the vardecl of the file where it is actually defined (instead of the vardecl that defines an external link to the variable)
     */
    public final Object getDefinition() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "definition", Optional.empty());
        	}
        	AVardecl result = this.getDefinitionImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "definition", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "definition", e);
        }
    }

    /**
     * Default implementation of the method used by the lara interpreter to select inits
     * @return 
     */
    public List<? extends AExpression> selectInit() {
        return select(pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AExpression.class, SelectOp.DESCENDANTS);
    }

    /**
     * Sets the given expression as the initialization of this vardecl. If undefined is passed and vardecl already has an initialization, removes that initialization
     * @param init 
     */
    public void setInitImpl(AExpression init) {
        throw new UnsupportedOperationException(get_class()+": Action setInit not implemented ");
    }

    /**
     * Sets the given expression as the initialization of this vardecl. If undefined is passed and vardecl already has an initialization, removes that initialization
     * @param init 
     */
    public final void setInit(AExpression init) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setInit", this, Optional.empty(), init);
        	}
        	this.setInitImpl(init);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setInit", this, Optional.empty(), init);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setInit", e);
        }
    }

    /**
     * Converts the given string to a literal expression and sets it as the initialization of this vardecl. If undefined is passed and vardecl already has an initialization, removes that initialization
     * @param init 
     */
    public void setInitImpl(String init) {
        throw new UnsupportedOperationException(get_class()+": Action setInit not implemented ");
    }

    /**
     * Converts the given string to a literal expression and sets it as the initialization of this vardecl. If undefined is passed and vardecl already has an initialization, removes that initialization
     * @param init 
     */
    public final void setInit(String init) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setInit", this, Optional.empty(), init);
        	}
        	this.setInitImpl(init);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setInit", this, Optional.empty(), init);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setInit", e);
        }
    }

    /**
     * If vardecl already has an initialization, removes it.
     * @param removeConst 
     */
    public void removeInitImpl(boolean removeConst) {
        throw new UnsupportedOperationException(get_class()+": Action removeInit not implemented ");
    }

    /**
     * If vardecl already has an initialization, removes it.
     * @param removeConst 
     */
    public final void removeInit(boolean removeConst) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "removeInit", this, Optional.empty(), removeConst);
        	}
        	this.removeInitImpl(removeConst);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "removeInit", this, Optional.empty(), removeConst);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "removeInit", e);
        }
    }

    /**
     * Creates a new varref based on this vardecl
     */
    public AVarref varrefImpl() {
        throw new UnsupportedOperationException(get_class()+": Action varref not implemented ");
    }

    /**
     * Creates a new varref based on this vardecl
     */
    public final AVarref varref() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "varref", this, Optional.empty());
        	}
        	AVarref result = this.varrefImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "varref", this, Optional.ofNullable(result));
        	}
        	return result;
        } catch(Exception e) {
        	throw new ActionException(get_class(), "varref", e);
        }
    }

    /**
     * Sets the storage class specifier, which can be none, extern, static, __private_extern__, autovardecl
     * @param storageClass 
     */
    public void setStorageClassImpl(String storageClass) {
        throw new UnsupportedOperationException(get_class()+": Action setStorageClass not implemented ");
    }

    /**
     * Sets the storage class specifier, which can be none, extern, static, __private_extern__, autovardecl
     * @param storageClass 
     */
    public final void setStorageClass(String storageClass) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setStorageClass", this, Optional.empty(), storageClass);
        	}
        	this.setStorageClassImpl(storageClass);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setStorageClass", this, Optional.empty(), storageClass);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setStorageClass", e);
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
     * Get value on attribute astAncestor
     * @return the attribute's value
     */
    @Override
    public AJoinPoint astAncestorImpl(String type) {
        return this.aDeclarator.astAncestorImpl(type);
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
     * Get value on attribute descendantsAndSelfArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] descendantsAndSelfArrayImpl(String type) {
        return this.aDeclarator.descendantsAndSelfArrayImpl(type);
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
     * Get value on attribute laraDescendantsArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] laraDescendantsArrayImpl(String type) {
        return this.aDeclarator.laraDescendantsArrayImpl(type);
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
     * Get value on attribute astChild
     * @return the attribute's value
     */
    @Override
    public AJoinPoint astChildImpl(Integer index) {
        return this.aDeclarator.astChildImpl(index);
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
     * Get value on attribute astIsInstance
     * @return the attribute's value
     */
    @Override
    public Boolean astIsInstanceImpl(String className) {
        return this.aDeclarator.astIsInstanceImpl(className);
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
     * Get value on attribute astParent
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getAstParentImpl() {
        return this.aDeclarator.getAstParentImpl();
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
     * Get value on attribute child
     * @return the attribute's value
     */
    @Override
    public AJoinPoint childImpl(Integer index) {
        return this.aDeclarator.childImpl(index);
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
    public AJoinPoint[] descendantsArrayImpl(String type) {
        return this.aDeclarator.descendantsArrayImpl(type);
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
     * Get value on attribute hasAstParent
     * @return the attribute's value
     */
    @Override
    public Boolean getHasAstParentImpl() {
        return this.aDeclarator.getHasAstParentImpl();
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
     * Get value on attribute firstJp
     * @return the attribute's value
     */
    @Override
    public AJoinPoint firstJpImpl(String type) {
        return this.aDeclarator.firstJpImpl(type);
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
     * Looks for an ancestor joinpoint name, walking back on the AST
     * @param type 
     */
    @Override
    public AJoinPoint getAncestorImpl(String type) {
        return this.aDeclarator.getAncestorImpl(type);
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
        	case "init": 
        		joinPointList = selectInit();
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
        case "storageClass": {
        	if(value instanceof String){
        		this.defStorageClassImpl((String)value);
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
        attributes.add("hasInit");
        attributes.add("init");
        attributes.add("initStyle");
        attributes.add("isParam");
        attributes.add("storageClass");
        attributes.add("isGlobal");
        attributes.add("definition");
    }

    /**
     * 
     */
    @Override
    protected void fillWithSelects(List<String> selects) {
        this.aDeclarator.fillWithSelects(selects);
        selects.add("init");
    }

    /**
     * 
     */
    @Override
    protected void fillWithActions(List<String> actions) {
        this.aDeclarator.fillWithActions(actions);
        actions.add("void setInit(expression)");
        actions.add("void setInit(String)");
        actions.add("void removeInit(boolean)");
        actions.add("varref varref()");
        actions.add("void setStorageClass(String)");
    }

    /**
     * Returns the join point type of this class
     * @return The join point type
     */
    @Override
    public String get_class() {
        return "vardecl";
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
    protected enum VardeclAttributes {
        HASINIT("hasInit"),
        INIT("init"),
        INITSTYLE("initStyle"),
        ISPARAM("isParam"),
        STORAGECLASS("storageClass"),
        ISGLOBAL("isGlobal"),
        DEFINITION("definition"),
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
        RIGHTJP("rightJp"),
        ISCILK("isCilk"),
        FILEPATH("filepath"),
        SCOPENODES("scopeNodes"),
        LARADESCENDANTS("laraDescendants"),
        CHILDREN("children"),
        FIRSTCHILD("firstChild"),
        NUMCHILDREN("numChildren"),
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
        private VardeclAttributes(String name){
            this.name = name;
        }
        /**
         * Return an attribute enumeration item from a given attribute name
         */
        public static Optional<VardeclAttributes> fromString(String name) {
            return Arrays.asList(values()).stream().filter(attr -> attr.name.equals(name)).findAny();
        }

        /**
         * Return a list of attributes in String format
         */
        public static List<String> getNames() {
            return Arrays.asList(values()).stream().map(VardeclAttributes::name).collect(Collectors.toList());
        }

        /**
         * True if the enum contains the given attribute name, false otherwise.
         */
        public static boolean contains(String name) {
            return fromString(name).isPresent();
        }
    }
}
