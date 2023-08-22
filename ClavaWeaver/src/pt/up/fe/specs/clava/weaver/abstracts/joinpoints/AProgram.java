package pt.up.fe.specs.clava.weaver.abstracts.joinpoints;

import org.lara.interpreter.weaver.interf.events.Stage;
import java.util.Optional;
import org.lara.interpreter.exception.AttributeException;
import java.util.List;
import org.lara.interpreter.weaver.interf.SelectOp;
import org.lara.interpreter.exception.ActionException;
import pt.up.fe.specs.clava.weaver.abstracts.ACxxWeaverJoinPoint;
import org.lara.interpreter.weaver.interf.JoinPoint;
import java.util.stream.Collectors;
import java.util.Arrays;

/**
 * Auto-Generated class for join point AProgram
 * This class is overwritten by the Weaver Generator.
 * 
 * Represents the complete program and is the top-most joinpoint in the hierarchy
 * @author Lara Weaver Generator
 */
public abstract class AProgram extends ACxxWeaverJoinPoint {

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
     * true if the program was compiled with a C++ standard
     */
    public abstract Boolean getIsCxxImpl();

    /**
     * true if the program was compiled with a C++ standard
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
     * The name of the standard (e.g., c99, c++11)
     */
    public abstract String getStandardImpl();

