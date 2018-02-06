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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.SourceRange;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.decl.CXXRecordDecl;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.decl.NamespaceDecl;
import pt.up.fe.specs.clava.ast.expr.CallExpr;
import pt.up.fe.specs.clava.ast.extra.data.IdNormalizer;
import pt.up.fe.specs.clava.ast.type.FunctionType;
import pt.up.fe.specs.clava.transform.call.CallInliner;
import pt.up.fe.specs.clava.utils.ExternalDependencies;
import pt.up.fe.specs.clava.utils.GlobalManager;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;

/**
 * Represents an application composed by a set of TranslationUnits.
 *
 * @author JoaoBispo
 *
 */
public class App extends ClavaNode {

    // This limits the number of App objects to one per thread
    // This is being used because o Qualifier needs to generate different code for restrict
    // depending on the current compilation standard
    // private static final ThreadLocal<Standard> CURRENT_STANDARD = new ThreadLocal<>();

    // public static Standard getCurrentStandard() {
    // Standard standard = CURRENT_STANDARD.get();
    // if (standard == null) {
    // SpecsLogs.msgLib("App.getCurrentStandard: App object has not been initialized");
    // }
    //
    // return standard;
    // }

    // private static final Set<String> EXTENSIONS_IMPLEMENTATION = new HashSet<>(Arrays.asList("c", "cpp", "cl"));
    // private static final Set<String> EXTENSIONS_HEADERS = new HashSet<>(Arrays.asList("h", "hpp"));
    // private static final Set<String> PERMITTED_EXTENSIONS = new HashSet<>(
    // SpecsCollections.concat(EXTENSIONS_IMPLEMENTATION, EXTENSIONS_HEADERS));

    // public static Set<String> getExtensionsHeaders() {
    // return EXTENSIONS_HEADERS;
    // }

    // public static Set<String> getExtensionsImplementation() {
    // return EXTENSIONS_IMPLEMENTATION;
    // }

    // public static Set<String> getPermittedExtensions() {
    // return PERMITTED_EXTENSIONS;
    // }

    // public static SourceType getSourceType(String filepath) {
    // return
    // }

    // public static boolean isImplementationFile(String filepath) {
    // return EXTENSIONS_IMPLEMENTATION.contains(SpecsIo.getExtension(filepath));
    // }
    //
    // public static boolean isHeaderFile(String filepath) {
    // return EXTENSIONS_HEADERS.contains(SpecsIo.getExtension(filepath));
    // }

    private static final FunctionDecl NO_FUNCTION_FOUND = ClavaNodeFactory.dummyFunctionDecl("No Function Found");

    // private List<File> sources;
    private Map<String, File> sourceFiles;

    private GlobalManager globalManager;
    private final Map<String, ClavaNode> nodesCache;
    private final Map<String, FunctionDecl> functionDeclarationCache;
    private final Map<String, FunctionDecl> functionDefinitionCache;

    private final IdNormalizer idNormalizer;
    private final CallInliner callInliner;

    private ExternalDependencies externalDependencies;
    // private Map<String, String> idsAlias;
    // private Map<String, List<Stmt>> inlineCache;

    // Can be used to store information that should be accessible through the application
    private DataStore appData;

    public App(Collection<TranslationUnit> children) {
        super(ClavaNodeInfo.undefinedInfo(), children);

        // sources = Collections.emptyList();
        sourceFiles = new HashMap<>();
        globalManager = new GlobalManager();
        nodesCache = new HashMap<>();
        functionDeclarationCache = new HashMap<>();
        functionDefinitionCache = new HashMap<>();
        appData = DataStore.newInstance("Clava App Data");

        this.idNormalizer = new IdNormalizer();
        this.callInliner = new CallInliner(idNormalizer);

        this.externalDependencies = new ExternalDependencies();
        // this.idsAlias = Collections.emptyMap();
        // this.inlineCache = new HashMap<>();

        // System.out.println("SETTING STANDARD:" + appData.get(ClavaOptions.STANDARD));

        // CURRENT_STANDARD.set(appData.get(ClavaOptions.STANDARD));
    }

    @Override
    protected ClavaNode copyPrivate() {
        App appCopy = new App(Collections.emptyList());

        // Copy fields of App DataStore
        appCopy.appData.addAll(appData);

        appCopy.externalDependencies = externalDependencies.copy();
        // Fields that might need to be copied:
        // globalManager
        // idNormalizer
        // callInliner

        return appCopy;
    }

