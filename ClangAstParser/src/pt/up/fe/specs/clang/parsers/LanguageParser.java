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

package pt.up.fe.specs.clang.parsers;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.suikasoft.jOptions.streamparser.LineStreamParsers;
import org.suikasoft.jOptions.streamparser.LineStreamWorker;

import pt.up.fe.specs.clang.dumper.ClangAstData;
import pt.up.fe.specs.clava.ast.extra.data.Language;
import pt.up.fe.specs.clava.ast.extra.data.OpenCLVersion;
import pt.up.fe.specs.util.utilities.LineStream;

public class LanguageParser implements LineStreamWorker<ClangAstData> {

    private static final String PARSER_ID = "<Compiler Instance Data>";

    @Override
    public String getId() {
        return PARSER_ID;
    }

    @Override
    public void init(ClangAstData data) {
        data.set(ClangAstData.FILE_LANGUAGE_DATA, new HashMap<>());
    }

    @Override
    public void apply(LineStream lineStream, ClangAstData data) {
        Map<File, Language> map = data.get(ClangAstData.FILE_LANGUAGE_DATA);

        File file = new File(lineStream.nextLine());
        Language language = new Language()
                .set(Language.LINE_COMMENT, LineStreamParsers.oneOrZero(lineStream))
                .set(Language.GNU_INLINE, LineStreamParsers.oneOrZero(lineStream))
                .set(Language.C99, LineStreamParsers.oneOrZero(lineStream))
                .set(Language.C11, LineStreamParsers.oneOrZero(lineStream))
                .set(Language.C_PLUS_PLUS, LineStreamParsers.oneOrZero(lineStream))
                .set(Language.C_PLUS_PLUS_11, LineStreamParsers.oneOrZero(lineStream))
                .set(Language.C_PLUS_PLUS_14, LineStreamParsers.oneOrZero(lineStream))
                .set(Language.C_PLUS_PLUS_17, LineStreamParsers.oneOrZero(lineStream))
                .set(Language.C_PLUS_PLUS_20, LineStreamParsers.oneOrZero(lineStream))
                .set(Language.C_PLUS_PLUS_23, LineStreamParsers.oneOrZero(lineStream))
                .set(Language.HAS_DIGRAPHS, LineStreamParsers.oneOrZero(lineStream))
                .set(Language.IS_GNU, LineStreamParsers.oneOrZero(lineStream))
                .set(Language.HEX_FLOATS, LineStreamParsers.oneOrZero(lineStream))
                .set(Language.OPEN_CL, LineStreamParsers.oneOrZero(lineStream))
                .set(Language.OPEN_CL_VERSION, fromClangNumber(LineStreamParsers.integer(lineStream)))
                .set(Language.NATIVE_HALF_TYPE, LineStreamParsers.oneOrZero(lineStream))
                .set(Language.CUDA, LineStreamParsers.oneOrZero(lineStream))
                .set(Language.HAS_BOOL, LineStreamParsers.oneOrZero(lineStream))
                .set(Language.HAS_HALF, LineStreamParsers.oneOrZero(lineStream))
                .set(Language.HAS_WCHAR, LineStreamParsers.oneOrZero(lineStream))
                .set(Language.CHAR_WIDTH, LineStreamParsers.integer(lineStream))
                .set(Language.FLOAT_WIDTH, LineStreamParsers.integer(lineStream))
                .set(Language.DOUBLE_WIDTH, LineStreamParsers.integer(lineStream))
                .set(Language.LONG_DOUBLE_WIDTH, LineStreamParsers.integer(lineStream))
                .set(Language.BOOL_WIDTH, LineStreamParsers.integer(lineStream))
                .set(Language.SHORT_WIDTH, LineStreamParsers.integer(lineStream))
                .set(Language.INT_WIDTH, LineStreamParsers.integer(lineStream))
                .set(Language.LONG_WIDTH, LineStreamParsers.integer(lineStream))
                .set(Language.LONG_LONG_WIDTH, LineStreamParsers.integer(lineStream));

        map.put(file, language);
    }

    private static OpenCLVersion fromClangNumber(int openclNumber) {
        if (openclNumber == 0) {
            return OpenCLVersion.NONE;
        }

        if (openclNumber == 100) {
            return OpenCLVersion.v1_0;
        }

        if (openclNumber == 110) {
            return OpenCLVersion.v1_1;
        }

        if (openclNumber == 120) {
            return OpenCLVersion.v1_2;
        }

        if (openclNumber == 200) {
            return OpenCLVersion.v2_0;
        }

        throw new RuntimeException("OpenCL version number not supported:" + openclNumber);
    }

}
