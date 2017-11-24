/**
 * Copyright 2017 SPeCS.
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

package pt.up.fe.specs.clava;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import pt.up.fe.specs.clava.ast.type.data.Qualifier;
import pt.up.fe.specs.util.SpecsIo;

public class ClavaCode {

    public static String getQualifiersCode(List<Qualifier> qualifiers) {
        return qualifiers.stream().map(Qualifier::getCode)
                .collect(Collectors.joining(" "));
    }

    public static String getRelativePath(File baseFile, File baseInputFolder) {
        String relativePath = SpecsIo.getRelativePath(baseFile, baseInputFolder);
    
        // Avoid writing outside of the destination folder, if relative path has '../', remove them
        while (relativePath.startsWith("../")) {
            relativePath = relativePath.substring("../".length());
        }
    
        return relativePath;
    }
}