    public DataStore getAppData() {
        return appData;
    }

    public ExternalDependencies getExternalDependencies() {
        return externalDependencies;
    }

    public void setIdsAlias(Map<String, String> idsAlias) {
        // this.idsAlias = idsAlias;
        this.idNormalizer.addAlias(idsAlias);
    }

    /**
     * getLocation() is not implemented for AppNode
     */
    @Override
    public SourceRange getLocation() {
        throw new RuntimeException("Not implemented for AppNode");
    }

    @Override
    public String getCode() {
        // return getCode(null);
        // }
        //
        // public String getCode() {
        StringBuilder code = new StringBuilder();

        for (TranslationUnit tu : getTranslationUnits()) {

            // String basepath = baseFolder != null ? SpecsIo.getRelativePath(new File(tu.getFolderpath()), baseFolder)
            // : tu.getFolderpath();
            //
            // code.append("/**** File '" + basepath + tu.getFilename() + "' ****/" + ln() + ln());
            code.append("/**** File '" + SpecsIo.normalizePath(tu.getRelativeFilepath()) + "' ****/"
                    + ln() + ln());
            code.append(tu.getCode());
            code.append(ln() + "/**** End File ****/" + ln() + ln());
        }

        return code.toString();
    }

    // public void setSources(List<File> sources) {
    // this.sources = sources;
    // }

    public List<TranslationUnit> getTranslationUnits() {
        return getChildren(TranslationUnit.class);
    }

    /*
    public void writeFromTopFile(File topFile, File destinationFolder) {
        // Check if topFile exists in the translation units
        String canonicalPath = SpecsIo.getCanonicalPath(topFile);
        if (!getTranslationUnits().stream()
                .filter(tu -> SpecsIo.getCanonicalPath(tu.getFile()).equals(canonicalPath))
                .findFirst()
                .isPresent()) {
    
            SpecsLogs.msgInfo(
                    "Could not find top file '" + topFile + "' in the translation units, returning without writing");
            return;
        }
    
        write(destinationFolder);
    
    }
    */

    // public void write(File baseInputFolder, File destinationFolder) {
    // write(baseInputFolder, destinationFolder, null);
    // }

    /**
     * 
     * @param baseInputFolder
     * @param destinationFolder
     */
    public void write(File destinationFolder) {
        for (Entry<File, String> entry : getCode(destinationFolder).entrySet()) {
            SpecsIo.write(entry.getKey(), entry.getValue());
        }
    }

    /**
     * TODO: Need map from generated files to original files
     * 
     * @param baseInputFolder
     * @param destinationFolder
     * @param filesToGenerate
     */
    /*
    public void write(File baseInputFolder, File destinationFolder, Set<String> filesToGenerate) {
        Map<File, String> codeMap = getCode(baseInputFolder, destinationFolder);
    
        boolean filterFilesToGenerate = enableModifiedFilesFilter(codeMap, filesToGenerate);
    
        for (Entry<File, String> entry : codeMap.entrySet()) {
            if (filterFilesToGenerate) {
                if (!filesToGenerate.contains(entry.getKey().getName())) {
    
                    continue;
                }
            }
    
            SpecsIo.write(entry.getKey(), entry.getValue());
        }
    }
    */
    // private static List<File> getAllSourcefiles(List<File> sources) {
    // return getAllSourcefiles(sources, false);
    // }

    private boolean enableModifiedFilesFilter(List<File> programFiles, Set<String> modifiedFiles) {
        // If set of files to generate is null, return false
        if (modifiedFiles == null) {
            return false;
        }

        // Check if all files have different names
        Set<String> filenames = new HashSet<>();
        for (File file : programFiles) {
            boolean newElement = filenames.add(file.getName());
            if (!newElement) {
                SpecsLogs.msgInfo("Cannot use generation of modified files only, found two files with the same name: '"
                        + file.getName() + "'");
                return false;
            }
        }

        return true;
    }

