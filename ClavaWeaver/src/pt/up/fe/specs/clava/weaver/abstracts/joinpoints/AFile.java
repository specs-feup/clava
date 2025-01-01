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
 * Auto-Generated class for join point AFile
 * This class is overwritten by the Weaver Generator.
 * 
 * Represents a source file (.c, .cpp., .cl, etc)
 * @author Lara Weaver Generator
 */
public abstract class AFile extends ACxxWeaverJoinPoint {

    /**
     * the name of the file
     */
    public abstract String getNameImpl();

    /**
     * the name of the file
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
     * a Java file to the file that originated this translation unit
     */
    public abstract Object getFileImpl();

    /**
     * a Java file to the file that originated this translation unit
     */
    public final Object getFile() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "file", Optional.empty());
        	}
        	Object result = this.getFileImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "file", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "file", e);
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
     * the folder of the source file
     */
    public abstract String getPathImpl();

    /**
     * the folder of the source file
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
     * the path to the file relative to the base source path
     */
    public abstract String getRelativeFilepathImpl();

    /**
     * the path to the file relative to the base source path
     */
    public final Object getRelativeFilepath() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "relativeFilepath", Optional.empty());
        	}
        	String result = this.getRelativeFilepathImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "relativeFilepath", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
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
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "relativeFolderpath", Optional.empty());
        	}
        	String result = this.getRelativeFolderpathImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "relativeFolderpath", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "relativeFolderpath", e);
        }
    }

    /**
     * 
     */
    public void defRelativeFolderpathImpl(String value) {
        throw new UnsupportedOperationException("Join point "+get_class()+": Action def relativeFolderpath with type String not implemented ");
    }

    /**
     * the path to the source folder that was given as the base folder of this file
     */
    public abstract String getBaseSourcePathImpl();

    /**
     * the path to the source folder that was given as the base folder of this file
     */
    public final Object getBaseSourcePath() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "baseSourcePath", Optional.empty());
        	}
        	String result = this.getBaseSourcePathImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "baseSourcePath", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "baseSourcePath", e);
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
     * true if this file is considered a header file
     */
    public abstract Boolean getIsHeaderImpl();

    /**
     * true if this file is considered a header file
     */
    public final Object getIsHeader() {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "isHeader", Optional.empty());
        	}
        	Boolean result = this.getIsHeaderImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "isHeader", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
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
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "isOpenCL", Optional.empty());
        	}
        	Boolean result = this.getIsOpenCLImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "isOpenCL", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "isOpenCL", e);
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
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "getDestinationFilepath", Optional.empty(), destinationFolderpath);
        	}
        	String result = this.getDestinationFilepathImpl(destinationFolderpath);
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "getDestinationFilepath", Optional.ofNullable(result), destinationFolderpath);
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "getDestinationFilepath", e);
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
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "sourceFoldername", Optional.empty());
        	}
        	String result = this.getSourceFoldernameImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "sourceFoldername", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "sourceFoldername", e);
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
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "hasParsingErrors", Optional.empty());
        	}
        	Boolean result = this.getHasParsingErrorsImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "hasParsingErrors", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "hasParsingErrors", e);
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
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "errorOutput", Optional.empty());
        	}
        	String result = this.getErrorOutputImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "errorOutput", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "errorOutput", e);
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
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.BEGIN, this, "includes", Optional.empty());
        	}
        	Object result = this.getIncludesImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAttribute(Stage.END, this, "includes", Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new AttributeException(get_class(), "includes", e);
        }
    }

    /**
     * Default implementation of the method used by the lara interpreter to select stmts
     * @return 
     */
    public List<? extends AStatement> selectStmt() {
        return select(pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AStatement.class, SelectOp.DESCENDANTS);
    }

    /**
     * Default implementation of the method used by the lara interpreter to select childStmts
     * @return 
     */
    public List<? extends AStatement> selectChildStmt() {
        return select(pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AStatement.class, SelectOp.DESCENDANTS);
    }

    /**
     * Default implementation of the method used by the lara interpreter to select functions
     * @return 
     */
    public List<? extends AFunction> selectFunction() {
        return select(pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AFunction.class, SelectOp.DESCENDANTS);
    }

    /**
     * Default implementation of the method used by the lara interpreter to select methods
     * @return 
     */
    public List<? extends AMethod> selectMethod() {
        return select(pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AMethod.class, SelectOp.DESCENDANTS);
    }

    /**
     * Default implementation of the method used by the lara interpreter to select records
     * @return 
     */
    public List<? extends ARecord> selectRecord() {
        return select(pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ARecord.class, SelectOp.DESCENDANTS);
    }

    /**
     * Default implementation of the method used by the lara interpreter to select structs
     * @return 
     */
    public List<? extends AStruct> selectStruct() {
        return select(pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AStruct.class, SelectOp.DESCENDANTS);
    }

    /**
     * Default implementation of the method used by the lara interpreter to select classs
     * @return 
     */
    public List<? extends AClass> selectClass() {
        return select(pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AClass.class, SelectOp.DESCENDANTS);
    }

    /**
     * Default implementation of the method used by the lara interpreter to select pragmas
     * @return 
     */
    public List<? extends APragma> selectPragma() {
        return select(pt.up.fe.specs.clava.weaver.abstracts.joinpoints.APragma.class, SelectOp.DESCENDANTS);
    }

    /**
     * Default implementation of the method used by the lara interpreter to select markers
     * @return 
     */
    public List<? extends AMarker> selectMarker() {
        return select(pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AMarker.class, SelectOp.DESCENDANTS);
    }

    /**
     * Default implementation of the method used by the lara interpreter to select tags
     * @return 
     */
    public List<? extends ATag> selectTag() {
        return select(pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ATag.class, SelectOp.DESCENDANTS);
    }

    /**
     * Default implementation of the method used by the lara interpreter to select vardecls
     * @return 
     */
    public List<? extends AVardecl> selectVardecl() {
        return select(pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AVardecl.class, SelectOp.DESCENDANTS);
    }

    /**
     * Default implementation of the method used by the lara interpreter to select comments
     * @return 
     */
    public List<? extends AComment> selectComment() {
        return select(pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AComment.class, SelectOp.DESCENDANTS);
    }

    /**
     * Default implementation of the method used by the lara interpreter to select includes
     * @return 
     */
    public List<? extends AInclude> selectInclude() {
        return select(pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AInclude.class, SelectOp.DESCENDANTS);
    }

    /**
     * Default implementation of the method used by the lara interpreter to select typedefDecls
     * @return 
     */
    public List<? extends ATypedefDecl> selectTypedefDecl() {
        return select(pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ATypedefDecl.class, SelectOp.DESCENDANTS);
    }

    /**
     * Default implementation of the method used by the lara interpreter to select decls
     * @return 
     */
    public List<? extends ADecl> selectDecl() {
        return select(pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ADecl.class, SelectOp.DESCENDANTS);
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
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "addCInclude", this, Optional.empty(), name, isAngled);
        	}
        	this.addCIncludeImpl(name, isAngled);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "addCInclude", this, Optional.empty(), name, isAngled);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "addCInclude", e);
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
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "addGlobal", this, Optional.empty(), name, type, initValue);
        	}
        	AVardecl result = this.addGlobalImpl(name, type, initValue);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "addGlobal", this, Optional.ofNullable(result), name, type, initValue);
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new ActionException(get_class(), "addGlobal", e);
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
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "write", this, Optional.empty(), destinationFoldername);
        	}
        	String result = this.writeImpl(destinationFoldername);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "write", this, Optional.ofNullable(result), destinationFoldername);
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new ActionException(get_class(), "write", e);
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
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setName", this, Optional.empty(), filename);
        	}
        	this.setNameImpl(filename);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setName", this, Optional.empty(), filename);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setName", e);
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
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "rebuild", this, Optional.empty());
        	}
        	AFile result = this.rebuildImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "rebuild", this, Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
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
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "rebuildTry", this, Optional.empty());
        	}
        	AJoinPoint result = this.rebuildTryImpl();
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "rebuildTry", this, Optional.ofNullable(result));
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new ActionException(get_class(), "rebuildTry", e);
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
    public final Object addFunction(String name) {
        try {
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "addFunction", this, Optional.empty(), name);
        	}
        	AJoinPoint result = this.addFunctionImpl(name);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "addFunction", this, Optional.ofNullable(result), name);
        	}
        	return result!=null?result:getUndefinedValue();
        } catch(Exception e) {
        	throw new ActionException(get_class(), "addFunction", e);
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
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.BEGIN, "setRelativeFolderpath", this, Optional.empty(), path);
        	}
        	this.setRelativeFolderpathImpl(path);
        	if(hasListeners()) {
        		eventTrigger().triggerAction(Stage.END, "setRelativeFolderpath", this, Optional.empty(), path);
        	}
        } catch(Exception e) {
        	throw new ActionException(get_class(), "setRelativeFolderpath", e);
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
        	case "method": 
        		joinPointList = selectMethod();
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
        	case "tag": 
        		joinPointList = selectTag();
        		break;
        	case "vardecl": 
        		joinPointList = selectVardecl();
        		break;
        	case "comment": 
        		joinPointList = selectComment();
        		break;
        	case "include": 
        		joinPointList = selectInclude();
        		break;
        	case "typedefDecl": 
        		joinPointList = selectTypedefDecl();
        		break;
        	case "decl": 
        		joinPointList = selectDecl();
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
        case "relativeFolderpath": {
        	if(value instanceof String){
        		this.defRelativeFolderpathImpl((String)value);
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
        attributes.add("baseSourcePath");
        attributes.add("errorOutput");
        attributes.add("file");
        attributes.add("getDestinationFilepath");
        attributes.add("hasMain");
        attributes.add("hasParsingErrors");
        attributes.add("includes");
        attributes.add("isCxx");
        attributes.add("isHeader");
        attributes.add("isOpenCL");
        attributes.add("name");
        attributes.add("path");
        attributes.add("relativeFilepath");
        attributes.add("relativeFolderpath");
        attributes.add("sourceFoldername");
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
        selects.add("method");
        selects.add("record");
        selects.add("struct");
        selects.add("class");
        selects.add("pragma");
        selects.add("marker");
        selects.add("tag");
        selects.add("vardecl");
        selects.add("comment");
        selects.add("include");
        selects.add("typedefDecl");
        selects.add("decl");
    }

    /**
     * 
     */
    @Override
    protected final void fillWithActions(List<String> actions) {
        super.fillWithActions(actions);
        actions.add("void addCInclude(String, boolean)");
        actions.add("joinpoint addFunction(String)");
        actions.add("vardecl addGlobal(String, joinpoint, String)");
        actions.add("void addInclude(String, boolean)");
        actions.add("void addIncludeJp(joinpoint)");
        actions.add("void insertBegin(joinpoint)");
        actions.add("void insertBegin(String)");
        actions.add("void insertEnd(joinpoint)");
        actions.add("void insertEnd(String)");
        actions.add("file rebuild()");
        actions.add("joinpoint rebuildTry()");
        actions.add("void setName(String)");
        actions.add("void setRelativeFolderpath(String)");
        actions.add("String write(String)");
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
        AST("ast"),
        ASTCHILDREN("astChildren"),
        ASTID("astId"),
        ASTISINSTANCE("astIsInstance"),
        ASTNAME("astName"),
        ASTNUMCHILDREN("astNumChildren"),
        BASESOURCEPATH("baseSourcePath"),
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
        ERROROUTPUT("errorOutput"),
        FILE("file"),
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
        GETDESTINATIONFILEPATH("getDestinationFilepath"),
        GETFIRSTJP("getFirstJp"),
        GETJAVAFIELDTYPE("getJavaFieldType"),
        GETKEYTYPE("getKeyType"),
        GETUSERFIELD("getUserField"),
        GETVALUE("getValue"),
        HASCHILDREN("hasChildren"),
        HASMAIN("hasMain"),
        HASNODE("hasNode"),
        HASPARENT("hasParent"),
        HASPARSINGERRORS("hasParsingErrors"),
        HASTYPE("hasType"),
        INCLUDES("includes"),
        INLINECOMMENTS("inlineComments"),
        ISCILK("isCilk"),
        ISCXX("isCxx"),
        ISHEADER("isHeader"),
        ISINSYSTEMHEADER("isInSystemHeader"),
        ISINSIDEHEADER("isInsideHeader"),
        ISINSIDELOOPHEADER("isInsideLoopHeader"),
        ISMACRO("isMacro"),
        ISOPENCL("isOpenCL"),
        JAVAFIELDS("javaFields"),
        JPID("jpId"),
        KEYS("keys"),
        LASTCHILD("lastChild"),
        LEFTJP("leftJp"),
        LINE("line"),
        LOCATION("location"),
        NAME("name"),
        NUMCHILDREN("numChildren"),
        ORIGINNODE("originNode"),
        PARENT("parent"),
        PARENTREGION("parentRegion"),
        PATH("path"),
        PRAGMAS("pragmas"),
        RELATIVEFILEPATH("relativeFilepath"),
        RELATIVEFOLDERPATH("relativeFolderpath"),
        RIGHTJP("rightJp"),
        ROOT("root"),
        SCOPENODES("scopeNodes"),
        SIBLINGSLEFT("siblingsLeft"),
        SIBLINGSRIGHT("siblingsRight"),
        SOURCEFOLDERNAME("sourceFoldername"),
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
