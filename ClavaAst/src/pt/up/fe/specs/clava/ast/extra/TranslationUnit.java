/**
 * Copyright 2016 SPeCS.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. under the License.
 */

package pt.up.fe.specs.clava.ast.extra;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.SourceRange;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.decl.IncludeDecl;
import pt.up.fe.specs.clava.ast.decl.NamedDecl;
import pt.up.fe.specs.clava.ast.extra.data.Language;
import pt.up.fe.specs.clava.ast.stmt.DeclStmt;
import pt.up.fe.specs.clava.utils.IncludeManager;
import pt.up.fe.specs.clava.utils.SourceType;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.treenode.NodeInsertUtils;
import pt.up.fe.specs.util.utilities.LineStream;
import pt.up.fe.specs.util.utilities.StringLines;

/**
 * Represents a source file.
 *
 * @author JoaoBispo
 *
 */
public class TranslationUnit extends ClavaNode {

    /// DATAKEYS BEGIN

    /**
     * The path corresponding to this Translation Unit.
     */
    public static final DataKey<File> SOURCE_FILE = KeyFactory.object("sourceFile", File.class);

    /**
     * Language-related information about how this Translation Unit.
     */
    public static final DataKey<Language> LANGUAGE = KeyFactory.object("language", Language.class)
            // TODO: Remove this after parsing of header files is implemented
            .setDefault(() -> new Language());

    /**
     * (Optional)<br>
     * Relative path refers to the last portion of the path of this file that should be considered as not part of the
     * original source folder.
     * <p>
     * For instance, if the path of the file is /folder1/folder2/file.h, and the source folder should be considered to
     * be /folder1, this method returns folder2.
     */
    public static final DataKey<String> RELATIVE_PATH = KeyFactory.string("relativePath");

    /**
     * If this source file was specified as being part of a source folder, returns the name of that folder. Otherwise,
     * returns empty.
     * <p>
     * For instance, if the path of the file is /folder1/folder2/file.h, and the source folder should be considered to
     * be /folder1, this method returns folder1.
     */
    public static final DataKey<Optional<String>> SOURCE_FOLDERNAME = KeyFactory.optional("sourceFoldername");

    /**
     * If set, the source code of this file will be equivalent to the given string.
     */
    public static final DataKey<Optional<String>> LITERAL_SOURCE = KeyFactory.optional("literalSource");

    public static final DataKey<Boolean> HAS_PARSING_ERRORS = KeyFactory.bool("hasParsingErrors");

    public static final DataKey<String> ERROR_OUTPUT = KeyFactory.string("errorOutput");

    /// DATAKEYS END

    // private static final Set<String> HEADER_EXTENSIONS = new HashSet<>(Arrays.asList("h", "hpp"));
    // private static final Set<String> CXX_EXTENSIONS = new HashSet<>(Arrays.asList("cpp", "hpp"));

    // public static Set<String> getHeaderExtensions() {
    // return HEADER_EXTENSIONS;
    // }

    // private final String filename;
    // private final String path;

    // Bookkeeping for the includes, will be initialize the first time it is accessed with getIncludes()
    private IncludeManager includes;

    private final Lazy<Boolean> isCxxUnit;

    // private String relativePath;

    public TranslationUnit(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);

        // List<IncludeDecl> includesList = children.stream()
        // // All children must be Decls
        // .map(Decl.class::cast)
        // .filter(decl -> decl instanceof IncludeDecl)
        // .map(include -> (IncludeDecl) include)
        // .collect(Collectors.toList());
        //
        // includes = new IncludeManager(includesList, this);

