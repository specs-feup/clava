package pt.up.fe.specs.clava.weaver.abstracts.joinpoints;

import org.lara.interpreter.exception.AttributeException;
import org.lara.interpreter.exception.ActionException;
import pt.up.fe.specs.clava.weaver.abstracts.ACxxWeaverJoinPoint;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.util.List;

/**
 * Auto-Generated class for join point AProgram
 * This class is overwritten by the Weaver Generator.
 * 
 * Represents the complete program and is the top-most joinpoint in the hierarchy
 * @author Lara Weaver Generator
 */
public abstract class AProgram extends ACxxWeaverJoinPoint {

    /**
     * Get value on attribute baseFolder
     * @return the attribute's value
     */
    public abstract String getBaseFolderImpl();

    /**
     * Get value on attribute baseFolder
     * @return the attribute's value
     */
    public final Object getBaseFolder() {
        try {
        	return this.getBaseFolderImpl();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "baseFolder", e);
        }
    }

    /**
     * Get value on attribute defaultFlags
     * @return the attribute's value
     */
    public abstract String[] getDefaultFlagsArrayImpl();

    /**
     * Get value on attribute defaultFlags
     * @return the attribute's value
     */
    public Object getDefaultFlagsImpl() {
        String[] stringArrayImpl0 = getDefaultFlagsArrayImpl();
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(stringArrayImpl0);
        return nativeArray0;
    }

    /**
     * Get value on attribute defaultFlags
     * @return the attribute's value
     */
    public final Object getDefaultFlags() {
        try {
        	return this.getDefaultFlagsImpl();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "defaultFlags", e);
        }
    }

    /**
     * Get value on attribute extraIncludes
     * @return the attribute's value
     */
    public abstract String[] getExtraIncludesArrayImpl();

    /**
     * paths to includes that the current program depends on
     */
    public Object getExtraIncludesImpl() {
        String[] stringArrayImpl0 = getExtraIncludesArrayImpl();
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(stringArrayImpl0);
        return nativeArray0;
    }

    /**
     * paths to includes that the current program depends on
     */
    public final Object getExtraIncludes() {
        try {
        	return this.getExtraIncludesImpl();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "extraIncludes", e);
        }
    }

    /**
     * Get value on attribute extraLibs
     * @return the attribute's value
     */
    public abstract String[] getExtraLibsArrayImpl();

    /**
     * link libraries of external projects the current program depends on
     */
    public Object getExtraLibsImpl() {
        String[] stringArrayImpl0 = getExtraLibsArrayImpl();
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(stringArrayImpl0);
        return nativeArray0;
    }

    /**
     * link libraries of external projects the current program depends on
     */
    public final Object getExtraLibs() {
        try {
        	return this.getExtraLibsImpl();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "extraLibs", e);
        }
    }

    /**
     * Get value on attribute extraProjects
     * @return the attribute's value
     */
    public abstract String[] getExtraProjectsArrayImpl();

    /**
     * paths to folders of projects that the current program depends on
     */
    public Object getExtraProjectsImpl() {
        String[] stringArrayImpl0 = getExtraProjectsArrayImpl();
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(stringArrayImpl0);
        return nativeArray0;
    }

    /**
     * paths to folders of projects that the current program depends on
     */
    public final Object getExtraProjects() {
        try {
        	return this.getExtraProjectsImpl();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "extraProjects", e);
        }
    }

    /**
     * Get value on attribute extraSources
     * @return the attribute's value
     */
    public abstract String[] getExtraSourcesArrayImpl();

    /**
     * paths to sources that the current program depends on
     */
    public Object getExtraSourcesImpl() {
        String[] stringArrayImpl0 = getExtraSourcesArrayImpl();
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(stringArrayImpl0);
        return nativeArray0;
    }

    /**
     * paths to sources that the current program depends on
     */
    public final Object getExtraSources() {
        try {
        	return this.getExtraSourcesImpl();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "extraSources", e);
        }
    }

    /**
     * Get value on attribute files
     * @return the attribute's value
     */
    public abstract AFile[] getFilesArrayImpl();

    /**
     * the source files in this program
     */
    public Object getFilesImpl() {
        AFile[] aFileArrayImpl0 = getFilesArrayImpl();
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(aFileArrayImpl0);
        return nativeArray0;
    }

    /**
     * the source files in this program
     */
    public final Object getFiles() {
        try {
        	return this.getFilesImpl();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "files", e);
        }
    }

    /**
     * Get value on attribute includeFolders
     * @return the attribute's value
     */
    public abstract String[] getIncludeFoldersArrayImpl();

    /**
     * Get value on attribute includeFolders
     * @return the attribute's value
     */
    public Object getIncludeFoldersImpl() {
        String[] stringArrayImpl0 = getIncludeFoldersArrayImpl();
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(stringArrayImpl0);
        return nativeArray0;
    }

    /**
     * Get value on attribute includeFolders
     * @return the attribute's value
     */
    public final Object getIncludeFolders() {
        try {
        	return this.getIncludeFoldersImpl();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "includeFolders", e);
        }
    }

    /**
     * true if the program was compiled with a C++ standard
     */
    public abstract Boolean getIsCxxImpl();

    /**
     * true if the program was compiled with a C++ standard
     */
    public final Object getIsCxx() {
        try {
        	return this.getIsCxxImpl();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isCxx", e);
        }
    }

    /**
     * a function join point with the main function of the program, if one is available
     */
    public abstract AFunction getMainImpl();

    /**
     * a function join point with the main function of the program, if one is available
     */
    public final Object getMain() {
        try {
        	return this.getMainImpl();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "main", e);
        }
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
        	return this.getNameImpl();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "name", e);
        }
    }

    /**
     * The name of the standard (e.g., c99, c++11)
     */
    public abstract String getStandardImpl();

    /**
     * The name of the standard (e.g., c99, c++11)
     */
    public final Object getStandard() {
        try {
        	return this.getStandardImpl();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "standard", e);
        }
    }

    /**
     * The flag of the standard (e.g., -std=c++11)
     */
    public abstract String getStdFlagImpl();

    /**
     * The flag of the standard (e.g., -std=c++11)
     */
    public final Object getStdFlag() {
        try {
        	return this.getStdFlagImpl();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "stdFlag", e);
        }
    }

    /**
     * Get value on attribute userFlags
     * @return the attribute's value
     */
    public abstract String[] getUserFlagsArrayImpl();

    /**
     * Get value on attribute userFlags
     * @return the attribute's value
     */
    public Object getUserFlagsImpl() {
        String[] stringArrayImpl0 = getUserFlagsArrayImpl();
        Object nativeArray0 = getWeaverEngine().getScriptEngine().toNativeArray(stringArrayImpl0);
        return nativeArray0;
    }

    /**
     * Get value on attribute userFlags
     * @return the attribute's value
     */
    public final Object getUserFlags() {
        try {
        	return this.getUserFlagsImpl();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "userFlags", e);
        }
    }

    /**
     * Get value on attribute weavingFolder
     * @return the attribute's value
     */
    public abstract String getWeavingFolderImpl();

    /**
     * Get value on attribute weavingFolder
     * @return the attribute's value
     */
    public final Object getWeavingFolder() {
        try {
        	return this.getWeavingFolderImpl();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "weavingFolder", e);
        }
    }

    /**
     * Adds a path to an include that the current program depends on
     * @param path 
     */
    public void addExtraIncludeImpl(String path) {
        throw new UnsupportedOperationException(get_class()+": Action addExtraInclude not implemented ");
    }

    /**
     * Adds a path to an include that the current program depends on
     * @param path 
     */
    public final void addExtraInclude(String path) {
        try {
        	this.addExtraIncludeImpl(path);
        } catch(Exception e) {
        	throw new ActionException(get_class(), "addExtraInclude", e);
        }
    }

    /**
     * Adds a path based on a git repository to an include that the current program depends on
     * @param gitRepo 
     * @param path 
     */
    public void addExtraIncludeFromGitImpl(String gitRepo, String path) {
        throw new UnsupportedOperationException(get_class()+": Action addExtraIncludeFromGit not implemented ");
    }

    /**
     * Adds a path based on a git repository to an include that the current program depends on
     * @param gitRepo 
     * @param path 
     */
    public final void addExtraIncludeFromGit(String gitRepo, String path) {
        try {
        	this.addExtraIncludeFromGitImpl(gitRepo, path);
        } catch(Exception e) {
        	throw new ActionException(get_class(), "addExtraIncludeFromGit", e);
        }
    }

    /**
     * Adds a library (e.g., -pthreads) that the current program depends on
     * @param lib 
     */
    public void addExtraLibImpl(String lib) {
        throw new UnsupportedOperationException(get_class()+": Action addExtraLib not implemented ");
    }

    /**
     * Adds a library (e.g., -pthreads) that the current program depends on
     * @param lib 
     */
    public final void addExtraLib(String lib) {
        try {
        	this.addExtraLibImpl(lib);
        } catch(Exception e) {
        	throw new ActionException(get_class(), "addExtraLib", e);
        }
    }

    /**
     * Adds a path to a source that the current program depends on
     * @param path 
     */
    public void addExtraSourceImpl(String path) {
        throw new UnsupportedOperationException(get_class()+": Action addExtraSource not implemented ");
    }

    /**
     * Adds a path to a source that the current program depends on
     * @param path 
     */
    public final void addExtraSource(String path) {
        try {
        	this.addExtraSourceImpl(path);
        } catch(Exception e) {
        	throw new ActionException(get_class(), "addExtraSource", e);
        }
    }

    /**
     * Adds a path based on a git repository to a source that the current program depends on
     * @param gitRepo 
     * @param path 
     */
    public void addExtraSourceFromGitImpl(String gitRepo, String path) {
        throw new UnsupportedOperationException(get_class()+": Action addExtraSourceFromGit not implemented ");
    }

    /**
     * Adds a path based on a git repository to a source that the current program depends on
     * @param gitRepo 
     * @param path 
     */
    public final void addExtraSourceFromGit(String gitRepo, String path) {
        try {
        	this.addExtraSourceFromGitImpl(gitRepo, path);
        } catch(Exception e) {
        	throw new ActionException(get_class(), "addExtraSourceFromGit", e);
        }
    }

    /**
     * Adds a file join point to the current program
     * @param file 
     */
    public AJoinPoint addFileImpl(AFile file) {
        throw new UnsupportedOperationException(get_class()+": Action addFile not implemented ");
    }

    /**
     * Adds a file join point to the current program
     * @param file 
     */
    public final Object addFile(AFile file) {
        try {
        	return this.addFileImpl(file);
        } catch(Exception e) {
        	throw new ActionException(get_class(), "addFile", e);
        }
    }

    /**
     * Adds a file join point to the current program, from the given path, which can be either a Java File or a String
     * @param filepath 
     */
    public AJoinPoint addFileFromPathImpl(Object filepath) {
        throw new UnsupportedOperationException(get_class()+": Action addFileFromPath not implemented ");
    }

    /**
     * Adds a file join point to the current program, from the given path, which can be either a Java File or a String
     * @param filepath 
     */
    public final Object addFileFromPath(Object filepath) {
        try {
        	return this.addFileFromPathImpl(filepath);
        } catch(Exception e) {
        	throw new ActionException(get_class(), "addFileFromPath", e);
        }
    }

    /**
     * Adds a path based on a git repository to a project that the current program depends on
     * @param gitRepo 
     * @param libs 
     * @param path 
     */
    public void addProjectFromGitImpl(String gitRepo, String[] libs, String path) {
        throw new UnsupportedOperationException(get_class()+": Action addProjectFromGit not implemented ");
    }

    /**
     * Adds a path based on a git repository to a project that the current program depends on
     * @param gitRepo 
     * @param libs 
     * @param path 
     */
    public final void addProjectFromGit(String gitRepo, Object[] libs, String path) {
        try {
        	this.addProjectFromGitImpl(gitRepo, pt.up.fe.specs.util.SpecsCollections.cast(libs, String.class), path);
        } catch(Exception e) {
        	throw new ActionException(get_class(), "addProjectFromGit", e);
        }
    }

    /**
     * Registers a function to be executed when the program exits
     * @param function 
     */
    public void atexitImpl(AFunction function) {
        throw new UnsupportedOperationException(get_class()+": Action atexit not implemented ");
    }

    /**
     * Registers a function to be executed when the program exits
     * @param function 
     */
    public final void atexit(AFunction function) {
        try {
        	this.atexitImpl(function);
        } catch(Exception e) {
        	throw new ActionException(get_class(), "atexit", e);
        }
    }

    /**
     * Discards the AST at the top of the ASt stack
     */
    public void popImpl() {
        throw new UnsupportedOperationException(get_class()+": Action pop not implemented ");
    }

    /**
     * Discards the AST at the top of the ASt stack
     */
    public final void pop() {
        try {
        	this.popImpl();
        } catch(Exception e) {
        	throw new ActionException(get_class(), "pop", e);
        }
    }

    /**
     * Creates a copy of the current AST and pushes it to the top of the AST stack
     */
    public void pushImpl() {
        throw new UnsupportedOperationException(get_class()+": Action push not implemented ");
    }

    /**
     * Creates a copy of the current AST and pushes it to the top of the AST stack
     */
    public final void push() {
        try {
        	this.pushImpl();
        } catch(Exception e) {
        	throw new ActionException(get_class(), "push", e);
        }
    }

    /**
     * Recompiles the program currently represented by the AST, transforming literal code into AST nodes. Returns true if all files could be parsed correctly, or false otherwise
     */
    public boolean rebuildImpl() {
        throw new UnsupportedOperationException(get_class()+": Action rebuild not implemented ");
    }

    /**
     * Recompiles the program currently represented by the AST, transforming literal code into AST nodes. Returns true if all files could be parsed correctly, or false otherwise
     */
    public final Object rebuild() {
        try {
        	return this.rebuildImpl();
        } catch(Exception e) {
        	throw new ActionException(get_class(), "rebuild", e);
        }
    }

    /**
     * Similar to rebuild, but tries to fix compilation errors. Resulting program may not represent the originally intended functionality
     */
    public void rebuildFuzzyImpl() {
        throw new UnsupportedOperationException(get_class()+": Action rebuildFuzzy not implemented ");
    }

    /**
     * Similar to rebuild, but tries to fix compilation errors. Resulting program may not represent the originally intended functionality
     */
    public final void rebuildFuzzy() {
        try {
        	this.rebuildFuzzyImpl();
        } catch(Exception e) {
        	throw new ActionException(get_class(), "rebuildFuzzy", e);
        }
    }

    /**
     * Returns the join point type of this class
     * @return The join point type
     */
    @Override
    public final String get_class() {
        return "program";
    }
    /**
     * 
     */
    protected enum ProgramAttributes {
        BASEFOLDER("baseFolder"),
        DEFAULTFLAGS("defaultFlags"),
        EXTRAINCLUDES("extraIncludes"),
        EXTRALIBS("extraLibs"),
        EXTRAPROJECTS("extraProjects"),
        EXTRASOURCES("extraSources"),
        FILES("files"),
        INCLUDEFOLDERS("includeFolders"),
        ISCXX("isCxx"),
        MAIN("main"),
        NAME("name"),
        STANDARD("standard"),
        STDFLAG("stdFlag"),
        USERFLAGS("userFlags"),
        WEAVINGFOLDER("weavingFolder"),
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
        private ProgramAttributes(String name){
            this.name = name;
        }
        /**
         * Return an attribute enumeration item from a given attribute name
         */
        public static Optional<ProgramAttributes> fromString(String name) {
            return Arrays.asList(values()).stream().filter(attr -> attr.name.equals(name)).findAny();
        }

        /**
         * Return a list of attributes in String format
         */
        public static List<String> getNames() {
            return Arrays.asList(values()).stream().map(ProgramAttributes::name).collect(Collectors.toList());
        }

        /**
         * True if the enum contains the given attribute name, false otherwise.
         */
        public static boolean contains(String name) {
            return fromString(name).isPresent();
        }
    }
}