    /*
    private static List<File> getAllSourcefiles(List<File> sources, boolean includeHeaders) {
    
        if (includeHeaders) {
            // return SpecsIo.getFiles(sources, PERMITTED_EXTENSIONS).stream()
            return SpecsIo.getFiles(sources, SourceType.getPermittedExceptions());
            // return SpecsIo.getFiles(sources, SourceType.getPermittedExceptions()).stream()
            // .map(filename -> new File(filename))
            // .collect(Collectors.toList());
        }
    
        List<File> allFiles = new ArrayList<>();
    
        // List<File> implementationFiles = SpecsIo.getFiles(sources, EXTENSIONS_IMPLEMENTATION).stream()
        List<File> implementationFiles = SpecsIo.getFiles(sources, SourceType.IMPLEMENTATION.getExtensions());
        // List<File> implementationFiles = SpecsIo.getFiles(sources,
        // SourceType.IMPLEMENTATION.getExtensions()).stream()
        // .map(filename -> new File(filename))
        // .collect(Collectors.toList());
    
        allFiles.addAll(implementationFiles);
    
        for (File sourceFolder : sources) {
            // allFiles.addAll(SpecsIo.getFilesRecursive(sourceFolder, EXTENSIONS_HEADERS));
            allFiles.addAll(SpecsIo.getFilesRecursive(sourceFolder, SourceType.HEADER.getExtensions()));
        }
    
        return allFiles;
    }
    */
    public Map<File, String> getCode(File destinationFolder) {
        return getCode(destinationFolder, null);
    }

    /**
     * 
     * @param baseInputFolder
     * @param destinationFolder
     * @param modifiedFiles
     *            a set of filenames which is a white-list for files that should be generated from the AST. If null,
     *            generates all files from the AST
     * @return
     */
    public Map<File, String> getCode(File destinationFolder, Set<String> modifiedFiles) {

        Map<File, String> files = new HashMap<>();

        boolean enableModifiedFilesFilter = enableModifiedFilesFilter(getFiles(), modifiedFiles);

        // Generate code for each translation unit considering the given destination
        // and using a path relative to the topFile

        for (TranslationUnit tUnit : getTranslationUnits()) {
            // String relativePath_old = ClavaCode.getRelativePath(new File(tUnit.getFolderpath()), baseInputFolder);
            // String relativePath = tUnit.getRelativeFolderpath(baseInputFolder);
            String relativePath = tUnit.getRelativeFolderpath();

            // Build destination path
            File actualDestinationFolder = SpecsIo.mkdir(new File(destinationFolder, relativePath));
            File destinationFile = new File(actualDestinationFolder, tUnit.getFilename());

            String code = getTuCode(tUnit, enableModifiedFilesFilter, modifiedFiles);

            files.put(destinationFile, code);
            // files.put(destinationFile, tUnit.getCode());
        }

        // List<File> localSources = sources.isEmpty() ? Arrays.asList(baseInputFolder) : sources;
        // List<File> allFiles = getAllSourcefiles(localSources, true);

        // System.out.println("All files:" + allFiles);

        Set<String> relativeWoven = files.keySet().stream()
                .map(file -> SpecsIo.getRelativePath(file, destinationFolder))
                .collect(Collectors.toSet());
        // System.out.println("GENERATED FILES:" + relativeWoven);

        // for (File file : allFiles) {
        for (Entry<String, File> sourceFile : sourceFiles.entrySet()) {
            File file = new File(sourceFile.getKey());

            // String relativeSource = SpecsIo.getRelativePath(file, baseInputFolder);
            // String clavaCodeOutput = ClavaCode.getRelativePath(file, baseInputFolder);
            String clavaCodeOutput = TranslationUnit.getRelativePath(file, sourceFile.getValue());
            // if (!relativeSource.equals(clavaCodeOutput)) {
            // SpecsLogs.msgWarn("TEMPORARY TEST: expected '" + clavaCodeOutput + "', got '" + relativeSource + "'");
            // }

            if (relativeWoven.contains(clavaCodeOutput)) {
                continue;
            }

            SpecsLogs.msgInfo("Creating empty source for file '" + clavaCodeOutput + "'");

            // Create translation unit for the missing file
            String relativePath = SpecsIo.getRelativePath(file.getParentFile(), sourceFile.getValue());
            // String relativePath = SpecsIo.getRelativePath(file.getParentFile(), baseInputFolder);

            // Avoid writing outside of the destination folder, if relative path has '../', remove them
            while (relativePath.startsWith("../")) {
                relativePath = relativePath.substring("../".length());
            }

            // Build destination path
            File actualDestinationFolder = SpecsIo.mkdir(new File(destinationFolder, relativePath));
            File destinationFile = new File(actualDestinationFolder, file.getName());

            TranslationUnit tUnit = ClavaNodeFactory.translationUnit(file.getName(), file.getParent(),
                    Collections.emptyList());

            files.put(destinationFile, tUnit.getCode());
        }

        return files;
    }

