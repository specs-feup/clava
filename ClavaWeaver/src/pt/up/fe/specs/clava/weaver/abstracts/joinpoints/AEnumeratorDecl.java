package pt.up.fe.specs.clava.weaver.abstracts.joinpoints;

import java.util.Map;
import java.util.Optional;
import java.util.List;
import org.lara.interpreter.weaver.interf.JoinPoint;
import java.util.stream.Collectors;
import java.util.Arrays;

/**
 * Auto-Generated class for join point AEnumeratorDecl
 * This class is overwritten by the Weaver Generator.
 * 
 * 
 * @author Lara Weaver Generator
 */
public abstract class AEnumeratorDecl extends ANamedDecl {

    protected ANamedDecl aNamedDecl;

    /**
     * 
     */
    public AEnumeratorDecl(ANamedDecl aNamedDecl){
        super(aNamedDecl);
        this.aNamedDecl = aNamedDecl;
    }
    /**
     * Get value on attribute name
     * @return the attribute's value
     */
    @Override
    public String getNameImpl() {
        return this.aNamedDecl.getNameImpl();
    }

    /**
     * Get value on attribute isPublic
     * @return the attribute's value
     */
    @Override
    public Boolean getIsPublicImpl() {
        return this.aNamedDecl.getIsPublicImpl();
    }

    /**
     * Get value on attribute qualifiedPrefix
     * @return the attribute's value
     */
    @Override
    public String getQualifiedPrefixImpl() {
        return this.aNamedDecl.getQualifiedPrefixImpl();
    }

    /**
     * Get value on attribute qualifiedName
     * @return the attribute's value
     */
    @Override
    public String getQualifiedNameImpl() {
        return this.aNamedDecl.getQualifiedNameImpl();
    }

    /**
     * 
     */
    public void defNameImpl(String value) {
        this.aNamedDecl.defNameImpl(value);
    }

    /**
     * 
     * @param node 
     */
    @Override
    public AJoinPoint replaceWithImpl(AJoinPoint node) {
        return this.aNamedDecl.replaceWithImpl(node);
    }

    /**
     * 
     * @param node 
     */
    @Override
    public AJoinPoint replaceWithImpl(String node) {
        return this.aNamedDecl.replaceWithImpl(node);
    }

    /**
     * 
     * @param node 
     */
    @Override
    public AJoinPoint insertBeforeImpl(AJoinPoint node) {
        return this.aNamedDecl.insertBeforeImpl(node);
    }

    /**
     * 
     * @param node 
     */
    @Override
    public AJoinPoint insertBeforeImpl(String node) {
        return this.aNamedDecl.insertBeforeImpl(node);
    }

    /**
     * 
     * @param node 
     */
    @Override
    public AJoinPoint insertAfterImpl(AJoinPoint node) {
        return this.aNamedDecl.insertAfterImpl(node);
    }

    /**
     * 
     * @param code 
     */
    @Override
    public AJoinPoint insertAfterImpl(String code) {
        return this.aNamedDecl.insertAfterImpl(code);
    }

    /**
     * 
     */
    @Override
    public void detachImpl() {
        this.aNamedDecl.detachImpl();
    }

    /**
     * 
     * @param type 
     */
    @Override
    public void setTypeImpl(AJoinPoint type) {
        this.aNamedDecl.setTypeImpl(type);
    }

    /**
     * Performs a copy of the node and its children, but not of the nodes in its fields
     */
    @Override
    public AJoinPoint copyImpl() {
        return this.aNamedDecl.copyImpl();
    }

    /**
     * Performs a copy of the node and its children, including the nodes in their fields (only the first level of field nodes, this function is not recursive)
     */
    @Override
    public AJoinPoint deepCopyImpl() {
        return this.aNamedDecl.deepCopyImpl();
    }

    /**
     * 
     * @param fieldName 
     * @param value 
     */
    @Override
    public Object setUserFieldImpl(String fieldName, Object value) {
        return this.aNamedDecl.setUserFieldImpl(fieldName, value);
    }

