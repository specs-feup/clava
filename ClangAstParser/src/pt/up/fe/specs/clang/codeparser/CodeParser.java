/**
 * Copyright 2018 SPeCS.
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

package pt.up.fe.specs.clang.codeparser;

import java.io.File;
import java.util.List;

import org.suikasoft.jOptions.DataStore.ADataClass;
import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;

import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.context.ClavaContext;

/**
 * Parses C/C++/OpenCL code into a Clava AST.
 * 
 * @author JoaoBispo
 *
 */
// public interface CodeParser<T extends CodeParser<T>> extends DataClass<T> {
// public interface CodeParser extends DataClass<CodeParser> {
public abstract class CodeParser extends ADataClass<CodeParser> {

    // BEGIN DATAKEY

    public static final DataKey<Boolean> SHOW_CLANG_DUMP = KeyFactory.bool("showClangDump");
    public static final DataKey<Boolean> SHOW_CLANG_AST = KeyFactory.bool("showClangAst");
    public static final DataKey<Boolean> SHOW_CLAVA_AST = KeyFactory.bool("showClavaAst");
    public static final DataKey<Boolean> SHOW_CODE = KeyFactory.bool("showCode");
    public static final DataKey<Boolean> USE_CUSTOM_RESOURCES = KeyFactory.bool("useCustomResources");
    public static final DataKey<Boolean> CLEAN = KeyFactory.bool("clean").setDefault(() -> true);
    public static final DataKey<String> CUDA_GPU_ARCH = KeyFactory.string("cudaGpuArch")
            .setLabel("CUDA GPU Arch (default: sm_30)")
            .setDefaultString("sm_30");
    public static final DataKey<String> CUDA_PATH = KeyFactory.string("cudaPath")
            .setLabel("CUDA Path (leave empty if in path)")
            .setDefaultString("");

    /**
     * Execution information, such as execution time and memory used.
     */
    public static final DataKey<Boolean> SHOW_EXEC_INFO = KeyFactory.bool("showExecInfo").setDefault(() -> true);

    /**
     * Applies several transformations to the Clava AST when parsing (e.g., normalization passes). By default is
     * enabled.
     */
    // public static final DataKey<Boolean> PROCESS_CLAVA_AST = KeyFactory.bool("processClavaAst").setDefault(() ->
    // true);

    // BEGIN DATAKEY

    public abstract App parse(List<File> sources, List<String> compilerOptions, ClavaContext context);

    public App parse(List<File> sources, List<String> compilerOptions) {
        return parse(sources, compilerOptions, new ClavaContext());
    }

    /**
     * Creates a new CodeParser instance.
     * 
     * @return currently returns an instance of ParallelCodeParser
     */
    public static CodeParser newInstance() {
        // return new MonolithicCodeParser();
        return new ParallelCodeParser();
    }
}
