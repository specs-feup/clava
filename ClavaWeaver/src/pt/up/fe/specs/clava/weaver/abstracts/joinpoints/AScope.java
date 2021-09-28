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
 * Auto-Generated class for join point AScope
 * This class is overwritten by the Weaver Generator.
 * 
 * Represents a group of statements
 * @author Lara Weaver Generator
 */
public abstract class AScope extends AStatement {

    protected AStatement aStatement;

    /**
     * 
     */
    public AScope(AStatement aStatement){
        this.aStatement = aStatement;
    }
    /**
     * The number of statements in the scope, including the statements inside the declaration and bodies of structures such as ifs and loops, and not considering comments and pragmas
     */
    public abstract Long getNumStatementsImpl();

    /**
     * The number of statements in the scope, including the statements inside the declaration and bodies of structures such as ifs and loops, and not considering comments and pragmas
     */
    public final Object getNumStatements() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "numStatements", Optional.empty());
        	}
        	Long result = this.getNumStatementsImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "numStatements", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "numStatements", e);
        }
    }

    /**
     * 
     * @param flat
     * @return 
     */
    public abstract Long numStatementsImpl(Boolean flat);

    /**
     * 
     * @param flat
     * @return 
     */
    public final Object numStatements(Boolean flat) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "numStatements", Optional.empty(), flat);
        	}
        	Long result = this.numStatementsImpl(flat);
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "numStatements", Optional.ofNullable(result), flat);
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "numStatements", e);
        }
    }

    /**
     * true if the scope does not have curly braces
     */
    public abstract Boolean getNakedImpl();

    /**
     * true if the scope does not have curly braces
     */
    public final Object getNaked() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "naked", Optional.empty());
        	}
        	Boolean result = this.getNakedImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "naked", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "naked", e);
        }
    }

    /**
     * 
     */
    public void defNakedImpl(Boolean value) {
        throw new UnsupportedOperationException("Join point "+get_class()+": Action def naked with type Boolean not implemented ");
    }

    /**
     * Get value on attribute stmts
     * @return the attribute's value
     */
    public abstract AStatement[] getStmtsArrayImpl();

    /**
     * Get value on attribute stmts
     * @return the attribute's value
     */
    public Object getStmtsImpl() {
        AStatement[] aStatementArrayImpl0 = getStmtsArrayImpl();
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(aStatementArrayImpl0);
        return nativeArray0;
    }

    /**
     * Get value on attribute stmts
     * @return the attribute's value
     */
    public final Object getStmts() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "stmts", Optional.empty());
        	}
        	Object result = this.getStmtsImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "stmts", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "stmts", e);
        }
    }

    /**
     * Get value on attribute firstStmt
     * @return the attribute's value
     */
    public abstract AStatement getFirstStmtImpl();

    /**
     * Get value on attribute firstStmt
     * @return the attribute's value
     */
    public final Object getFirstStmt() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "firstStmt", Optional.empty());
        	}
        	AStatement result = this.getFirstStmtImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "firstStmt", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "firstStmt", e);
        }
    }

    /**
     * Get value on attribute lastStmt
     * @return the attribute's value
     */
    public abstract AStatement getLastStmtImpl();

    /**
     * Get value on attribute lastStmt
     * @return the attribute's value
     */
    public final Object getLastStmt() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "lastStmt", Optional.empty());
        	}
        	AStatement result = this.getLastStmtImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "lastStmt", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "lastStmt", e);
        }
    }

    /**
     * The statement that owns the scope (e.g., function, loop...)
     */
    public abstract AJoinPoint getOwnerImpl();

    /**
     * The statement that owns the scope (e.g., function, loop...)
     */
    public final Object getOwner() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "owner", Optional.empty());
        	}
        	AJoinPoint result = this.getOwnerImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "owner", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "owner", e);
        }
    }

    /**
     * Default implementation of the method used by the lara interpreter to select stmts
     * @return 
     */
    public List<? extends AStatement> selectStmt() {
        return select(pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AStatement.class, SelectOp.DESCENDANTS);
    }

    /**
     * Default implementation of the method used by the lara interpreter to select childStmts
     * @return 
     */
    public List<? extends AStatement> selectChildStmt() {
        return select(pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AStatement.class, SelectOp.DESCENDANTS);
    }

    /**
     * Default implementation of the method used by the lara interpreter to select scopes
     * @return 
     */
    public List<? extends AScope> selectScope() {
        return select(pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AScope.class, SelectOp.DESCENDANTS);
    }

    /**
     * Default implementation of the method used by the lara interpreter to select ifs
     * @return 
     */
    public List<? extends AIf> selectIf() {
        return select(pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AIf.class, SelectOp.DESCENDANTS);
    }

    /**
     * Default implementation of the method used by the lara interpreter to select loops
     * @return 
     */
    public List<? extends ALoop> selectLoop() {
        return select(pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ALoop.class, SelectOp.DESCENDANTS);
    }

    /**
     * Default implementation of the method used by the lara interpreter to select pragmas
     * @return 
     */
    public List<? extends APragma> selectPragma() {
        return select(pt.up.fe.specs.clava.weaver.abstracts.joinpoints.APragma.class, SelectOp.DESCENDANTS);
    }

    /**
     * Default implementation of the method used by the lara interpreter to select markers
     * @return 
     */
    public List<? extends AMarker> selectMarker() {
        return select(pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AMarker.class, SelectOp.DESCENDANTS);
    }

    /**
     * Default implementation of the method used by the lara interpreter to select tags
     * @return 
     */
    public List<? extends ATag> selectTag() {
        return select(pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ATag.class, SelectOp.DESCENDANTS);
    }

    /**
     * Default implementation of the method used by the lara interpreter to select omps
     * @return 
     */
    public List<? extends AOmp> selectOmp() {
        return select(pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AOmp.class, SelectOp.DESCENDANTS);
    }

    /**
     * Default implementation of the method used by the lara interpreter to select comments
     * @return 
     */
    public List<? extends AComment> selectComment() {
        return select(pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AComment.class, SelectOp.DESCENDANTS);
    }

    /**
     * Default implementation of the method used by the lara interpreter to select returnStmts
     * @return 
     */
    public List<? extends AReturnStmt> selectReturnStmt() {
        return select(pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AReturnStmt.class, SelectOp.DESCENDANTS);
    }

    /**
     * Default implementation of the method used by the lara interpreter to select cilkFors
     * @return 
     */
    public List<? extends ACilkFor> selectCilkFor() {
        return select(pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ACilkFor.class, SelectOp.DESCENDANTS);
    }

    /**
     * Default implementation of the method used by the lara interpreter to select cilkSyncs
     * @return 
     */
    public List<? extends ACilkSync> selectCilkSync() {
        return select(pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ACilkSync.class, SelectOp.DESCENDANTS);
    }

    /**
     * 
     * @param node 
     */
    public AJoinPoint insertBeginImpl(AJoinPoint node) {
        throw new UnsupportedOperationException(get_class()+": Action insertBegin not implemented ");
    }

    /**
     * 
     * @param node 
     */
    public final AJoinPoint insertBegin(AJoinPoint node) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "insertBegin", this, Optional.empty(), node);
        	}
        	AJoinPoint result = this.insertBeginImpl(node);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "insertBegin", this, Optional.ofNullable(result), node);
        	}
        	return result;
        } catch(Exception e) {
        	throw new ActionException(get_class(), "insertBegin", e);
        }
    }

    /**
     * 
     * @param code 
     */
    public AJoinPoint insertBeginImpl(String code) {
        throw new UnsupportedOperationException(get_class()+": Action insertBegin not implemented ");
    }

    /**
     * 
     * @param code 
     */
    public final AJoinPoint insertBegin(String code) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "insertBegin", this, Optional.empty(), code);
        	}
        	AJoinPoint result = this.insertBeginImpl(code);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "insertBegin", this, Optional.ofNullable(result), code);
        	}
        	return result;
        } catch(Exception e) {
        	throw new ActionException(get_class(), "insertBegin", e);
        }
    }

    /**
     * 
     * @param node 
     */
    public AJoinPoint insertEndImpl(AJoinPoint node) {
        throw new UnsupportedOperationException(get_class()+": Action insertEnd not implemented ");
    }

    /**
     * 
     * @param node 
     */
    public final AJoinPoint insertEnd(AJoinPoint node) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "insertEnd", this, Optional.empty(), node);
        	}
        	AJoinPoint result = this.insertEndImpl(node);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "insertEnd", this, Optional.ofNullable(result), node);
        	}
        	return result;
        } catch(Exception e) {
        	throw new ActionException(get_class(), "insertEnd", e);
        }
    }

    /**
     * 
     * @param code 
     */
    public AJoinPoint insertEndImpl(String code) {
        throw new UnsupportedOperationException(get_class()+": Action insertEnd not implemented ");
    }

    /**
     * 
     * @param code 
     */
    public final AJoinPoint insertEnd(String code) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "insertEnd", this, Optional.empty(), code);
        	}
        	AJoinPoint result = this.insertEndImpl(code);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "insertEnd", this, Optional.ofNullable(result), code);
        	}
        	return result;
        } catch(Exception e) {
        	throw new ActionException(get_class(), "insertEnd", e);
        }
    }

    /**
     * Adds a new local variable to this scope
     * @param name 
     * @param type 
     * @param initValue 
     */
    public AJoinPoint addLocalImpl(String name, AJoinPoint type, String initValue) {
        throw new UnsupportedOperationException(get_class()+": Action addLocal not implemented ");
    }

    /**
     * Adds a new local variable to this scope
     * @param name 
     * @param type 
     * @param initValue 
     */
    public final AJoinPoint addLocal(String name, AJoinPoint type, String initValue) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "addLocal", this, Optional.empty(), name, type, initValue);
        	}
        	AJoinPoint result = this.addLocalImpl(name, type, initValue);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "addLocal", this, Optional.ofNullable(result), name, type, initValue);
        	}
        	return result;
        } catch(Exception e) {
        	throw new ActionException(get_class(), "addLocal", e);
        }
    }

    /**
     * Overload which does not initialize the local variable
     * @param name 
     * @param type 
     */
    public AJoinPoint addLocalImpl(String name, AJoinPoint type) {
        throw new UnsupportedOperationException(get_class()+": Action addLocal not implemented ");
    }

    /**
     * Overload which does not initialize the local variable
     * @param name 
     * @param type 
     */
    public final AJoinPoint addLocal(String name, AJoinPoint type) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "addLocal", this, Optional.empty(), name, type);
        	}
        	AJoinPoint result = this.addLocalImpl(name, type);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "addLocal", this, Optional.ofNullable(result), name, type);
        	}
        	return result;
        } catch(Exception e) {
        	throw new ActionException(get_class(), "addLocal", e);
        }
    }

    /**
     * Sets the 'naked' status of a scope (a scope is naked if it does not have curly braces)
     * @param isNaked 
     */
    public void setNakedImpl(Boolean isNaked) {
        throw new UnsupportedOperationException(get_class()+": Action setNaked not implemented ");
    }

    /**
     * Sets the 'naked' status of a scope (a scope is naked if it does not have curly braces)
     * @param isNaked 
     */
    public final void setNaked(Boolean isNaked) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setNaked", this, Optional.empty(), isNaked);
        	}
        	this.setNakedImpl(isNaked);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setNaked", this, Optional.empty(), isNaked);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setNaked", e);
        }
    }

    /**
     * Clears the contents of this scope (untested)
     */
    public void clearImpl() {
        throw new UnsupportedOperationException(get_class()+": Action clear not implemented ");
    }

    /**
     * Clears the contents of this scope (untested)
     */
    public final void clear() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "clear", this, Optional.empty());
        	}
        	this.clearImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "clear", this, Optional.empty());
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "clear", e);
        }
    }

    /**
     * CFG tester
     */
    public String cfgImpl() {
        throw new UnsupportedOperationException(get_class()+": Action cfg not implemented ");
    }

    /**
     * CFG tester
     */
    public final String cfg() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "cfg", this, Optional.empty());
        	}
        	String result = this.cfgImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "cfg", this, Optional.ofNullable(result));
        	}
        	return result;
        } catch(Exception e) {
        	throw new ActionException(get_class(), "cfg", e);
        }
    }

    /**
     * DFG tester
     */
    public String dfgImpl() {
        throw new UnsupportedOperationException(get_class()+": Action dfg not implemented ");
    }

    /**
     * DFG tester
     */
    public final String dfg() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "dfg", this, Optional.empty());
        	}
        	String result = this.dfgImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "dfg", this, Optional.ofNullable(result));
        	}
        	return result;
        } catch(Exception e) {
        	throw new ActionException(get_class(), "dfg", e);
        }
    }

    /**
     * Get value on attribute isFirst
     * @return the attribute's value
     */
    @Override
    public Boolean getIsFirstImpl() {
        return this.aStatement.getIsFirstImpl();
    }

    /**
     * Get value on attribute isLast
     * @return the attribute's value
     */
    @Override
    public Boolean getIsLastImpl() {
        return this.aStatement.getIsLastImpl();
    }

    /**
     * Method used by the lara interpreter to select exprs
     * @return 
     */
    @Override
    public List<? extends AExpression> selectExpr() {
        return this.aStatement.selectExpr();
    }

    /**
     * Method used by the lara interpreter to select childExprs
     * @return 
     */
    @Override
    public List<? extends AExpression> selectChildExpr() {
        return this.aStatement.selectChildExpr();
    }

    /**
     * Method used by the lara interpreter to select calls
     * @return 
     */
    @Override
    public List<? extends ACall> selectCall() {
        return this.aStatement.selectCall();
    }

    /**
     * Method used by the lara interpreter to select stmtCalls
     * @return 
     */
    @Override
    public List<? extends ACall> selectStmtCall() {
        return this.aStatement.selectStmtCall();
    }

    /**
     * Method used by the lara interpreter to select memberCalls
     * @return 
     */
    @Override
    public List<? extends AMemberCall> selectMemberCall() {
        return this.aStatement.selectMemberCall();
    }

    /**
     * Method used by the lara interpreter to select memberAccesss
     * @return 
     */
    @Override
    public List<? extends AMemberAccess> selectMemberAccess() {
        return this.aStatement.selectMemberAccess();
    }

    /**
     * Method used by the lara interpreter to select arrayAccesss
     * @return 
     */
    @Override
    public List<? extends AArrayAccess> selectArrayAccess() {
        return this.aStatement.selectArrayAccess();
    }

    /**
     * Method used by the lara interpreter to select vardecls
     * @return 
     */
    @Override
    public List<? extends AVardecl> selectVardecl() {
        return this.aStatement.selectVardecl();
    }

    /**
     * Method used by the lara interpreter to select varrefs
     * @return 
     */
    @Override
    public List<? extends AVarref> selectVarref() {
        return this.aStatement.selectVarref();
    }

    /**
     * Method used by the lara interpreter to select ops
     * @return 
     */
    @Override
    public List<? extends AOp> selectOp() {
        return this.aStatement.selectOp();
    }

    /**
     * Method used by the lara interpreter to select binaryOps
     * @return 
     */
    @Override
    public List<? extends ABinaryOp> selectBinaryOp() {
        return this.aStatement.selectBinaryOp();
    }

    /**
     * Method used by the lara interpreter to select unaryOps
     * @return 
     */
    @Override
    public List<? extends AUnaryOp> selectUnaryOp() {
        return this.aStatement.selectUnaryOp();
    }

    /**
     * Method used by the lara interpreter to select newExprs
     * @return 
     */
    @Override
    public List<? extends ANewExpr> selectNewExpr() {
        return this.aStatement.selectNewExpr();
    }

    /**
     * Method used by the lara interpreter to select deleteExprs
     * @return 
     */
    @Override
    public List<? extends ADeleteExpr> selectDeleteExpr() {
        return this.aStatement.selectDeleteExpr();
    }

    /**
     * Method used by the lara interpreter to select cilkSpawns
     * @return 
     */
    @Override
    public List<? extends ACilkSpawn> selectCilkSpawn() {
        return this.aStatement.selectCilkSpawn();
    }

    /**
     * Replaces this join point with the given join
     * @param node 
     */
    @Override
    public AJoinPoint replaceWithImpl(AJoinPoint node) {
        return this.aStatement.replaceWithImpl(node);
    }

    /**
     * Overload which accepts a string
     * @param node 
     */
    @Override
    public AJoinPoint replaceWithImpl(String node) {
        return this.aStatement.replaceWithImpl(node);
    }

    /**
     * Inserts the given join point before this join point
     * @param node 
     */
    @Override
    public AJoinPoint insertBeforeImpl(AJoinPoint node) {
        return this.aStatement.insertBeforeImpl(node);
    }

    /**
     * Overload which accepts a string
     * @param node 
     */
    @Override
    public AJoinPoint insertBeforeImpl(String node) {
        return this.aStatement.insertBeforeImpl(node);
    }

    /**
     * Inserts the given join point after this join point
     * @param node 
     */
    @Override
    public AJoinPoint insertAfterImpl(AJoinPoint node) {
        return this.aStatement.insertAfterImpl(node);
    }

    /**
     * Overload which accepts a string
     * @param code 
     */
    @Override
    public AJoinPoint insertAfterImpl(String code) {
        return this.aStatement.insertAfterImpl(code);
    }

    /**
     * Removes the node associated to this joinpoint from the AST
     */
    @Override
    public void detachImpl() {
        this.aStatement.detachImpl();
    }

    /**
     * Sets the type of a node, if it has a type
     * @param type 
     */
    @Override
    public void setTypeImpl(AType type) {
        this.aStatement.setTypeImpl(type);
    }

    /**
     * Performs a copy of the node and its children, but not of the nodes in its fields
     */
    @Override
    public AJoinPoint copyImpl() {
        return this.aStatement.copyImpl();
    }

    /**
     * Performs a copy of the node and its children, including the nodes in their fields (only the first level of field nodes, this function is not recursive)
     */
    @Override
    public AJoinPoint deepCopyImpl() {
        return this.aStatement.deepCopyImpl();
    }

    /**
     * Associates arbitrary values to nodes of the AST
     * @param fieldName 
     * @param value 
     */
    @Override
    public Object setUserFieldImpl(String fieldName, Object value) {
        return this.aStatement.setUserFieldImpl(fieldName, value);
    }

    /**
     * Overload which accepts a map
     * @param fieldNameAndValue 
     */
    @Override
    public Object setUserFieldImpl(Map<?, ?> fieldNameAndValue) {
        return this.aStatement.setUserFieldImpl(fieldNameAndValue);
    }

    /**
     * Sets the value associated with the given property key
     * @param key 
     * @param value 
     */
    @Override
    public AJoinPoint setValueImpl(String key, Object value) {
        return this.aStatement.setValueImpl(key, value);
    }

    /**
     * Adds a message that will be printed to the user after weaving finishes. Identical messages are removed
     * @param message 
     */
    @Override
    public void messageToUserImpl(String message) {
        this.aStatement.messageToUserImpl(message);
    }

    /**
     * Removes the children of this node
     */
    @Override
    public void removeChildrenImpl() {
        this.aStatement.removeChildrenImpl();
    }

    /**
     * Replaces the first child, or inserts the join point if no child is present
     * @param node 
     */
    @Override
    public void setFirstChildImpl(AJoinPoint node) {
        this.aStatement.setFirstChildImpl(node);
    }

    /**
     * Replaces the last child, or inserts the join point if no child is present
     * @param node 
     */
    @Override
    public void setLastChildImpl(AJoinPoint node) {
        this.aStatement.setLastChildImpl(node);
    }

    /**
     * Replaces this join point with a comment with the same contents as .code
     */
    @Override
    public AJoinPoint toCommentImpl() {
        return this.aStatement.toCommentImpl();
    }

    /**
     * Replaces this join point with a comment with the same contents as .code
     * @param prefix 
     */
    @Override
    public AJoinPoint toCommentImpl(String prefix) {
        return this.aStatement.toCommentImpl(prefix);
    }

    /**
     * Replaces this join point with a comment with the same contents as .code
     * @param prefix 
     * @param suffix 
     */
    @Override
    public AJoinPoint toCommentImpl(String prefix, String suffix) {
        return this.aStatement.toCommentImpl(prefix, suffix);
    }

    /**
     * 
     * @param position 
     * @param code 
     */
    @Override
    public AJoinPoint[] insertImpl(String position, String code) {
        return this.aStatement.insertImpl(position, code);
    }

    /**
     * 
     * @param position 
     * @param code 
     */
    @Override
    public AJoinPoint[] insertImpl(String position, JoinPoint code) {
        return this.aStatement.insertImpl(position, code);
    }

    /**
     * 
     */
    @Override
    public Optional<? extends AStatement> getSuper() {
        return Optional.of(this.aStatement);
    }

    /**
     * 
     */
    @Override
    public List<? extends JoinPoint> select(String selectName) {
        List<? extends JoinPoint> joinPointList;
        switch(selectName) {
        	case "stmt": 
        		joinPointList = selectStmt();
        		break;
        	case "childStmt": 
        		joinPointList = selectChildStmt();
        		break;
        	case "scope": 
        		joinPointList = selectScope();
        		break;
        	case "if": 
        		joinPointList = selectIf();
        		break;
        	case "loop": 
        		joinPointList = selectLoop();
        		break;
        	case "pragma": 
        		joinPointList = selectPragma();
        		break;
        	case "marker": 
        		joinPointList = selectMarker();
        		break;
        	case "tag": 
        		joinPointList = selectTag();
        		break;
        	case "omp": 
        		joinPointList = selectOmp();
        		break;
        	case "comment": 
        		joinPointList = selectComment();
        		break;
        	case "returnStmt": 
        		joinPointList = selectReturnStmt();
        		break;
        	case "cilkFor": 
        		joinPointList = selectCilkFor();
        		break;
        	case "cilkSync": 
        		joinPointList = selectCilkSync();
        		break;
        	case "expr": 
        		joinPointList = selectExpr();
        		break;
        	case "childExpr": 
        		joinPointList = selectChildExpr();
        		break;
        	case "call": 
        		joinPointList = selectCall();
        		break;
        	case "stmtCall": 
        		joinPointList = selectStmtCall();
        		break;
        	case "memberCall": 
        		joinPointList = selectMemberCall();
        		break;
        	case "memberAccess": 
        		joinPointList = selectMemberAccess();
        		break;
        	case "arrayAccess": 
        		joinPointList = selectArrayAccess();
        		break;
        	case "vardecl": 
        		joinPointList = selectVardecl();
        		break;
        	case "varref": 
        		joinPointList = selectVarref();
        		break;
        	case "op": 
        		joinPointList = selectOp();
        		break;
        	case "binaryOp": 
        		joinPointList = selectBinaryOp();
        		break;
        	case "unaryOp": 
        		joinPointList = selectUnaryOp();
        		break;
        	case "newExpr": 
        		joinPointList = selectNewExpr();
        		break;
        	case "deleteExpr": 
        		joinPointList = selectDeleteExpr();
        		break;
        	case "cilkSpawn": 
        		joinPointList = selectCilkSpawn();
        		break;
        	default:
        		joinPointList = this.aStatement.select(selectName);
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
        case "lastChild": {
        	if(value instanceof AJoinPoint){
        		this.defLastChildImpl((AJoinPoint)value);
        		return;
        	}
        	this.unsupportedTypeForDef(attribute, value);
        }
        case "naked": {
        	if(value instanceof Boolean){
        		this.defNakedImpl((Boolean)value);
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
        this.aStatement.fillWithAttributes(attributes);
        attributes.add("numStatements");
        attributes.add("numStatements");
        attributes.add("naked");
        attributes.add("stmts");
        attributes.add("firstStmt");
        attributes.add("lastStmt");
        attributes.add("owner");
    }

    /**
     * 
     */
    @Override
    protected void fillWithSelects(List<String> selects) {
        this.aStatement.fillWithSelects(selects);
        selects.add("stmt");
        selects.add("childStmt");
        selects.add("scope");
        selects.add("if");
        selects.add("loop");
        selects.add("pragma");
        selects.add("marker");
        selects.add("tag");
        selects.add("omp");
        selects.add("comment");
        selects.add("returnStmt");
        selects.add("cilkFor");
        selects.add("cilkSync");
    }

    /**
     * 
     */
    @Override
    protected void fillWithActions(List<String> actions) {
        this.aStatement.fillWithActions(actions);
        actions.add("joinpoint insertBegin(joinpoint)");
        actions.add("joinpoint insertBegin(String)");
        actions.add("joinpoint insertEnd(joinpoint)");
        actions.add("joinpoint insertEnd(string)");
        actions.add("joinpoint addLocal(String, joinpoint, String)");
        actions.add("joinpoint addLocal(String, joinpoint)");
        actions.add("void setNaked(Boolean)");
        actions.add("void clear()");
        actions.add("string cfg()");
        actions.add("string dfg()");
    }

    /**
     * Returns the join point type of this class
     * @return The join point type
     */
    @Override
    public String get_class() {
        return "scope";
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
        return this.aStatement.instanceOf(joinpointClass);
    }
    /**
     * 
     */
    protected enum ScopeAttributes {
        NUMSTATEMENTS("numStatements"),
        NAKED("naked"),
        STMTS("stmts"),
        FIRSTSTMT("firstStmt"),
        LASTSTMT("lastStmt"),
        OWNER("owner"),
        ISFIRST("isFirst"),
        ISLAST("isLast"),
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
        ASTCHILD("astChild"),
        ASTNAME("astName"),
        JPID("jpId"),
        ASTID("astId"),
        CONTAINS("contains"),
        ASTISINSTANCE("astIsInstance"),
        FILENAME("filename"),
        JAVAFIELDS("javaFields"),
        ASTPARENT("astParent"),
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
        HASPARENT("hasParent");
        private String name;

        /**
         * 
         */
        private ScopeAttributes(String name){
            this.name = name;
        }
        /**
         * Return an attribute enumeration item from a given attribute name
         */
        public static Optional<ScopeAttributes> fromString(String name) {
            return Arrays.asList(values()).stream().filter(attr -> attr.name.equals(name)).findAny();
        }

        /**
         * Return a list of attributes in String format
         */
        public static List<String> getNames() {
            return Arrays.asList(values()).stream().map(ScopeAttributes::name).collect(Collectors.toList());
        }

        /**
         * True if the enum contains the given attribute name, false otherwise.
         */
        public static boolean contains(String name) {
            return fromString(name).isPresent();
        }
    }
}
