package pt.up.fe.specs.clava.weaver.abstracts.joinpoints;

import org.lara.interpreter.weaver.interf.events.Stage;
import java.util.Optional;
import org.lara.interpreter.exception.AttributeException;
import java.util.List;
import org.lara.interpreter.exception.ActionException;
import pt.up.fe.specs.clava.weaver.abstracts.ACxxWeaverJoinPoint;
import org.lara.interpreter.weaver.interf.JoinPoint;
import java.util.stream.Collectors;
import java.util.Arrays;

/**
 * Auto-Generated class for join point AScope
 * This class is overwritten by the Weaver Generator.
 * 
 * 
 * @author Lara Weaver Generator
 */
public abstract class AScope extends ACxxWeaverJoinPoint {

    /**
     * Get value on attribute numStatements
     * @return the attribute's value
     */
    public abstract Integer getNumStatementsImpl();

    /**
     * Get value on attribute numStatements
     * @return the attribute's value
     */
    public final Object getNumStatements() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "numStatements", Optional.empty());
        	}
        	Integer result = this.getNumStatementsImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "numStatements", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "numStatements", e);
        }
    }

    /**
     * Method used by the lara interpreter to select stmts
     * @return 
     */
    public abstract List<? extends AStatement> selectStmt();

    /**
     * Method used by the lara interpreter to select childStmts
     * @return 
     */
    public abstract List<? extends AStatement> selectChildStmt();

    /**
     * Method used by the lara interpreter to select scopes
     * @return 
     */
    public abstract List<? extends AScope> selectScope();

    /**
     * Method used by the lara interpreter to select ifs
     * @return 
     */
    public abstract List<? extends AIf> selectIf();

    /**
     * Method used by the lara interpreter to select loops
     * @return 
     */
    public abstract List<? extends ALoop> selectLoop();

    /**
     * Method used by the lara interpreter to select pragmas
     * @return 
     */
    public abstract List<? extends APragma> selectPragma();

    /**
     * Method used by the lara interpreter to select markers
     * @return 
     */
    public abstract List<? extends AMarker> selectMarker();

    /**
     * Method used by the lara interpreter to select omps
     * @return 
     */
    public abstract List<? extends AOmp> selectOmp();

    /**
     * Method used by the lara interpreter to select comments
     * @return 
     */
    public abstract List<? extends AComment> selectComment();

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
     * 
     */
    @Override
    public final List<? extends JoinPoint> select(String selectName) {
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
        	case "omp": 
        		joinPointList = selectOmp();
        		break;
        	case "comment": 
        		joinPointList = selectComment();
        		break;
        	default:
        		joinPointList = super.select(selectName);
        		break;
        }
        return joinPointList;
    }

    /**
     * 
     */
    @Override
    protected final void fillWithAttributes(List<String> attributes) {
        super.fillWithAttributes(attributes);
        attributes.add("numStatements");
    }

    /**
     * 
     */
    @Override
    protected final void fillWithSelects(List<String> selects) {
        super.fillWithSelects(selects);
        selects.add("stmt");
        selects.add("childStmt");
        selects.add("scope");
        selects.add("if");
        selects.add("loop");
        selects.add("pragma");
        selects.add("marker");
        selects.add("omp");
        selects.add("comment");
    }

    /**
     * 
     */
    @Override
    protected final void fillWithActions(List<String> actions) {
        super.fillWithActions(actions);
        actions.add("joinpoint insertBegin(joinpoint)");
        actions.add("joinpoint insertBegin(String)");
        actions.add("joinpoint insertEnd(joinpoint)");
        actions.add("joinpoint insertEnd(string)");
        actions.add("void clear()");
    }

    /**
     * Returns the join point type of this class
     * @return The join point type
     */
    @Override
    public final String get_class() {
        return "scope";
    }
    /**
     * 
     */
    protected enum ScopeAttributes {
        NUMSTATEMENTS("numStatements"),
        PARENT("parent"),
        ASTANCESTOR("astAncestor"),
        AST("ast"),
        CODE("code"),
        ISINSIDELOOPHEADER("isInsideLoopHeader"),
        LINE("line"),
        ASTNUMCHILDREN("astNumChildren"),
        TYPE("type"),
        ROOT("root"),
        JAVAVALUE("javaValue"),
        CHAINANCESTOR("chainAncestor"),
        CHAIN("chain"),
        JOINPOINTTYPE("joinpointType"),
        CURRENTREGION("currentRegion"),
        ANCESTOR("ancestor"),
        ASTCHILD("astChild"),
        PARENTREGION("parentRegion"),
        ASTNAME("astName"),
        ASTID("astId"),
        CONTAINS("contains"),
        JAVAFIELDS("javaFields"),
        ASTPARENT("astParent"),
        SETUSERFIELD("setUserField"),
        JAVAFIELDTYPE("javaFieldType"),
        LOCATION("location"),
        GETUSERFIELD("getUserField"),
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
