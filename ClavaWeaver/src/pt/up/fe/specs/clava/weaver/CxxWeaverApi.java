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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.weaver.abstracts.ACxxWeaverJoinPoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AInclude;

public class CxxWeaverApi {

    /*
    public static Optional<ClavaNode> findJp(ClavaNode node) {
        // Get translation unit
        TranslationUnit tu = node.getAncestorTry(TranslationUnit.class).orElse(null);
        if (tu == null) {
            return Optional.empty();
        }
    
        String id = node.getExtendedId().orElse(null);
        if (id == null) {
            return Optional.empty();
        }
    
        ACxxWeaverJoinPoint jp = findJp(tu.getFile().getPath(), id);
        if (jp == null) {
            return Optional.empty();
        }
    
        return Optional.of(jp.getNode());
    }
    */

    /*
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
    */
    public static ACxxWeaverJoinPoint findJp(String filepath, String astId) {
        // Get AST at the top of the stack
        App topAst = CxxWeaver.getCxxWeaver().getApp();

        return topAst.find(filepath, astId)
                .map(node -> CxxJoinpoints.create(node, null))
                .orElse(null);
    }

    public static void writeCode(File outputFolder) {
        CxxWeaver.getCxxWeaver().writeCode(outputFolder);
    }

    public static List<AInclude> getAvailableUserIncludes() {
        return CxxWeaver.getCxxWeaver().getAvailableIncludes().stream()
                .map(CxxWeaver.getFactory()::includeDecl)
                .map(includeDecl -> (AInclude) CxxJoinpoints.create(includeDecl, null))
                .collect(Collectors.toList());
    }

    public static Set<String> getIncludeFolders() {
        return CxxWeaver.getCxxWeaver().getIncludeFolders();
    }

}
