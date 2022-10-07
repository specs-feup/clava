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

    // Bookkeeping for the includes, will be initialize the first time it is accessed with getIncludes()
    private IncludeManager includes;

    private final Lazy<Boolean> isCxxUnit;

    public TranslationUnit(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);

        isCxxUnit = Lazy.newInstanceSerializable(this::testIsCXXUnit);
    }

    @Override
    public ClavaNode addChild(ClavaNode child) {
        // If IncludeDecl, treat it differently
        if (child instanceof IncludeDecl) {
            return addIncludePrivate((IncludeDecl) child);
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
        }

        // Add child normally
        return super.addChild(index, child);
    }

    @Override
    public ClavaNode removeChild(int index) {

        // If child is an IncludeDecl, update IncludesManager
        if (getChild(index) instanceof IncludeDecl) {
            getIncludes().remove((IncludeDecl) getChild(index));
        }

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

    public static void setDataStore(File sourceFile, DataStore data) {
        data.set(TranslationUnit.SOURCE_FILE, sourceFile);

        // Normalize path
        String filepath = SpecsIo.getCanonicalPath(sourceFile);

        // If source file does not exist, return default values

        if (!sourceFile.isFile()) {
            data.set(ClavaNode.LOCATION, new SourceRange(filepath, 1, 1, 1, 1));
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
        if (baseSourceFolder == null) {

            // Only return complete resource path if it is not absolute
            String relativePath = filepath.isAbsolute() ? filepath.getName() : filepath.getPath();

            return relativePath;
        }

        // Calculate relative path
        String relativePath = SpecsIo.getRelativePath(filepath, baseSourceFolder);

        return relativePath;
    }

    @Override
    public String getCode() {

        var literalCode = get(LITERAL_SOURCE).orElse(null);
        if (literalCode != null) {
            return literalCode;
        }

        String body = getChildrenStream()
                .map(this::getChildCode)
                .collect(Collectors.joining(ln()));

        // If header file, add include guards
        if (isHeaderFile()) {
            body = addIncludeGuards(body);
        }

        return body;
    }

    /**
     *
     * @param relativePath
     */
    public void setRelativePath(String relativePath) {

        if (relativePath == null || relativePath.isEmpty()) {
            set(RELATIVE_PATH, null);

            return;
        }

        // Verify relative path
        if (!isRelativePathValid(relativePath)) {
            SpecsLogs.msgInfo("Could not set relative path '" + relativePath
                    + "', it does not match the end of the translation unit path: " + getFolderpath());
            return;
        }

        set(RELATIVE_PATH, relativePath);
    }

    public boolean isRelativePathValid(String relativePath) {
        File path = get(SOURCE_FILE).getParentFile();
        if (path != null) {
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

            if (index != -1) {
                lines.set(index, lines.get(index) + commentCode);
            }
            // Could not find code where to insert inline comment, just add to the beginning
            else {
                lines.add(0, commentCode);
            }

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
        filenameId = filenameId.toUpperCase();

        builder.append("#ifndef ").append(filenameId).append(ln());
        builder.append("#define ").append(filenameId).append(ln() + ln());
        builder.append(body);
        builder.append(ln() + "#endif" + ln());

        return builder.toString();
    }

    public String getFilename() {
        return get(SOURCE_FILE).getName();
    }

    /**
     *
     * @return the path to the folder of this file
     */
    public Optional<String> getFolderpath() {
        return Optional.ofNullable(get(SOURCE_FILE).getParent());
    }

    public void setFile(File sourceFile) {
        set(SOURCE_FILE, sourceFile);
    }

    public File getFile() {
        return get(SOURCE_FILE);
    }

    public boolean isHeaderFile() {
        return SourceType.isHeader(getFile());
    }

    public boolean isOpenCLFile() {
        String extension = SpecsIo.getExtension(getFilename());

        return extension.toLowerCase().equals("cl");
    }

    public boolean isCUDAFile() {
        String extension = SpecsIo.getExtension(getFilename());

        return extension.toLowerCase().equals("cu");
    }

    public boolean isCXXUnit() {
        return isCxxUnit.get();
    }

    private boolean testIsCXXUnit() {
        // 1) Check if file has CXX extension.
        // Cannot test for C extensions because you can have C++ code inside .c files, for instance.
        if (SourceType.isCxxExtension(SpecsIo.getExtension(getFilename()))) {
            return true;
        }

        // 2) Check if file has CXX-exclusive nodes
        return getDescendantsStream()
                .filter(ClavaNode::isCxxNode)
                .findFirst()
                .isPresent();
    }

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
        // Get relative path to include the file in this file
        String relativePath = translationUnit.getRelativeFilepath();

        addInclude(relativePath, isAngled);
    }

    public void addInclude(String includeName, boolean isAngled) {
        addIncludePrivate(getFactory().includeDecl(includeName, isAngled));
    }

    public void addInclude(IncludeDecl include) {
        addInclude(include.getInclude().getInclude(), include.getInclude().isAngled());
    }

    public void addCInclude(String includeName, boolean isAngled) {
        addIncludePrivate(getFactory().cIncludeDecl(includeName, isAngled));
    }

    public void addDeclaration(NamedDecl namedDecl) {

        DeclStmt stmt = getFactory().declStmt(namedDecl);

        // Find insertion point. Insert after last include
        List<ClavaNode> includes = getChildrenStream()
                .filter(child -> child instanceof IncludeDecl)
                .collect(Collectors.toList());

        if (includes.isEmpty()) {
            // Add at the beginning of the translation unit
            addChild(0, stmt);
            return;
        }

        NodeInsertUtils.insertAfter(includes.get(includes.size() - 1), stmt);

        /*
        // Find insertion point. Insert before first function declaration
        Optional<ClavaNode> firstFunction = getChildrenStream()
                .filter(child -> child instanceof FunctionDecl)
                .findFirst();
        
        DeclStmt stmt = getFactory().declStmt(namedDecl);
        
        if (firstFunction.isPresent()) {
            NodeInsertUtils.insertBefore(firstFunction.get(), stmt);
            return;
        }
        
        
        // Add at the end of the translation unit
        addChild(getNumChildren(), stmt);
        */
    }

    public File write(File destinationFolder) {

        File actualDestinationFolder = getRelativeFolderpath()
                .map(relativePath -> SpecsIo.mkdir(new File(destinationFolder, relativePath)))
                .orElse(destinationFolder);

        File destinationFile = new File(actualDestinationFolder, getFilename());

        SpecsIo.write(destinationFile, getCode());

        return destinationFile;
    }

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
    }

    /**
     * The path of this file, relative to the include folders of the project.
     *
     * @return
     */
    public String getRelativeFilepath() {
        return getRelativeFolderpath()
                .map(relativePath -> relativePath + "/" + getFilename())
                .orElse(getFilename());
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

        // Check if there is a relative path defined
        Optional<String> relativeFolderpathTry = getRelativeFolderpath();
        if (!relativeFolderpathTry.isPresent()) {
            return baseFolder;
        }

        String relativeFolderpath = relativeFolderpathTry.get();

        return new File(baseFolder, relativeFolderpath);
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
