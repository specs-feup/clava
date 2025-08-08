package pt.up.fe.specs.clava.weaver.abstracts.joinpoints;

import org.lara.interpreter.weaver.interf.events.Stage;
import java.util.Optional;
import org.lara.interpreter.exception.AttributeException;
import org.lara.interpreter.exception.ActionException;
import java.util.List;
import org.lara.interpreter.weaver.interf.JoinPoint;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Arrays;

/**
 * Auto-Generated class for join point AOmp
 * This class is overwritten by the Weaver Generator.
 * 
 * Represents an OpenMP pragma (e.g., #pragma omp parallel)
 * @author Lara Weaver Generator
 */
public abstract class AOmp extends APragma {

    protected APragma aPragma;

    /**
     * 
     */
    public AOmp(APragma aPragma){
        this.aPragma = aPragma;
    }
    /**
     * Get value on attribute clauseKinds
     * @return the attribute's value
     */
    public abstract String[] getClauseKindsArrayImpl();

    /**
     * The names of the kinds of all clauses in the pragma, or empty array if no clause is defined
     */
    public Object getClauseKindsImpl() {
        String[] stringArrayImpl0 = getClauseKindsArrayImpl();
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(stringArrayImpl0);
        return nativeArray0;
    }

    /**
     * The names of the kinds of all clauses in the pragma, or empty array if no clause is defined
     */
    public final Object getClauseKinds() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "clauseKinds", Optional.empty());
        	}
        	Object result = this.getClauseKindsImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "clauseKinds", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "clauseKinds", e);
        }
    }

    /**
     * An integer expression, or undefined if no 'collapse' clause is defined
     */
    public abstract String getCollapseImpl();

    /**
     * An integer expression, or undefined if no 'collapse' clause is defined
     */
    public final Object getCollapse() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "collapse", Optional.empty());
        	}
        	String result = this.getCollapseImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "collapse", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "collapse", e);
        }
    }

    /**
     * Get value on attribute copyin
     * @return the attribute's value
     */
    public abstract String[] getCopyinArrayImpl();

    /**
     * The variable names of all copyin clauses, or empty array if no copyin clause is defined
     */
    public Object getCopyinImpl() {
        String[] stringArrayImpl0 = getCopyinArrayImpl();
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(stringArrayImpl0);
        return nativeArray0;
    }

    /**
     * The variable names of all copyin clauses, or empty array if no copyin clause is defined
     */
    public final Object getCopyin() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "copyin", Optional.empty());
        	}
        	Object result = this.getCopyinImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "copyin", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "copyin", e);
        }
    }

    /**
     * One of 'shared' or 'none', or undefined if no 'default' clause is defined
     */
    public abstract String getDefaultImpl();

    /**
     * One of 'shared' or 'none', or undefined if no 'default' clause is defined
     */
    public final Object getDefault() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "default", Optional.empty());
        	}
        	String result = this.getDefaultImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "default", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "default", e);
        }
    }

    /**
     * Get value on attribute firstprivate
     * @return the attribute's value
     */
    public abstract String[] getFirstprivateArrayImpl();

    /**
     * The variable names of all firstprivate clauses, or empty array if no firstprivate clause is defined
     */
    public Object getFirstprivateImpl() {
        String[] stringArrayImpl0 = getFirstprivateArrayImpl();
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(stringArrayImpl0);
        return nativeArray0;
    }

    /**
     * The variable names of all firstprivate clauses, or empty array if no firstprivate clause is defined
     */
    public final Object getFirstprivate() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "firstprivate", Optional.empty());
        	}
        	Object result = this.getFirstprivateImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "firstprivate", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "firstprivate", e);
        }
    }

    /**
     * 
     * @param kind
     * @return 
     */
    public abstract String[] getReductionArrayImpl(String kind);

    /**
     * 
     * @param kind
     * @return 
     */
    public Object getReductionImpl(String kind) {
        String[] stringArrayImpl0 = getReductionArrayImpl(kind);
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(stringArrayImpl0);
        return nativeArray0;
    }

    /**
     * 
     * @param kind
     * @return 
     */
    public final Object getReduction(String kind) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "getReduction", Optional.empty(), kind);
        	}
        	Object result = this.getReductionImpl(kind);
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "getReduction", Optional.ofNullable(result), kind);
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "getReduction", e);
        }
    }

    /**
     * 
     * @param clauseName
     * @return 
     */
    public abstract Boolean hasClauseImpl(String clauseName);

    /**
     * 
     * @param clauseName
     * @return 
     */
    public final Object hasClause(String clauseName) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "hasClause", Optional.empty(), clauseName);
        	}
        	Boolean result = this.hasClauseImpl(clauseName);
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "hasClause", Optional.ofNullable(result), clauseName);
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "hasClause", e);
        }
    }

    /**
     * 
     * @param clauseName
     * @return 
     */
    public abstract Boolean isClauseLegalImpl(String clauseName);

    /**
     * 
     * @param clauseName
     * @return 
     */
    public final Object isClauseLegal(String clauseName) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "isClauseLegal", Optional.empty(), clauseName);
        	}
        	Boolean result = this.isClauseLegalImpl(clauseName);
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "isClauseLegal", Optional.ofNullable(result), clauseName);
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isClauseLegal", e);
        }
    }

    /**
     * The kind of the directive
     */
    public abstract String getKindImpl();

    /**
     * The kind of the directive
     */
    public final Object getKind() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "kind", Optional.empty());
        	}
        	String result = this.getKindImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "kind", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "kind", e);
        }
    }

    /**
     * Get value on attribute lastprivate
     * @return the attribute's value
     */
    public abstract String[] getLastprivateArrayImpl();

    /**
     * The variable names of all lastprivate clauses, or empty array if no lastprivate clause is defined
     */
    public Object getLastprivateImpl() {
        String[] stringArrayImpl0 = getLastprivateArrayImpl();
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(stringArrayImpl0);
        return nativeArray0;
    }

    /**
     * The variable names of all lastprivate clauses, or empty array if no lastprivate clause is defined
     */
    public final Object getLastprivate() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "lastprivate", Optional.empty());
        	}
        	Object result = this.getLastprivateImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "lastprivate", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "lastprivate", e);
        }
    }

    /**
     * An integer expression, or undefined if no 'num_threads' clause is defined
     */
    public abstract String getNumThreadsImpl();

    /**
     * An integer expression, or undefined if no 'num_threads' clause is defined
     */
    public final Object getNumThreads() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "numThreads", Optional.empty());
        	}
        	String result = this.getNumThreadsImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "numThreads", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "numThreads", e);
        }
    }

    /**
     * 
     */
    public void defNumThreadsImpl(String value) {
        throw new UnsupportedOperationException("Join point "+get_class()+": Action def numThreads with type String not implemented ");
    }

    /**
     * An integer expression, or undefined if no 'ordered' clause with a parameter is defined
     */
    public abstract String getOrderedImpl();

    /**
     * An integer expression, or undefined if no 'ordered' clause with a parameter is defined
     */
    public final Object getOrdered() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "ordered", Optional.empty());
        	}
        	String result = this.getOrderedImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "ordered", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "ordered", e);
        }
    }

    /**
     * Get value on attribute _private
     * @return the attribute's value
     */
    public abstract String[] getPrivateArrayImpl();

    /**
     * The variable names of all private clauses, or empty array if no private clause is defined
     */
    public Object getPrivateImpl() {
        String[] stringArrayImpl0 = getPrivateArrayImpl();
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(stringArrayImpl0);
        return nativeArray0;
    }

    /**
     * The variable names of all private clauses, or empty array if no private clause is defined
     */
    public final Object getPrivate() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "private", Optional.empty());
        	}
        	Object result = this.getPrivateImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "private", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "private", e);
        }
    }

    /**
     * One of 'master', 'close' or 'spread', or undefined if no 'proc_bind' clause is defined
     */
    public abstract String getProcBindImpl();

    /**
     * One of 'master', 'close' or 'spread', or undefined if no 'proc_bind' clause is defined
     */
    public final Object getProcBind() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "procBind", Optional.empty());
        	}
        	String result = this.getProcBindImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "procBind", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "procBind", e);
        }
    }

    /**
     * 
     */
    public void defProcBindImpl(String value) {
        throw new UnsupportedOperationException("Join point "+get_class()+": Action def procBind with type String not implemented ");
    }

    /**
     * Get value on attribute reductionKinds
     * @return the attribute's value
     */
    public abstract String[] getReductionKindsArrayImpl();

    /**
     * The reduction kinds in the reductions clauses of the this pragma, or empty array if no reduction is defined
     */
    public Object getReductionKindsImpl() {
        String[] stringArrayImpl0 = getReductionKindsArrayImpl();
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(stringArrayImpl0);
        return nativeArray0;
    }

    /**
     * The reduction kinds in the reductions clauses of the this pragma, or empty array if no reduction is defined
     */
    public final Object getReductionKinds() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "reductionKinds", Optional.empty());
        	}
        	Object result = this.getReductionKindsImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "reductionKinds", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "reductionKinds", e);
        }
    }

    /**
     * An integer expression, or undefined if no 'schedule' clause with chunk size is defined
     */
    public abstract String getScheduleChunkSizeImpl();

    /**
     * An integer expression, or undefined if no 'schedule' clause with chunk size is defined
     */
    public final Object getScheduleChunkSize() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "scheduleChunkSize", Optional.empty());
        	}
        	String result = this.getScheduleChunkSizeImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "scheduleChunkSize", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "scheduleChunkSize", e);
        }
    }

    /**
     * One of 'static', 'dynamic', 'guided', 'auto' or 'runtime', or undefined if no 'schedule' clause is defined
     */
    public abstract String getScheduleKindImpl();

    /**
     * One of 'static', 'dynamic', 'guided', 'auto' or 'runtime', or undefined if no 'schedule' clause is defined
     */
    public final Object getScheduleKind() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "scheduleKind", Optional.empty());
        	}
        	String result = this.getScheduleKindImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "scheduleKind", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "scheduleKind", e);
        }
    }

    /**
     * Get value on attribute scheduleModifiers
     * @return the attribute's value
     */
    public abstract String[] getScheduleModifiersArrayImpl();

    /**
     * A list with possible values of 'monotonic', 'nonmonotonic' or 'simd', or undefined if no 'schedule' clause with modifiers is defined
     */
    public Object getScheduleModifiersImpl() {
        String[] stringArrayImpl0 = getScheduleModifiersArrayImpl();
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(stringArrayImpl0);
        return nativeArray0;
    }

    /**
     * A list with possible values of 'monotonic', 'nonmonotonic' or 'simd', or undefined if no 'schedule' clause with modifiers is defined
     */
    public final Object getScheduleModifiers() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "scheduleModifiers", Optional.empty());
        	}
        	Object result = this.getScheduleModifiersImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "scheduleModifiers", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "scheduleModifiers", e);
        }
    }

    /**
     * Get value on attribute shared
     * @return the attribute's value
     */
    public abstract String[] getSharedArrayImpl();

    /**
     * The variable names of all shared clauses, or empty array if no shared clause is defined
     */
    public Object getSharedImpl() {
        String[] stringArrayImpl0 = getSharedArrayImpl();
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(stringArrayImpl0);
        return nativeArray0;
    }

    /**
     * The variable names of all shared clauses, or empty array if no shared clause is defined
     */
    public final Object getShared() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "shared", Optional.empty());
        	}
        	Object result = this.getSharedImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "shared", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "shared", e);
        }
    }

    /**
     * Removes any clause of the given kind from the OpenMP pragma
     * @param clauseKind 
     */
    public void removeClauseImpl(String clauseKind) {
        throw new UnsupportedOperationException(get_class()+": Action removeClause not implemented ");
    }

    /**
     * Removes any clause of the given kind from the OpenMP pragma
     * @param clauseKind 
     */
    public final void removeClause(String clauseKind) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "removeClause", this, Optional.empty(), clauseKind);
        	}
        	this.removeClauseImpl(clauseKind);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "removeClause", this, Optional.empty(), clauseKind);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "removeClause", e);
        }
    }

    /**
     * Sets the value of the collapse clause of an OpenMP pragma
     * @param newExpr 
     */
    public void setCollapseImpl(String newExpr) {
        throw new UnsupportedOperationException(get_class()+": Action setCollapse not implemented ");
    }

    /**
     * Sets the value of the collapse clause of an OpenMP pragma
     * @param newExpr 
     */
    public final void setCollapse(String newExpr) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setCollapse", this, Optional.empty(), newExpr);
        	}
        	this.setCollapseImpl(newExpr);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setCollapse", this, Optional.empty(), newExpr);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setCollapse", e);
        }
    }

    /**
     * Sets the value of the collapse clause of an OpenMP pragma
     * @param newExpr 
     */
    public void setCollapseImpl(int newExpr) {
        throw new UnsupportedOperationException(get_class()+": Action setCollapse not implemented ");
    }

    /**
     * Sets the value of the collapse clause of an OpenMP pragma
     * @param newExpr 
     */
    public final void setCollapse(int newExpr) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setCollapse", this, Optional.empty(), newExpr);
        	}
        	this.setCollapseImpl(newExpr);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setCollapse", this, Optional.empty(), newExpr);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setCollapse", e);
        }
    }

    /**
     * Sets the variables of a copyin clause of an OpenMP pragma
     * @param newVariables 
     */
    public void setCopyinImpl(String[] newVariables) {
        throw new UnsupportedOperationException(get_class()+": Action setCopyin not implemented ");
    }

    /**
     * Sets the variables of a copyin clause of an OpenMP pragma
     * @param newVariables 
     */
    public final void setCopyin(Object[] newVariables) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setCopyin", this, Optional.empty(), new Object[] { newVariables});
        	}
        	this.setCopyinImpl(pt.up.fe.specs.util.SpecsCollections.cast(newVariables, String.class));
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setCopyin", this, Optional.empty(), new Object[] { newVariables});
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setCopyin", e);
        }
    }

    /**
     * Sets the value of the default clause of an OpenMP pragma
     * @param newDefault 
     */
    public void setDefaultImpl(String newDefault) {
        throw new UnsupportedOperationException(get_class()+": Action setDefault not implemented ");
    }

    /**
     * Sets the value of the default clause of an OpenMP pragma
     * @param newDefault 
     */
    public final void setDefault(String newDefault) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setDefault", this, Optional.empty(), newDefault);
        	}
        	this.setDefaultImpl(newDefault);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setDefault", this, Optional.empty(), newDefault);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setDefault", e);
        }
    }

    /**
     * Sets the variables of a firstprivate clause of an OpenMP pragma
     * @param newVariables 
     */
    public void setFirstprivateImpl(String[] newVariables) {
        throw new UnsupportedOperationException(get_class()+": Action setFirstprivate not implemented ");
    }

    /**
     * Sets the variables of a firstprivate clause of an OpenMP pragma
     * @param newVariables 
     */
    public final void setFirstprivate(Object[] newVariables) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setFirstprivate", this, Optional.empty(), new Object[] { newVariables});
        	}
        	this.setFirstprivateImpl(pt.up.fe.specs.util.SpecsCollections.cast(newVariables, String.class));
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setFirstprivate", this, Optional.empty(), new Object[] { newVariables});
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setFirstprivate", e);
        }
    }

    /**
     * Sets the directive kind of the OpenMP pragma. Any unsupported clauses will be discarded
     * @param directiveKind 
     */
    public void setKindImpl(String directiveKind) {
        throw new UnsupportedOperationException(get_class()+": Action setKind not implemented ");
    }

    /**
     * Sets the directive kind of the OpenMP pragma. Any unsupported clauses will be discarded
     * @param directiveKind 
     */
    public final void setKind(String directiveKind) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setKind", this, Optional.empty(), directiveKind);
        	}
        	this.setKindImpl(directiveKind);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setKind", this, Optional.empty(), directiveKind);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setKind", e);
        }
    }

    /**
     * Sets the variables of a lastprivate clause of an OpenMP pragma
     * @param newVariables 
     */
    public void setLastprivateImpl(String[] newVariables) {
        throw new UnsupportedOperationException(get_class()+": Action setLastprivate not implemented ");
    }

    /**
     * Sets the variables of a lastprivate clause of an OpenMP pragma
     * @param newVariables 
     */
    public final void setLastprivate(Object[] newVariables) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setLastprivate", this, Optional.empty(), new Object[] { newVariables});
        	}
        	this.setLastprivateImpl(pt.up.fe.specs.util.SpecsCollections.cast(newVariables, String.class));
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setLastprivate", this, Optional.empty(), new Object[] { newVariables});
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setLastprivate", e);
        }
    }

    /**
     * Sets the value of the num_threads clause of an OpenMP pragma
     * @param newExpr 
     */
    public void setNumThreadsImpl(String newExpr) {
        throw new UnsupportedOperationException(get_class()+": Action setNumThreads not implemented ");
    }

    /**
     * Sets the value of the num_threads clause of an OpenMP pragma
     * @param newExpr 
     */
    public final void setNumThreads(String newExpr) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setNumThreads", this, Optional.empty(), newExpr);
        	}
        	this.setNumThreadsImpl(newExpr);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setNumThreads", this, Optional.empty(), newExpr);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setNumThreads", e);
        }
    }

    /**
     * Sets the value of the ordered clause of an OpenMP pragma
     * @param parameters 
     */
    public void setOrderedImpl(String parameters) {
        throw new UnsupportedOperationException(get_class()+": Action setOrdered not implemented ");
    }

    /**
     * Sets the value of the ordered clause of an OpenMP pragma
     * @param parameters 
     */
    public final void setOrdered(String parameters) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setOrdered", this, Optional.empty(), parameters);
        	}
        	this.setOrderedImpl(parameters);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setOrdered", this, Optional.empty(), parameters);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setOrdered", e);
        }
    }

    /**
     * Sets the variables of a private clause of an OpenMP pragma
     * @param newVariables 
     */
    public void setPrivateImpl(String[] newVariables) {
        throw new UnsupportedOperationException(get_class()+": Action setPrivate not implemented ");
    }

    /**
     * Sets the variables of a private clause of an OpenMP pragma
     * @param newVariables 
     */
    public final void setPrivate(Object[] newVariables) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setPrivate", this, Optional.empty(), new Object[] { newVariables});
        	}
        	this.setPrivateImpl(pt.up.fe.specs.util.SpecsCollections.cast(newVariables, String.class));
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setPrivate", this, Optional.empty(), new Object[] { newVariables});
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setPrivate", e);
        }
    }

    /**
     * Sets the value of the proc_bind clause of an OpenMP pragma
     * @param newBind 
     */
    public void setProcBindImpl(String newBind) {
        throw new UnsupportedOperationException(get_class()+": Action setProcBind not implemented ");
    }

    /**
     * Sets the value of the proc_bind clause of an OpenMP pragma
     * @param newBind 
     */
    public final void setProcBind(String newBind) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setProcBind", this, Optional.empty(), newBind);
        	}
        	this.setProcBindImpl(newBind);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setProcBind", this, Optional.empty(), newBind);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setProcBind", e);
        }
    }

    /**
     * Sets the variables for a given kind of a reduction clause of an OpenMP pragma
     * @param kind 
     * @param newVariables 
     */
    public void setReductionImpl(String kind, String[] newVariables) {
        throw new UnsupportedOperationException(get_class()+": Action setReduction not implemented ");
    }

    /**
     * Sets the variables for a given kind of a reduction clause of an OpenMP pragma
     * @param kind 
     * @param newVariables 
     */
    public final void setReduction(String kind, Object[] newVariables) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setReduction", this, Optional.empty(), kind, newVariables);
        	}
        	this.setReductionImpl(kind, pt.up.fe.specs.util.SpecsCollections.cast(newVariables, String.class));
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setReduction", this, Optional.empty(), kind, newVariables);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setReduction", e);
        }
    }

    /**
     * Sets the value of the chunck size in the schedule clause of an OpenMP pragma. Can only be called if there is already a schedule clause in the directive, otherwise throws an exception
     * @param chunkSize 
     */
    public void setScheduleChunkSizeImpl(String chunkSize) {
        throw new UnsupportedOperationException(get_class()+": Action setScheduleChunkSize not implemented ");
    }

    /**
     * Sets the value of the chunck size in the schedule clause of an OpenMP pragma. Can only be called if there is already a schedule clause in the directive, otherwise throws an exception
     * @param chunkSize 
     */
    public final void setScheduleChunkSize(String chunkSize) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setScheduleChunkSize", this, Optional.empty(), chunkSize);
        	}
        	this.setScheduleChunkSizeImpl(chunkSize);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setScheduleChunkSize", this, Optional.empty(), chunkSize);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setScheduleChunkSize", e);
        }
    }

    /**
     * Sets the value of the chunck size in the schedule clause of an OpenMP pragma. Can only be called if there is already a schedule clause in the directive, otherwise throws an exception
     * @param chunkSize 
     */
    public void setScheduleChunkSizeImpl(int chunkSize) {
        throw new UnsupportedOperationException(get_class()+": Action setScheduleChunkSize not implemented ");
    }

    /**
     * Sets the value of the chunck size in the schedule clause of an OpenMP pragma. Can only be called if there is already a schedule clause in the directive, otherwise throws an exception
     * @param chunkSize 
     */
    public final void setScheduleChunkSize(int chunkSize) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setScheduleChunkSize", this, Optional.empty(), chunkSize);
        	}
        	this.setScheduleChunkSizeImpl(chunkSize);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setScheduleChunkSize", this, Optional.empty(), chunkSize);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setScheduleChunkSize", e);
        }
    }

    /**
     * Sets the value of the schedule clause of an OpenMP pragma
     * @param scheduleKind 
     */
    public void setScheduleKindImpl(String scheduleKind) {
        throw new UnsupportedOperationException(get_class()+": Action setScheduleKind not implemented ");
    }

    /**
     * Sets the value of the schedule clause of an OpenMP pragma
     * @param scheduleKind 
     */
    public final void setScheduleKind(String scheduleKind) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setScheduleKind", this, Optional.empty(), scheduleKind);
        	}
        	this.setScheduleKindImpl(scheduleKind);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setScheduleKind", this, Optional.empty(), scheduleKind);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setScheduleKind", e);
        }
    }

    /**
     * Sets the value of the modifiers in the schedule clause of an OpenMP pragma. Can only be called if there is already a schedule clause in the directive, otherwise throws an exception
     * @param modifiers 
     */
    public void setScheduleModifiersImpl(String[] modifiers) {
        throw new UnsupportedOperationException(get_class()+": Action setScheduleModifiers not implemented ");
    }

    /**
     * Sets the value of the modifiers in the schedule clause of an OpenMP pragma. Can only be called if there is already a schedule clause in the directive, otherwise throws an exception
     * @param modifiers 
     */
    public final void setScheduleModifiers(Object[] modifiers) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setScheduleModifiers", this, Optional.empty(), new Object[] { modifiers});
        	}
        	this.setScheduleModifiersImpl(pt.up.fe.specs.util.SpecsCollections.cast(modifiers, String.class));
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setScheduleModifiers", this, Optional.empty(), new Object[] { modifiers});
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setScheduleModifiers", e);
        }
    }

    /**
     * Sets the variables of a shared clause of an OpenMP pragma
     * @param newVariables 
     */
    public void setSharedImpl(String[] newVariables) {
        throw new UnsupportedOperationException(get_class()+": Action setShared not implemented ");
    }

    /**
     * Sets the variables of a shared clause of an OpenMP pragma
     * @param newVariables 
     */
    public final void setShared(Object[] newVariables) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setShared", this, Optional.empty(), new Object[] { newVariables});
        	}
        	this.setSharedImpl(pt.up.fe.specs.util.SpecsCollections.cast(newVariables, String.class));
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setShared", this, Optional.empty(), new Object[] { newVariables});
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setShared", e);
        }
    }

    /**
     * Get value on attribute content
     * @return the attribute's value
     */
    @Override
    public String getContentImpl() {
        return this.aPragma.getContentImpl();
    }

    /**
     * Get value on attribute getTargetNodesArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] getTargetNodesArrayImpl(String endPragma) {
        return this.aPragma.getTargetNodesArrayImpl(endPragma);
    }

    /**
     * Get value on attribute name
     * @return the attribute's value
     */
    @Override
    public String getNameImpl() {
        return this.aPragma.getNameImpl();
    }

    /**
     * Get value on attribute target
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getTargetImpl() {
        return this.aPragma.getTargetImpl();
    }

    /**
     * Method used by the lara interpreter to select targets
     * @return 
     */
    @Override
    public List<? extends AJoinPoint> selectTarget() {
        return this.aPragma.selectTarget();
    }

    /**
     * Get value on attribute ast
     * @return the attribute's value
     */
    @Override
    public String getAstImpl() {
        return this.aPragma.getAstImpl();
    }

    /**
     * Get value on attribute astChildrenArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] getAstChildrenArrayImpl() {
        return this.aPragma.getAstChildrenArrayImpl();
    }

    /**
     * Get value on attribute astId
     * @return the attribute's value
     */
    @Override
    public String getAstIdImpl() {
        return this.aPragma.getAstIdImpl();
    }

    /**
     * Get value on attribute astIsInstance
     * @return the attribute's value
     */
    @Override
    public Boolean astIsInstanceImpl(String className) {
        return this.aPragma.astIsInstanceImpl(className);
    }

    /**
     * Get value on attribute astName
     * @return the attribute's value
     */
    @Override
    public String getAstNameImpl() {
        return this.aPragma.getAstNameImpl();
    }

    /**
     * Get value on attribute astNumChildren
     * @return the attribute's value
     */
    @Override
    public Integer getAstNumChildrenImpl() {
        return this.aPragma.getAstNumChildrenImpl();
    }

    /**
     * Get value on attribute bitWidth
     * @return the attribute's value
     */
    @Override
    public Integer getBitWidthImpl() {
        return this.aPragma.getBitWidthImpl();
    }

    /**
     * Get value on attribute chainArrayImpl
     * @return the attribute's value
     */
    @Override
    public String[] getChainArrayImpl() {
        return this.aPragma.getChainArrayImpl();
    }

    /**
     * Get value on attribute childrenArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] getChildrenArrayImpl() {
        return this.aPragma.getChildrenArrayImpl();
    }

    /**
     * Get value on attribute code
     * @return the attribute's value
     */
    @Override
    public String getCodeImpl() {
        return this.aPragma.getCodeImpl();
    }

    /**
     * Get value on attribute column
     * @return the attribute's value
     */
    @Override
    public Integer getColumnImpl() {
        return this.aPragma.getColumnImpl();
    }

    /**
     * Get value on attribute contains
     * @return the attribute's value
     */
    @Override
    public Boolean containsImpl(AJoinPoint jp) {
        return this.aPragma.containsImpl(jp);
    }

    /**
     * Get value on attribute currentRegion
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getCurrentRegionImpl() {
        return this.aPragma.getCurrentRegionImpl();
    }

    /**
     * Get value on attribute data
     * @return the attribute's value
     */
    @Override
    public Object getDataImpl() {
        return this.aPragma.getDataImpl();
    }

    /**
     * Get value on attribute depth
     * @return the attribute's value
     */
    @Override
    public Integer getDepthImpl() {
        return this.aPragma.getDepthImpl();
    }

    /**
     * Get value on attribute descendantsArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] getDescendantsArrayImpl() {
        return this.aPragma.getDescendantsArrayImpl();
    }

    /**
     * Get value on attribute endColumn
     * @return the attribute's value
     */
    @Override
    public Integer getEndColumnImpl() {
        return this.aPragma.getEndColumnImpl();
    }

    /**
     * Get value on attribute endLine
     * @return the attribute's value
     */
    @Override
    public Integer getEndLineImpl() {
        return this.aPragma.getEndLineImpl();
    }

    /**
     * Get value on attribute filename
     * @return the attribute's value
     */
    @Override
    public String getFilenameImpl() {
        return this.aPragma.getFilenameImpl();
    }

    /**
     * Get value on attribute filepath
     * @return the attribute's value
     */
    @Override
    public String getFilepathImpl() {
        return this.aPragma.getFilepathImpl();
    }

    /**
     * Get value on attribute firstChild
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getFirstChildImpl() {
        return this.aPragma.getFirstChildImpl();
    }

    /**
     * Get value on attribute getAncestor
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getAncestorImpl(String type) {
        return this.aPragma.getAncestorImpl(type);
    }

    /**
     * Get value on attribute getAstAncestor
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getAstAncestorImpl(String type) {
        return this.aPragma.getAstAncestorImpl(type);
    }

    /**
     * Get value on attribute getAstChild
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getAstChildImpl(int index) {
        return this.aPragma.getAstChildImpl(index);
    }

    /**
     * Get value on attribute getChainAncestor
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getChainAncestorImpl(String type) {
        return this.aPragma.getChainAncestorImpl(type);
    }

    /**
     * Get value on attribute getChild
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getChildImpl(int index) {
        return this.aPragma.getChildImpl(index);
    }

    /**
     * Get value on attribute getDescendantsArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] getDescendantsArrayImpl(String type) {
        return this.aPragma.getDescendantsArrayImpl(type);
    }

    /**
     * Get value on attribute getDescendantsAndSelfArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] getDescendantsAndSelfArrayImpl(String type) {
        return this.aPragma.getDescendantsAndSelfArrayImpl(type);
    }

    /**
     * Get value on attribute getFirstJp
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getFirstJpImpl(String type) {
        return this.aPragma.getFirstJpImpl(type);
    }

    /**
     * Get value on attribute getJavaFieldType
     * @return the attribute's value
     */
    @Override
    public String getJavaFieldTypeImpl(String fieldName) {
        return this.aPragma.getJavaFieldTypeImpl(fieldName);
    }

    /**
     * Get value on attribute getKeyType
     * @return the attribute's value
     */
    @Override
    public Object getKeyTypeImpl(String key) {
        return this.aPragma.getKeyTypeImpl(key);
    }

    /**
     * Get value on attribute getUserField
     * @return the attribute's value
     */
    @Override
    public Object getUserFieldImpl(String fieldName) {
        return this.aPragma.getUserFieldImpl(fieldName);
    }

    /**
     * Get value on attribute getValue
     * @return the attribute's value
     */
    @Override
    public Object getValueImpl(String key) {
        return this.aPragma.getValueImpl(key);
    }

    /**
     * Get value on attribute hasChildren
     * @return the attribute's value
     */
    @Override
    public Boolean getHasChildrenImpl() {
        return this.aPragma.getHasChildrenImpl();
    }

    /**
     * Get value on attribute hasNode
     * @return the attribute's value
     */
    @Override
    public Boolean hasNodeImpl(Object nodeOrJp) {
        return this.aPragma.hasNodeImpl(nodeOrJp);
    }

    /**
     * Get value on attribute hasParent
     * @return the attribute's value
     */
    @Override
    public Boolean getHasParentImpl() {
        return this.aPragma.getHasParentImpl();
    }

    /**
     * Get value on attribute hasType
     * @return the attribute's value
     */
    @Override
    public Boolean getHasTypeImpl() {
        return this.aPragma.getHasTypeImpl();
    }

    /**
     * Get value on attribute inlineCommentsArrayImpl
     * @return the attribute's value
     */
    @Override
    public AComment[] getInlineCommentsArrayImpl() {
        return this.aPragma.getInlineCommentsArrayImpl();
    }

    /**
     * Get value on attribute isCilk
     * @return the attribute's value
     */
    @Override
    public Boolean getIsCilkImpl() {
        return this.aPragma.getIsCilkImpl();
    }

    /**
     * Get value on attribute isInSystemHeader
     * @return the attribute's value
     */
    @Override
    public Boolean getIsInSystemHeaderImpl() {
        return this.aPragma.getIsInSystemHeaderImpl();
    }

    /**
     * Get value on attribute isInsideHeader
     * @return the attribute's value
     */
    @Override
    public Boolean getIsInsideHeaderImpl() {
        return this.aPragma.getIsInsideHeaderImpl();
    }

    /**
     * Get value on attribute isInsideLoopHeader
     * @return the attribute's value
     */
    @Override
    public Boolean getIsInsideLoopHeaderImpl() {
        return this.aPragma.getIsInsideLoopHeaderImpl();
    }

    /**
     * Get value on attribute isMacro
     * @return the attribute's value
     */
    @Override
    public Boolean getIsMacroImpl() {
        return this.aPragma.getIsMacroImpl();
    }

    /**
     * Get value on attribute javaFieldsArrayImpl
     * @return the attribute's value
     */
    @Override
    public String[] getJavaFieldsArrayImpl() {
        return this.aPragma.getJavaFieldsArrayImpl();
    }

    /**
     * Get value on attribute jpFieldsArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] jpFieldsArrayImpl(Boolean recursive) {
        return this.aPragma.jpFieldsArrayImpl(recursive);
    }

    /**
     * Get value on attribute jpId
     * @return the attribute's value
     */
    @Override
    public String getJpIdImpl() {
        return this.aPragma.getJpIdImpl();
    }

    /**
     * Get value on attribute keysArrayImpl
     * @return the attribute's value
     */
    @Override
    public String[] getKeysArrayImpl() {
        return this.aPragma.getKeysArrayImpl();
    }

    /**
     * Get value on attribute lastChild
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getLastChildImpl() {
        return this.aPragma.getLastChildImpl();
    }

    /**
     * Get value on attribute leftJp
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getLeftJpImpl() {
        return this.aPragma.getLeftJpImpl();
    }

    /**
     * Get value on attribute line
     * @return the attribute's value
     */
    @Override
    public Integer getLineImpl() {
        return this.aPragma.getLineImpl();
    }

    /**
     * Get value on attribute location
     * @return the attribute's value
     */
    @Override
    public String getLocationImpl() {
        return this.aPragma.getLocationImpl();
    }

    /**
     * Get value on attribute numChildren
     * @return the attribute's value
     */
    @Override
    public Integer getNumChildrenImpl() {
        return this.aPragma.getNumChildrenImpl();
    }

    /**
     * Get value on attribute originNode
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getOriginNodeImpl() {
        return this.aPragma.getOriginNodeImpl();
    }

    /**
     * Get value on attribute parent
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getParentImpl() {
        return this.aPragma.getParentImpl();
    }

    /**
     * Get value on attribute parentRegion
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getParentRegionImpl() {
        return this.aPragma.getParentRegionImpl();
    }

    /**
     * Get value on attribute pragmasArrayImpl
     * @return the attribute's value
     */
    @Override
    public APragma[] getPragmasArrayImpl() {
        return this.aPragma.getPragmasArrayImpl();
    }

    /**
     * Get value on attribute rightJp
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getRightJpImpl() {
        return this.aPragma.getRightJpImpl();
    }

    /**
     * Get value on attribute root
     * @return the attribute's value
     */
    @Override
    public AProgram getRootImpl() {
        return this.aPragma.getRootImpl();
    }

    /**
     * Get value on attribute scopeNodesArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] getScopeNodesArrayImpl() {
        return this.aPragma.getScopeNodesArrayImpl();
    }

    /**
     * Get value on attribute siblingsLeftArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] getSiblingsLeftArrayImpl() {
        return this.aPragma.getSiblingsLeftArrayImpl();
    }

    /**
     * Get value on attribute siblingsRightArrayImpl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint[] getSiblingsRightArrayImpl() {
        return this.aPragma.getSiblingsRightArrayImpl();
    }

    /**
     * Get value on attribute stmt
     * @return the attribute's value
     */
    @Override
    public AStatement getStmtImpl() {
        return this.aPragma.getStmtImpl();
    }

    /**
     * Get value on attribute type
     * @return the attribute's value
     */
    @Override
    public AType getTypeImpl() {
        return this.aPragma.getTypeImpl();
    }

    /**
     * Performs a copy of the node and its children, but not of the nodes in its fields
     */
    @Override
    public AJoinPoint copyImpl() {
        return this.aPragma.copyImpl();
    }

    /**
     * Clears all properties from the .data object
     */
    @Override
    public void dataClearImpl() {
        this.aPragma.dataClearImpl();
    }

    /**
     * Performs a copy of the node and its children, including the nodes in their fields (only the first level of field nodes, this function is not recursive)
     */
    @Override
    public AJoinPoint deepCopyImpl() {
        return this.aPragma.deepCopyImpl();
    }

    /**
     * Removes the node associated to this joinpoint from the AST
     */
    @Override
    public AJoinPoint detachImpl() {
        return this.aPragma.detachImpl();
    }

    /**
     * 
     * @param position 
     * @param code 
     */
    @Override
    public AJoinPoint[] insertImpl(String position, String code) {
        return this.aPragma.insertImpl(position, code);
    }

    /**
     * 
     * @param position 
     * @param code 
     */
    @Override
    public AJoinPoint[] insertImpl(String position, JoinPoint code) {
        return this.aPragma.insertImpl(position, code);
    }

    /**
     * Inserts the given join point after this join point
     * @param node 
     */
    @Override
    public AJoinPoint insertAfterImpl(AJoinPoint node) {
        return this.aPragma.insertAfterImpl(node);
    }

    /**
     * Overload which accepts a string
     * @param code 
     */
    @Override
    public AJoinPoint insertAfterImpl(String code) {
        return this.aPragma.insertAfterImpl(code);
    }

    /**
     * Inserts the given join point before this join point
     * @param node 
     */
    @Override
    public AJoinPoint insertBeforeImpl(AJoinPoint node) {
        return this.aPragma.insertBeforeImpl(node);
    }

    /**
     * Overload which accepts a string
     * @param node 
     */
    @Override
    public AJoinPoint insertBeforeImpl(String node) {
        return this.aPragma.insertBeforeImpl(node);
    }

    /**
     * Adds a message that will be printed to the user after weaving finishes. Identical messages are removed
     * @param message 
     */
    @Override
    public void messageToUserImpl(String message) {
        this.aPragma.messageToUserImpl(message);
    }

    /**
     * Removes the children of this node
     */
    @Override
    public void removeChildrenImpl() {
        this.aPragma.removeChildrenImpl();
    }

    /**
     * Replaces this node with the given node
     * @param node 
     */
    @Override
    public AJoinPoint replaceWithImpl(AJoinPoint node) {
        return this.aPragma.replaceWithImpl(node);
    }

    /**
     * Overload which accepts a string
     * @param node 
     */
    @Override
    public AJoinPoint replaceWithImpl(String node) {
        return this.aPragma.replaceWithImpl(node);
    }

    /**
     * Overload which accepts a list of join points
     * @param node 
     */
    @Override
    public AJoinPoint replaceWithImpl(AJoinPoint[] node) {
        return this.aPragma.replaceWithImpl(node);
    }

    /**
     * Overload which accepts a list of strings
     * @param node 
     */
    @Override
    public AJoinPoint replaceWithStringsImpl(String[] node) {
        return this.aPragma.replaceWithStringsImpl(node);
    }

    /**
     * 
     * @param content 
     */
    @Override
    public void setContentImpl(String content) {
        this.aPragma.setContentImpl(content);
    }

    /**
     * Setting data directly is not supported, this action just emits a warning and does nothing
     * @param source 
     */
    @Override
    public void setDataImpl(Object source) {
        this.aPragma.setDataImpl(source);
    }

    /**
     * Replaces the first child, or inserts the join point if no child is present. Returns the replaced child, or undefined if there was no child present.
     * @param node 
     */
    @Override
    public AJoinPoint setFirstChildImpl(AJoinPoint node) {
        return this.aPragma.setFirstChildImpl(node);
    }

    /**
     * Sets the commented that are embedded in a node
     * @param comments 
     */
    @Override
    public void setInlineCommentsImpl(String[] comments) {
        this.aPragma.setInlineCommentsImpl(comments);
    }

    /**
     * Sets the commented that are embedded in a node
     * @param comments 
     */
    @Override
    public void setInlineCommentsImpl(String comments) {
        this.aPragma.setInlineCommentsImpl(comments);
    }

    /**
     * Replaces the last child, or inserts the join point if no child is present. Returns the replaced child, or undefined if there was no child present.
     * @param node 
     */
    @Override
    public AJoinPoint setLastChildImpl(AJoinPoint node) {
        return this.aPragma.setLastChildImpl(node);
    }

    /**
     * 
     * @param name 
     */
    @Override
    public void setNameImpl(String name) {
        this.aPragma.setNameImpl(name);
    }

    /**
     * Sets the type of a node, if it has a type
     * @param type 
     */
    @Override
    public void setTypeImpl(AType type) {
        this.aPragma.setTypeImpl(type);
    }

    /**
     * Associates arbitrary values to nodes of the AST
     * @param fieldName 
     * @param value 
     */
    @Override
    public Object setUserFieldImpl(String fieldName, Object value) {
        return this.aPragma.setUserFieldImpl(fieldName, value);
    }

    /**
     * Overload which accepts a map
     * @param fieldNameAndValue 
     */
    @Override
    public Object setUserFieldImpl(Map<?, ?> fieldNameAndValue) {
        return this.aPragma.setUserFieldImpl(fieldNameAndValue);
    }

    /**
     * Sets the value associated with the given property key
     * @param key 
     * @param value 
     */
    @Override
    public AJoinPoint setValueImpl(String key, Object value) {
        return this.aPragma.setValueImpl(key, value);
    }

    /**
     * Replaces this join point with a comment with the same contents as .code
     * @param prefix 
     * @param suffix 
     */
    @Override
    public AJoinPoint toCommentImpl(String prefix, String suffix) {
        return this.aPragma.toCommentImpl(prefix, suffix);
    }

    /**
     * 
     */
    @Override
    public Optional<? extends APragma> getSuper() {
        return Optional.of(this.aPragma);
    }

    /**
     * 
     */
    @Override
    public final List<? extends JoinPoint> select(String selectName) {
        List<? extends JoinPoint> joinPointList;
        switch(selectName) {
        	case "target": 
        		joinPointList = selectTarget();
        		break;
        	default:
        		joinPointList = this.aPragma.select(selectName);
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
        case "numThreads": {
        	if(value instanceof String){
        		this.defNumThreadsImpl((String)value);
        		return;
        	}
        	this.unsupportedTypeForDef(attribute, value);
        }
        case "procBind": {
        	if(value instanceof String){
        		this.defProcBindImpl((String)value);
        		return;
        	}
        	this.unsupportedTypeForDef(attribute, value);
        }
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
    protected final void fillWithAttributes(List<String> attributes) {
        this.aPragma.fillWithAttributes(attributes);
        attributes.add("clauseKinds");
        attributes.add("collapse");
        attributes.add("copyin");
        attributes.add("default");
        attributes.add("firstprivate");
        attributes.add("getReduction");
        attributes.add("hasClause");
        attributes.add("isClauseLegal");
        attributes.add("kind");
        attributes.add("lastprivate");
        attributes.add("numThreads");
        attributes.add("ordered");
        attributes.add("private");
        attributes.add("procBind");
        attributes.add("reductionKinds");
        attributes.add("scheduleChunkSize");
        attributes.add("scheduleKind");
        attributes.add("scheduleModifiers");
        attributes.add("shared");
    }

    /**
     * 
     */
    @Override
    protected final void fillWithSelects(List<String> selects) {
        this.aPragma.fillWithSelects(selects);
    }

    /**
     * 
     */
    @Override
    protected final void fillWithActions(List<String> actions) {
        this.aPragma.fillWithActions(actions);
        actions.add("void removeClause(String)");
        actions.add("void setCollapse(String)");
        actions.add("void setCollapse(int)");
        actions.add("void setCopyin(String[])");
        actions.add("void setDefault(String)");
        actions.add("void setFirstprivate(String[])");
        actions.add("void setKind(String)");
        actions.add("void setLastprivate(String[])");
        actions.add("void setNumThreads(String)");
        actions.add("void setOrdered(String)");
        actions.add("void setPrivate(String[])");
        actions.add("void setProcBind(String)");
        actions.add("void setReduction(String, String[])");
        actions.add("void setScheduleChunkSize(String)");
        actions.add("void setScheduleChunkSize(int)");
        actions.add("void setScheduleKind(String)");
        actions.add("void setScheduleModifiers(String[])");
        actions.add("void setShared(String[])");
    }

    /**
     * Returns the join point type of this class
     * @return The join point type
     */
    @Override
    public final String get_class() {
        return "omp";
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
        return this.aPragma.instanceOf(joinpointClass);
    }
    /**
     * 
     */
    protected enum OmpAttributes {
        CLAUSEKINDS("clauseKinds"),
        COLLAPSE("collapse"),
        COPYIN("copyin"),
        DEFAULT("default"),
        FIRSTPRIVATE("firstprivate"),
        GETREDUCTION("getReduction"),
        HASCLAUSE("hasClause"),
        ISCLAUSELEGAL("isClauseLegal"),
        KIND("kind"),
        LASTPRIVATE("lastprivate"),
        NUMTHREADS("numThreads"),
        ORDERED("ordered"),
        PRIVATE("private"),
        PROCBIND("procBind"),
        REDUCTIONKINDS("reductionKinds"),
        SCHEDULECHUNKSIZE("scheduleChunkSize"),
        SCHEDULEKIND("scheduleKind"),
        SCHEDULEMODIFIERS("scheduleModifiers"),
        SHARED("shared"),
        CONTENT("content"),
        GETTARGETNODES("getTargetNodes"),
        NAME("name"),
        TARGET("target"),
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
        JPFIELDS("jpFields"),
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
        private OmpAttributes(String name){
            this.name = name;
        }
        /**
         * Return an attribute enumeration item from a given attribute name
         */
        public static Optional<OmpAttributes> fromString(String name) {
            return Arrays.asList(values()).stream().filter(attr -> attr.name.equals(name)).findAny();
        }

        /**
         * Return a list of attributes in String format
         */
        public static List<String> getNames() {
            return Arrays.asList(values()).stream().map(OmpAttributes::name).collect(Collectors.toList());
        }

        /**
         * True if the enum contains the given attribute name, false otherwise.
         */
        public static boolean contains(String name) {
            return fromString(name).isPresent();
        }
    }
}
