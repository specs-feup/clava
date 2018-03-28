package pt.up.fe.specs.clava.weaver.abstracts.joinpoints;

import org.lara.interpreter.weaver.interf.events.Stage;
import java.util.Optional;
import org.lara.interpreter.exception.AttributeException;
import javax.script.Bindings;
import java.util.List;
import org.lara.interpreter.exception.ActionException;
import java.util.Map;
import org.lara.interpreter.weaver.interf.JoinPoint;
import java.util.stream.Collectors;
import java.util.Arrays;

/**
 * Auto-Generated class for join point ACall
 * This class is overwritten by the Weaver Generator.
 * 
 * 
 * @author Lara Weaver Generator
 */
public abstract class ACall extends AExpression {

    protected AExpression aExpression;

    /**
     * 
     */
    public ACall(AExpression aExpression){
        this.aExpression = aExpression;
    }
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
     * 
     */
    public void defNameImpl(String value) {
        throw new UnsupportedOperationException("Join point "+get_class()+": Action def name with type String not implemented ");
    }

    /**
     * Get value on attribute numArgs
     * @return the attribute's value
     */
    public abstract Integer getNumArgsImpl();

    /**
     * Get value on attribute numArgs
     * @return the attribute's value
     */
    public final Object getNumArgs() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "numArgs", Optional.empty());
        	}
        	Integer result = this.getNumArgsImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "numArgs", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "numArgs", e);
        }
    }

    /**
     * Get value on attribute memberNames
     * @return the attribute's value
     */
    public abstract String[] getMemberNamesArrayImpl();

    /**
     * Get value on attribute memberNames
     * @return the attribute's value
     */
    public Bindings getMemberNamesImpl() {
        String[] stringArrayImpl0 = getMemberNamesArrayImpl();
        Bindings nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(stringArrayImpl0);
        return nativeArray0;
    }

    /**
     * Get value on attribute memberNames
     * @return the attribute's value
     */
    public final Object getMemberNames() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "memberNames", Optional.empty());
        	}
        	Bindings result = this.getMemberNamesImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "memberNames", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "memberNames", e);
        }
    }

    /**
     * a 'function' join point that represents the function declaration of the call; 'undefined' if no declaration was found
     */
    public abstract AJoinPoint getDeclarationImpl();

    /**
     * a 'function' join point that represents the function declaration of the call; 'undefined' if no declaration was found
     */
    public final Object getDeclaration() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "declaration", Optional.empty());
        	}
        	AJoinPoint result = this.getDeclarationImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "declaration", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "declaration", e);
        }
    }

    /**
     * a 'function' join point that represents the function definition of the call; 'undefined' if no definition was found
     */
    public abstract AJoinPoint getDefinitionImpl();

    /**
     * a 'function' join point that represents the function definition of the call; 'undefined' if no definition was found
     */
    public final Object getDefinition() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "definition", Optional.empty());
        	}
        	AJoinPoint result = this.getDefinitionImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "definition", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "definition", e);
        }
    }

    /**
     * Get value on attribute argList
     * @return the attribute's value
     */
    public abstract AExpression[] getArgListArrayImpl();

    /**
     * an alias for 'args'
     */
    public Bindings getArgListImpl() {
        AExpression[] aExpressionArrayImpl0 = getArgListArrayImpl();
        Bindings nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(aExpressionArrayImpl0);
        return nativeArray0;
    }

    /**
     * an alias for 'args'
     */
    public final Object getArgList() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "argList", Optional.empty());
        	}
        	Bindings result = this.getArgListImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "argList", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "argList", e);
        }
    }

    /**
     * Get value on attribute args
     * @return the attribute's value
     */
    public abstract AExpression[] getArgsArrayImpl();

    /**
     * an array with the arguments of the call
     */
    public Bindings getArgsImpl() {
        AExpression[] aExpressionArrayImpl0 = getArgsArrayImpl();
        Bindings nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(aExpressionArrayImpl0);
        return nativeArray0;
    }

    /**
     * an array with the arguments of the call
     */
    public final Object getArgs() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "args", Optional.empty());
        	}
        	Bindings result = this.getArgsImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "args", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "args", e);
        }
    }

    /**
     * 
     * @param index
     * @return 
     */
    public abstract AExpression argImpl(int index);

    /**
     * 
     * @param index
     * @return 
     */
    public final Object arg(int index) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "arg", Optional.empty(), index);
        	}
        	AExpression result = this.argImpl(index);
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "arg", Optional.ofNullable(result), index);
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "arg", e);
        }
    }

    /**
     * the return type of the call
     */
    public abstract AType getReturnTypeImpl();

    /**
     * the return type of the call
     */
    public final Object getReturnType() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "returnType", Optional.empty());
        	}
        	AType result = this.getReturnTypeImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "returnType", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "returnType", e);
        }
    }

    /**
     * the function type of the call, which includes the return type and the types of the parameters
     */
    public abstract AFunctionType getFunctionTypeImpl();

    /**
     * the function type of the call, which includes the return type and the types of the parameters
     */
    public final Object getFunctionType() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "functionType", Optional.empty());
        	}
        	AFunctionType result = this.getFunctionTypeImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "functionType", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "functionType", e);
        }
    }

    /**
     * Get value on attribute isMemberAccess
     * @return the attribute's value
     */
    public abstract Boolean getIsMemberAccessImpl();

    /**
     * Get value on attribute isMemberAccess
     * @return the attribute's value
     */
    public final Object getIsMemberAccess() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "isMemberAccess", Optional.empty());
        	}
        	Boolean result = this.getIsMemberAccessImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "isMemberAccess", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isMemberAccess", e);
        }
    }

    /**
     * Get value on attribute memberAccess
     * @return the attribute's value
     */
    public abstract AMemberAccess getMemberAccessImpl();

    /**
     * Get value on attribute memberAccess
     * @return the attribute's value
     */
    public final Object getMemberAccess() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "memberAccess", Optional.empty());
        	}
        	AMemberAccess result = this.getMemberAccessImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "memberAccess", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "memberAccess", e);
        }
    }

    /**
     * Get value on attribute isStmtCall
     * @return the attribute's value
     */
    public abstract Boolean getIsStmtCallImpl();

    /**
     * Get value on attribute isStmtCall
     * @return the attribute's value
     */
    public final Object getIsStmtCall() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "isStmtCall", Optional.empty());
        	}
        	Boolean result = this.getIsStmtCallImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "isStmtCall", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isStmtCall", e);
        }
    }

    /**
     * Method used by the lara interpreter to select callees
     * @return 
     */
    public abstract List<? extends AExpression> selectCallee();

    /**
     * Method used by the lara interpreter to select args
     * @return 
     */
    public abstract List<? extends AExpression> selectArg();

    /**
     * 
     * @param name 
     */
    public void setNameImpl(String name) {
        throw new UnsupportedOperationException(get_class()+": Action setName not implemented ");
    }

    /**
     * 
     * @param name 
     */
    public final void setName(String name) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setName", this, Optional.empty(), name);
        	}
        	this.setNameImpl(name);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setName", this, Optional.empty(), name);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setName", e);
        }
    }

    /**
     * Wraps this call with a possibly new wrapping function
     * @param name 
     */
    public void wrapImpl(String name) {
        throw new UnsupportedOperationException(get_class()+": Action wrap not implemented ");
    }

    /**
     * Wraps this call with a possibly new wrapping function
     * @param name 
     */
    public final void wrap(String name) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "wrap", this, Optional.empty(), name);
        	}
        	this.wrapImpl(name);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "wrap", this, Optional.empty(), name);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "wrap", e);
        }
    }

    /**
     * Inlines this call, if possible
     */
    public void inlineImpl() {
        throw new UnsupportedOperationException(get_class()+": Action inline not implemented ");
    }

    /**
     * Inlines this call, if possible
     */
    public final void inline() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "inline", this, Optional.empty());
        	}
        	this.inlineImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "inline", this, Optional.empty());
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "inline", e);
        }
    }

    /**
     * 
     * @param index 
     * @param expr 
     */
    public void setArgFromStringImpl(int index, String expr) {
        throw new UnsupportedOperationException(get_class()+": Action setArgFromString not implemented ");
    }

    /**
     * 
     * @param index 
     * @param expr 
     */
    public final void setArgFromString(int index, String expr) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setArgFromString", this, Optional.empty(), index, expr);
        	}
        	this.setArgFromStringImpl(index, expr);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setArgFromString", this, Optional.empty(), index, expr);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setArgFromString", e);
        }
    }

    /**
     * 
     * @param index 
     * @param expr 
     */
    public void setArgImpl(Integer index, AExpression expr) {
        throw new UnsupportedOperationException(get_class()+": Action setArg not implemented ");
    }

    /**
     * 
     * @param index 
     * @param expr 
     */
    public final void setArg(Integer index, AExpression expr) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setArg", this, Optional.empty(), index, expr);
        	}
        	this.setArgImpl(index, expr);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setArg", this, Optional.empty(), index, expr);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setArg", e);
        }
    }

    /**
     * Get value on attribute vardecl
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getVardeclImpl() {
        return this.aExpression.getVardeclImpl();
    }

    /**
     * Get value on attribute use
     * @return the attribute's value
     */
    @Override
    public String getUseImpl() {
        return this.aExpression.getUseImpl();
    }

    /**
     * Get value on attribute isFunctionArgument
     * @return the attribute's value
     */
    @Override
    public Boolean getIsFunctionArgumentImpl() {
        return this.aExpression.getIsFunctionArgumentImpl();
    }

    /**
     * Get value on attribute implicitCast
     * @return the attribute's value
     */
    @Override
    public ACast getImplicitCastImpl() {
        return this.aExpression.getImplicitCastImpl();
    }

    /**
     * Method used by the lara interpreter to select vardecls
     * @return 
     */
    @Override
    public List<? extends AVardecl> selectVardecl() {
        return this.aExpression.selectVardecl();
    }

    /**
     * 
     * @param node 
     */
    @Override
    public AJoinPoint replaceWithImpl(AJoinPoint node) {
        return this.aExpression.replaceWithImpl(node);
    }

    /**
     * 
     * @param node 
     */
    @Override
    public AJoinPoint insertBeforeImpl(AJoinPoint node) {
        return this.aExpression.insertBeforeImpl(node);
    }

    /**
     * 
     * @param node 
     */
    @Override
    public AJoinPoint insertBeforeImpl(String node) {
        return this.aExpression.insertBeforeImpl(node);
    }

    /**
     * 
     * @param node 
     */
    @Override
    public AJoinPoint insertAfterImpl(AJoinPoint node) {
        return this.aExpression.insertAfterImpl(node);
    }

    /**
     * 
     * @param code 
     */
    @Override
    public AJoinPoint insertAfterImpl(String code) {
        return this.aExpression.insertAfterImpl(code);
    }

    /**
     * 
     */
    @Override
    public void detachImpl() {
        this.aExpression.detachImpl();
    }

    /**
     * 
     * @param type 
     */
    @Override
    public void setTypeImpl(AJoinPoint type) {
        this.aExpression.setTypeImpl(type);
    }

    /**
     * 
     */
    @Override
    public AJoinPoint copyImpl() {
        return this.aExpression.copyImpl();
    }

    /**
     * 
     * @param fieldName 
     * @param value 
     */
    @Override
    public Object setUserFieldImpl(String fieldName, Object value) {
        return this.aExpression.setUserFieldImpl(fieldName, value);
    }

    /**
     * 
     * @param fieldNameAndValue 
     */
    @Override
    public Object setUserFieldImpl(Map<?, ?> fieldNameAndValue) {
        return this.aExpression.setUserFieldImpl(fieldNameAndValue);
    }

    /**
     * 
     * @param message 
     */
    @Override
    public void messageToUserImpl(String message) {
        this.aExpression.messageToUserImpl(message);
    }

    /**
     * 
     * @param position 
     * @param code 
     */
    @Override
    public void insertImpl(String position, String code) {
        this.aExpression.insertImpl(position, code);
    }

    /**
     * 
     */
    @Override
    public String toString() {
        return this.aExpression.toString();
    }

    /**
     * 
     */
    @Override
    public Optional<? extends AExpression> getSuper() {
        return Optional.of(this.aExpression);
    }

    /**
     * 
     */
    @Override
    public List<? extends JoinPoint> select(String selectName) {
        List<? extends JoinPoint> joinPointList;
        switch(selectName) {
        	case "callee": 
        		joinPointList = selectCallee();
        		break;
        	case "arg": 
        		joinPointList = selectArg();
        		break;
        	case "vardecl": 
        		joinPointList = selectVardecl();
        		break;
        	default:
        		joinPointList = this.aExpression.select(selectName);
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
        	if(value instanceof AJoinPoint){
        		this.defTypeImpl((AJoinPoint)value);
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
        default: throw new UnsupportedOperationException("Join point "+get_class()+": attribute '"+attribute+"' cannot be defined");
        }
    }

    /**
     * 
     */
    @Override
    protected void fillWithAttributes(List<String> attributes) {
        this.aExpression.fillWithAttributes(attributes);
        attributes.add("name");
        attributes.add("numArgs");
        attributes.add("memberNames");
        attributes.add("declaration");
        attributes.add("definition");
        attributes.add("argList");
        attributes.add("args");
        attributes.add("arg");
        attributes.add("returnType");
        attributes.add("functionType");
        attributes.add("isMemberAccess");
        attributes.add("memberAccess");
        attributes.add("isStmtCall");
    }

    /**
     * 
     */
    @Override
    protected void fillWithSelects(List<String> selects) {
        this.aExpression.fillWithSelects(selects);
        selects.add("callee");
        selects.add("arg");
    }

    /**
     * 
     */
    @Override
    protected void fillWithActions(List<String> actions) {
        this.aExpression.fillWithActions(actions);
        actions.add("void setName(string)");
        actions.add("void wrap(string)");
        actions.add("void inline()");
        actions.add("void setArgFromString(int, string)");
        actions.add("void setArg(Integer, expression)");
    }

    /**
     * Returns the join point type of this class
     * @return The join point type
     */
    @Override
    public String get_class() {
        return "call";
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
        return this.aExpression.instanceOf(joinpointClass);
    }
    /**
     * 
     */
    protected enum CallAttributes {
        NAME("name"),
        NUMARGS("numArgs"),
        MEMBERNAMES("memberNames"),
        DECLARATION("declaration"),
        DEFINITION("definition"),
        ARGLIST("argList"),
        ARGS("args"),
        ARG("arg"),
        RETURNTYPE("returnType"),
        FUNCTIONTYPE("functionType"),
        ISMEMBERACCESS("isMemberAccess"),
        MEMBERACCESS("memberAccess"),
        ISSTMTCALL("isStmtCall"),
        VARDECL("vardecl"),
        USE("use"),
        ISFUNCTIONARGUMENT("isFunctionArgument"),
        IMPLICITCAST("implicitCast"),
        PARENT("parent"),
        ASTANCESTOR("astAncestor"),
        AST("ast"),
        CODE("code"),
        ISINSIDELOOPHEADER("isInsideLoopHeader"),
        LINE("line"),
        DESCENDANTSANDSELF("descendantsAndSelf"),
        ASTNUMCHILDREN("astNumChildren"),
        TYPE("type"),
        DESCENDANTS("descendants"),
        ASTCHILDREN("astChildren"),
        ROOT("root"),
        JAVAVALUE("javaValue"),
        CHAINANCESTOR("chainAncestor"),
        CHAIN("chain"),
        JOINPOINTTYPE("joinpointType"),
        CURRENTREGION("currentRegion"),
        ANCESTOR("ancestor"),
        HASASTPARENT("hasAstParent"),
        ASTCHILD("astChild"),
        PARENTREGION("parentRegion"),
        ASTNAME("astName"),
        ASTID("astId"),
        CONTAINS("contains"),
        JAVAFIELDS("javaFields"),
        ASTPARENT("astParent"),
        JAVAFIELDTYPE("javaFieldType"),
        USERFIELD("userField"),
        LOCATION("location"),
        HASNODE("hasNode"),
        GETUSERFIELD("getUserField"),
        HASPARENT("hasParent");
        private String name;

        /**
         * 
         */
        private CallAttributes(String name){
            this.name = name;
        }
        /**
         * Return an attribute enumeration item from a given attribute name
         */
        public static Optional<CallAttributes> fromString(String name) {
            return Arrays.asList(values()).stream().filter(attr -> attr.name.equals(name)).findAny();
        }

        /**
         * Return a list of attributes in String format
         */
        public static List<String> getNames() {
            return Arrays.asList(values()).stream().map(CallAttributes::name).collect(Collectors.toList());
        }

        /**
         * True if the enum contains the given attribute name, false otherwise.
         */
        public static boolean contains(String name) {
            return fromString(name).isPresent();
        }
    }
}
