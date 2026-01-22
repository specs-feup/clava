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
 * specific language governing permissions and limitations under the License.
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

    public static ACxxWeaverJoinPoint findJp(CxxWeaver weaver, String filepath, String astId) {
        // Get AST at the top of the stack
        App topAst = weaver.getApp();

        return topAst.find(filepath, astId)
                .map(node -> CxxJoinpoints.create(node, weaver))
                .orElse(null);
    }

    public static void writeCode(CxxWeaver weaver, File outputFolder) {
        weaver.writeCode(outputFolder);
    }

    public static List<AInclude> getAvailableUserIncludes(CxxWeaver weaver) {
        return weaver.getAvailableIncludes().stream()
                .map(weaver.getFactory()::includeDecl)
                .map(includeDecl -> CxxJoinpoints.create(includeDecl, weaver, AInclude.class))
                .collect(Collectors.toList());
    }

    public static Set<String> getIncludeFolders(CxxWeaver weaver) {
        return weaver.getIncludeFolders();
    }

}
