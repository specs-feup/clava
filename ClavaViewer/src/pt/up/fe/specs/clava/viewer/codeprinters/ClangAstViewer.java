/**
 * Copyright 2015 SPeCS.
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

package pt.up.fe.specs.clava.viewer.codeprinters;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clang.ClangAstParser;
import pt.up.fe.specs.clang.ast.genericnode.ClangRootNode;
import pt.up.fe.specs.clava.viewer.ClavaViewerKeys;
import pt.up.fe.specs.clava.viewer.CodeViewer;
import pt.up.fe.specs.clava.viewer.ContentType;
import pt.up.fe.specs.util.SpecsIo;

public class ClangAstViewer implements CodeViewer {

    @Override
    public String getCode(String code, DataStore data) {

        boolean isCpp = data.get(ClavaViewerKeys.IS_CPP);
        String extension = isCpp ? "cpp" : "c";
        // Create file from given code
        File cppFile = new File("clava_printer_test." + extension);

        SpecsIo.write(cppFile, code);

        List<String> options = new ArrayList<>();
        if (isCpp) {
            options.add("-std=c++11");
        }

        ClangRootNode ast = new ClangAstParser().parse(Arrays.asList(cppFile.getAbsolutePath()), options);

        SpecsIo.delete(cppFile);

        return ast.toString();
    }

    @Override
    public String getContentType() {
        return ContentType.TEXT_CPP.getName();
    }

}
