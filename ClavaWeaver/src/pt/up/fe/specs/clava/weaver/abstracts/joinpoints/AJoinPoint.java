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
        case "data": {
        	if(value instanceof Object){
        		this.defDataImpl((Object)value);
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
        case "type": {
        	if(value instanceof AType){
        		this.defTypeImpl((AType)value);
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
        actions.add("copy()");
        actions.add("dataClear()");
        actions.add("deepCopy()");
        actions.add("detach()");
        actions.add("insertAfter(AJoinPoint node)");
        actions.add("insertAfter(String code)");
        actions.add("insertBefore(AJoinPoint node)");
        actions.add("insertBefore(String node)");
        actions.add("messageToUser(String message)");
        actions.add("removeChildren()");
        actions.add("replaceWith(AJoinPoint node)");
        actions.add("replaceWith(String node)");
        actions.add("replaceWith(AJoinPoint[] node)");
        actions.add("replaceWithStrings(String[] node)");
        actions.add("setData(Object source)");
        actions.add("setFirstChild(AJoinPoint node)");
        actions.add("setInlineComments(String[] comments)");
        actions.add("setInlineComments(String comments)");
        actions.add("setLastChild(AJoinPoint node)");
        actions.add("setType(AType type)");
        actions.add("setUserField(String fieldName, Object value)");
        actions.add("setUserField(Map<?, ?> fieldNameAndValue)");
        actions.add("setValue(String key, Object value)");
        actions.add("toComment(String prefix, String suffix)");
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
    public final Object copy() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "copy", this, Optional.empty());
        	}
        	AJoinPoint result = this.copyImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "copy", this, Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new ActionException(get_class(), "copy", e);
        }
    }

    /**
     * Clears all properties from the .data object
     */
    public void dataClearImpl() {
        throw new UnsupportedOperationException(get_class()+": Action dataClear not implemented ");
    }

    /**
     * Clears all properties from the .data object
     */
    public final void dataClear() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "dataClear", this, Optional.empty());
        	}
        	this.dataClearImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "dataClear", this, Optional.empty());
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "dataClear", e);
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
    public final Object deepCopy() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "deepCopy", this, Optional.empty());
        	}
        	AJoinPoint result = this.deepCopyImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "deepCopy", this, Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new ActionException(get_class(), "deepCopy", e);
        }
    }

    /**
     * Removes the node associated to this joinpoint from the AST
     */
    public AJoinPoint detachImpl() {
        throw new UnsupportedOperationException(get_class()+": Action detach not implemented ");
    }

    /**
     * Removes the node associated to this joinpoint from the AST
     */
    public final Object detach() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "detach", this, Optional.empty());
        	}
        	AJoinPoint result = this.detachImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "detach", this, Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new ActionException(get_class(), "detach", e);
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
    public final Object insertAfter(AJoinPoint node) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "insertAfter", this, Optional.empty(), node);
        	}
        	AJoinPoint result = this.insertAfterImpl(node);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "insertAfter", this, Optional.ofNullable(result), node);
        	}
        	return result!=null?result:getUndefinedValue();
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
    public final Object insertAfter(String code) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "insertAfter", this, Optional.empty(), code);
        	}
        	AJoinPoint result = this.insertAfterImpl(code);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "insertAfter", this, Optional.ofNullable(result), code);
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new ActionException(get_class(), "insertAfter", e);
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
    public final Object insertBefore(AJoinPoint node) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "insertBefore", this, Optional.empty(), node);
        	}
        	AJoinPoint result = this.insertBeforeImpl(node);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "insertBefore", this, Optional.ofNullable(result), node);
        	}
        	return result!=null?result:getUndefinedValue();
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
    public final Object insertBefore(String node) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "insertBefore", this, Optional.empty(), node);
        	}
        	AJoinPoint result = this.insertBeforeImpl(node);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "insertBefore", this, Optional.ofNullable(result), node);
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new ActionException(get_class(), "insertBefore", e);
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
     * Replaces this node with the given node
     * @param node 
     */
    public AJoinPoint replaceWithImpl(AJoinPoint node) {
        throw new UnsupportedOperationException(get_class()+": Action replaceWith not implemented ");
    }

    /**
     * Replaces this node with the given node
     * @param node 
     */
    public final Object replaceWith(AJoinPoint node) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "replaceWith", this, Optional.empty(), node);
        	}
        	AJoinPoint result = this.replaceWithImpl(node);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "replaceWith", this, Optional.ofNullable(result), node);
        	}
        	return result!=null?result:getUndefinedValue();
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
    public final Object replaceWith(String node) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "replaceWith", this, Optional.empty(), node);
        	}
        	AJoinPoint result = this.replaceWithImpl(node);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "replaceWith", this, Optional.ofNullable(result), node);
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new ActionException(get_class(), "replaceWith", e);
        }
    }

    /**
     * Overload which accepts a list of join points
     * @param node 
     */
    public AJoinPoint replaceWithImpl(AJoinPoint[] node) {
        throw new UnsupportedOperationException(get_class()+": Action replaceWith not implemented ");
    }

    /**
     * Overload which accepts a list of join points
     * @param node 
     */
    public final Object replaceWith(Object[] node) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "replaceWith", this, Optional.empty(), new Object[] { node});
        	}
        	AJoinPoint result = this.replaceWithImpl(pt.up.fe.specs.util.SpecsCollections.cast(node, AJoinPoint.class));
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "replaceWith", this, Optional.ofNullable(result), new Object[] { node});
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new ActionException(get_class(), "replaceWith", e);
        }
    }

    /**
     * Overload which accepts a list of strings
     * @param node 
     */
    public AJoinPoint replaceWithStringsImpl(String[] node) {
        throw new UnsupportedOperationException(get_class()+": Action replaceWithStrings not implemented ");
    }

    /**
     * Overload which accepts a list of strings
     * @param node 
     */
    public final Object replaceWithStrings(Object[] node) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "replaceWithStrings", this, Optional.empty(), new Object[] { node});
        	}
        	AJoinPoint result = this.replaceWithStringsImpl(pt.up.fe.specs.util.SpecsCollections.cast(node, String.class));
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "replaceWithStrings", this, Optional.ofNullable(result), new Object[] { node});
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new ActionException(get_class(), "replaceWithStrings", e);
        }
    }

    /**
     * Setting data directly is not supported, this action just emits a warning and does nothing
     * @param source 
     */
    public void setDataImpl(Object source) {
        throw new UnsupportedOperationException(get_class()+": Action setData not implemented ");
    }

    /**
     * Setting data directly is not supported, this action just emits a warning and does nothing
     * @param source 
     */
    public final void setData(Object source) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setData", this, Optional.empty(), source);
        	}
        	this.setDataImpl(source);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setData", this, Optional.empty(), source);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setData", e);
        }
    }

    /**
     * Replaces the first child, or inserts the join point if no child is present. Returns the replaced child, or undefined if there was no child present.
     * @param node 
     */
    public AJoinPoint setFirstChildImpl(AJoinPoint node) {
        throw new UnsupportedOperationException(get_class()+": Action setFirstChild not implemented ");
    }

    /**
     * Replaces the first child, or inserts the join point if no child is present. Returns the replaced child, or undefined if there was no child present.
     * @param node 
     */
    public final Object setFirstChild(AJoinPoint node) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setFirstChild", this, Optional.empty(), node);
        	}
        	AJoinPoint result = this.setFirstChildImpl(node);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setFirstChild", this, Optional.ofNullable(result), node);
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setFirstChild", e);
        }
    }

    /**
     * Sets the commented that are embedded in a node
     * @param comments 
     */
    public void setInlineCommentsImpl(String[] comments) {
        throw new UnsupportedOperationException(get_class()+": Action setInlineComments not implemented ");
    }

    /**
     * Sets the commented that are embedded in a node
     * @param comments 
     */
    public final void setInlineComments(Object[] comments) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setInlineComments", this, Optional.empty(), new Object[] { comments});
        	}
        	this.setInlineCommentsImpl(pt.up.fe.specs.util.SpecsCollections.cast(comments, String.class));
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setInlineComments", this, Optional.empty(), new Object[] { comments});
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setInlineComments", e);
        }
    }

    /**
     * Sets the commented that are embedded in a node
     * @param comments 
     */
    public void setInlineCommentsImpl(String comments) {
        throw new UnsupportedOperationException(get_class()+": Action setInlineComments not implemented ");
    }

    /**
     * Sets the commented that are embedded in a node
     * @param comments 
     */
    public final void setInlineComments(String comments) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setInlineComments", this, Optional.empty(), comments);
        	}
        	this.setInlineCommentsImpl(comments);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setInlineComments", this, Optional.empty(), comments);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setInlineComments", e);
        }
    }

    /**
     * Replaces the last child, or inserts the join point if no child is present. Returns the replaced child, or undefined if there was no child present.
     * @param node 
     */
    public AJoinPoint setLastChildImpl(AJoinPoint node) {
        throw new UnsupportedOperationException(get_class()+": Action setLastChild not implemented ");
    }

    /**
     * Replaces the last child, or inserts the join point if no child is present. Returns the replaced child, or undefined if there was no child present.
     * @param node 
     */
    public final Object setLastChild(AJoinPoint node) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setLastChild", this, Optional.empty(), node);
        	}
        	AJoinPoint result = this.setLastChildImpl(node);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setLastChild", this, Optional.ofNullable(result), node);
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setLastChild", e);
        }
    }

    /**
     * Sets the type of a node, if it has a type
     * @param type 
     */
    public void setTypeImpl(AType type) {
        throw new UnsupportedOperationException(get_class()+": Action setType not implemented ");
    }

    /**
     * Sets the type of a node, if it has a type
     * @param type 
     */
    public final void setType(AType type) {
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
        	return result!=null?result:getUndefinedValue();
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
        	return result!=null?result:getUndefinedValue();
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
    public final Object setValue(String key, Object value) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setValue", this, Optional.empty(), key, value);
        	}
        	AJoinPoint result = this.setValueImpl(key, value);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setValue", this, Optional.ofNullable(result), key, value);
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setValue", e);
        }
    }

    /**
     * Replaces this join point with a comment with the same contents as .code
     * @param prefix 
     * @param suffix 
     */
    public AJoinPoint toCommentImpl(String prefix, String suffix) {
        throw new UnsupportedOperationException(get_class()+": Action toComment not implemented ");
    }

    /**
     * Replaces this join point with a comment with the same contents as .code
     * @param prefix 
     * @param suffix 
     */
    public final Object toComment(String prefix, String suffix) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "toComment", this, Optional.empty(), prefix, suffix);
        	}
        	AJoinPoint result = this.toCommentImpl(prefix, suffix);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "toComment", this, Optional.ofNullable(result), prefix, suffix);
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new ActionException(get_class(), "toComment", e);
        }
    }

    /**
     * 
     */
    @Override
    protected void fillWithAttributes(List<String> attributes) {
        // Default attributes
        super.fillWithAttributes(attributes);
        
        //Attributes available for all join points
        attributes.add("ast");
        attributes.add("astChildren");
        attributes.add("astId");
        attributes.add("astIsInstance(String className)");
        attributes.add("astName");
        attributes.add("astNumChildren");
        attributes.add("bitWidth");
        attributes.add("chain");
        attributes.add("children");
        attributes.add("code");
        attributes.add("column");
        attributes.add("contains(AJoinPoint jp)");
        attributes.add("currentRegion");
        attributes.add("data");
        attributes.add("depth");
        attributes.add("descendants");
        attributes.add("endColumn");
        attributes.add("endLine");
        attributes.add("filename");
        attributes.add("filepath");
        attributes.add("firstChild");
        attributes.add("getAncestor(String type)");
        attributes.add("getAstAncestor(String type)");
        attributes.add("getAstChild(int index)");
        attributes.add("getChainAncestor(String type)");
        attributes.add("getChild(int index)");
        attributes.add("getDescendants(String type)");
        attributes.add("getDescendantsAndSelf(String type)");
        attributes.add("getFirstJp(String type)");
        attributes.add("getJavaFieldType(String fieldName)");
        attributes.add("getKeyType(String key)");
        attributes.add("getUserField(String fieldName)");
        attributes.add("getValue(String key)");
        attributes.add("hasChildren");
        attributes.add("hasNode(Object nodeOrJp)");
        attributes.add("hasParent");
        attributes.add("hasType");
        attributes.add("inlineComments");
        attributes.add("isCilk");
        attributes.add("isInSystemHeader");
        attributes.add("isInsideHeader");
        attributes.add("isInsideLoopHeader");
        attributes.add("isMacro");
        attributes.add("javaFields");
        attributes.add("jpFieldsRecursive");
        attributes.add("jpId");
        attributes.add("keys");
        attributes.add("lastChild");
        attributes.add("leftJp");
        attributes.add("line");
        attributes.add("location");
        attributes.add("numChildren");
        attributes.add("originNode");
        attributes.add("parent");
        attributes.add("parentRegion");
        attributes.add("pragmas");
        attributes.add("rightJp");
        attributes.add("root");
        attributes.add("scopeNodes");
        attributes.add("siblingsLeft");
        attributes.add("siblingsRight");
        attributes.add("stmt");
        attributes.add("type");
    }

    /**
     * String with a dump of the AST representation starting from this node. This representation corresponds to the internal Java representation of the ClavaAst, where the node names correspond to Java classes. To get an equivalent representation with join point names, use the attribute 'dump'
     */
    public abstract String getAstImpl();

    /**
     * String with a dump of the AST representation starting from this node. This representation corresponds to the internal Java representation of the ClavaAst, where the node names correspond to Java classes. To get an equivalent representation with join point names, use the attribute 'dump'
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
     * String that uniquely identifies this node
     */
    public abstract String getAstIdImpl();

    /**
     * String that uniquely identifies this node
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
     * The name of the Java class of this node, which is similar to the equivalent node in Clang AST
     */
    public abstract String getAstNameImpl();

    /**
     * The name of the Java class of this node, which is similar to the equivalent node in Clang AST
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
     * The bit width of the type returned by this join point, in relation to the definitions of its Translation Unit, or undefined if there is no type or bitwidth defined, or if the join point is not in a TranslationUnit
     */
    public abstract Integer getBitWidthImpl();

    /**
     * The bit width of the type returned by this join point, in relation to the definitions of its Translation Unit, or undefined if there is no type or bitwidth defined, or if the join point is not in a TranslationUnit
     */
    public final Object getBitWidth() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "bitWidth", Optional.empty());
        	}
        	Integer result = this.getBitWidthImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "bitWidth", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "bitWidth", e);
        }
    }

    /**
     * Get value on attribute chain
     * @return the attribute's value
     */
    public abstract String[] getChainArrayImpl();

    /**
     * String list of the names of the join points that form a path from the root to this node
     */
    public Object getChainImpl() {
        String[] stringArrayImpl0 = getChainArrayImpl();
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(stringArrayImpl0);
        return nativeArray0;
    }

    /**
     * String list of the names of the join points that form a path from the root to this node
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
     * String with the code represented by this node
     */
    public abstract String getCodeImpl();

    /**
     * String with the code represented by this node
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
     * Returns the node that declares the scope of this node
     */
    public abstract AJoinPoint getCurrentRegionImpl();

    /**
     * Returns the node that declares the scope of this node
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
     * JS object associated with this node, containing parsed data of #pragma clava data when the node can be a target of pragmas. This is a special object, managed internally, and cannot be reassigned, to change its contents requires using key-value pairs. If the node can be the target of a pragma, the information stored in this object is persisted between rebuilds.
     */
    public abstract Object getDataImpl();

    /**
     * 
     */
    public void defDataImpl(Object value) {
        throw new UnsupportedOperationException("Join point "+get_class()+": Action def data with type Object not implemented ");
    }

    /**
     * JS object associated with this node, containing parsed data of #pragma clava data when the node can be a target of pragmas. This is a special object, managed internally, and cannot be reassigned, to change its contents requires using key-value pairs. If the node can be the target of a pragma, the information stored in this object is persisted between rebuilds.
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
     * The name of the file where the code of this node is located, if available
     */
    public abstract String getFilenameImpl();

    /**
     * The name of the file where the code of this node is located, if available
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
     * 
     * @param type
     * @return 
     */
    public abstract AJoinPoint getAncestorImpl(String type);

    /**
     * 
     * @param type
     * @return 
     */
    public final Object getAncestor(String type) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "getAncestor", Optional.empty(), type);
        	}
        	AJoinPoint result = this.getAncestorImpl(type);
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "getAncestor", Optional.ofNullable(result), type);
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "getAncestor", e);
        }
    }

    /**
     * 
     * @param type
     * @return 
     */
    public abstract AJoinPoint getAstAncestorImpl(String type);

    /**
     * 
     * @param type
     * @return 
     */
    public final Object getAstAncestor(String type) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "getAstAncestor", Optional.empty(), type);
        	}
        	AJoinPoint result = this.getAstAncestorImpl(type);
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "getAstAncestor", Optional.ofNullable(result), type);
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "getAstAncestor", e);
        }
    }

    /**
     * 
     * @param index
     * @return 
     */
    public abstract AJoinPoint getAstChildImpl(int index);

    /**
     * 
     * @param index
     * @return 
     */
    public final Object getAstChild(int index) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "getAstChild", Optional.empty(), index);
        	}
        	AJoinPoint result = this.getAstChildImpl(index);
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "getAstChild", Optional.ofNullable(result), index);
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "getAstChild", e);
        }
    }

    /**
     * 
     * @param type
     * @return 
     */
    public abstract AJoinPoint getChainAncestorImpl(String type);

    /**
     * 
     * @param type
     * @return 
     */
    public final Object getChainAncestor(String type) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "getChainAncestor", Optional.empty(), type);
        	}
        	AJoinPoint result = this.getChainAncestorImpl(type);
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "getChainAncestor", Optional.ofNullable(result), type);
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "getChainAncestor", e);
        }
    }

    /**
     * 
     * @param index
     * @return 
     */
    public abstract AJoinPoint getChildImpl(int index);

    /**
     * 
     * @param index
     * @return 
     */
    public final Object getChild(int index) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "getChild", Optional.empty(), index);
        	}
        	AJoinPoint result = this.getChildImpl(index);
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "getChild", Optional.ofNullable(result), index);
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "getChild", e);
        }
    }

    /**
     * 
     * @param type
     * @return 
     */
    public abstract AJoinPoint[] getDescendantsArrayImpl(String type);

    /**
     * 
     * @param type
     * @return 
     */
    public Object getDescendantsImpl(String type) {
        AJoinPoint[] aJoinPointArrayImpl0 = getDescendantsArrayImpl(type);
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(aJoinPointArrayImpl0);
        return nativeArray0;
    }

    /**
     * 
     * @param type
     * @return 
     */
    public final Object getDescendants(String type) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "getDescendants", Optional.empty(), type);
        	}
        	Object result = this.getDescendantsImpl(type);
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "getDescendants", Optional.ofNullable(result), type);
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "getDescendants", e);
        }
    }

    /**
     * 
     * @param type
     * @return 
     */
    public abstract AJoinPoint[] getDescendantsAndSelfArrayImpl(String type);

    /**
     * 
     * @param type
     * @return 
     */
    public Object getDescendantsAndSelfImpl(String type) {
        AJoinPoint[] aJoinPointArrayImpl0 = getDescendantsAndSelfArrayImpl(type);
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(aJoinPointArrayImpl0);
        return nativeArray0;
    }

    /**
     * 
     * @param type
     * @return 
     */
    public final Object getDescendantsAndSelf(String type) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "getDescendantsAndSelf", Optional.empty(), type);
        	}
        	Object result = this.getDescendantsAndSelfImpl(type);
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "getDescendantsAndSelf", Optional.ofNullable(result), type);
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "getDescendantsAndSelf", e);
        }
    }

    /**
     * 
     * @param type
     * @return 
     */
    public abstract AJoinPoint getFirstJpImpl(String type);

    /**
     * 
     * @param type
     * @return 
     */
    public final Object getFirstJp(String type) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "getFirstJp", Optional.empty(), type);
        	}
        	AJoinPoint result = this.getFirstJpImpl(type);
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "getFirstJp", Optional.ofNullable(result), type);
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "getFirstJp", e);
        }
    }

    /**
     * 
     * @param fieldName
     * @return 
     */
    public abstract String getJavaFieldTypeImpl(String fieldName);

    /**
     * 
     * @param fieldName
     * @return 
     */
    public final Object getJavaFieldType(String fieldName) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "getJavaFieldType", Optional.empty(), fieldName);
        	}
        	String result = this.getJavaFieldTypeImpl(fieldName);
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "getJavaFieldType", Optional.ofNullable(result), fieldName);
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "getJavaFieldType", e);
        }
    }

    /**
     * 
     * @param key
     * @return 
     */
    public abstract Object getKeyTypeImpl(String key);

    /**
     * 
     * @param key
     * @return 
     */
    public final Object getKeyType(String key) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "getKeyType", Optional.empty(), key);
        	}
        	Object result = this.getKeyTypeImpl(key);
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "getKeyType", Optional.ofNullable(result), key);
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "getKeyType", e);
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
     * true if this node has a parent
     */
    public abstract Boolean getHasParentImpl();

    /**
     * true if this node has a parent
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
     * true, if the join point has a type
     */
    public abstract Boolean getHasTypeImpl();

    /**
     * true, if the join point has a type
     */
    public final Object getHasType() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "hasType", Optional.empty());
        	}
        	Boolean result = this.getHasTypeImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "hasType", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "hasType", e);
        }
    }

    /**
     * Get value on attribute inlineComments
     * @return the attribute's value
     */
    public abstract AComment[] getInlineCommentsArrayImpl();

    /**
     * Returns comments that are not explicitly in the AST, but embedded in other nodes
     */
    public Object getInlineCommentsImpl() {
        AComment[] aCommentArrayImpl0 = getInlineCommentsArrayImpl();
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(aCommentArrayImpl0);
        return nativeArray0;
    }

    /**
     * 
     */
    public void defInlineCommentsImpl(String[] value) {
        throw new UnsupportedOperationException("Join point "+get_class()+": Action def inlineComments with type String not implemented ");
    }

    /**
     * 
     */
    public void defInlineCommentsImpl(String value) {
        throw new UnsupportedOperationException("Join point "+get_class()+": Action def inlineComments with type String not implemented ");
    }

    /**
     * Returns comments that are not explicitly in the AST, but embedded in other nodes
     */
    public final Object getInlineComments() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "inlineComments", Optional.empty());
        	}
        	Object result = this.getInlineCommentsImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "inlineComments", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "inlineComments", e);
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
     * true, if the join point is part of a system header file
     */
    public abstract Boolean getIsInSystemHeaderImpl();

    /**
     * true, if the join point is part of a system header file
     */
    public final Object getIsInSystemHeader() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "isInSystemHeader", Optional.empty());
        	}
        	Boolean result = this.getIsInSystemHeaderImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "isInSystemHeader", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isInSystemHeader", e);
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
     * Get value on attribute javaFields
     * @return the attribute's value
     */
    public abstract String[] getJavaFieldsArrayImpl();

    /**
     * [DEPRECATED: used attribute 'keys' instead, together with 'getValue'] The names of the Java fields of this node. Can be used as key of the attribute 'javaValue'
     */
    public Object getJavaFieldsImpl() {
        String[] stringArrayImpl0 = getJavaFieldsArrayImpl();
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(stringArrayImpl0);
        return nativeArray0;
    }

    /**
     * [DEPRECATED: used attribute 'keys' instead, together with 'getValue'] The names of the Java fields of this node. Can be used as key of the attribute 'javaValue'
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
     * Get value on attribute jpFieldsRecursive
     * @return the attribute's value
     */
    public abstract AJoinPoint[] getJpFieldsRecursiveArrayImpl();

    /**
     * List with the values of fields that are join points, recursively
     */
    public Object getJpFieldsRecursiveImpl() {
        AJoinPoint[] aJoinPointArrayImpl0 = getJpFieldsRecursiveArrayImpl();
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(aJoinPointArrayImpl0);
        return nativeArray0;
    }

    /**
     * List with the values of fields that are join points, recursively
     */
    public final Object getJpFieldsRecursive() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "jpFieldsRecursive", Optional.empty());
        	}
        	Object result = this.getJpFieldsRecursiveImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "jpFieldsRecursive", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "jpFieldsRecursive", e);
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
     * Get value on attribute keys
     * @return the attribute's value
     */
    public abstract String[] getKeysArrayImpl();

    /**
     * A list of the properties currently supported by this node. Can be used as parameter of the attribute 'getValue'
     */
    public Object getKeysImpl() {
        String[] stringArrayImpl0 = getKeysArrayImpl();
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(stringArrayImpl0);
        return nativeArray0;
    }

    /**
     * A list of the properties currently supported by this node. Can be used as parameter of the attribute 'getValue'
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
     * Returns the node that came before this node, or undefined if there is none
     */
    public abstract AJoinPoint getLeftJpImpl();

    /**
     * Returns the node that came before this node, or undefined if there is none
     */
    public final Object getLeftJp() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "leftJp", Optional.empty());
        	}
        	AJoinPoint result = this.getLeftJpImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "leftJp", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "leftJp", e);
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
     * A string with information about the file and code position of this node, if available
     */
    public abstract String getLocationImpl();

    /**
     * A string with information about the file and code position of this node, if available
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
     * If this join point was not originally from the parsed AST, returns the first join point of the original AST that contributed to its origin
     */
    public abstract AJoinPoint getOriginNodeImpl();

    /**
     * If this join point was not originally from the parsed AST, returns the first join point of the original AST that contributed to its origin
     */
    public final Object getOriginNode() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "originNode", Optional.empty());
        	}
        	AJoinPoint result = this.getOriginNodeImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "originNode", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "originNode", e);
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
     * Returns the node that declares the scope that is a parent of the scope of this node
     */
    public abstract AJoinPoint getParentRegionImpl();

    /**
     * Returns the node that declares the scope that is a parent of the scope of this node
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
     * Returns the node that comes after this node, or undefined if there is none
     */
    public abstract AJoinPoint getRightJpImpl();

    /**
     * Returns the node that comes after this node, or undefined if there is none
     */
    public final Object getRightJp() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "rightJp", Optional.empty());
        	}
        	AJoinPoint result = this.getRightJpImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "rightJp", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "rightJp", e);
        }
    }

    /**
     * Returns the 'program' joinpoint
     */
    public abstract AProgram getRootImpl();

    /**
     * Returns the 'program' joinpoint
     */
    public final Object getRoot() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "root", Optional.empty());
        	}
        	AProgram result = this.getRootImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "root", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "root", e);
        }
    }

    /**
     * Get value on attribute scopeNodes
     * @return the attribute's value
     */
    public abstract AJoinPoint[] getScopeNodesArrayImpl();

    /**
     * the nodes of the scope of the current join point. If this node has a body (e.g., loop, function) corresponds to the children of the body. Otherwise, returns an empty array
     */
    public Object getScopeNodesImpl() {
        AJoinPoint[] aJoinPointArrayImpl0 = getScopeNodesArrayImpl();
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(aJoinPointArrayImpl0);
        return nativeArray0;
    }

    /**
     * the nodes of the scope of the current join point. If this node has a body (e.g., loop, function) corresponds to the children of the body. Otherwise, returns an empty array
     */
    public final Object getScopeNodes() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "scopeNodes", Optional.empty());
        	}
        	Object result = this.getScopeNodesImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "scopeNodes", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "scopeNodes", e);
        }
    }

    /**
     * Get value on attribute siblingsLeft
     * @return the attribute's value
     */
    public abstract AJoinPoint[] getSiblingsLeftArrayImpl();

    /**
     * Returns an array with the siblings that came before this node
     */
    public Object getSiblingsLeftImpl() {
        AJoinPoint[] aJoinPointArrayImpl0 = getSiblingsLeftArrayImpl();
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(aJoinPointArrayImpl0);
        return nativeArray0;
    }

    /**
     * Returns an array with the siblings that came before this node
     */
    public final Object getSiblingsLeft() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "siblingsLeft", Optional.empty());
        	}
        	Object result = this.getSiblingsLeftImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "siblingsLeft", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "siblingsLeft", e);
        }
    }

    /**
     * Get value on attribute siblingsRight
     * @return the attribute's value
     */
    public abstract AJoinPoint[] getSiblingsRightArrayImpl();

    /**
     * Returns an array with the siblings that come after this node
     */
    public Object getSiblingsRightImpl() {
        AJoinPoint[] aJoinPointArrayImpl0 = getSiblingsRightArrayImpl();
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(aJoinPointArrayImpl0);
        return nativeArray0;
    }

    /**
     * Returns an array with the siblings that come after this node
     */
    public final Object getSiblingsRight() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "siblingsRight", Optional.empty());
        	}
        	Object result = this.getSiblingsRightImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "siblingsRight", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "siblingsRight", e);
        }
    }

    /**
     * Converts this join point to a statement, or returns undefined if it was not possible
     */
    public abstract AStatement getStmtImpl();

    /**
     * Converts this join point to a statement, or returns undefined if it was not possible
     */
    public final Object getStmt() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "stmt", Optional.empty());
        	}
        	AStatement result = this.getStmtImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "stmt", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "stmt", e);
        }
    }

    /**
     * Get value on attribute type
     * @return the attribute's value
     */
    public abstract AType getTypeImpl();

    /**
     * 
     */
    public void defTypeImpl(AType value) {
        throw new UnsupportedOperationException("Join point "+get_class()+": Action def type with type AType not implemented ");
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
        	AType result = this.getTypeImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "type", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "type", e);
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