    /**
     * The name of the standard (e.g., c99, c++11)
     */
    public final Object getStandard() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "standard", Optional.empty());
        	}
        	String result = this.getStandardImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "standard", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
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
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "stdFlag", Optional.empty());
        	}
        	String result = this.getStdFlagImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "stdFlag", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "stdFlag", e);
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
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "defaultFlags", Optional.empty());
        	}
        	Object result = this.getDefaultFlagsImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "defaultFlags", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "defaultFlags", e);
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
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "userFlags", Optional.empty());
        	}
        	Object result = this.getUserFlagsImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "userFlags", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "userFlags", e);
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
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "includeFolders", Optional.empty());
        	}
        	Object result = this.getIncludeFoldersImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "includeFolders", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "includeFolders", e);
        }
    }

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
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "baseFolder", Optional.empty());
        	}
        	String result = this.getBaseFolderImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "baseFolder", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "baseFolder", e);
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
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "weavingFolder", Optional.empty());
        	}
        	String result = this.getWeavingFolderImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "weavingFolder", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "weavingFolder", e);
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
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "extraSources", Optional.empty());
        	}
        	Object result = this.getExtraSourcesImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "extraSources", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "extraSources", e);
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
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "extraIncludes", Optional.empty());
        	}
        	Object result = this.getExtraIncludesImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "extraIncludes", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "extraIncludes", e);
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
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "extraProjects", Optional.empty());
        	}
        	Object result = this.getExtraProjectsImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "extraProjects", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "extraProjects", e);
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
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "extraLibs", Optional.empty());
        	}
        	Object result = this.getExtraLibsImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "extraLibs", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "extraLibs", e);
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
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "main", Optional.empty());
        	}
        	AFunction result = this.getMainImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "main", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "main", e);
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
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "files", Optional.empty());
        	}
        	Object result = this.getFilesImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "files", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "files", e);
        }
    }

    /**
     * Default implementation of the method used by the lara interpreter to select files
     * @return 
     */
    public List<? extends AFile> selectFile() {
        return select(pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AFile.class, SelectOp.DESCENDANTS);
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
    public final boolean rebuild() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "rebuild", this, Optional.empty());
        	}
        	boolean result = this.rebuildImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "rebuild", this, Optional.ofNullable(result));
        	}
        	return result;
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
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "rebuildFuzzy", this, Optional.empty());
        	}
        	this.rebuildFuzzyImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "rebuildFuzzy", this, Optional.empty());
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "rebuildFuzzy", e);
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
    public final AJoinPoint addFile(AFile file) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "addFile", this, Optional.empty(), file);
        	}
        	AJoinPoint result = this.addFileImpl(file);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "addFile", this, Optional.ofNullable(result), file);
        	}
        	return result;
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
    public final AJoinPoint addFileFromPath(Object filepath) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "addFileFromPath", this, Optional.empty(), filepath);
        	}
        	AJoinPoint result = this.addFileFromPathImpl(filepath);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "addFileFromPath", this, Optional.ofNullable(result), filepath);
        	}
        	return result;
        } catch(Exception e) {
        	throw new ActionException(get_class(), "addFileFromPath", e);
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
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "push", this, Optional.empty());
        	}
        	this.pushImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "push", this, Optional.empty());
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "push", e);
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
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "pop", this, Optional.empty());
        	}
        	this.popImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "pop", this, Optional.empty());
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "pop", e);
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
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "addExtraInclude", this, Optional.empty(), path);
        	}
        	this.addExtraIncludeImpl(path);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "addExtraInclude", this, Optional.empty(), path);
        	}
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
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "addExtraIncludeFromGit", this, Optional.empty(), gitRepo, path);
        	}
        	this.addExtraIncludeFromGitImpl(gitRepo, path);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "addExtraIncludeFromGit", this, Optional.empty(), gitRepo, path);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "addExtraIncludeFromGit", e);
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
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "addExtraSource", this, Optional.empty(), path);
        	}
        	this.addExtraSourceImpl(path);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "addExtraSource", this, Optional.empty(), path);
        	}
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
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "addExtraSourceFromGit", this, Optional.empty(), gitRepo, path);
        	}
        	this.addExtraSourceFromGitImpl(gitRepo, path);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "addExtraSourceFromGit", this, Optional.empty(), gitRepo, path);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "addExtraSourceFromGit", e);
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
    public final void addProjectFromGit(String gitRepo, String[] libs, String path) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "addProjectFromGit", this, Optional.empty(), gitRepo, libs, path);
        	}
        	this.addProjectFromGitImpl(gitRepo, libs, path);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "addProjectFromGit", this, Optional.empty(), gitRepo, libs, path);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "addProjectFromGit", e);
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
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "addExtraLib", this, Optional.empty(), lib);
        	}
        	this.addExtraLibImpl(lib);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "addExtraLib", this, Optional.empty(), lib);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "addExtraLib", e);
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
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "atexit", this, Optional.empty(), function);
        	}
        	this.atexitImpl(function);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "atexit", this, Optional.empty(), function);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "atexit", e);
        }
    }

    /**
     * 
     */
    @Override
    public final List<? extends JoinPoint> select(String selectName) {
        List<? extends JoinPoint> joinPointList;
        switch(selectName) {
        	case "file": 
        		joinPointList = selectFile();
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
    public final void defImpl(String attribute, Object value) {
        switch(attribute){
        case "data": {
        	if(value instanceof Object){
        		this.defDataImpl((Object)value);
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
        default: throw new UnsupportedOperationException("Join point "+get_class()+": attribute '"+attribute+"' cannot be defined");
        }
    }

    /**
     * 
     */
    @Override
    protected final void fillWithAttributes(List<String> attributes) {
        super.fillWithAttributes(attributes);
        attributes.add("name");
        attributes.add("isCxx");
        attributes.add("standard");
        attributes.add("stdFlag");
        attributes.add("defaultFlags");
        attributes.add("userFlags");
        attributes.add("includeFolders");
        attributes.add("baseFolder");
        attributes.add("weavingFolder");
        attributes.add("extraSources");
        attributes.add("extraIncludes");
        attributes.add("extraProjects");
        attributes.add("extraLibs");
        attributes.add("main");
        attributes.add("files");
    }

    /**
     * 
     */
    @Override
    protected final void fillWithSelects(List<String> selects) {
        super.fillWithSelects(selects);
        selects.add("file");
    }

    /**
     * 
     */
    @Override
    protected final void fillWithActions(List<String> actions) {
        super.fillWithActions(actions);
        actions.add("boolean rebuild()");
        actions.add("void rebuildFuzzy()");
        actions.add("joinpoint addFile(file)");
        actions.add("joinpoint addFileFromPath(Object)");
        actions.add("void push()");
        actions.add("void pop()");
        actions.add("void addExtraInclude(String)");
        actions.add("void addExtraIncludeFromGit(String, String)");
        actions.add("void addExtraSource(String)");
        actions.add("void addExtraSourceFromGit(String, String)");
        actions.add("void addProjectFromGit(String, String[], String)");
        actions.add("void addExtraLib(String)");
        actions.add("void atexit(function)");
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
        NAME("name"),
        ISCXX("isCxx"),
        STANDARD("standard"),
        STDFLAG("stdFlag"),
        DEFAULTFLAGS("defaultFlags"),
        USERFLAGS("userFlags"),
        INCLUDEFOLDERS("includeFolders"),
        BASEFOLDER("baseFolder"),
        WEAVINGFOLDER("weavingFolder"),
        EXTRASOURCES("extraSources"),
        EXTRAINCLUDES("extraIncludes"),
        EXTRAPROJECTS("extraProjects"),
        EXTRALIBS("extraLibs"),
        MAIN("main"),
        FILES("files"),
        PARENT("parent"),
        AST("ast"),
        SIBLINGSLEFT("siblingsLeft"),
        DATA("data"),
        HASCHILDREN("hasChildren"),
        TYPE("type"),
        SIBLINGSRIGHT("siblingsRight"),
        RIGHTJP("rightJp"),
        ISCILK("isCilk"),
        FILEPATH("filepath"),
        SCOPENODES("scopeNodes"),
        CHILDREN("children"),
        FIRSTCHILD("firstChild"),
        NUMCHILDREN("numChildren"),
        LEFTJP("leftJp"),
        INLINECOMMENTS("inlineComments"),
        ASTNAME("astName"),
        JPID("jpId"),
        ASTID("astId"),
        CONTAINS("contains"),
        FILENAME("filename"),
        JAVAFIELDS("javaFields"),
        ISINSYSTEMHEADER("isInSystemHeader"),
        BITWIDTH("bitWidth"),
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
        KEYTYPE("keyType"),
        CHAINANCESTOR("chainAncestor"),
        CHAIN("chain"),
        JOINPOINTTYPE("joinpointType"),
        CURRENTREGION("currentRegion"),
        COLUMN("column"),
        PARENTREGION("parentRegion"),
        DEPTH("depth"),
        JAVAFIELDTYPE("javaFieldType"),
        LOCATION("location"),
        HASTYPE("hasType"),
        PRAGMAS("pragmas"),
        STMT("stmt"),
        HASPARENT("hasParent");
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
