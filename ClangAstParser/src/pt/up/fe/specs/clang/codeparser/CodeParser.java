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

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;

import pt.up.fe.specs.clava.ast.extra.App;

/**
 * Parses C/C++/OpenCL code into a Clava AST.
 * 
 * @author JoaoBispo
 *
 */
public interface CodeParser {

    // BEGIN DATAKEY
    public static final DataKey<Boolean> SHOW_CLANG_DUMP = KeyFactory.bool("showClangDump");
    public static final DataKey<Boolean> SHOW_CLANG_AST = KeyFactory.bool("showClangAst");
    public static final DataKey<Boolean> SHOW_CLAVA_AST = KeyFactory.bool("showClavaAst");
    public static final DataKey<Boolean> SHOW_CODE = KeyFactory.bool("showCode");
    public static final DataKey<Boolean> USE_CUSTOM_RESOURCES = KeyFactory.bool("useCustomResources");
    // BEGIN DATAKEY

    App parse(List<File> sources, List<String> compilerOptions);

}
