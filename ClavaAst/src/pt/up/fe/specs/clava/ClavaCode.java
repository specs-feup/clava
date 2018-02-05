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

    public static String getQualifiersCode(List<Qualifier> qualifiers, boolean isCxx) {
        return qualifiers.stream().map(qualifier -> qualifier.getCode(isCxx))
                .collect(Collectors.joining(" "));
    }

    /**
     * 
     * @param sourcePath
     * @param baseInputFolder
     * @deprecated use TranslationUnit.getRelativeFilepath and .getRelativeFolderpath
     * @return
     */
    @Deprecated
    public static String getRelativePath(File sourcePath, File baseInputFolder) {
        // If base file does not exist yet, just return it
        if (!sourcePath.exists()) {
            return sourcePath.getPath();
        }

        // No base input folder specified, just return source path
        if (baseInputFolder == null) {
            return sourcePath.getPath();
        }

        String relativePath = SpecsIo.getRelativePath(sourcePath, baseInputFolder);

        // Avoid writing outside of the destination folder, if relative path has '../', remove them
        while (relativePath.startsWith("../")) {
            relativePath = relativePath.substring("../".length());
        }

        return relativePath;
    }
}
