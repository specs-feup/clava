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
 * Auto-Generated class for join point AFile
 * This class is overwritten by the Weaver Generator.
 * 
 * 
 * @author Lara Weaver Generator
 */
public abstract class AFile extends ACxxWeaverJoinPoint {

    /**
     * Get value on attribute name
     * @return the attribute's value
     */
    public abstract String getNameImpl();

    /**
     * Get value on attribute name
     * @return the attribute's value
     */
    public final Object getName() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "name", Optional.empty());
        	}
        	String result = this.getNameImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "name", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "name", e);
        }
    }

    /**
     * Get value on attribute hasMain
     * @return the attribute's value
     */
    public abstract Boolean getHasMainImpl();

    /**
     * Get value on attribute hasMain
     * @return the attribute's value
     */
    public final Object getHasMain() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "hasMain", Optional.empty());
        	}
        	Boolean result = this.getHasMainImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "hasMain", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "hasMain", e);
        }
    }

    /**
     * Get value on attribute path
     * @return the attribute's value
     */
    public abstract String getPathImpl();

    /**
     * Get value on attribute path
     * @return the attribute's value
     */
    public final Object getPath() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "path", Optional.empty());
        	}
        	String result = this.getPathImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "path", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "path", e);
        }
    }

    /**
     * Get value on attribute filepath
     * @return the attribute's value
     */
    public abstract String getFilepathImpl();

    /**
     * Get value on attribute filepath
     * @return the attribute's value
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
     * Get value on attribute isCxx
     * @return the attribute's value
     */
    public abstract Boolean getIsCxxImpl();

    /**
     * Get value on attribute isCxx
     * @return the attribute's value
     */
    public final Object getIsCxx() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "isCxx", Optional.empty());
        	}
        	Boolean result = this.getIsCxxImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "isCxx", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isCxx", e);
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
     * Method used by the lara interpreter to select functions
     * @return 
     */
    public abstract List<? extends AFunction> selectFunction();

    /**
     * Method used by the lara interpreter to select records
     * @return 
     */
    public abstract List<? extends ARecord> selectRecord();

    /**
     * Method used by the lara interpreter to select structs
     * @return 
     */
    public abstract List<? extends AStruct> selectStruct();

    /**
     * Method used by the lara interpreter to select classs
     * @return 
     */
    public abstract List<? extends AClass> selectClass();

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
     * Method used by the lara interpreter to select vardecls
     * @return 
     */
    public abstract List<? extends AVardecl> selectVardecl();

    /**
     * Method used by the lara interpreter to select comments
     * @return 
     */
    public abstract List<? extends AComment> selectComment();

    /**
     * 
     * @param name 
     * @param isAngled 
     */
    public void addIncludeImpl(String name, boolean isAngled) {
        throw new UnsupportedOperationException(get_class()+": Action addInclude not implemented ");
    }

    /**
     * 
     * @param name 
     * @param isAngled 
     */
    public final void addInclude(String name, boolean isAngled) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "addInclude", this, Optional.empty(), name, isAngled);
        	}
        	this.addIncludeImpl(name, isAngled);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "addInclude", this, Optional.empty(), name, isAngled);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "addInclude", e);
        }
    }

    /**
     * 
     * @param name 
     */
    public void addIncludeImpl(String name) {
        throw new UnsupportedOperationException(get_class()+": Action addInclude not implemented ");
    }

    /**
     * 
     * @param name 
     */
    public final void addInclude(String name) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "addInclude", this, Optional.empty(), name);
        	}
        	this.addIncludeImpl(name);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "addInclude", this, Optional.empty(), name);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "addInclude", e);
        }
    }

    /**
     * 
     * @param jp 
     */
    public void addIncludeJpImpl(AJoinPoint jp) {
        throw new UnsupportedOperationException(get_class()+": Action addIncludeJp not implemented ");
    }

    /**
     * 
     * @param jp 
     */
    public final void addIncludeJp(AJoinPoint jp) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "addIncludeJp", this, Optional.empty(), jp);
        	}
        	this.addIncludeJpImpl(jp);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "addIncludeJp", this, Optional.empty(), jp);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "addIncludeJp", e);
        }
    }

    /**
     * 
     * @param name 
     * @param type 
     * @param initValue 
     */
    public void addGlobalImpl(String name, AJoinPoint type, String initValue) {
        throw new UnsupportedOperationException(get_class()+": Action addGlobal not implemented ");
    }

    /**
     * 
     * @param name 
     * @param type 
     * @param initValue 
     */
    public final void addGlobal(String name, AJoinPoint type, String initValue) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "addGlobal", this, Optional.empty(), name, type, initValue);
        	}
        	this.addGlobalImpl(name, type, initValue);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "addGlobal", this, Optional.empty(), name, type, initValue);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "addGlobal", e);
        }
    }

    /**
     * Adds the node in the join point to the start of the file
     * @param node 
     */
    public void insertBeginImpl(AJoinPoint node) {
        throw new UnsupportedOperationException(get_class()+": Action insertBegin not implemented ");
    }

    /**
     * Adds the node in the join point to the start of the file
     * @param node 
     */
    public final void insertBegin(AJoinPoint node) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "insertBegin", this, Optional.empty(), node);
        	}
        	this.insertBeginImpl(node);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "insertBegin", this, Optional.empty(), node);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "insertBegin", e);
        }
    }

    /**
     * Adds the String as a Decl to the end of the file
     * @param code 
     */
    public void insertBeginImpl(String code) {
        throw new UnsupportedOperationException(get_class()+": Action insertBegin not implemented ");
    }

    /**
     * Adds the String as a Decl to the end of the file
     * @param code 
     */
    public final void insertBegin(String code) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "insertBegin", this, Optional.empty(), code);
        	}
        	this.insertBeginImpl(code);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "insertBegin", this, Optional.empty(), code);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "insertBegin", e);
        }
    }

    /**
     * Adds the node in the join point to the end of the file
     * @param node 
     */
    public void insertEndImpl(AJoinPoint node) {
        throw new UnsupportedOperationException(get_class()+": Action insertEnd not implemented ");
    }

    /**
     * Adds the node in the join point to the end of the file
     * @param node 
     */
    public final void insertEnd(AJoinPoint node) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "insertEnd", this, Optional.empty(), node);
        	}
        	this.insertEndImpl(node);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "insertEnd", this, Optional.empty(), node);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "insertEnd", e);
        }
    }

    /**
     * Adds the String as a Decl to the end of the file
     * @param code 
     */
    public void insertEndImpl(String code) {
        throw new UnsupportedOperationException(get_class()+": Action insertEnd not implemented ");
    }

    /**
     * Adds the String as a Decl to the end of the file
     * @param code 
     */
    public final void insertEnd(String code) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "insertEnd", this, Optional.empty(), code);
        	}
        	this.insertEndImpl(code);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "insertEnd", this, Optional.empty(), code);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "insertEnd", e);
        }
    }

    /**
     * Adds a function to the file that returns void and has no parameters
     * @param name 
     */
    public AJoinPoint addFunctionImpl(String name) {
        throw new UnsupportedOperationException(get_class()+": Action addFunction not implemented ");
    }

    /**
     * Adds a function to the file that returns void and has no parameters
     * @param name 
     */
    public final AJoinPoint addFunction(String name) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "addFunction", this, Optional.empty(), name);
        	}
        	AJoinPoint result = this.addFunctionImpl(name);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "addFunction", this, Optional.ofNullable(result), name);
        	}
        	return result;
        } catch(Exception e) {
        	throw new ActionException(get_class(), "addFunction", e);
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
        	case "function": 
        		joinPointList = selectFunction();
        		break;
        	case "record": 
        		joinPointList = selectRecord();
        		break;
        	case "struct": 
        		joinPointList = selectStruct();
        		break;
        	case "class": 
        		joinPointList = selectClass();
        		break;
        	case "pragma": 
        		joinPointList = selectPragma();
        		break;
        	case "marker": 
        		joinPointList = selectMarker();
        		break;
        	case "vardecl": 
        		joinPointList = selectVardecl();
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
        attributes.add("name");
        attributes.add("hasMain");
        attributes.add("path");
        attributes.add("filepath");
        attributes.add("isCxx");
    }

    /**
     * 
     */
    @Override
    protected final void fillWithSelects(List<String> selects) {
        super.fillWithSelects(selects);
        selects.add("stmt");
        selects.add("childStmt");
        selects.add("function");
        selects.add("record");
        selects.add("struct");
        selects.add("class");
        selects.add("pragma");
        selects.add("marker");
        selects.add("vardecl");
        selects.add("comment");
    }

    /**
     * 
     */
    @Override
    protected final void fillWithActions(List<String> actions) {
        super.fillWithActions(actions);
        actions.add("void addInclude(string, boolean)");
        actions.add("void addInclude(string)");
        actions.add("void addIncludeJp(joinpoint)");
        actions.add("void addGlobal(String, joinpoint, String)");
        actions.add("void insertBegin(joinpoint)");
        actions.add("void insertBegin(String)");
        actions.add("void insertEnd(joinpoint)");
        actions.add("void insertEnd(String)");
        actions.add("joinpoint addFunction(String)");
    }

    /**
     * Returns the join point type of this class
     * @return The join point type
     */
    @Override
    public final String get_class() {
        return "file";
    }
    /**
     * 
     */
    protected enum FileAttributes {
        NAME("name"),
        HASMAIN("hasMain"),
        PATH("path"),
        FILEPATH("filepath"),
        ISCXX("isCxx"),
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
        private FileAttributes(String name){
            this.name = name;
        }
        /**
         * Return an attribute enumeration item from a given attribute name
         */
        public static Optional<FileAttributes> fromString(String name) {
            return Arrays.asList(values()).stream().filter(attr -> attr.name.equals(name)).findAny();
        }

        /**
         * Return a list of attributes in String format
         */
        public static List<String> getNames() {
            return Arrays.asList(values()).stream().map(FileAttributes::name).collect(Collectors.toList());
        }

        /**
         * True if the enum contains the given attribute name, false otherwise.
         */
        public static boolean contains(String name) {
            return fromString(name).isPresent();
        }
    }
}
