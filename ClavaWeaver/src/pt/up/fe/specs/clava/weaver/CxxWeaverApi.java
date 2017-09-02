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

package pt.up.fe.specs.clava.weaver;

import java.io.File;

import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.weaver.abstracts.ACxxWeaverJoinPoint;

public class CxxWeaverApi {

    public static ACxxWeaverJoinPoint findJp(String filepath, String astId) {
        // Get AST at the top of the stack
        App topAst = CxxWeaver.getCxxWeaver().getApp();

        File originalFilepath = new File(filepath);
        TranslationUnit tu = topAst.getTranslationUnits().stream()
                .filter(node -> node.getFile().equals(originalFilepath))
                .findFirst()
                .orElse(null);

        if (tu == null) {
            return null;
        }

        return tu.getDescendantsAndSelfStream()
                // Filter nodes that do not have an id equal to the given id
                .filter(node -> node.getExtendedId().map(astId::equals).orElse(false))
                .findFirst()
                .map(node -> CxxJoinpoints.create(node, null))
                .orElse(null);
    }

    public static void writeCode(File outputFolder) {
        CxxWeaver.getCxxWeaver().writeCode(outputFolder);
    }

}