        isCxxUnit = Lazy.newInstanceSerializable(this::testIsCXXUnit);
    }

    // @Override
    // public ClavaNode copy(boolean keepId) {
    // TranslationUnit newTunit = (TranslationUnit) super.copy(keepId);
    // newTunit.includes = includes.copy(newTunit);
    //
    // return newTunit;
    // }

    @Override
    public ClavaNode addChild(ClavaNode child) {
        // If IncludeDecl, treat it differently
        if (child instanceof IncludeDecl) {
            return addIncludePrivate((IncludeDecl) child);
            // return;

        }

        // Add child normally
        return super.addChild(child);
    }

    @Override
    public ClavaNode addChild(int index, ClavaNode child) {
        // If IncludeDecl, treat it differently
        if (child instanceof IncludeDecl) {
            ClavaLog.warning(
                    "Using .addChild(int index, ClavaNode chilld) with an IncludeDecl node, the index value will be ignored");
            return addIncludePrivate((IncludeDecl) child);
            // return;
        }

        // Add child normally
        return super.addChild(index, child);
    }

    @Override
    public ClavaNode removeChild(int index) {
        // if (getChild(index) instanceof IncludeDecl) {
        // System.out.println("REMOVING: " + getChild(index).getCode());
        // System.out.println("REMOVE BEFORE: " + getIncludes().getCurrentIncludes());
        // System.out.println("LAST BEFORE: " + getIncludes().getLastInclude());
        // }

        // If child is an IncludeDecl, update IncludesManager
        if (getChild(index) instanceof IncludeDecl) {
            getIncludes().remove((IncludeDecl) getChild(index));
        }

        // if (getChild(index) instanceof IncludeDecl) {
        // System.out.println("REMOVE AFTER: " + getIncludes().getCurrentIncludes());
        // System.out.println("LAST AFTER: " + getIncludes().getLastInclude());
        // }
        return super.removeChild(index);
    }

    /**
     * Manages adding an include to the translation unit.
     *
     * @param include
     */
    private IncludeDecl addIncludePrivate(IncludeDecl include) {
        int insertIndex = getIncludes().addInclude(include);

        if (insertIndex == -1) {
            return null;
        }

        return (IncludeDecl) super.addChild(insertIndex, include);
    }

    /*
    private TranslationUnit(String filename, String folderPath, Collection<Decl> declarations) {
        super(createInfo(new ArrayList<>(declarations), new File(folderPath, filename).getPath()), declarations);
    
        this.filename = filename;
        path = processFilePath(folderPath);
    
        List<IncludeDecl> includesList = declarations.stream()
                .filter(decl -> decl instanceof IncludeDecl)
                .map(include -> (IncludeDecl) include)
                .collect(Collectors.toList());
    
        includes = new IncludeManager(includesList, this);
    
        isCxxUnit = Lazy.newInstance(this::testIsCXXUnit);
    
        // relativePath = null;
    }
    */

    // public TranslationUnit(File sourceFile, Collection<Decl> declarations) {
    // this(new LegacyToDataStore()
    // .setNodeInfo(createInfo(new ArrayList<>(declarations), sourceFile.getPath()))
    // .set(SOURCE_FILE, sourceFile).getData(), declarations);
    // }

    /*
    private String processFilePath(String filePath) {
        if (filePath == null) {
            return null;
        }
    
        if (filePath.isEmpty()) {
            return null;
        }
    
        // Should convert to absolute?
    
        return SpecsIo.getCanonicalPath(new File(filePath));
    }
    */

    /*
    public static ClavaNodeInfo createInfo(List<Decl> declarations, String filepath) {
        if (declarations.isEmpty()) {
            return ClavaNodeInfo.undefinedInfo(new SourceRange(filepath, 1, 1, 1, 1));
        }
    
        // TODO: Only consider locations which have the same source file?
        System.out.println("FILEPATH:" + filepath);
        Decl firstDecl = declarations.get(0);
        int startLine = firstDecl.getLocation().getStartLine();
        int startCol = firstDecl.getLocation().getStartCol();
        int endLine = firstDecl.getLocation().getEndLine();
        int endCol = firstDecl.getLocation().getEndCol();
        System.out.println("FIRST DECL START FILEPATH:" + firstDecl.getLocation().getStartFilepath());
        System.out.println("FIRST DECL END FILEPATH:" + firstDecl.getLocation().getEndFilepath());
        // if (!firstDecl.getLocation().getFilepath().equals(filepath)) {
        // throw new RuntimeException("First decl filepath '" + firstDecl.getLocation().getFilepath()
        // + "' diff than given filepath '" + filepath + "'");
        // }
        for (int i = 1; i < declarations.size(); i++) {
            Decl decl = declarations.get(i);
            System.out.println("DECL START FILEPATH:" + decl.getLocation().getStartFilepath());
            System.out.println("DECL END FILEPATH:" + decl.getLocation().getEndFilepath());
            // if (!decl.getLocation().getFilepath().equals(filepath)) {
            // throw new RuntimeException(" decl filepath '" + decl.getLocation().getFilepath()
            // + "' diff than given filepath '" + filepath + "'");
            // }
    
            // If line came first, replace
            if (decl.getLocation().getStartLine() < startLine) {
                startLine = decl.getLocation().getStartLine();
                startCol = decl.getLocation().getStartCol();
            }
            // If the same line, use smaller start column
            else if (decl.getLocation().getStartLine() == startLine) {
                startCol = Math.min(decl.getLocation().getStartCol(), startCol);
            }
    
            // If last line came last, use it
            if (decl.getLocation().getEndLine() > endLine) {
                endLine = decl.getLocation().getEndLine();
                endCol = decl.getLocation().getEndCol();
            }
    
            else if (decl.getLocation().getEndLine() == endLine) {
                endCol = Math.max(decl.getLocation().getEndCol(), endCol);
            }
    
        }
    
        return ClavaNodeInfo.undefinedInfo(new SourceRange(filepath, startLine, startCol, endLine, endCol));
    }
    */

    public static void setDataStore(File sourceFile, DataStore data) {
        data.set(TranslationUnit.SOURCE_FILE, sourceFile);

        // Normalize path
        String filepath = SpecsIo.getCanonicalPath(sourceFile);

        // If source file does not exist, return default values

        // if (declarations.isEmpty()) {
        if (!sourceFile.isFile()) {
            data.set(ClavaNode.LOCATION, new SourceRange(filepath, 1, 1, 1, 1));
            // return ClavaNodeInfo.undefinedInfo(new SourceRange(filepath, 1, 1, 1, 1));
            return;
        }

        // Start line and col of tunit is always 1
        int startLine = 1;
        int startCol = 1;
        int endLine = -1;
        int endCol = -1;

        // Count number of lines, and number of characters in the last line
        try (LineStream lineStream = LineStream.newInstance(sourceFile)) {
            int numLines = 0;
            String lastLine = null;
            while (lineStream.hasNextLine()) {
                lastLine = lineStream.nextLine();
                numLines++;
            }

            if (lastLine == null) {
                endLine = 1;
                endCol = 1;
            } else {
                endLine = numLines;
                endCol = lastLine.length();
            }
        }

        data.set(ClavaNode.LOCATION, new SourceRange(filepath, startLine, startCol, endLine, endCol));
    }

    public static String getRelativePath(File filepath, File baseSourceFolder) {

        // Normalize baseSourceFolder
        if (baseSourceFolder != null && baseSourceFolder.getPath().isEmpty()) {
            baseSourceFolder = null;
        }

        // If source path does not exist yet, or no base source folder specified, just return filename
        // if (!filepath.exists() || baseSourceFolder == null) {
        if (baseSourceFolder == null) {
            // System.out.println("FILEPATH:" + filepath);
            // Only return complete resource path if it is not absolute
            String relativePath = filepath.isAbsolute() ? filepath.getName() : filepath.getPath();
            // System.out.println("BASESOURCE FOLDER:" + baseSourceFolder);
            // System.out.println("FILEPATH:" + filepath);
            // System.out.println("IS ABSOLUTE:" + filepath.isAbsolute());
            // System.out.println("FILEPATH GETNAME:" + filepath.getName());
            // System.out.println("RELATIVE PATH BEFORE:" + relativePath);

            // if (baseSourceFolder != null) {
            // relativePath = new File(baseSourceFolder, relativePath).getPath();
            // }
            // System.out.println("RELATIVE PATH AFTER:" + relativePath);
            // return sourcePath.getPath();
            // System.out.println("getRelativePath:" + relativePath);

            // return filepath.getName();
            return relativePath;
        }

        // No base input folder specified, just return source path
        // if (baseSourceFolder == null) {
        // return sourcePath.getPath();
        // }

        // Calculate relative path
        String relativePath = SpecsIo.getRelativePath(filepath, baseSourceFolder);

        // Avoid writing outside of the destination folder, if relative path has '../', remove them
        // while (relativePath.startsWith("../")) {
        // relativePath = relativePath.substring("../".length());
        // }

        return relativePath;
    }

    // @Override
    // protected ClavaNode copyPrivate() {
    // return new TranslationUnit(filename, path, Collections.emptyList());
    // }

    @Override
    public String getCode() {

        var literalCode = get(LITERAL_SOURCE).orElse(null);
        if (literalCode != null) {
            return literalCode;
        }

        String body = getChildrenStream()
                // String body = getDecls().stream()
                // .map(child -> child.getCode())
                .map(this::getChildCode)
                .collect(Collectors.joining(ln()));

        // If header file, add include guards
        if (isHeaderFile()) {
            body = addIncludeGuards(body);
        }

        return body;
    }

    /**
     * 'Source path' refers to the path that was given as the base for the source file of this Translation Unit. It
     * corresponds to an ancestor folder of the file, which was given as a folder where to look for sources.
     *
     * @return if set, returns the original source path of this translation unit
     */
    /**
     * Relative path refers to the last portion of the path of this file that should be considered as not part of the
     * original source folder.
     * <p>
     * For instance, if the path of the file is /folder1/folder2/file.h, but the source folder should be considered to
     * be /folder1, this method returns folder2.
     *
     * @return
     */
    /*
    public Optional<String> getRelativePath() {
        return Optional.ofNullable(relativePath);
    }
    */

    /**
     *
     * @param relativePath
     */
    public void setRelativePath(String relativePath) {
        // System.out.println("SET 2 BEFORE: " + get(RELATIVE_PATH));

        if (relativePath == null || relativePath.isEmpty()) {
            set(RELATIVE_PATH, null);
            // System.out.println("SET 2 AFTER 1: " + get(RELATIVE_PATH));

            return;
        }
        // if (relativePath == null) {
        // this.relativePath = null;
        // return;
        // }

        // Verify relative path
        if (!isRelativePathValid(relativePath)) {
            SpecsLogs.msgInfo("Could not set relative path '" + relativePath
                    + "', it does not match the end of the translation unit path: " + getFolderpath());
            return;
        }

        set(RELATIVE_PATH, relativePath);
        // System.out.println("SET 2 AFTER 2: " + get(RELATIVE_PATH));

        // this.relativePath = relativePath.isEmpty() ? null : relativePath;
    }

    // public void setSourceFoldername(String sourceFoldername) {
    // if (sourceFoldername == null) {
    // set(SOURCE_FOLDERNAME, Optional.empty());
    // return;
    // }
    //
    // set(SOURCE_FOLDERNAME, Optional.of(sourceFoldername));
    // // this.relativePath = relativePath.isEmpty() ? null : relativePath;
    // }

    public boolean isRelativePathValid(String relativePath) {
        File path = get(SOURCE_FILE).getParentFile();
        if (path != null) {
            // File currentPath = new File(path);
            File currentPath = path;
            File currentRelativePath = new File(relativePath);
            while (currentRelativePath != null) {

                if (!currentRelativePath.getName().equals(currentPath.getName())) {
                    return false;
                }

                currentRelativePath = currentRelativePath.getParentFile();
                currentPath = currentPath.getParentFile();
            }
        }

        return true;
    }

    private String getChildCode(ClavaNode decl) {
        String code = decl.getCode();
        String commentCode = decl.getInlineCommentsCode();

        // If not empty, add comment to first line
        if (!commentCode.isEmpty()) {
            List<String> lines = StringLines.getLines(code);

            // Find first non-empty line
            int index = IntStream.range(0, lines.size())
                    .filter(i -> !lines.get(i).trim().isEmpty())
                    .findFirst()
                    .orElse(-1);
            // .orElseThrow(() -> new RuntimeException(
            // "Expected to find non-empty code: " + decl.getCode() + "\n" + decl));

            if (index != -1) {
                lines.set(index, lines.get(index) + commentCode);
            }
            // Could not find code where to insert inline comment, just add to the beginning
            else {
                lines.add(0, commentCode);
            }

            // lines.set(index, lines.get(index) + commentCode);

            code = lines.stream().collect(Collectors.joining(ln()));
        }

        return code;
    }

    private String addIncludeGuards(String body) {
        StringBuilder builder = new StringBuilder();

        // Replace '.' with '_' and surround id with "_", to avoid problems such as the id starting with a number
        String filenameId = "_" + getFilename().replace(".", "_") + "_";

        // Other sanitizations
        filenameId = filenameId.replace("-", "_");

        // Prepend a few characters to guarantee uniqueness
        // filenameId = filenameId + UUID.randomUUID().toString().substring(0, 3);
        filenameId = filenameId.toUpperCase();

        builder.append("#ifndef ").append(filenameId).append(ln());
        builder.append("#define ").append(filenameId).append(ln() + ln());
        builder.append(body);
        builder.append(ln() + "#endif" + ln());

        return builder.toString();
    }

    public String getFilename() {
        return get(SOURCE_FILE).getName();
        // return filename;
    }

    /**
     *
     * @return the path to the folder of this file
     */
    public Optional<String> getFolderpath() {
        return Optional.ofNullable(get(SOURCE_FILE).getParent());
        // return Optional.ofNullable(path);
    }

    /*
    public String getFilepath() {
        return getLocation().getFilepath();
    }
    */

    public void setFile(File sourceFile) {
        set(SOURCE_FILE, sourceFile);
    }

    public File getFile() {
        return get(SOURCE_FILE);
        // if (path == null) {
        // return new File(filename);
        // }
        //
        // return new File(path, filename);
    }

    public boolean isHeaderFile() {
        return SourceType.isHeader(getFile());
        // String extension = SpecsIo.getExtension(getFilename());
        // return SourceType.HEADER.hasExtension(extension);
        //
        // // return HEADER_EXTENSIONS.contains(extension.toLowerCase());
    }

    public boolean isOpenCLFile() {
        String extension = SpecsIo.getExtension(getFilename());

        return extension.toLowerCase().equals("cl");
    }

    public boolean isCXXUnit() {
        return isCxxUnit.get();
    }

    private boolean testIsCXXUnit() {
        // 1) Check if file has CXX extension.
        // Cannot test for C extensions because you can have C++ code inside .c files, for instance.
        // if (CXX_EXTENSIONS.contains(SpecsIo.getExtension(filename))) {
        if (SourceType.isCxxExtension(SpecsIo.getExtension(getFilename()))) {
            return true;
        }

        // 2) Check if file has CXX-exclusive nodes
        return getDescendantsStream()
                // .filter(node -> node.getNodeName().startsWith("CXX"))
                .filter(ClavaNode::isCxxNode)
                .findFirst()
                .isPresent();
    }

    // @Override
    // public String toContentString() {
    // return super.toContentString() + filename + " ";
    // }

    public IncludeManager getIncludes() {
        if (includes == null) {
            includes = new IncludeManager();
        }

        return includes;
    }

    /**
     * Helper method which sets 'isAngled' to false.
     *
     * @param translationUnit
     */
    public void addInclude(TranslationUnit translationUnit) {
        addInclude(translationUnit, false);
    }

    /**
     * Adds the given translation unit as an include of this translation unit.
     *
     * @param translationUnit
     */
    private void addInclude(TranslationUnit translationUnit, boolean isAngled) {
        // private void addInclude(TranslationUnit translationUnit, boolean isAngled, File baseSourceFolder) {
        String relativePath = translationUnit.getRelativeFilepath();
        // If file does not exist yet, relative path is the path of the file itself
        // Otherwise, calculate
        // File tuFile = translationUnit.getFile();

        // String relativePath = tuFile.exists() ? SpecsIo.getRelativePath(tuFile, getFile()) : tuFile.getPath();

        // Get relative path to include the file in this file
        // String relativePath = SpecsIo.getRelativePath(translationUnit.getFile(), getFile());
        // System.out.println("RELATIVE PATH:" + relativePath);
        addInclude(relativePath, isAngled);
    }

    public void addInclude(String includeName, boolean isAngled) {
        // if (includeName.startsWith("..")) {
        // throw new RuntimeException("STOP");
        // }
        // System.out.println("INCLUDE NAME:" + includeName);

        // getIncludes().addInclude(getFactory().includeDecl(includeName, isAngled));
        addIncludePrivate(getFactory().includeDecl(includeName, isAngled));
    }

    public void addInclude(IncludeDecl include) {
        addInclude(include.getInclude().getInclude(), include.getInclude().isAngled());
    }

    public void addCInclude(String includeName, boolean isAngled) {
        addIncludePrivate(getFactory().cIncludeDecl(includeName, isAngled));
    }

    public void addDeclaration(NamedDecl namedDecl) {
        // Find insertion point. Insert before first function declaration
        Optional<ClavaNode> firstFunction = getChildrenStream()
                .filter(child -> child instanceof FunctionDecl)
                .findFirst();

        DeclStmt stmt = getFactory().declStmt(namedDecl);
        // DeclStmt stmt = ClavaNodeFactory.declStmt(ClavaNodeInfo.undefinedInfo(), Arrays.asList(varDecl));

        if (firstFunction.isPresent()) {
            NodeInsertUtils.insertBefore(firstFunction.get(), stmt);
            return;
        }

        // Add at the end of the translation unit
        addChild(getNumChildren(), stmt);

    }

    // public void write(File destinationFolder, File baseInputFolder) {
    public File write(File destinationFolder) {
        // String relativePath = getRelativeFolderpath();
        File actualDestinationFolder = getRelativeFolderpath()
                .map(relativePath -> SpecsIo.mkdir(new File(destinationFolder, relativePath)))
                .orElse(destinationFolder);
        // File actualDestinationFolder = SpecsIo.mkdir(new File(destinationFolder, relativePath));
        File destinationFile = new File(actualDestinationFolder, getFilename());

        SpecsIo.write(destinationFile, getCode());

        return destinationFile;
    }

    /**
     * @deprecated
     * @param baseSourceFolder
     * @return
     */
    /*
    @Deprecated
    public String getRelativeFolderpath(File baseSourceFolder) {
        return getRelativePath(new File(path), baseSourceFolder);
        // // If file does not exist yet, or base source folder is null, return its path
        // if (!getFile().isFile() || baseSourceFolder == null) {
        // return getFolderpath();
        // }
        //
        // return getAppTry()
        // .map(app -> app.getRelativeFolderPath(baseSourceFolder, this))
        // .orElse(getFolderpath());
    
    }
    */

    // * <p>
    // * Relative path example: if this file is included using "folder/header.h", returns "folder".

    /**
     * The folder of this file, relative to the include folders of the project.
     *
     * <p>
     * Relative path refers to the last portion of the path of this file that should be considered as not part of the
     * original source folder.
     * <p>
     * For instance, if the path of the file is /folder1/folder2/file.h, but the source folder should be considered to
     * be /folder1, this method returns folder2.
     *
     * *
     *
     *
     * @return
     */
    public Optional<String> getRelativeFolderpath() {
        return getTry(RELATIVE_PATH);

        // return Optional.ofNullable(relativePath);
    }

    // public void setRelativeFolderpath(String path) {
    // // System.out.println("SET 1 BEFORE: " + get(RELATIVE_PATH));
    // set(RELATIVE_PATH, path);
    // // System.out.println("SET 1 AFTER: " + get(RELATIVE_PATH));
    // // return Optional.ofNullable(relativePath);
    // }

    /**
     * @deprecated
     * @param baseSourceFolder
     * @return
     */
    /*
    @Deprecated
    public String getRelativeFilepath(File baseSourceFolder) {
        return getRelativePath(getFile(), baseSourceFolder);
        // // If file does not exist yet, or base source folder is null, return its path
        // if (!getFile().isFile() || baseSourceFolder == null) {
        // return getFile().getPath();
        // }
        //
        // return getAppTry()
        // .map(app -> app.getRelativePath(baseSourceFolder, this))
        // .orElse(getFile().getPath());
    }
    */

    /**
     * The path of this file, relative to the include folders of the project.
     *
     * @return
     */
    public String getRelativeFilepath() {
        return getRelativeFolderpath()
                .map(relativePath -> relativePath + "/" + getFilename())
                .orElse(getFilename());

        // return getRelativePath(getFile(), sourcePath);
    }

    /**
     * The base destination folder of the file. If a sourcePath is defined, it is not taken into account for the
     * destination folder.
     *
     * @param baseFolder
     * @param flattenFolder
     * @return
     */
    public File getDestinationFolder(File baseFolder, boolean flattenFolder) {
        if (flattenFolder) {
            return baseFolder;
        }
        // System.out.println("RELATIVE FOLDER PATH: " + getRelativeFolderpath());
        // Check if there is a relative path defined
        Optional<String> relativeFolderpathTry = getRelativeFolderpath();
        if (!relativeFolderpathTry.isPresent()) {
            return baseFolder;
        }

        String relativeFolderpath = relativeFolderpathTry.get();

        return new File(baseFolder, relativeFolderpath);

        // // Check if there is a path defined
        // Optional<String> folderpathTry = getFolderpath();
        // // System.out.println("FOLDERPATH:" + folderpath);
        // if (!folderpathTry.isPresent()) {
        // return baseFolder;
        // }
        //
        // String folderpath = folderpathTry.get();
        // File folder = new File(folderpath);
        //
        // // If folderpath is absolute, return only last name; otherwise, use it fully
        // String path = folder.isAbsolute() ? folder.getName() : folderpath;
        // return new File(baseFolder, path);

    }

    public File getDestinationFile(File destinationFolder) {
        File destinationFolderWithSourceName = get(SOURCE_FOLDERNAME)
                .map(sourceFoldername -> new File(destinationFolder, sourceFoldername)).orElse(destinationFolder);

        // Build destination path
        File destinationFolderWithRelativePath = getRelativeFolderpath()
                .map(relativePath -> SpecsIo.mkdir(new File(destinationFolderWithSourceName, relativePath)))
                .orElse(destinationFolderWithSourceName);

        return new File(destinationFolderWithRelativePath, getFilename());

    }

    public void setLanguage(Language language) {
        set(LANGUAGE, language);
    }

    public boolean hasInclude(String includeName, boolean isAngled) {
        return getIncludes().hasInclude(includeName, isAngled, getFactory());
    }

    @Override
    public String getStableId() {
        return "file$" + getRelativeFilepath();
    }

}
