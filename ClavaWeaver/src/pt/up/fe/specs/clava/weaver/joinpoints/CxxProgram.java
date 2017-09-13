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

package pt.up.fe.specs.clava.weaver.joinpoints;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaOptions;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.abstracts.ACxxWeaverJoinPoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AFile;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AProgram;
import pt.up.fe.specs.util.SpecsLogs;

public class CxxProgram extends AProgram {

    private final String name;
    private final App app;
    private final CxxWeaver weaver;
    // private final File baseFolder;

    // private final List<String> parserOptions;

    // public CxxProgram(File baseFolder, App app, List<String> parserOptions) {
    // this.baseFolder = baseFolder;
    // this.app = app;
    // this.parserOptions = parserOptions;
    // }

    public CxxProgram(String name, App app, CxxWeaver weaver) {
        this.name = name;
        this.app = app;
        this.weaver = weaver;
    }

    @Override
    public List<? extends AFile> selectFile() {
        return app.getTranslationUnits().stream()
                .map(tunit -> CxxJoinpoints.create(tunit, this, AFile.class))
                .collect(Collectors.toList());
    }

    @Override
    public App getNode() {
        return app;
    }

    @Override
    public String getNameImpl() {
        return name;
    }

    /**
     * CxxProgram is the top-level node.
     */
    @Override
    public ACxxWeaverJoinPoint getParentImpl() {
        return null;
    }

    public CxxWeaver getWeaver() {
        return weaver;
    }

    @Override
    public void rebuildImpl() {
        SpecsLogs.msgInfo("Rebuilding tree...");
        weaver.rebuildAst(true);
    }

    @Override
    public void addFileImpl(AFile file) {
        TranslationUnit tu = (TranslationUnit) file.getNode();
        app.addFile(tu);
    }

    @Override
    public String[] getIncludeFoldersArrayImpl() {
        Set<String> includeFolders = weaver.getIncludeFolders();

        return includeFolders.toArray(new String[0]);
    }

    @Override
    public String getStandardImpl() {
        return weaver.getConfig().get(ClavaOptions.STANDARD).getString();
    }

    @Override
    public String getStdFlagImpl() {
        return weaver.getStdFlag();
    }

    @Override
    public String[] getDefaultFlagsArrayImpl() {
        return CxxWeaver.getDefaultFlags().toArray(new String[0]);
    }

    @Override
    public String[] getUserFlagsArrayImpl() {
        return weaver.getUserFlags().toArray(new String[0]);
    }

    @Override
    public void messageToUserImpl(String message) {
        weaver.addMessageToUser(message);
    }

    @Override
    public String getBaseFolderImpl() {
        List<File> sources = getWeaver().getSources();
        if (sources.isEmpty()) {
            SpecsLogs.msgWarn("Expected at least program to have one source folder, found none");
            return null;
        }
        File path = sources.get(0);
        File baseFolder = path.isFile() ? path.getParentFile() : path;

        return baseFolder.getAbsolutePath();
    }

    public DataStore getAppData() {
        return app.getAppData();
    }

    @Override
    public String getCodeImpl() {
        return app.getCode(new File(getBaseFolderImpl()));
    }

    @Override
    public void pushImpl() {
        weaver.pushAst();
    }

    @Override
    public void popImpl() {
        weaver.popAst();
    }
}
