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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodes;
import pt.up.fe.specs.clava.ClavaOptions;
import pt.up.fe.specs.clava.SourceRange;
import pt.up.fe.specs.clava.ast.decl.CXXRecordDecl;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.decl.NamespaceDecl;
import pt.up.fe.specs.clava.ast.expr.CallExpr;
import pt.up.fe.specs.clava.ast.extra.data.IdNormalizer;
import pt.up.fe.specs.clava.ast.type.FunctionType;
import pt.up.fe.specs.clava.ast.type.RecordType;
import pt.up.fe.specs.clava.language.Standard;
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

    /// DATAKEYS BEGIN

    /**
     * External dependencies of this application.
     */
    public final static DataKey<ExternalDependencies> EXTERNAL_DEPENDENCIES = KeyFactory
            .object("externalDependencies", ExternalDependencies.class)
            .setDefault(() -> new ExternalDependencies())
            .setCopyFunction(dependencies -> dependencies.copy());

    /**
     * Can be used to store arbitrary information that should be accessible through the application
     *
     */
    public final static DataKey<DataStore> APP_DATA = KeyFactory.object("appData", DataStore.class)
            .setDefault(() -> DataStore.newInstance("Clava App Arbitrary Data"))
            .setCopyFunction(dataStore -> DataStore.newInstance(dataStore.getName(), dataStore));

    public final static DataKey<Boolean> HAS_PARSING_ERRORS = KeyFactory.bool("hasParsingErrors");

    /// DATAKEYS END

    // private static final FunctionDecl NO_FUNCTION_FOUND = ClavaNodeFactory.dummyFunctionDecl("No Function Found");

    // private List<File> sources;
    // private Map<String, File> sourceFiles;
    private Map<File, File> sourceFiles;
    private Map<File, String> sourceFoldernames;

    private GlobalManager globalManager;
    private final Map<String, ClavaNode> nodesCache;
    private final Map<String, FunctionDecl> functionDeclarationCache;
    private final Map<String, FunctionDecl> functionDefinitionCache;

    private final IdNormalizer idNormalizer;
    private final CallInliner callInliner;

    private FunctionDecl noFunctionFound = null;

    // private ExternalDependencies externalDependencies;
    // private Map<String, String> idsAlias;
    // private Map<String, List<Stmt>> inlineCache;

    // Can be used to store information that should be accessible through the application
    // private DataStore appData;

    /**
     * Legacy.
     *
     * @deprecated
     * @param children
     */
    /*
    @Deprecated
    public App(Collection<TranslationUnit> children) {
        super(ClavaNodeInfo.undefinedInfo(), children);
    
        // sources = Collections.emptyList();
        sourceFiles = new HashMap<>();
        globalManager = new GlobalManager();
        nodesCache = new HashMap<>();
        functionDeclarationCache = new HashMap<>();
        functionDefinitionCache = new HashMap<>();
        appData = DataStore.newInstance("Clava App Data");
    
        idNormalizer = new IdNormalizer();
        callInliner = new CallInliner(idNormalizer);
    
        externalDependencies = new ExternalDependencies();
        // this.idsAlias = Collections.emptyMap();
        // this.inlineCache = new HashMap<>();
    
        // System.out.println("SETTING STANDARD:" + appData.get(ClavaOptions.STANDARD));
    
        // CURRENT_STANDARD.set(appData.get(ClavaOptions.STANDARD));
    }
    */

    /**
     *
     * @param dataStore
     * @param children
     */
    public App(DataStore dataStore, Collection<? extends ClavaNode> children) {
        super(dataStore, children);
        // super(ClavaNodeInfo.undefinedInfo(), children);

        // sources = Collections.emptyList();
        sourceFiles = new HashMap<>();
        globalManager = new GlobalManager();
        nodesCache = new HashMap<>();
        functionDeclarationCache = new HashMap<>();
        functionDefinitionCache = new HashMap<>();
        // appData = DataStore.newInstance("Clava App Data");

        idNormalizer = new IdNormalizer();
        callInliner = new CallInliner(idNormalizer);

        // normalizeFields();

        // getDataI().add(EXTERNAL_DEPENDENCIES, new ExternalDependencies());
        // externalDependencies = new ExternalDependencies();
        // this.idsAlias = Collections.emptyMap();
        // this.inlineCache = new HashMap<>();

        // System.out.println("SETTING STANDARD:" + appData.get(ClavaOptions.STANDARD));

        // CURRENT_STANDARD.set(appData.get(ClavaOptions.STANDARD));
    }

    /**
     * Clears cached data.
     */
    public void clearCache() {
        nodesCache.clear();
        functionDeclarationCache.clear();
        functionDefinitionCache.clear();
    }

    /*
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
    */

    // /**
    // * Ensures that fields of the AST point to nodes in other TranslationUnits, when that is the case.
    // */
    // @SuppressWarnings("unchecked")
    // private void normalizeFields() {
    // Map<String, ClavaNode> locationToNodeMap = new HashMap<>();
    // for (TranslationUnit tUnit : getTranslationUnits()) {
    // tUnit.getDescendantsAndSelfStream()
    // .filter(node -> node.getLocation().isValid())
    // .filter(node -> !IGNORE_CLASSES.contains(node.getClass()))
    // .forEach(node -> App.addNode(node, locationToNodeMap));
    // // .collect(Collectors.toMap(node -> node.getLocation(), node -> node));
    // }
    //
    // for (ClavaNode node : locationToNodeMap.values()) {
    // for (DataKey<?> key : node.getKeysWithNodes()) {
    // // Field is a ClavaNode
    // if (key.getValueClass().equals(ClavaNode.class)) {
    // ClavaNode value = (ClavaNode) node.get(key);
    // ClavaNode normalizedNode = locationToNodeMap.get(getLocationId(value));
    // if (normalizedNode == null) {
    // continue;
    // }
    // System.out.println("REPLACING " + value + " with " + normalizedNode);
    // node.set((DataKey<Object>) key, normalizedNode);
    // continue;
    // }
    //
    // // Field is a Optional<ClavaNode>
    // if (key.getValueClass().equals(Optional.class)) {
    // ClavaNode value = ((Optional<ClavaNode>) node.get(key)).get();
    // ClavaNode normalizedNode = locationToNodeMap.get(getLocationId(value));
    // if (normalizedNode == null) {
    // continue;
    // }
    // System.out.println("REPLACING " + value + " with " + normalizedNode);
    // node.set((DataKey<Object>) key, Optional.of(normalizedNode));
    // continue;
    // }
    //
    // ClavaLog.warning("Case not implemented: " + key);
    // }
    // }
    //
    // // TODO Auto-generated method stub
    //
    // }
    //
    // private static void addNode(ClavaNode node, Map<String, ClavaNode> locationToNodeMap) {
    // String id = getLocationId(node);
    // ClavaNode previousNode = locationToNodeMap.put(id, node);
    // SpecsCheck.checkArgument(previousNode == null,
    // () -> "Found repeated location '" + id + "'.\nOriginal node: " + previousNode
    // + "\nNew node:" + node);
    // }
    //
    // private static String getLocationId(ClavaNode node) {
    // return node.getClass().getSimpleName() + "_" + node.getLocation();
    // }

    public DataStore getAppData() {
        return get(APP_DATA);
        // return appData;
    }

    public ExternalDependencies getExternalDependencies() {
        return get(EXTERNAL_DEPENDENCIES);
        // return externalDependencies;
    }

    public void setIdsAlias(Map<String, String> idsAlias) {
        // this.idsAlias = idsAlias;
        idNormalizer.addAlias(idsAlias);
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
    // public List<File> write(File destinationFolder) {
    // // Previous default behavior was to flatten the structure of the output folder
    // return write(destinationFolder, true);
    // }

    /**
     * Writes the source-code of the current application to the given destination folder.
     * 
     * @param destinationFolder
     * @return
     */
    public List<File> write(File destinationFolder) {
        List<File> writtenFiles = new ArrayList<>();

        for (Entry<File, String> entry : getCode(destinationFolder).entrySet()) {
            SpecsIo.write(entry.getKey(), entry.getValue());
            writtenFiles.add(entry.getKey());
        }

        return writtenFiles;
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
        // and using a path relative to the source path

        for (TranslationUnit tUnit : getTranslationUnits()) {
            // File destinationFile = buildDestinationFile(tUnit, destinationFolder, flattenFolders);
            File destinationFile = tUnit.getDestinationFile(destinationFolder);
            String code = getTuCode(tUnit, enableModifiedFilesFilter, modifiedFiles);

            files.put(destinationFile, code);

            // files.put(destinationFile, tUnit.getCode());
        }

        return files;
    }

    /*
    public File buildDestinationFile(TranslationUnit tUnit, File destinationFolder, boolean flattenFolders) {
        File actualDestinationFolder = tUnit.getDestinationFolder(destinationFolder, flattenFolders);
               String relativePath = tUnit.getRelativeFolderpath();
    
        // Build destination path
        actualDestinationFolder = SpecsIo.mkdir(new File(actualDestinationFolder, relativePath));
    
        return new File(actualDestinationFolder, tUnit.getFilename());
    }
    */
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
                // .filter(node -> node.getId().isPresent())
                // .filter(node -> node.getId().get().getExtendedId().equals(normalizedId))
                .filter(node -> node.getExtendedId().isPresent())
                .filter(node -> node.getExtendedId().get().equals(normalizedId))
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

        // ClavaLog.debug("Looking for function declaration for " + declName);

        // Check if node was already asked
        FunctionDecl cachedNode = cache.get(getFunctionId(declName, functionType));
        if (cachedNode != null) {
            // Check if no function is available
            if (cachedNode == getNoFunctionFound()) {
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
                // .filter(fdecl -> fdecl.getFunctionType().getCode().equals(functionType.getCode()))
                .filter(fdecl -> fdecl.getFunctionType().equals(functionType))
                // Filter by const
                .filter(fdecl -> fdecl.getFunctionType().isConst() == functionType.isConst())
                .findFirst()
                // Normalize FunctionDecl before returning
                .map(fdecl -> (FunctionDecl) ClavaNodes.normalizeDecl(fdecl));

        // Store return in cache
        cache.put(getFunctionId(declName, functionType), functionDeclaration.orElse(getNoFunctionFound()));

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
        return getCXXRecordDeclTry(namespace, declName)
                .orElseThrow(() -> new RuntimeException(
                        "Could not find CXXRecordDecl with name '" + (namespace == null ? "" : namespace + "::")
                                + declName + "'"));
    }

    public Optional<CXXRecordDecl> getCXXRecordDeclTry(String namespace, String declName) {
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
                .findFirst();
        // .orElseThrow(() -> new RuntimeException(
        // "Could not find CXXRecordDecl with name '" + (namespace == null ? "" : namespace + "::")
        // + declName + "'"));

    }

    /**
     * Helper method that accepts a RecordType.
     *
     * @param recordType
     * @return
     */
    public Optional<CXXRecordDecl> getCXXRecordDeclTry(RecordType recordType) {
        return getCXXRecordDeclTry(recordType.getNamespace().orElse(null), recordType.getSimpleRecordName());
    }

    public GlobalManager getGlobalManager() {
        return globalManager;
    }

    public TranslationUnit addFile(TranslationUnit tu) {
        return (TranslationUnit) addChild(tu);
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
     * Helper method that accepts a map that has String instead of File as keys.
     *
     * @param allStringFiles
     */
    public void setSourcesFromStrings(Map<String, File> allStringFiles) {
        Map<File, File> allFiles = new HashMap<>();

        for (Entry<String, File> entry : allStringFiles.entrySet()) {
            allFiles.put(new File(entry.getKey()), entry.getValue());
        }

        setSources(allFiles);
    }

    /**
     * Sets the sources of each Translation Unit that will be used to calculate the relative paths.
     * 
     * <p>
     * Receives a map of App files to the corresponding base source folder
     *
     * @param allFiles
     */
    public void setSources(Map<File, File> allFiles) {

        // Set sourceFiles
        sourceFiles = allFiles;

        // System.out.println("ALL FILES:" + allFiles);
        for (TranslationUnit tu : getTranslationUnits()) {
            // System.out.println("FINDING RELATIVE PATH FOR " + tu.getFile());
            // Find the corresponding source
            File sourcePath = sourceFiles.get(tu.getFile());
            // System.out.println("SOURCE PATH: " + sourcePath);
            if (sourcePath == null) {
                tu.setRelativePath(null);
                continue;
            }

            // Calculate the relative path between the sourcePath and the file folder
            String relativePath = SpecsIo.getRelativePath(tu.getFile().getParentFile(), sourcePath);

            tu.setRelativePath(relativePath);
        }
    }

    /**
     * Sets the source folder names of each Translation Unit, which will be used to determine the folder to which the
     * source will be written.
     * 
     * <p>
     * Receives a map of App files to the corresponding source folder name, if any
     *
     * @param allFiles
     */
    public void setSourceFoldernames(Map<File, String> sourceFoldernames) {

        // Set sourceFiles
        this.sourceFoldernames = sourceFoldernames;

        for (TranslationUnit tu : getTranslationUnits()) {
            // Find the corresponding source
            String sourceFoldername = sourceFoldernames.get(tu.getFile());

            // No problem if sourceFoldername is null, will be converted to Optional.empty()
            tu.setOptional(TranslationUnit.SOURCE_FOLDERNAME, sourceFoldername);
        }
    }

    public void addConfig(DataStore config) {
        get(APP_DATA).addAll(config);
        // appData.addAll(config);
    }

    public Standard getStandard() {
        // TODO: Should standard be in Context instead?
        return get(APP_DATA).get(ClavaOptions.STANDARD);
        // return appData.get(ClavaOptions.STANDARD);
    }

    private FunctionDecl getNoFunctionFound() {
        if (noFunctionFound == null) {
            noFunctionFound = getFactory().functionDecl("No Function Found",
                    getFactory().dummyType("dummy function type"));
        }

        return noFunctionFound;
        // private static final FunctionDecl NO_FUNCTION_FOUND = ClavaNodeFactory.dummyFunctionDecl("No Function
        // Found");

    }

    public TranslationUnit getTranslationUnit(SourceRange location) {
        for (TranslationUnit unit : getTranslationUnits()) {
            if (unit.getLocation().toString().equals(location.toString())) {
                return unit;
            }
        }

        throw new RuntimeException("Could not find");
    }

    /**
     * Creates a copy of this App, pushes it to the stack of Apps in ClavaContext, returns the new copied App.
     *
     * @return the new App in the top of the stack.
     */
    public App pushAst() {
        // Create a copy of the App
        App newApp = (App) copy(true);

        // Push new App in ClavaContext
        get(App.CONTEXT).pushApp(newApp);

        return newApp;
    }

    /**
     * Removes the top-most App from the stack
     *
     * @return
     */
    // public App popAst() {
    // // Pop top-most App in ClavaContext
    // return get(App.CONTEXT).popApp();
    //
    // // Return app now on top
    // // return get(App.CONTEXT).getApp();
    // }

    /**
     * When an App is copied, it needs to create a new ClavaContext for itself.
     */
    /*
    @Override
    public App copy(boolean keepId) {
    
        ClavaContext currentContext = getContext();
    
        // Create new context
        ClavaContext newContext = new ClavaContext(currentContext);
    
        // Before copy, temporarily set new context, so that all copied nodes point to the new Context
        // set(CONTEXT, newContext);
    
        // Copy App
        App newApp = (App) super.copy(keepId);
    
        System.out.println("ORIGINAL APP:" + hashCode());
        System.out.println("COPY APP:" + newApp.hashCode());
    
        // Restore current context
        // set(CONTEXT, currentContext);
    
        // Set the new App in the new Context
        newContext.set(ClavaContext.APP, newApp);
    
        // Set the new Context in all new App nodes
        newApp.getDescendantsAndSelfStream().forEach(node -> node.set(CONTEXT, newContext));
    
        return newApp;
    }
    */
}
