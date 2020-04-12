package pt.up.fe.specs.clava.weaver.abstracts.joinpoints;

import org.lara.interpreter.weaver.interf.JoinPoint;
import pt.up.fe.specs.clava.ClavaNode;
import java.util.List;
import org.lara.interpreter.weaver.interf.events.Stage;
import java.util.Optional;
import org.lara.interpreter.exception.ActionException;
import java.util.Map;
import org.lara.interpreter.exception.AttributeException;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import org.lara.interpreter.weaver.interf.SelectOp;

/**
 * Abstract class containing the global attributes and default action exception.
 * This class is overwritten when the weaver generator is executed.
 * @author Lara Weaver Generator
 */
public abstract class AJoinPoint extends JoinPoint {

    /**
     * 
     */
    @Override
    public boolean same(JoinPoint iJoinPoint) {
        if (this.get_class().equals(iJoinPoint.get_class())) {
        
                return this.compareNodes((AJoinPoint) iJoinPoint);
            }
            return false;
    }

    /**
     * Compares the two join points based on their node reference of the used compiler/parsing tool.<br>
     * This is the default implementation for comparing two join points. <br>
     * <b>Note for developers:</b> A weaver may override this implementation in the editable abstract join point, so
     * the changes are made for all join points, or override this method in specific join points.
     */
    public boolean compareNodes(AJoinPoint aJoinPoint) {
        return this.getNode().equals(aJoinPoint.getNode());
    }

    /**
     * Returns the tree node reference of this join point.<br><b>NOTE</b>This method is essentially used to compare two join points
     * @return Tree node reference
     */
    public abstract ClavaNode getNode();

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
        case "firstChild": {
        	if(value instanceof AJoinPoint){
        		this.defFirstChildImpl((AJoinPoint)value);
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
        default: throw new UnsupportedOperationException("Join point "+get_class()+": attribute '"+attribute+"' cannot be defined");
        }
    }

    /**
     * 
     */
    @Override
    protected void fillWithActions(List<String> actions) {
        actions.add("replaceWith(AJoinPoint node)");
        actions.add("replaceWith(String node)");
        actions.add("insertBefore(AJoinPoint node)");
        actions.add("insertBefore(String node)");
        actions.add("insertAfter(AJoinPoint node)");
        actions.add("insertAfter(String code)");
        actions.add("detach()");
        actions.add("setType(AJoinPoint type)");
        actions.add("copy()");
        actions.add("deepCopy()");
        actions.add("setUserField(String fieldName, Object value)");
        actions.add("setUserField(Map<?, ?> fieldNameAndValue)");
        actions.add("setValue(String key, Object value)");
        actions.add("messageToUser(String message)");
        actions.add("removeChildren()");
        actions.add("setFirstChild(AJoinPoint node)");
        actions.add("setLastChild(AJoinPoint node)");
    }

    /**
     * Replaces this join point with the given join
     * @param node 
     */
    public AJoinPoint replaceWithImpl(AJoinPoint node) {
        throw new UnsupportedOperationException(get_class()+": Action replaceWith not implemented ");
    }

