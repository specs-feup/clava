package pt.up.fe.specs.clava.weaver.abstracts.joinpoints;

import org.lara.interpreter.weaver.interf.JoinPoint;
import pt.up.fe.specs.clava.ClavaNode;
import org.lara.interpreter.exception.ActionException;
import java.util.Map;
import org.lara.interpreter.exception.AttributeException;
import pt.up.fe.specs.clava.weaver.CxxWeaver;

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
        	return this.copyImpl();
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
        	this.dataClearImpl();
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
        	return this.deepCopyImpl();
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
        	return this.detachImpl();
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
        	return this.insertAfterImpl(node);
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
        	return this.insertAfterImpl(code);
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
        	return this.insertBeforeImpl(node);
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
        	return this.insertBeforeImpl(node);
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
        	this.messageToUserImpl(message);
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
        	this.removeChildrenImpl();
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
        	return this.replaceWithImpl(node);
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
        	return this.replaceWithImpl(node);
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
        	return this.replaceWithImpl(pt.up.fe.specs.util.SpecsCollections.cast(node, AJoinPoint.class));
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
        	return this.replaceWithStringsImpl(pt.up.fe.specs.util.SpecsCollections.cast(node, String.class));
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
        	this.setDataImpl(source);
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
        	return this.setFirstChildImpl(node);
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
        	this.setInlineCommentsImpl(pt.up.fe.specs.util.SpecsCollections.cast(comments, String.class));
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
        	this.setInlineCommentsImpl(comments);
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
        	return this.setLastChildImpl(node);
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
        	this.setTypeImpl(type);
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
        	return this.setUserFieldImpl(fieldName, value);
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
        	return this.setUserFieldImpl(fieldNameAndValue);
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
        	return this.setValueImpl(key, value);
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
        	return this.toCommentImpl(prefix, suffix);
        } catch(Exception e) {
        	throw new ActionException(get_class(), "toComment", e);
        }
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
        	return this.getAstImpl();
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
        	return this.getAstChildrenImpl();
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
        	return this.getAstIdImpl();
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
        	return this.astIsInstanceImpl(className);
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
        	return this.getAstNameImpl();
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
        	return this.getAstNumChildrenImpl();
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
        	return this.getBitWidthImpl();
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
        	return this.getChainImpl();
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
        	return this.getChildrenImpl();
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
        	return this.getCodeImpl();
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
        	return this.getColumnImpl();
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
        	return this.containsImpl(jp);
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
        	return this.getCurrentRegionImpl();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "currentRegion", e);
        }
    }

    /**
     * JS object associated with this node, containing parsed data of #pragma clava data when the node can be a target of pragmas. This is a special object, managed internally, and cannot be reassigned, to change its contents requires using key-value pairs. If the node can be the target of a pragma, the information stored in this object is persisted between rebuilds.
     */
    public abstract Object getDataImpl();

    /**
     * JS object associated with this node, containing parsed data of #pragma clava data when the node can be a target of pragmas. This is a special object, managed internally, and cannot be reassigned, to change its contents requires using key-value pairs. If the node can be the target of a pragma, the information stored in this object is persisted between rebuilds.
     */
    public final Object getData() {
        try {
        	return this.getDataImpl();
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
        	return this.getDepthImpl();
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
        	return this.getDescendantsImpl();
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
        	return this.getEndColumnImpl();
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
        	return this.getEndLineImpl();
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
        	return this.getFilenameImpl();
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
        	return this.getFilepathImpl();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "filepath", e);
        }
    }

    /**
     * Returns the first child of this node, or undefined if it has no child
     */
    public abstract AJoinPoint getFirstChildImpl();

    /**
     * Returns the first child of this node, or undefined if it has no child
     */
    public final Object getFirstChild() {
        try {
        	return this.getFirstChildImpl();
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
        	return this.getAncestorImpl(type);
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
        	return this.getAstAncestorImpl(type);
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
        	return this.getAstChildImpl(index);
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
        	return this.getChainAncestorImpl(type);
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
        	return this.getChildImpl(index);
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
        	return this.getDescendantsImpl(type);
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
        	return this.getDescendantsAndSelfImpl(type);
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
        	return this.getFirstJpImpl(type);
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
        	return this.getJavaFieldTypeImpl(fieldName);
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
        	return this.getKeyTypeImpl(key);
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
        	return this.getUserFieldImpl(fieldName);
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
        	return this.getValueImpl(key);
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
        	return this.getHasChildrenImpl();
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
        	return this.hasNodeImpl(nodeOrJp);
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
        	return this.getHasParentImpl();
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
        	return this.getHasTypeImpl();
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
     * Returns comments that are not explicitly in the AST, but embedded in other nodes
     */
    public final Object getInlineComments() {
        try {
        	return this.getInlineCommentsImpl();
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
        	return this.getIsCilkImpl();
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
        	return this.getIsInSystemHeaderImpl();
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
        	return this.getIsInsideHeaderImpl();
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
        	return this.getIsInsideLoopHeaderImpl();
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
        	return this.getIsMacroImpl();
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
        	return this.getJavaFieldsImpl();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "javaFields", e);
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
        	return this.getJpIdImpl();
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
        	return this.getKeysImpl();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "keys", e);
        }
    }

    /**
     * Returns the last child of this node, or undefined if it has no child
     */
    public abstract AJoinPoint getLastChildImpl();

    /**
     * Returns the last child of this node, or undefined if it has no child
     */
    public final Object getLastChild() {
        try {
        	return this.getLastChildImpl();
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
        	return this.getLeftJpImpl();
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
        	return this.getLineImpl();
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
        	return this.getLocationImpl();
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
        	return this.getNumChildrenImpl();
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
        	return this.getOriginNodeImpl();
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
        	return this.getParentImpl();
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
        	return this.getParentRegionImpl();
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
        	return this.getPragmasImpl();
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
        	return this.getRightJpImpl();
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
        	return this.getRootImpl();
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
        	return this.getScopeNodesImpl();
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
        	return this.getSiblingsLeftImpl();
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
        	return this.getSiblingsRightImpl();
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
        	return this.getStmtImpl();
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
     * Get value on attribute type
     * @return the attribute's value
     */
    public final Object getType() {
        try {
        	return this.getTypeImpl();
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
}