    private String getTuCode(TranslationUnit tUnit, boolean enableModifiedFilesFilter, Set<String> modifiedFiles) {
        // If modified files filter is not enable, generate code from the translation unit
        if (!enableModifiedFilesFilter) {
            return tUnit.getCode();
        }

        // If file was modified, generate code from the translation uni
        if (modifiedFiles.contains(tUnit.getFilename())) {
            return tUnit.getCode();
        }

        File originalFile = tUnit.getFile();

        // If the original file does not exist, generate code
        if (!originalFile.isFile()) {
            return tUnit.getCode();
        }

        // Otherwise, return the original file
        // String relativeSource = ClavaCode.getRelativePath(originalFile, baseInputFolder);
        // String relativeSource = tUnit.getRelativeFilepath(baseInputFolder);
        String relativeSource = tUnit.getRelativeFilepath();
        SpecsLogs.msgInfo("Using original source for file '" + relativeSource + "'");
        return SpecsIo.read(originalFile);
    }

    /**
     * 
     * @return list of current files in the program
     */
    public List<File> getFiles() {
        return getTranslationUnits().stream()
                .map(TranslationUnit::getFile)
                .collect(Collectors.toList());
    }

    // public String getRelativePath(File baseInputFolder, TranslationUnit tUnit) {
    // return ClavaCode.getRelativePath(tUnit.getFile(), baseInputFolder);
    // }
    //
    // public String getRelativeFolderPath(File baseInputFolder, TranslationUnit tUnit) {
    // return ClavaCode.getRelativePath(tUnit.getFile().getParentFile(), baseInputFolder);
    // }

    public Optional<TranslationUnit> getFile(String filename) {
        return getTranslationUnits().stream()
                .filter(tu -> tu.getFilename().equals(filename))
                .findFirst();
    }

    public ClavaNode getNode(String id) {
        return getNodeTry(id)
                .orElseThrow(() -> new RuntimeException("Could not find node with id '" + id + "'"));
    }

    public Optional<ClavaNode> getNodeTry(String id) {

        // Check if id is an alias
        String normalizedId = idNormalizer.normalize(id);

        // Check if node was already asked
        ClavaNode cachedNode = nodesCache.get(normalizedId);
        if (cachedNode != null) {
            return Optional.of(cachedNode);
        }

        Optional<ClavaNode> askedNode = getDescendantsAndSelfStream()
                .filter(node -> node.getId().isPresent())
                .filter(node -> node.getId().get().getExtendedId().equals(normalizedId))
                .findFirst();

        askedNode.ifPresent(node -> nodesCache.put(normalizedId, node));

        return askedNode;
    }

    // private String normalizeId(String id) {
    // String unaliasedId = idsAlias.get(id);
    //
    // return unaliasedId != null ? unaliasedId : id;
    // }

    public Optional<FunctionDecl> getFunctionDeclaration(String declName, FunctionType functionType) {
        return getFunctionDeclaration(declName, functionType, functionDeclarationCache, false);
    }

    public Optional<FunctionDecl> getFunctionDefinition(String declName, FunctionType functionType) {
        return getFunctionDeclaration(declName, functionType, functionDefinitionCache, true);
    }

    private Optional<FunctionDecl> getFunctionDeclaration(String declName, FunctionType functionType,
            Map<String, FunctionDecl> cache, boolean hasBody) {

        // Check if node was already asked
        FunctionDecl cachedNode = cache.get(getFunctionId(declName, functionType));
        if (cachedNode != null) {
            // Check if no function is available
            if (cachedNode == NO_FUNCTION_FOUND) {
                return Optional.empty();
            }

            return Optional.of(cachedNode);
        }

        Optional<FunctionDecl> functionDeclaration = getDescendantsStream()
                .filter(FunctionDecl.class::isInstance)
                .map(FunctionDecl.class::cast)
                // Check hasBody flag
                .filter(fdecl -> fdecl.hasBody() == hasBody)
                // Filter by name
                .filter(fdecl -> fdecl.getDeclName().equals(declName))
                // Filter by type
                .filter(fdecl -> fdecl.getFunctionType().getCode().equals(functionType.getCode()))
                .findFirst();

        // Store return in cache
        cache.put(getFunctionId(declName, functionType), functionDeclaration.orElse(NO_FUNCTION_FOUND));

        return functionDeclaration;
    }