    /**
     * Replaces this join point with the given join
     * @param node 
     */
    public final AJoinPoint replaceWith(AJoinPoint node) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "replaceWith", this, Optional.empty(), node);
        	}
        	AJoinPoint result = this.replaceWithImpl(node);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "replaceWith", this, Optional.ofNullable(result), node);
        	}
        	return result;
        } catch(Exception e) {
        	throw new ActionException(get_class(), "replaceWith", e);
        }
    }

    /**
     * Overload which accepts a string
     * @param node 
     */
    public AJoinPoint replaceWithImpl(String node) {
        throw new UnsupportedOperationException(get_class()+": Action replaceWith not implemented ");
    }

    /**
     * Overload which accepts a string
     * @param node 
     */
    public final AJoinPoint replaceWith(String node) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "replaceWith", this, Optional.empty(), node);
        	}
        	AJoinPoint result = this.replaceWithImpl(node);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "replaceWith", this, Optional.ofNullable(result), node);
        	}
        	return result;
        } catch(Exception e) {
        	throw new ActionException(get_class(), "replaceWith", e);
        }
    }

    /**
     * Inserts the given join point before this join point
     * @param node 
     */
    public AJoinPoint insertBeforeImpl(AJoinPoint node) {
        throw new UnsupportedOperationException(get_class()+": Action insertBefore not implemented ");
    }

    /**
     * Inserts the given join point before this join point
     * @param node 
     */
    public final AJoinPoint insertBefore(AJoinPoint node) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "insertBefore", this, Optional.empty(), node);
        	}
        	AJoinPoint result = this.insertBeforeImpl(node);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "insertBefore", this, Optional.ofNullable(result), node);
        	}
        	return result;
        } catch(Exception e) {
        	throw new ActionException(get_class(), "insertBefore", e);
        }
    }

    /**
     * Overload which accepts a string
     * @param node 
     */
    public AJoinPoint insertBeforeImpl(String node) {
        throw new UnsupportedOperationException(get_class()+": Action insertBefore not implemented ");
    }

    /**
     * Overload which accepts a string
     * @param node 
     */
    public final AJoinPoint insertBefore(String node) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "insertBefore", this, Optional.empty(), node);
        	}
        	AJoinPoint result = this.insertBeforeImpl(node);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "insertBefore", this, Optional.ofNullable(result), node);
        	}
        	return result;
        } catch(Exception e) {
        	throw new ActionException(get_class(), "insertBefore", e);
        }
    }

    /**
     * Inserts the given join point after this join point
     * @param node 
     */
    public AJoinPoint insertAfterImpl(AJoinPoint node) {
        throw new UnsupportedOperationException(get_class()+": Action insertAfter not implemented ");
    }

    /**
     * Inserts the given join point after this join point
     * @param node 
     */
    public final AJoinPoint insertAfter(AJoinPoint node) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "insertAfter", this, Optional.empty(), node);
        	}
        	AJoinPoint result = this.insertAfterImpl(node);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "insertAfter", this, Optional.ofNullable(result), node);
        	}
        	return result;
        } catch(Exception e) {
        	throw new ActionException(get_class(), "insertAfter", e);
        }
    }

    /**
     * Overload which accepts a string
     * @param code 
     */
    public AJoinPoint insertAfterImpl(String code) {
        throw new UnsupportedOperationException(get_class()+": Action insertAfter not implemented ");
    }

    /**
     * Overload which accepts a string
     * @param code 
     */
    public final AJoinPoint insertAfter(String code) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "insertAfter", this, Optional.empty(), code);
        	}
        	AJoinPoint result = this.insertAfterImpl(code);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "insertAfter", this, Optional.ofNullable(result), code);
        	}
        	return result;
        } catch(Exception e) {
        	throw new ActionException(get_class(), "insertAfter", e);
        }
    }

    /**
     * Removes the node associated to this joinpoint from the AST
     */
    public void detachImpl() {
        throw new UnsupportedOperationException(get_class()+": Action detach not implemented ");
    }

    /**
     * Removes the node associated to this joinpoint from the AST
     */
    public final void detach() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "detach", this, Optional.empty());
        	}
        	this.detachImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "detach", this, Optional.empty());
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "detach", e);
        }
    }

    /**
     * Sets the type of a node, if it has a type
     * @param type 
     */
    public void setTypeImpl(AJoinPoint type) {
        throw new UnsupportedOperationException(get_class()+": Action setType not implemented ");
    }

    /**
     * Sets the type of a node, if it has a type
     * @param type 
     */
    public final void setType(AJoinPoint type) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setType", this, Optional.empty(), type);
        	}
        	this.setTypeImpl(type);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setType", this, Optional.empty(), type);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setType", e);
        }
    }

    /**
     * Performs a copy of the node and its children, but not of the nodes in its fields
     */
    public AJoinPoint copyImpl() {
        throw new UnsupportedOperationException(get_class()+": Action copy not implemented ");
    }

    /**
     * Performs a copy of the node and its children, but not of the nodes in its fields
     */
    public final AJoinPoint copy() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "copy", this, Optional.empty());
        	}
        	AJoinPoint result = this.copyImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "copy", this, Optional.ofNullable(result));
        	}
        	return result;
        } catch(Exception e) {
        	throw new ActionException(get_class(), "copy", e);
        }
    }

    /**
     * Performs a copy of the node and its children, including the nodes in their fields (only the first level of field nodes, this function is not recursive)
     */
    public AJoinPoint deepCopyImpl() {
        throw new UnsupportedOperationException(get_class()+": Action deepCopy not implemented ");
    }

    /**
     * Performs a copy of the node and its children, including the nodes in their fields (only the first level of field nodes, this function is not recursive)
     */
    public final AJoinPoint deepCopy() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "deepCopy", this, Optional.empty());
        	}
        	AJoinPoint result = this.deepCopyImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "deepCopy", this, Optional.ofNullable(result));
        	}
        	return result;
        } catch(Exception e) {
        	throw new ActionException(get_class(), "deepCopy", e);
        }
    }

    /**
     * Associates arbitrary values to nodes of the AST
     * @param fieldName 
     * @param value 
     */
    public Object setUserFieldImpl(String fieldName, Object value) {
        throw new UnsupportedOperationException(get_class()+": Action setUserField not implemented ");
    }

    /**
     * Associates arbitrary values to nodes of the AST
     * @param fieldName 
     * @param value 
     */
    public final Object setUserField(String fieldName, Object value) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setUserField", this, Optional.empty(), fieldName, value);
        	}
        	Object result = this.setUserFieldImpl(fieldName, value);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setUserField", this, Optional.ofNullable(result), fieldName, value);
        	}
        	return result;
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setUserField", e);
        }
    }

    /**
     * Overload which accepts a map
     * @param fieldNameAndValue 
     */
    public Object setUserFieldImpl(Map<?, ?> fieldNameAndValue) {
        throw new UnsupportedOperationException(get_class()+": Action setUserField not implemented ");
    }

    /**
     * Overload which accepts a map
     * @param fieldNameAndValue 
     */
    public final Object setUserField(Map<?, ?> fieldNameAndValue) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setUserField", this, Optional.empty(), fieldNameAndValue);
        	}
        	Object result = this.setUserFieldImpl(fieldNameAndValue);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setUserField", this, Optional.ofNullable(result), fieldNameAndValue);
        	}
        	return result;
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setUserField", e);
        }
    }

    /**
     * Sets the value associated with the given property key
     * @param key 
     * @param value 
     */
    public AJoinPoint setValueImpl(String key, Object value) {
        throw new UnsupportedOperationException(get_class()+": Action setValue not implemented ");
    }

    /**
     * Sets the value associated with the given property key
     * @param key 
     * @param value 
     */
    public final AJoinPoint setValue(String key, Object value) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setValue", this, Optional.empty(), key, value);
        	}
        	AJoinPoint result = this.setValueImpl(key, value);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setValue", this, Optional.ofNullable(result), key, value);
        	}
        	return result;
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setValue", e);
        }
    }

    /**
     * Adds a message that will be printed to the user after weaving finishes. Identical messages are removed
     * @param message 
     */
    public void messageToUserImpl(String message) {
        throw new UnsupportedOperationException(get_class()+": Action messageToUser not implemented ");
    }

    /**
     * Adds a message that will be printed to the user after weaving finishes. Identical messages are removed
     * @param message 
     */
    public final void messageToUser(String message) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "messageToUser", this, Optional.empty(), message);
        	}
        	this.messageToUserImpl(message);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "messageToUser", this, Optional.empty(), message);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "messageToUser", e);
        }
    }

    /**
     * Removes the children of this node
     */
    public void removeChildrenImpl() {
        throw new UnsupportedOperationException(get_class()+": Action removeChildren not implemented ");
    }

    /**
     * Removes the children of this node
     */
    public final void removeChildren() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "removeChildren", this, Optional.empty());
        	}
        	this.removeChildrenImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "removeChildren", this, Optional.empty());
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "removeChildren", e);
        }
    }

    /**
     * Replaces the first child, or inserts the join point if no child is present
     * @param node 
     */
    public void setFirstChildImpl(AJoinPoint node) {
        throw new UnsupportedOperationException(get_class()+": Action setFirstChild not implemented ");
    }

    /**
     * Replaces the first child, or inserts the join point if no child is present
     * @param node 
     */
    public final void setFirstChild(AJoinPoint node) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setFirstChild", this, Optional.empty(), node);
        	}
        	this.setFirstChildImpl(node);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setFirstChild", this, Optional.empty(), node);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setFirstChild", e);
        }
    }

    /**
     * Replaces the last child, or inserts the join point if no child is present
     * @param node 
     */
    public void setLastChildImpl(AJoinPoint node) {
        throw new UnsupportedOperationException(get_class()+": Action setLastChild not implemented ");
    }

    /**
     * Replaces the last child, or inserts the join point if no child is present
     * @param node 
     */
    public final void setLastChild(AJoinPoint node) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setLastChild", this, Optional.empty(), node);
        	}
        	this.setLastChildImpl(node);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setLastChild", this, Optional.empty(), node);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setLastChild", e);
        }
    }

    /**
     * 
     */
    @Override
    protected void fillWithAttributes(List<String> attributes) {
        //Attributes available for all join points
        attributes.add("root");
        attributes.add("parent");
        attributes.add("ancestor(String type)");
        attributes.add("descendants");
        attributes.add("descendants(String type)");
        attributes.add("descendantsAndSelf(String type)");
        attributes.add("chainAncestor(String type)");
        attributes.add("astParent");
        attributes.add("astAncestor(String type)");
        attributes.add("contains(AJoinPoint jp)");
        attributes.add("hasParent");
        attributes.add("hasAstParent");
        attributes.add("firstJp(String type)");
        attributes.add("line");
        attributes.add("column");
        attributes.add("endLine");
        attributes.add("endColumn");
        attributes.add("location");
        attributes.add("filename");
        attributes.add("filepath");
        attributes.add("astId");
        attributes.add("ast");
        attributes.add("code");
        attributes.add("joinpointType");
        attributes.add("type");
        attributes.add("astName");
        attributes.add("astNumChildren");
        attributes.add("astChildren");
        attributes.add("astChild(Integer index)");
        attributes.add("numChildren");
        attributes.add("children");
        attributes.add("child(Integer index)");
        attributes.add("astIsInstance(String className)");
        attributes.add("hasNode(Object nodeOrJp)");
        attributes.add("chain");
        attributes.add("javaFields");
        attributes.add("javaValue(String fieldName)");
        attributes.add("javaFieldType(String fieldName)");
        attributes.add("isInsideLoopHeader");
        attributes.add("isInsideHeader");
        attributes.add("getUserField(String fieldName)");
        attributes.add("userField(String fieldName)");
        attributes.add("parentRegion");
        attributes.add("currentRegion");
        attributes.add("pragmas");
        attributes.add("data");
        attributes.add("keys");
        attributes.add("getValue(String key)");
        attributes.add("keyType(String key)");
        attributes.add("isMacro");
        attributes.add("firstChild");
        attributes.add("lastChild");
        attributes.add("hasChildren");
        attributes.add("isCilk");
        attributes.add("depth");
        attributes.add("jpId");
    }

    /**
     * Returns the 'program' joinpoint
     */
    public abstract AJoinPoint getRootImpl();

    /**
     * Returns the 'program' joinpoint
     */
    public final Object getRoot() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "root", Optional.empty());
        	}
        	AJoinPoint result = this.getRootImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "root", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "root", e);
        }
    }

    /**
     * Returns the parent node in the AST, or undefined if it is the root node
     */
    public abstract AJoinPoint getParentImpl();

    /**
     * Returns the parent node in the AST, or undefined if it is the root node
     */
    public final Object getParent() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "parent", Optional.empty());
        	}
        	AJoinPoint result = this.getParentImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "parent", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "parent", e);
        }
    }

    /**
     * 
     * @param type
     * @return 
     */
    public abstract AJoinPoint ancestorImpl(String type);

    /**
     * 
     * @param type
     * @return 
     */
    public final Object ancestor(String type) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "ancestor", Optional.empty(), type);
        	}
        	AJoinPoint result = this.ancestorImpl(type);
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "ancestor", Optional.ofNullable(result), type);
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "ancestor", e);
        }
    }

    /**
     * Get value on attribute descendants
     * @return the attribute's value
     */
    public abstract AJoinPoint[] getDescendantsArrayImpl();

    /**
     * Retrieves all descendants of the join point
     */
    public Object getDescendantsImpl() {
        AJoinPoint[] aJoinPointArrayImpl0 = getDescendantsArrayImpl();
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(aJoinPointArrayImpl0);
        return nativeArray0;
    }

    /**
     * Retrieves all descendants of the join point
     */
    public final Object getDescendants() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "descendants", Optional.empty());
        	}
        	Object result = this.getDescendantsImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "descendants", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "descendants", e);
        }
    }

    /**
     * 
     * @param type
     * @return 
     */
    public abstract AJoinPoint[] descendantsArrayImpl(String type);

    /**
     * 
     * @param type
     * @return 
     */
    public Object descendantsImpl(String type) {
        AJoinPoint[] aJoinPointArrayImpl0 = descendantsArrayImpl(type);
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(aJoinPointArrayImpl0);
        return nativeArray0;
    }

    /**
     * 
     * @param type
     * @return 
     */
    public final Object descendants(String type) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "descendants", Optional.empty(), type);
        	}
        	Object result = this.descendantsImpl(type);
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "descendants", Optional.ofNullable(result), type);
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "descendants", e);
        }
    }

    /**
     * 
     * @param type
     * @return 
     */
    public abstract AJoinPoint[] descendantsAndSelfArrayImpl(String type);

    /**
     * 
     * @param type
     * @return 
     */
    public Object descendantsAndSelfImpl(String type) {
        AJoinPoint[] aJoinPointArrayImpl0 = descendantsAndSelfArrayImpl(type);
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(aJoinPointArrayImpl0);
        return nativeArray0;
    }

    /**
     * 
     * @param type
     * @return 
     */
    public final Object descendantsAndSelf(String type) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "descendantsAndSelf", Optional.empty(), type);
        	}
        	Object result = this.descendantsAndSelfImpl(type);
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "descendantsAndSelf", Optional.ofNullable(result), type);
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "descendantsAndSelf", e);
        }
    }

    /**
     * 
     * @param type
     * @return 
     */
    public abstract AJoinPoint chainAncestorImpl(String type);

    /**
     * 
     * @param type
     * @return 
     */
    public final Object chainAncestor(String type) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "chainAncestor", Optional.empty(), type);
        	}
        	AJoinPoint result = this.chainAncestorImpl(type);
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "chainAncestor", Optional.ofNullable(result), type);
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "chainAncestor", e);
        }
    }

    /**
     * Returns the parent node in the AST, or undefined if it is the root node
     */
    public abstract AJoinPoint getAstParentImpl();

    /**
     * Returns the parent node in the AST, or undefined if it is the root node
     */
    public final Object getAstParent() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "astParent", Optional.empty());
        	}
        	AJoinPoint result = this.getAstParentImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "astParent", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "astParent", e);
        }
    }

    /**
     * 
     * @param type
     * @return 
     */
    public abstract AJoinPoint astAncestorImpl(String type);

    /**
     * 
     * @param type
     * @return 
     */
    public final Object astAncestor(String type) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "astAncestor", Optional.empty(), type);
        	}
        	AJoinPoint result = this.astAncestorImpl(type);
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "astAncestor", Optional.ofNullable(result), type);
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "astAncestor", e);
        }
    }

    /**
     * 
     * @param jp
     * @return 
     */
    public abstract Boolean containsImpl(AJoinPoint jp);

    /**
     * 
     * @param jp
     * @return 
     */
    public final Object contains(AJoinPoint jp) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "contains", Optional.empty(), jp);
        	}
        	Boolean result = this.containsImpl(jp);
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "contains", Optional.ofNullable(result), jp);
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "contains", e);
        }
    }

    /**
     * Get value on attribute hasParent
     * @return the attribute's value
     */
    public abstract Boolean getHasParentImpl();

    /**
     * Get value on attribute hasParent
     * @return the attribute's value
     */
    public final Object getHasParent() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "hasParent", Optional.empty());
        	}
        	Boolean result = this.getHasParentImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "hasParent", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "hasParent", e);
        }
    }

    /**
     * Get value on attribute hasAstParent
     * @return the attribute's value
     */
    public abstract Boolean getHasAstParentImpl();

    /**
     * Get value on attribute hasAstParent
     * @return the attribute's value
     */
    public final Object getHasAstParent() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "hasAstParent", Optional.empty());
        	}
        	Boolean result = this.getHasAstParentImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "hasAstParent", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "hasAstParent", e);
        }
    }

    /**
     * 
     * @param type
     * @return 
     */
    public abstract AJoinPoint firstJpImpl(String type);

    /**
     * 
     * @param type
     * @return 
     */
    public final Object firstJp(String type) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "firstJp", Optional.empty(), type);
        	}
        	AJoinPoint result = this.firstJpImpl(type);
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "firstJp", Optional.ofNullable(result), type);
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "firstJp", e);
        }
    }

    /**
     * The starting line of the current node in the original code
     */
    public abstract Integer getLineImpl();

    /**
     * The starting line of the current node in the original code
     */
    public final Object getLine() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "line", Optional.empty());
        	}
        	Integer result = this.getLineImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "line", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "line", e);
        }
    }

    /**
     * The starting column of the current node in the original code
     */
    public abstract Integer getColumnImpl();

    /**
     * The starting column of the current node in the original code
     */
    public final Object getColumn() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "column", Optional.empty());
        	}
        	Integer result = this.getColumnImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "column", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "column", e);
        }
    }

    /**
     * The ending line of the current node in the original code
     */
    public abstract Integer getEndLineImpl();

    /**
     * The ending line of the current node in the original code
     */
    public final Object getEndLine() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "endLine", Optional.empty());
        	}
        	Integer result = this.getEndLineImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "endLine", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "endLine", e);
        }
    }

    /**
     * The ending column of the current node in the original code
     */
    public abstract Integer getEndColumnImpl();

    /**
     * The ending column of the current node in the original code
     */
    public final Object getEndColumn() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "endColumn", Optional.empty());
        	}
        	Integer result = this.getEndColumnImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "endColumn", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "endColumn", e);
        }
    }

    /**
     * Get value on attribute location
     * @return the attribute's value
     */
    public abstract String getLocationImpl();

    /**
     * Get value on attribute location
     * @return the attribute's value
     */
    public final Object getLocation() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "location", Optional.empty());
        	}
        	String result = this.getLocationImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "location", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "location", e);
        }
    }

    /**
     * Get value on attribute filename
     * @return the attribute's value
     */
    public abstract String getFilenameImpl();

    /**
     * Get value on attribute filename
     * @return the attribute's value
     */
    public final Object getFilename() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "filename", Optional.empty());
        	}
        	String result = this.getFilenameImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "filename", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "filename", e);
        }
    }

    /**
     * the complete path to the file where the code of this node comes from
     */
    public abstract String getFilepathImpl();

    /**
     * the complete path to the file where the code of this node comes from
     */
    public final Object getFilepath() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "filepath", Optional.empty());
        	}
        	String result = this.getFilepathImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "filepath", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "filepath", e);
        }
    }

    /**
     * Get value on attribute astId
     * @return the attribute's value
     */
    public abstract String getAstIdImpl();

    /**
     * Get value on attribute astId
     * @return the attribute's value
     */
    public final Object getAstId() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "astId", Optional.empty());
        	}
        	String result = this.getAstIdImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "astId", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "astId", e);
        }
    }

    /**
     * Get value on attribute ast
     * @return the attribute's value
     */
    public abstract String getAstImpl();

    /**
     * Get value on attribute ast
     * @return the attribute's value
     */
    public final Object getAst() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "ast", Optional.empty());
        	}
        	String result = this.getAstImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "ast", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "ast", e);
        }
    }

    /**
     * Get value on attribute code
     * @return the attribute's value
     */
    public abstract String getCodeImpl();

    /**
     * Get value on attribute code
     * @return the attribute's value
     */
    public final Object getCode() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "code", Optional.empty());
        	}
        	String result = this.getCodeImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "code", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "code", e);
        }
    }

    /**
     * [DEPRECATED: please use joinPointType instead (uppercase P)]
     */
    public abstract String getJoinpointTypeImpl();

    /**
     * [DEPRECATED: please use joinPointType instead (uppercase P)]
     */
    public final Object getJoinpointType() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "joinpointType", Optional.empty());
        	}
        	String result = this.getJoinpointTypeImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "joinpointType", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "joinpointType", e);
        }
    }

    /**
     * Get value on attribute type
     * @return the attribute's value
     */
    public abstract AJoinPoint getTypeImpl();

    /**
     * 
     */
    public void defTypeImpl(AJoinPoint value) {
        throw new UnsupportedOperationException("Join point "+get_class()+": Action def type with type AJoinPoint not implemented ");
    }

    /**
     * Get value on attribute type
     * @return the attribute's value
     */
    public final Object getType() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "type", Optional.empty());
        	}
        	AJoinPoint result = this.getTypeImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "type", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "type", e);
        }
    }

    /**
     * Get value on attribute astName
     * @return the attribute's value
     */
    public abstract String getAstNameImpl();

    /**
     * Get value on attribute astName
     * @return the attribute's value
     */
    public final Object getAstName() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "astName", Optional.empty());
        	}
        	String result = this.getAstNameImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "astName", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "astName", e);
        }
    }

    /**
     * Returns the number of children of the node, considering null nodes
     */
    public abstract Integer getAstNumChildrenImpl();

    /**
     * Returns the number of children of the node, considering null nodes
     */
    public final Object getAstNumChildren() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "astNumChildren", Optional.empty());
        	}
        	Integer result = this.getAstNumChildrenImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "astNumChildren", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "astNumChildren", e);
        }
    }

    /**
     * Get value on attribute astChildren
     * @return the attribute's value
     */
    public abstract AJoinPoint[] getAstChildrenArrayImpl();

    /**
     * Returns an array with the children of the node, considering null nodes
     */
    public Object getAstChildrenImpl() {
        AJoinPoint[] aJoinPointArrayImpl0 = getAstChildrenArrayImpl();
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(aJoinPointArrayImpl0);
        return nativeArray0;
    }

    /**
     * Returns an array with the children of the node, considering null nodes
     */
    public final Object getAstChildren() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "astChildren", Optional.empty());
        	}
        	Object result = this.getAstChildrenImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "astChildren", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "astChildren", e);
        }
    }

    /**
     * 
     * @param index
     * @return 
     */
    public abstract AJoinPoint astChildImpl(Integer index);

    /**
     * 
     * @param index
     * @return 
     */
    public final Object astChild(Integer index) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "astChild", Optional.empty(), index);
        	}
        	AJoinPoint result = this.astChildImpl(index);
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "astChild", Optional.ofNullable(result), index);
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "astChild", e);
        }
    }

    /**
     * Returns the number of children of the node, ignoring null nodes
     */
    public abstract Integer getNumChildrenImpl();

    /**
     * Returns the number of children of the node, ignoring null nodes
     */
    public final Object getNumChildren() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "numChildren", Optional.empty());
        	}
        	Integer result = this.getNumChildrenImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "numChildren", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "numChildren", e);
        }
    }

    /**
     * Get value on attribute children
     * @return the attribute's value
     */
    public abstract AJoinPoint[] getChildrenArrayImpl();

    /**
     * Returns an array with the children of the node, ignoring null nodes
     */
    public Object getChildrenImpl() {
        AJoinPoint[] aJoinPointArrayImpl0 = getChildrenArrayImpl();
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(aJoinPointArrayImpl0);
        return nativeArray0;
    }

    /**
     * Returns an array with the children of the node, ignoring null nodes
     */
    public final Object getChildren() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "children", Optional.empty());
        	}
        	Object result = this.getChildrenImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "children", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "children", e);
        }
    }

    /**
     * 
     * @param index
     * @return 
     */
    public abstract AJoinPoint childImpl(Integer index);

    /**
     * 
     * @param index
     * @return 
     */
    public final Object child(Integer index) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "child", Optional.empty(), index);
        	}
        	AJoinPoint result = this.childImpl(index);
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "child", Optional.ofNullable(result), index);
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "child", e);
        }
    }

    /**
     * 
     * @param className
     * @return 
     */
    public abstract Boolean astIsInstanceImpl(String className);

    /**
     * 
     * @param className
     * @return 
     */
    public final Object astIsInstance(String className) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "astIsInstance", Optional.empty(), className);
        	}
        	Boolean result = this.astIsInstanceImpl(className);
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "astIsInstance", Optional.ofNullable(result), className);
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "astIsInstance", e);
        }
    }

    /**
     * 
     * @param nodeOrJp
     * @return 
     */
    public abstract Boolean hasNodeImpl(Object nodeOrJp);

    /**
     * 
     * @param nodeOrJp
     * @return 
     */
    public final Object hasNode(Object nodeOrJp) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "hasNode", Optional.empty(), nodeOrJp);
        	}
        	Boolean result = this.hasNodeImpl(nodeOrJp);
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "hasNode", Optional.ofNullable(result), nodeOrJp);
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "hasNode", e);
        }
    }

    /**
     * Get value on attribute chain
     * @return the attribute's value
     */
    public abstract String[] getChainArrayImpl();

    /**
     * Get value on attribute chain
     * @return the attribute's value
     */
    public Object getChainImpl() {
        String[] stringArrayImpl0 = getChainArrayImpl();
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(stringArrayImpl0);
        return nativeArray0;
    }

    /**
     * Get value on attribute chain
     * @return the attribute's value
     */
    public final Object getChain() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "chain", Optional.empty());
        	}
        	Object result = this.getChainImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "chain", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "chain", e);
        }
    }

    /**
     * Get value on attribute javaFields
     * @return the attribute's value
     */
    public abstract String[] getJavaFieldsArrayImpl();

    /**
     * Get value on attribute javaFields
     * @return the attribute's value
     */
    public Object getJavaFieldsImpl() {
        String[] stringArrayImpl0 = getJavaFieldsArrayImpl();
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(stringArrayImpl0);
        return nativeArray0;
    }

    /**
     * Get value on attribute javaFields
     * @return the attribute's value
     */
    public final Object getJavaFields() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "javaFields", Optional.empty());
        	}
        	Object result = this.getJavaFieldsImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "javaFields", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "javaFields", e);
        }
    }

    /**
     * 
     * @param fieldName
     * @return 
     */
    public abstract Object javaValueImpl(String fieldName);

    /**
     * 
     * @param fieldName
     * @return 
     */
    public final Object javaValue(String fieldName) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "javaValue", Optional.empty(), fieldName);
        	}
        	Object result = this.javaValueImpl(fieldName);
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "javaValue", Optional.ofNullable(result), fieldName);
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "javaValue", e);
        }
    }

    /**
     * 
     * @param fieldName
     * @return 
     */
    public abstract String javaFieldTypeImpl(String fieldName);

    /**
     * 
     * @param fieldName
     * @return 
     */
    public final Object javaFieldType(String fieldName) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "javaFieldType", Optional.empty(), fieldName);
        	}
        	String result = this.javaFieldTypeImpl(fieldName);
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "javaFieldType", Optional.ofNullable(result), fieldName);
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "javaFieldType", e);
        }
    }

    /**
     * true, if the join point is inside a loop header (e.g., for, while)
     */
    public abstract Boolean getIsInsideLoopHeaderImpl();

    /**
     * true, if the join point is inside a loop header (e.g., for, while)
     */
    public final Object getIsInsideLoopHeader() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "isInsideLoopHeader", Optional.empty());
        	}
        	Boolean result = this.getIsInsideLoopHeaderImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "isInsideLoopHeader", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isInsideLoopHeader", e);
        }
    }

    /**
     * true, if the join point is inside a header (e.g., if condition, for, while)
     */
    public abstract Boolean getIsInsideHeaderImpl();

    /**
     * true, if the join point is inside a header (e.g., if condition, for, while)
     */
    public final Object getIsInsideHeader() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "isInsideHeader", Optional.empty());
        	}
        	Boolean result = this.getIsInsideHeaderImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "isInsideHeader", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isInsideHeader", e);
        }
    }

    /**
     * 
     * @param fieldName
     * @return 
     */
    public abstract Object getUserFieldImpl(String fieldName);

    /**
     * 
     * @param fieldName
     * @return 
     */
    public final Object getUserField(String fieldName) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "getUserField", Optional.empty(), fieldName);
        	}
        	Object result = this.getUserFieldImpl(fieldName);
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "getUserField", Optional.ofNullable(result), fieldName);
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "getUserField", e);
        }
    }

    /**
     * 
     * @param fieldName
     * @return 
     */
    public abstract Object userFieldImpl(String fieldName);

    /**
     * 
     * @param fieldName
     * @return 
     */
    public final Object userField(String fieldName) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "userField", Optional.empty(), fieldName);
        	}
        	Object result = this.userFieldImpl(fieldName);
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "userField", Optional.ofNullable(result), fieldName);
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "userField", e);
        }
    }

    /**
     * Get value on attribute parentRegion
     * @return the attribute's value
     */
    public abstract AJoinPoint getParentRegionImpl();

    /**
     * Get value on attribute parentRegion
     * @return the attribute's value
     */
    public final Object getParentRegion() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "parentRegion", Optional.empty());
        	}
        	AJoinPoint result = this.getParentRegionImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "parentRegion", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "parentRegion", e);
        }
    }

    /**
     * Get value on attribute currentRegion
     * @return the attribute's value
     */
    public abstract AJoinPoint getCurrentRegionImpl();

    /**
     * Get value on attribute currentRegion
     * @return the attribute's value
     */
    public final Object getCurrentRegion() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "currentRegion", Optional.empty());
        	}
        	AJoinPoint result = this.getCurrentRegionImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "currentRegion", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "currentRegion", e);
        }
    }

    /**
     * Get value on attribute pragmas
     * @return the attribute's value
     */
    public abstract APragma[] getPragmasArrayImpl();

    /**
     * The pragmas associated with this node
     */
    public Object getPragmasImpl() {
        APragma[] aPragmaArrayImpl0 = getPragmasArrayImpl();
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(aPragmaArrayImpl0);
        return nativeArray0;
    }

    /**
     * The pragmas associated with this node
     */
    public final Object getPragmas() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "pragmas", Optional.empty());
        	}
        	Object result = this.getPragmasImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "pragmas", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "pragmas", e);
        }
    }

    /**
     * JS object with the parsed data of #pragma clava data, associated with this node
     */
    public abstract Object getDataImpl();

    /**
     * JS object with the parsed data of #pragma clava data, associated with this node
     */
    public final Object getData() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "data", Optional.empty());
        	}
        	Object result = this.getDataImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "data", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "data", e);
        }
    }

    /**
     * Get value on attribute keys
     * @return the attribute's value
     */
    public abstract String[] getKeysArrayImpl();

    /**
     * A list of the properties currently supported by this node
     */
    public Object getKeysImpl() {
        String[] stringArrayImpl0 = getKeysArrayImpl();
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(stringArrayImpl0);
        return nativeArray0;
    }

    /**
     * A list of the properties currently supported by this node
     */
    public final Object getKeys() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "keys", Optional.empty());
        	}
        	Object result = this.getKeysImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "keys", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "keys", e);
        }
    }

    /**
     * 
     * @param key
     * @return 
     */
    public abstract Object getValueImpl(String key);

    /**
     * 
     * @param key
     * @return 
     */
    public final Object getValue(String key) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "getValue", Optional.empty(), key);
        	}
        	Object result = this.getValueImpl(key);
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "getValue", Optional.ofNullable(result), key);
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "getValue", e);
        }
    }

    /**
     * 
     * @param key
     * @return 
     */
    public abstract Object keyTypeImpl(String key);

    /**
     * 
     * @param key
     * @return 
     */
    public final Object keyType(String key) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "keyType", Optional.empty(), key);
        	}
        	Object result = this.keyTypeImpl(key);
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "keyType", Optional.ofNullable(result), key);
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "keyType", e);
        }
    }

    /**
     * true if any descendant or the node itself was defined as a macro
     */
    public abstract Boolean getIsMacroImpl();

    /**
     * true if any descendant or the node itself was defined as a macro
     */
    public final Object getIsMacro() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "isMacro", Optional.empty());
        	}
        	Boolean result = this.getIsMacroImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "isMacro", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isMacro", e);
        }
    }

    /**
     * Returns the first child of this node, or undefined if it has no child
     */
    public abstract AJoinPoint getFirstChildImpl();

    /**
     * 
     */
    public void defFirstChildImpl(AJoinPoint value) {
        throw new UnsupportedOperationException("Join point "+get_class()+": Action def firstChild with type AJoinPoint not implemented ");
    }

    /**
     * Returns the first child of this node, or undefined if it has no child
     */
    public final Object getFirstChild() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "firstChild", Optional.empty());
        	}
        	AJoinPoint result = this.getFirstChildImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "firstChild", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "firstChild", e);
        }
    }

    /**
     * Returns the last child of this node, or undefined if it has no child
     */
    public abstract AJoinPoint getLastChildImpl();

    /**
     * 
     */
    public void defLastChildImpl(AJoinPoint value) {
        throw new UnsupportedOperationException("Join point "+get_class()+": Action def lastChild with type AJoinPoint not implemented ");
    }

    /**
     * Returns the last child of this node, or undefined if it has no child
     */
    public final Object getLastChild() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "lastChild", Optional.empty());
        	}
        	AJoinPoint result = this.getLastChildImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "lastChild", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "lastChild", e);
        }
    }

    /**
     * true if the node has children, false otherwise
     */
    public abstract Boolean getHasChildrenImpl();

    /**
     * true if the node has children, false otherwise
     */
    public final Object getHasChildren() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "hasChildren", Optional.empty());
        	}
        	Boolean result = this.getHasChildrenImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "hasChildren", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "hasChildren", e);
        }
    }

    /**
     * true if this is a Cilk node (i.e., cilk_spawn, cilk_sync or cilk_for)
     */
    public abstract Boolean getIsCilkImpl();

    /**
     * true if this is a Cilk node (i.e., cilk_spawn, cilk_sync or cilk_for)
     */
    public final Object getIsCilk() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "isCilk", Optional.empty());
        	}
        	Boolean result = this.getIsCilkImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "isCilk", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isCilk", e);
        }
    }

    /**
     * the depth of this join point in the AST. If it is the root join point returns 0, if it is a child of the root node returns 1, etc.
     */
    public abstract Integer getDepthImpl();

    /**
     * the depth of this join point in the AST. If it is the root join point returns 0, if it is a child of the root node returns 1, etc.
     */
    public final Object getDepth() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "depth", Optional.empty());
        	}
        	Integer result = this.getDepthImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "depth", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "depth", e);
        }
    }

    /**
     * Id that is based on the position of the node in the code, and should remain stable between compilations (warning: only a few nodes - file, function, loop - currently support it)
     */
    public abstract String getJpIdImpl();

    /**
     * Id that is based on the position of the node in the code, and should remain stable between compilations (warning: only a few nodes - file, function, loop - currently support it)
     */
    public final Object getJpId() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "jpId", Optional.empty());
        	}
        	String result = this.getJpIdImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "jpId", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "jpId", e);
        }
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
        return super.instanceOf(joinpointClass);
    }

    /**
     * Returns the Weaving Engine this join point pertains to.
     */
    @Override
    public CxxWeaver getWeaverEngine() {
        return CxxWeaver.getCxxWeaver();
    }

    /**
     * Generic select function, used by the default select implementations.
     */
    public abstract <T extends AJoinPoint> List<? extends T> select(Class<T> joinPointClass, SelectOp op);
}
