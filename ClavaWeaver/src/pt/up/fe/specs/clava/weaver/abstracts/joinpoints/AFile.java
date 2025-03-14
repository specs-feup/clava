package pt.up.fe.specs.clava.weaver.abstracts.joinpoints;

import org.lara.interpreter.exception.AttributeException;
import org.lara.interpreter.exception.ActionException;
import pt.up.fe.specs.clava.weaver.abstracts.ACxxWeaverJoinPoint;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.util.List;

/**
 * Auto-Generated class for join point AFile
 * This class is overwritten by the Weaver Generator.
 * 
 * Represents a source file (.c, .cpp., .cl, etc)
 * @author Lara Weaver Generator
 */
public abstract class AFile extends ACxxWeaverJoinPoint {

    /**
     * the path to the source folder that was given as the base folder of this file
     */
    public abstract String getBaseSourcePathImpl();

    /**
     * the path to the source folder that was given as the base folder of this file
     */
    public final Object getBaseSourcePath() {
        try {
        	return this.getBaseSourcePathImpl();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "baseSourcePath", e);
        }
    }

    /**
     * the output of the parser if there were errors during parsing
     */
    public abstract String getErrorOutputImpl();

    /**
     * the output of the parser if there were errors during parsing
     */
    public final Object getErrorOutput() {
        try {
        	return this.getErrorOutputImpl();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "errorOutput", e);
        }
    }

    /**
     * a Java file to the file that originated this translation unit
     */
    public abstract Object getFileImpl();

    /**
     * a Java file to the file that originated this translation unit
     */
    public final Object getFile() {
        try {
        	return this.getFileImpl();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "file", e);
        }
    }

    /**
     * 
     * @param destinationFolderpath
     * @return 
     */
    public abstract String getDestinationFilepathImpl(String destinationFolderpath);

    /**
     * 
     * @param destinationFolderpath
     * @return 
     */
    public final Object getDestinationFilepath(String destinationFolderpath) {
        try {
        	return this.getDestinationFilepathImpl(destinationFolderpath);
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "getDestinationFilepath", e);
        }
    }

    /**
     * true if this file contains a 'main' method
     */
    public abstract Boolean getHasMainImpl();

    /**
     * true if this file contains a 'main' method
     */
    public final Object getHasMain() {
        try {
        	return this.getHasMainImpl();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "hasMain", e);
        }
    }

    /**
     * true if there were errors during parsing
     */
    public abstract Boolean getHasParsingErrorsImpl();

    /**
     * true if there were errors during parsing
     */
    public final Object getHasParsingErrors() {
        try {
        	return this.getHasParsingErrorsImpl();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "hasParsingErrors", e);
        }
    }

    /**
     * Get value on attribute includes
     * @return the attribute's value
     */
    public abstract AInclude[] getIncludesArrayImpl();

    /**
     * the includes of this file
     */
    public Object getIncludesImpl() {
        AInclude[] aIncludeArrayImpl0 = getIncludesArrayImpl();
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(aIncludeArrayImpl0);
        return nativeArray0;
    }

    /**
     * the includes of this file
     */
    public final Object getIncludes() {
        try {
        	return this.getIncludesImpl();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "includes", e);
        }
    }

    /**
     * true if this file is considered a C++ file
     */
    public abstract Boolean getIsCxxImpl();

    /**
     * true if this file is considered a C++ file
     */
    public final Object getIsCxx() {
        try {
        	return this.getIsCxxImpl();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isCxx", e);
        }
    }

    /**
     * true if this file is considered a header file
     */
    public abstract Boolean getIsHeaderImpl();

    /**
     * true if this file is considered a header file
     */
    public final Object getIsHeader() {
        try {
        	return this.getIsHeaderImpl();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isHeader", e);
        }
    }

    /**
     * true if this file is an OpenCL filetype
     */
    public abstract Boolean getIsOpenCLImpl();

    /**
     * true if this file is an OpenCL filetype
     */
    public final Object getIsOpenCL() {
        try {
        	return this.getIsOpenCLImpl();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isOpenCL", e);
        }
    }

    /**
     * the name of the file
     */
    public abstract String getNameImpl();

    /**
     * the name of the file
     */
    public final Object getName() {
        try {
        	return this.getNameImpl();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "name", e);
        }
    }

    /**
     * the folder of the source file
     */
    public abstract String getPathImpl();

    /**
     * the folder of the source file
     */
    public final Object getPath() {
        try {
        	return this.getPathImpl();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "path", e);
        }
    }

    /**
     * the path to the file relative to the base source path
     */
    public abstract String getRelativeFilepathImpl();

    /**
     * the path to the file relative to the base source path
     */
    public final Object getRelativeFilepath() {
        try {
        	return this.getRelativeFilepathImpl();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "relativeFilepath", e);
        }
    }

    /**
     * the path to the folder of the source file relative to the base source path
     */
    public abstract String getRelativeFolderpathImpl();

    /**
     * the path to the folder of the source file relative to the base source path
     */
    public final Object getRelativeFolderpath() {
        try {
        	return this.getRelativeFolderpathImpl();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "relativeFolderpath", e);
        }
    }

    /**
     * the name of the source folder of this file, or undefined if it has none
     */
    public abstract String getSourceFoldernameImpl();

    /**
     * the name of the source folder of this file, or undefined if it has none
     */
    public final Object getSourceFoldername() {
        try {
        	return this.getSourceFoldernameImpl();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "sourceFoldername", e);
        }
    }

    /**
     * Adds a C include to the current file. If the file already has the include, it does nothing
     * @param name 
     * @param isAngled 
     */
    public void addCIncludeImpl(String name, boolean isAngled) {
        throw new UnsupportedOperationException(get_class()+": Action addCInclude not implemented ");
    }

    /**
     * Adds a C include to the current file. If the file already has the include, it does nothing
     * @param name 
     * @param isAngled 
     */
    public final void addCInclude(String name, boolean isAngled) {
        try {
        	this.addCIncludeImpl(name, isAngled);
        } catch(Exception e) {
        	throw new ActionException(get_class(), "addCInclude", e);
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
    public final Object addFunction(String name) {
        try {
        	return this.addFunctionImpl(name);
        } catch(Exception e) {
        	throw new ActionException(get_class(), "addFunction", e);
        }
    }

    /**
     * Adds a global variable to this file
     * @param name 
     * @param type 
     * @param initValue 
     */
    public AVardecl addGlobalImpl(String name, AJoinPoint type, String initValue) {
        throw new UnsupportedOperationException(get_class()+": Action addGlobal not implemented ");
    }

    /**
     * Adds a global variable to this file
     * @param name 
     * @param type 
     * @param initValue 
     */
    public final Object addGlobal(String name, AJoinPoint type, String initValue) {
        try {
        	return this.addGlobalImpl(name, type, initValue);
        } catch(Exception e) {
        	throw new ActionException(get_class(), "addGlobal", e);
        }
    }

    /**
     * Adds an include to the current file. If the file already has the include, it does nothing
     * @param name 
     * @param isAngled 
     */
    public void addIncludeImpl(String name, boolean isAngled) {
        throw new UnsupportedOperationException(get_class()+": Action addInclude not implemented ");
    }

    /**
     * Adds an include to the current file. If the file already has the include, it does nothing
     * @param name 
     * @param isAngled 
     */
    public final void addInclude(String name, boolean isAngled) {
        try {
        	this.addIncludeImpl(name, isAngled);
        } catch(Exception e) {
        	throw new ActionException(get_class(), "addInclude", e);
        }
    }

    /**
     * Overload of addInclude which accepts a join point
     * @param jp 
     */
    public void addIncludeJpImpl(AJoinPoint jp) {
        throw new UnsupportedOperationException(get_class()+": Action addIncludeJp not implemented ");
    }

    /**
     * Overload of addInclude which accepts a join point
     * @param jp 
     */
    public final void addIncludeJp(AJoinPoint jp) {
        try {
        	this.addIncludeJpImpl(jp);
        } catch(Exception e) {
        	throw new ActionException(get_class(), "addIncludeJp", e);
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
        	this.insertBeginImpl(node);
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
        	this.insertBeginImpl(code);
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
        	this.insertEndImpl(node);
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
        	this.insertEndImpl(code);
        } catch(Exception e) {
        	throw new ActionException(get_class(), "insertEnd", e);
        }
    }

    /**
     * Recompiles only this file, returns a join point to the new recompiled file, or throws an exception if a problem happens
     */
    public AFile rebuildImpl() {
        throw new UnsupportedOperationException(get_class()+": Action rebuild not implemented ");
    }

    /**
     * Recompiles only this file, returns a join point to the new recompiled file, or throws an exception if a problem happens
     */
    public final Object rebuild() {
        try {
        	return this.rebuildImpl();
        } catch(Exception e) {
        	throw new ActionException(get_class(), "rebuild", e);
        }
    }

    /**
     * Recompiles only this file, returns a join point to the new recompiled file, or returns a clavaException join point if a problem happens
     */
    public AJoinPoint rebuildTryImpl() {
        throw new UnsupportedOperationException(get_class()+": Action rebuildTry not implemented ");
    }

    /**
     * Recompiles only this file, returns a join point to the new recompiled file, or returns a clavaException join point if a problem happens
     */
    public final Object rebuildTry() {
        try {
        	return this.rebuildTryImpl();
        } catch(Exception e) {
        	throw new ActionException(get_class(), "rebuildTry", e);
        }
    }

    /**
     * Changes the name of the file
     * @param filename 
     */
    public void setNameImpl(String filename) {
        throw new UnsupportedOperationException(get_class()+": Action setName not implemented ");
    }

    /**
     * Changes the name of the file
     * @param filename 
     */
    public final void setName(String filename) {
        try {
        	this.setNameImpl(filename);
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setName", e);
        }
    }

    /**
     * Sets the path to the folder of the source file relative to the base source path
     * @param path 
     */
    public void setRelativeFolderpathImpl(String path) {
        throw new UnsupportedOperationException(get_class()+": Action setRelativeFolderpath not implemented ");
    }

    /**
     * Sets the path to the folder of the source file relative to the base source path
     * @param path 
     */
    public final void setRelativeFolderpath(String path) {
        try {
        	this.setRelativeFolderpathImpl(path);
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setRelativeFolderpath", e);
        }
    }

    /**
     * Writes the code of this file to a given folder
     * @param destinationFoldername 
     */
    public String writeImpl(String destinationFoldername) {
        throw new UnsupportedOperationException(get_class()+": Action write not implemented ");
    }

    /**
     * Writes the code of this file to a given folder
     * @param destinationFoldername 
     */
    public final Object write(String destinationFoldername) {
        try {
        	return this.writeImpl(destinationFoldername);
        } catch(Exception e) {
        	throw new ActionException(get_class(), "write", e);
        }
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
        BASESOURCEPATH("baseSourcePath"),
        ERROROUTPUT("errorOutput"),
        FILE("file"),
        GETDESTINATIONFILEPATH("getDestinationFilepath"),
        HASMAIN("hasMain"),
        HASPARSINGERRORS("hasParsingErrors"),
        INCLUDES("includes"),
        ISCXX("isCxx"),
        ISHEADER("isHeader"),
        ISOPENCL("isOpenCL"),
        NAME("name"),
        PATH("path"),
        RELATIVEFILEPATH("relativeFilepath"),
        RELATIVEFOLDERPATH("relativeFolderpath"),
        SOURCEFOLDERNAME("sourceFoldername"),
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