    /**
     * 
     * @param fieldNameAndValue 
     */
    @Override
    public Object setUserFieldImpl(Map<?, ?> fieldNameAndValue) {
        return this.aNamedDecl.setUserFieldImpl(fieldNameAndValue);
    }

    /**
     * Sets the value associated with the given property key
     * @param key 
     * @param value 
     */
    @Override
    public AJoinPoint setValueImpl(String key, Object value) {
        return this.aNamedDecl.setValueImpl(key, value);
    }

    /**
     * 
     * @param message 
     */
    @Override
    public void messageToUserImpl(String message) {
        this.aNamedDecl.messageToUserImpl(message);
    }

    /**
     * Sets the name of this namedDecl
     * @param name 
     */
    @Override
    public void setNameImpl(String name) {
        this.aNamedDecl.setNameImpl(name);
    }

    /**
     * 
     * @param position 
     * @param code 
     */
    @Override
    public void insertImpl(String position, String code) {
        this.aNamedDecl.insertImpl(position, code);
    }

    /**
     * 
     */
    @Override
    public String toString() {
        return this.aNamedDecl.toString();
    }

    /**
     * 
     */
    @Override
    public Optional<? extends ANamedDecl> getSuper() {
        return Optional.of(this.aNamedDecl);
    }

    /**
     * 
     */
    @Override
    public final List<? extends JoinPoint> select(String selectName) {
        List<? extends JoinPoint> joinPointList;
        switch(selectName) {
        	default:
        		joinPointList = this.aNamedDecl.select(selectName);
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
    protected final void fillWithAttributes(List<String> attributes) {
        this.aNamedDecl.fillWithAttributes(attributes);
    }

    /**
     * 
     */
    @Override
    protected final void fillWithSelects(List<String> selects) {
        this.aNamedDecl.fillWithSelects(selects);
    }

    /**
     * 
     */
    @Override
    protected final void fillWithActions(List<String> actions) {
        this.aNamedDecl.fillWithActions(actions);
    }

    /**
     * Returns the join point type of this class
     * @return The join point type
     */
    @Override
    public final String get_class() {
        return "enumeratorDecl";
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
        return this.aNamedDecl.instanceOf(joinpointClass);
    }
    /**
     * 
     */
    protected enum EnumeratorDeclAttributes {
        NAME("name"),
        ISPUBLIC("isPublic"),
        QUALIFIEDPREFIX("qualifiedPrefix"),
        QUALIFIEDNAME("qualifiedName"),
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
        DESCENDANTSANDSELF("descendantsAndSelf"),
        ASTNUMCHILDREN("astNumChildren"),
        TYPE("type"),
        DESCENDANTS("descendants"),
        ASTCHILDREN("astChildren"),
        ROOT("root"),
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
        JAVAFIELDS("javaFields"),
        ASTPARENT("astParent"),
        JAVAFIELDTYPE("javaFieldType"),
        USERFIELD("userField"),
        LOCATION("location"),
        HASNODE("hasNode"),
        GETUSERFIELD("getUserField"),
        PRAGMAS("pragmas"),
        HASPARENT("hasParent");
        private String name;

        /**
         * 
         */
        private EnumeratorDeclAttributes(String name){
            this.name = name;
        }
        /**
         * Return an attribute enumeration item from a given attribute name
         */
        public static Optional<EnumeratorDeclAttributes> fromString(String name) {
            return Arrays.asList(values()).stream().filter(attr -> attr.name.equals(name)).findAny();
        }

        /**
         * Return a list of attributes in String format
         */
        public static List<String> getNames() {
            return Arrays.asList(values()).stream().map(EnumeratorDeclAttributes::name).collect(Collectors.toList());
        }

        /**
         * True if the enum contains the given attribute name, false otherwise.
         */
        public static boolean contains(String name) {
            return fromString(name).isPresent();
        }
    }
}
