package pt.up.fe.specs.clava.weaver.abstracts.joinpoints;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.lara.interpreter.weaver.interf.JoinPoint;
import java.util.stream.Collectors;
import java.util.Arrays;

/**
 * Auto-Generated class for join point AParam
 * This class is overwritten by the Weaver Generator.
 * 
 * 
 * @author Lara Weaver Generator
 */
public abstract class AParam extends AVardecl {

    protected AVardecl aVardecl;

    /**
     * 
     */
    public AParam(AVardecl aVardecl){
        super(aVardecl);
        this.aVardecl = aVardecl;
    }
    /**
     * Get value on attribute hasInit
     * @return the attribute's value
     */
    @Override
    public Boolean getHasInitImpl() {
        return this.aVardecl.getHasInitImpl();
    }

    /**
     * Get value on attribute init
     * @return the attribute's value
     */
    @Override
    public AJoinPoint getInitImpl() {
        return this.aVardecl.getInitImpl();
    }

    /**
     * Get value on attribute isParam
     * @return the attribute's value
     */
    @Override
    public Boolean getIsParamImpl() {
        return this.aVardecl.getIsParamImpl();
    }

    /**
     * Get value on attribute storageClass
     * @return the attribute's value
     */
    @Override
    public String getStorageClassImpl() {
        return this.aVardecl.getStorageClassImpl();
    }

    /**
     * Method used by the lara interpreter to select inits
     * @return 
     */
    @Override
    public List<? extends AExpression> selectInit() {
        return this.aVardecl.selectInit();
    }

    /**
     * Get value on attribute name
     * @return the attribute's value
     */
    @Override
    public String getNameImpl() {
        return this.aVardecl.getNameImpl();
    }

    /**
     * Get value on attribute isPublic
     * @return the attribute's value
     */
    @Override
    public Boolean getIsPublicImpl() {
        return this.aVardecl.getIsPublicImpl();
    }

    /**
     * 
     */
    public void defNameImpl(String value) {
        this.aVardecl.defNameImpl(value);
    }

    /**
     * 
     * @param node 
     */
    @Override
    public AJoinPoint replaceWithImpl(AJoinPoint node) {
        return this.aVardecl.replaceWithImpl(node);
    }

    /**
     * 
     * @param node 
     */
    @Override
    public AJoinPoint insertBeforeImpl(AJoinPoint node) {
        return this.aVardecl.insertBeforeImpl(node);
    }

    /**
     * 
     * @param node 
     */
    @Override
    public AJoinPoint insertBeforeImpl(String node) {
        return this.aVardecl.insertBeforeImpl(node);
    }

    /**
     * 
     * @param node 
     */
    @Override
    public AJoinPoint insertAfterImpl(AJoinPoint node) {
        return this.aVardecl.insertAfterImpl(node);
    }

    /**
     * 
     * @param code 
     */
    @Override
    public AJoinPoint insertAfterImpl(String code) {
        return this.aVardecl.insertAfterImpl(code);
    }

    /**
     * 
     */
    @Override
    public void detachImpl() {
        this.aVardecl.detachImpl();
    }

    /**
     * 
     * @param type 
     */
    @Override
    public void setTypeImpl(AJoinPoint type) {
        this.aVardecl.setTypeImpl(type);
    }

    /**
     * 
     */
    @Override
    public AJoinPoint copyImpl() {
        return this.aVardecl.copyImpl();
    }

    /**
     * 
     * @param fieldName 
     * @param value 
     */
    @Override
    public Object setUserFieldImpl(String fieldName, Object value) {
        return this.aVardecl.setUserFieldImpl(fieldName, value);
    }

    /**
     * 
     * @param fieldNameAndValue 
     */
    @Override
    public Object setUserFieldImpl(Map<?, ?> fieldNameAndValue) {
        return this.aVardecl.setUserFieldImpl(fieldNameAndValue);
    }

    /**
     * 
     * @param message 
     */
    @Override
    public void messageToUserImpl(String message) {
        this.aVardecl.messageToUserImpl(message);
    }

    /**
     * 
     * @param init 
     */
    @Override
    public void setInitImpl(AJoinPoint init) {
        this.aVardecl.setInitImpl(init);
    }

    /**
     * 
     * @param init 
     */
    @Override
    public void setInitImpl(String init) {
        this.aVardecl.setInitImpl(init);
    }

    /**
     * 
     * @param position 
     * @param code 
     */
    @Override
    public void insertImpl(String position, String code) {
        this.aVardecl.insertImpl(position, code);
    }

    /**
     * 
     */
    @Override
    public String toString() {
        return this.aVardecl.toString();
    }

    /**
     * 
     */
    @Override
    public Optional<? extends AVardecl> getSuper() {
        return Optional.of(this.aVardecl);
    }

    /**
     * 
     */
    @Override
    public final List<? extends JoinPoint> select(String selectName) {
        List<? extends JoinPoint> joinPointList;
        switch(selectName) {
        	case "init": 
        		joinPointList = selectInit();
        		break;
        	default:
        		joinPointList = this.aVardecl.select(selectName);
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
        this.aVardecl.fillWithAttributes(attributes);
    }

    /**
     * 
     */
    @Override
    protected final void fillWithSelects(List<String> selects) {
        this.aVardecl.fillWithSelects(selects);
    }

    /**
     * 
     */
    @Override
    protected final void fillWithActions(List<String> actions) {
        this.aVardecl.fillWithActions(actions);
    }

    /**
     * Returns the join point type of this class
     * @return The join point type
     */
    @Override
    public final String get_class() {
        return "param";
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
        return this.aVardecl.instanceOf(joinpointClass);
    }
    /**
     * 
     */
    protected enum ParamAttributes {
        HASINIT("hasInit"),
        INIT("init"),
        ISPARAM("isParam"),
        STORAGECLASS("storageClass"),
        NAME("name"),
        ISPUBLIC("isPublic"),
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
        ASTISINSTANCE("astIsInstance"),
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
        private ParamAttributes(String name){
            this.name = name;
        }
        /**
         * Return an attribute enumeration item from a given attribute name
         */
        public static Optional<ParamAttributes> fromString(String name) {
            return Arrays.asList(values()).stream().filter(attr -> attr.name.equals(name)).findAny();
        }

        /**
         * Return a list of attributes in String format
         */
        public static List<String> getNames() {
            return Arrays.asList(values()).stream().map(ParamAttributes::name).collect(Collectors.toList());
        }

        /**
         * True if the enum contains the given attribute name, false otherwise.
         */
        public static boolean contains(String name) {
            return fromString(name).isPresent();
        }
    }
}
