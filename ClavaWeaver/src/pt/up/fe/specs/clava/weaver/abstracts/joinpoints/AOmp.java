package pt.up.fe.specs.clava.weaver.abstracts.joinpoints;

import org.lara.interpreter.weaver.interf.events.Stage;
import java.util.Optional;
import org.lara.interpreter.exception.AttributeException;
import javax.script.Bindings;
import org.lara.interpreter.exception.ActionException;
import java.util.List;
import java.util.Map;
import org.lara.interpreter.weaver.interf.JoinPoint;
import java.util.stream.Collectors;
import java.util.Arrays;

/**
 * Auto-Generated class for join point AOmp
 * This class is overwritten by the Weaver Generator.
 * 
 * 
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
     * Get value on attribute _private
     * @return the attribute's value
     */
    public abstract String[] getPrivateArrayImpl();

    /**
     * The variable names of all private clauses, or empty array if no private clause is defined
     */
    public Bindings getPrivateImpl() {
        String[] stringArrayImpl0 = getPrivateArrayImpl();
        Bindings nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(stringArrayImpl0);
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
        	Bindings result = this.getPrivateImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "private", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "private", e);
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
     * Get value on attribute clauseKinds
     * @return the attribute's value
     */
    public abstract String[] getClauseKindsArrayImpl();

    /**
     * The names of the kinds of all clauses in the pragma, or empty array if no clause is defined
     */
    public Bindings getClauseKindsImpl() {
        String[] stringArrayImpl0 = getClauseKindsArrayImpl();
        Bindings nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(stringArrayImpl0);
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
        	Bindings result = this.getClauseKindsImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "clauseKinds", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "clauseKinds", e);
        }
    }

    /**
     * 
     * @param kind
     * @return 
     */
    public abstract String[] reductionArrayImpl(String kind);

    /**
     * 
     * @param kind
     * @return 
     */
    public Bindings reductionImpl(String kind) {
        String[] stringArrayImpl0 = reductionArrayImpl(kind);
        Bindings nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(stringArrayImpl0);
        return nativeArray0;
    }

    /**
     * 
     * @param kind
     * @return 
     */
    public final Object reduction(String kind) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "reduction", Optional.empty(), kind);
        	}
        	Bindings result = this.reductionImpl(kind);
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "reduction", Optional.ofNullable(result), kind);
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "reduction", e);
        }
    }

    /**
     * Get value on attribute reductionKinds
     * @return the attribute's value
     */
    public abstract String[] getReductionKindsArrayImpl();

    /**
     * The reduction kinds in the reductions clauses of the this pragma, or empty array if no reduction is defined
     */
    public Bindings getReductionKindsImpl() {
        String[] stringArrayImpl0 = getReductionKindsArrayImpl();
        Bindings nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(stringArrayImpl0);
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
        	Bindings result = this.getReductionKindsImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "reductionKinds", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "reductionKinds", e);
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
    public Bindings getFirstprivateImpl() {
        String[] stringArrayImpl0 = getFirstprivateArrayImpl();
        Bindings nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(stringArrayImpl0);
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
        	Bindings result = this.getFirstprivateImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "firstprivate", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "firstprivate", e);
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
    public Bindings getLastprivateImpl() {
        String[] stringArrayImpl0 = getLastprivateArrayImpl();
        Bindings nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(stringArrayImpl0);
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
        	Bindings result = this.getLastprivateImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "lastprivate", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "lastprivate", e);
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
    public Bindings getSharedImpl() {
        String[] stringArrayImpl0 = getSharedArrayImpl();
        Bindings nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(stringArrayImpl0);
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
        	Bindings result = this.getSharedImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "shared", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "shared", e);
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
    public Bindings getCopyinImpl() {
        String[] stringArrayImpl0 = getCopyinArrayImpl();
        Bindings nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(stringArrayImpl0);
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
        	Bindings result = this.getCopyinImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "copyin", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "copyin", e);
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
     * Get value on attribute scheduleModifiers
     * @return the attribute's value
     */
    public abstract String[] getScheduleModifiersArrayImpl();

    /**
     * A list with possible values of 'monotonic', 'nonmonotonic' or 'simd', or undefined if no 'schedule' clause with modifiers is defined
     */
    public Bindings getScheduleModifiersImpl() {
        String[] stringArrayImpl0 = getScheduleModifiersArrayImpl();
        Bindings nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(stringArrayImpl0);
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
        	Bindings result = this.getScheduleModifiersImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "scheduleModifiers", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "scheduleModifiers", e);
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
    public final void setPrivate(String[] newVariables) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setPrivate", this, Optional.empty(), newVariables);
        	}
        	this.setPrivateImpl(newVariables);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setPrivate", this, Optional.empty(), newVariables);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setPrivate", e);
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
    public final void setReduction(String kind, String[] newVariables) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setReduction", this, Optional.empty(), kind, newVariables);
        	}
        	this.setReductionImpl(kind, newVariables);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setReduction", this, Optional.empty(), kind, newVariables);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setReduction", e);
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
    public final void setFirstprivate(String[] newVariables) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setFirstprivate", this, Optional.empty(), newVariables);
        	}
        	this.setFirstprivateImpl(newVariables);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setFirstprivate", this, Optional.empty(), newVariables);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setFirstprivate", e);
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
    public final void setLastprivate(String[] newVariables) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setLastprivate", this, Optional.empty(), newVariables);
        	}
        	this.setLastprivateImpl(newVariables);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setLastprivate", this, Optional.empty(), newVariables);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setLastprivate", e);
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
    public final void setShared(String[] newVariables) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setShared", this, Optional.empty(), newVariables);
        	}
        	this.setSharedImpl(newVariables);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setShared", this, Optional.empty(), newVariables);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setShared", e);
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
    public final void setCopyin(String[] newVariables) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setCopyin", this, Optional.empty(), newVariables);
        	}
        	this.setCopyinImpl(newVariables);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setCopyin", this, Optional.empty(), newVariables);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setCopyin", e);
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
    public final void setScheduleModifiers(String[] modifiers) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setScheduleModifiers", this, Optional.empty(), modifiers);
        	}
        	this.setScheduleModifiersImpl(modifiers);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setScheduleModifiers", this, Optional.empty(), modifiers);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setScheduleModifiers", e);
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
     * Sets an ordered clause without parameters in the OpenMP pragma
     */
    public void setOrderedImpl() {
        throw new UnsupportedOperationException(get_class()+": Action setOrdered not implemented ");
    }

    /**
     * Sets an ordered clause without parameters in the OpenMP pragma
     */
    public final void setOrdered() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setOrdered", this, Optional.empty());
        	}
        	this.setOrderedImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setOrdered", this, Optional.empty());
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setOrdered", e);
        }
    }

    /**
     * Sets the value of the ordered clause of an OpenMP pragma
     * @param newExpr 
     */
    public void setOrderedImpl(String newExpr) {
        throw new UnsupportedOperationException(get_class()+": Action setOrdered not implemented ");
    }

    /**
     * Sets the value of the ordered clause of an OpenMP pragma
     * @param newExpr 
     */
    public final void setOrdered(String newExpr) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setOrdered", this, Optional.empty(), newExpr);
        	}
        	this.setOrderedImpl(newExpr);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setOrdered", this, Optional.empty(), newExpr);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setOrdered", e);
        }
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
     * Get value on attribute content
     * @return the attribute's value
     */
    @Override
    public String getContentImpl() {
        return this.aPragma.getContentImpl();
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
     * Replaces this join point with the given join
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
     * Removes the node associated to this joinpoint from the AST
     */
    @Override
    public void detachImpl() {
        this.aPragma.detachImpl();
    }

    /**
     * Sets the type of a node, if it has a type
     * @param type 
     */
    @Override
    public void setTypeImpl(AJoinPoint type) {
        this.aPragma.setTypeImpl(type);
    }

    /**
     * Performs a copy of the node and its children, but not of the nodes in its fields
     */
    @Override
    public AJoinPoint copyImpl() {
        return this.aPragma.copyImpl();
    }

    /**
     * Performs a copy of the node and its children, including the nodes in their fields (only the first level of field nodes, this function is not recursive)
     */
    @Override
    public AJoinPoint deepCopyImpl() {
        return this.aPragma.deepCopyImpl();
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
     * 
     * @param name 
     */
    @Override
    public void setNameImpl(String name) {
        this.aPragma.setNameImpl(name);
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
     */
    @Override
    public String toString() {
        return this.aPragma.toString();
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
        case "type": {
        	if(value instanceof AJoinPoint){
        		this.defTypeImpl((AJoinPoint)value);
        		return;
        	}
        	this.unsupportedTypeForDef(attribute, value);
        }
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
        default: throw new UnsupportedOperationException("Join point "+get_class()+": attribute '"+attribute+"' cannot be defined");
        }
    }

    /**
     * 
     */
    @Override
    protected final void fillWithAttributes(List<String> attributes) {
        this.aPragma.fillWithAttributes(attributes);
        attributes.add("kind");
        attributes.add("numThreads");
        attributes.add("procBind");
        attributes.add("private");
        attributes.add("hasClause");
        attributes.add("isClauseLegal");
        attributes.add("clauseKinds");
        attributes.add("reduction");
        attributes.add("reductionKinds");
        attributes.add("default");
        attributes.add("firstprivate");
        attributes.add("lastprivate");
        attributes.add("shared");
        attributes.add("copyin");
        attributes.add("scheduleKind");
        attributes.add("scheduleChunkSize");
        attributes.add("scheduleModifiers");
        attributes.add("collapse");
        attributes.add("ordered");
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
        actions.add("void setKind(String)");
        actions.add("void removeClause(String)");
        actions.add("void setNumThreads(String)");
        actions.add("void setProcBind(String)");
        actions.add("void setPrivate(String[])");
        actions.add("void setReduction(String, String[])");
        actions.add("void setDefault(String)");
        actions.add("void setFirstprivate(String[])");
        actions.add("void setLastprivate(String[])");
        actions.add("void setShared(String[])");
        actions.add("void setCopyin(String[])");
        actions.add("void setScheduleKind(String)");
        actions.add("void setScheduleChunkSize(String)");
        actions.add("void setScheduleModifiers(String[])");
        actions.add("void setCollapse(String)");
        actions.add("void setOrdered()");
        actions.add("void setOrdered(String)");
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
        KIND("kind"),
        NUMTHREADS("numThreads"),
        PROCBIND("procBind"),
        PRIVATE("private"),
        HASCLAUSE("hasClause"),
        ISCLAUSELEGAL("isClauseLegal"),
        CLAUSEKINDS("clauseKinds"),
        REDUCTION("reduction"),
        REDUCTIONKINDS("reductionKinds"),
        DEFAULT("default"),
        FIRSTPRIVATE("firstprivate"),
        LASTPRIVATE("lastprivate"),
        SHARED("shared"),
        COPYIN("copyin"),
        SCHEDULEKIND("scheduleKind"),
        SCHEDULECHUNKSIZE("scheduleChunkSize"),
        SCHEDULEMODIFIERS("scheduleModifiers"),
        COLLAPSE("collapse"),
        ORDERED("ordered"),
        NAME("name"),
        TARGET("target"),
        CONTENT("content"),
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
        ISINSIDEHEADER("isInsideHeader"),
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