    private static String getFunctionId(String declName, FunctionType functionType) {
        return functionType.getCode(declName);
    }

    /**
     * @deprecated use the version that has a namespace as argument
     * @param declName
     * @return
     */
    @Deprecated
    public CXXRecordDecl getCXXRecordDecl(String declName) {
        return getDescendantsStream().filter(child -> child instanceof CXXRecordDecl)
                .map(child -> (CXXRecordDecl) child)
                .filter(recordDecl -> recordDecl.getDeclName().equals(declName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Could not find CXXRecordDecl with name '" + declName + "'"));

    }

    public CXXRecordDecl getCXXRecordDecl(String namespace, String declName) {
        // Iterate over translation units, NamespaceDecl and CXXRecordDecl without namespace are directly under TUs
        Stream<ClavaNode> cxxRecordCandidates = getTranslationUnits().stream()
                .flatMap(tu -> tu.getChildrenStream());

        // If there is a namespace, filter the stream for the corresponding NamespaceDecl first
        if (namespace != null) {
            cxxRecordCandidates = cxxRecordCandidates
                    .filter(child -> child instanceof NamespaceDecl)
                    .map(namespaceDecl -> (NamespaceDecl) namespaceDecl)
                    .filter(namespaceDecl -> namespaceDecl.getDeclName().equals(namespace))
                    .flatMap(namespaceDecl -> namespaceDecl.getChildrenStream());
        }

        // Find CXXRecordDecl
        return cxxRecordCandidates.filter(child -> child instanceof CXXRecordDecl)
                .map(child -> (CXXRecordDecl) child)
                .filter(recordDecl -> recordDecl.getDeclName().equals(declName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        "Could not find CXXRecordDecl with name '" + (namespace == null ? "" : namespace + "::")
                                + declName + "'"));

    }

    public GlobalManager getGlobalManager() {
        return globalManager;
    }

    public void addFile(TranslationUnit tu) {
        addChild(tu);
    }

    public boolean inline(CallExpr callExpr) {
        return callInliner.inline(callExpr);
    }

    /**
     * Looks for a node based on the filepath and the id.
     * 
     * @param filepath
     * @param astId
     * @return
     */
    public Optional<ClavaNode> find(String filepath, String astId) {

        File originalFilepath = new File(filepath);
        TranslationUnit tu = getTranslationUnits().stream()
                .filter(node -> node.getFile().equals(originalFilepath))
                .findFirst()
                .orElse(null);

        if (tu == null) {
            return Optional.empty();
        }

        return tu.getDescendantsAndSelfStream()
                // Filter nodes that do not have an id equal to the given id
                .filter(node -> node.getExtendedId().map(astId::equals).orElse(false))
                .findFirst();
    }

    public Optional<ClavaNode> find(ClavaNode node) {

        // Get translation unit
        TranslationUnit tu = node.getAncestorTry(TranslationUnit.class).orElse(null);
        if (tu == null) {
            return Optional.empty();
        }

        String id = node.getExtendedId().orElse(null);
        if (id == null) {
            return Optional.empty();
        }

        return find(tu.getFile().getPath(), id);
    }

    /**
     * Sets the sources of each Translation Unit.
     * 
     * @param allFiles
     */
    public void setSources(Map<String, File> allFiles) {

        // Set sourceFiles
        this.sourceFiles = allFiles;

        for (TranslationUnit tu : getTranslationUnits()) {
            // Find the corresponding source
            File sourcePath = allFiles.get(tu.getFilepath());
            if (sourcePath == null) {
                // SpecsLogs.msgWarn("Could not find source path for TU '" + tu.getFilepath() + "'. Table:" + allFiles);
                continue;
            }

            tu.setSourcePath(sourcePath);
        }
    }
}
