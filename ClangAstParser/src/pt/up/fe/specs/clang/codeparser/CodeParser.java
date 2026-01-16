/**
 * Copyright 2018 SPeCS.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. under the License.
 */

package pt.up.fe.specs.clang.codeparser;

import org.suikasoft.jOptions.DataStore.ADataClass;
import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import pt.up.fe.specs.clang.ClangResources;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.context.ClavaContext;

import java.io.File;
import java.util.List;

/**
 * Parses C/C++/OpenCL code into a Clava AST.
 *
 * @author JoaoBispo
 *
 */
public abstract class CodeParser extends ADataClass<CodeParser> {

    // BEGIN DATAKEY

    public static final DataKey<Boolean> SHOW_CLANG_DUMP = KeyFactory.bool("showClangDump");
    public static final DataKey<Boolean> SHOW_CLAVA_AST = KeyFactory.bool("showClavaAst");
    public static final DataKey<Boolean> SHOW_CODE = KeyFactory.bool("showCode");
    public static final DataKey<Boolean> USE_CUSTOM_RESOURCES = KeyFactory.bool("useCustomResources");
    public static final DataKey<Boolean> CLEAN = KeyFactory.bool("clean").setDefault(() -> true);
    public static final DataKey<String> CUDA_GPU_ARCH = KeyFactory.string("cudaGpuArch")
            .setLabel("CUDA GPU Arch (default: sm_52)")
            .setDefaultString("sm_52");
    public static final DataKey<String> CUDA_PATH = KeyFactory.string("cudaPath")
            .setLabel("CUDA Path (empty: uses system installed; <builtin>: uses builtin version)")
            .setDefaultString("");
    public static final DataKey<File> CUSTOM_CLANG_AST_DUMPER_EXE = KeyFactory.file("customClangAstDumperExe")
            .setLabel("Custom ClangAstDumper executable file");

    public static final DataKey<File> DUMPER_FOLDER = KeyFactory.folder("dumperFolder")
            .setLabel("The work folder for the clang-dumper. Clava will look for it in this folder, and if not found, will download it. If not set, a temporary folder will be used.")
            .setDefault(ClangResources::getDefaultTempFolder);

    /**
     * Execution information, such as execution time and memory used.
     */
    public static final DataKey<Boolean> SHOW_EXEC_INFO = KeyFactory.bool("showExecInfo").setDefault(() -> true);

    // END DATAKEY

    private final static String BUILT_IN_CUDALIB = "<BUILTIN>";

    public static String getBuiltinOption() {
        return BUILT_IN_CUDALIB;
    }

    public abstract App parse(List<File> sources, List<String> compilerOptions, ClavaContext context);

    /**
     *
     * @param sources
     * @param compilerOptions flags compatible with C/C++ compilers such as Clang or GCC
     * @return
     */
    public App parse(List<File> sources, List<String> compilerOptions) {
        return parse(sources, compilerOptions, new ClavaContext());
    }

    /**
     * Creates a new CodeParser instance.
     *
     * @return currently returns an instance of ParallelCodeParser
     */
    public static CodeParser newInstance() {
        return new ParallelCodeParser();
    }
}
