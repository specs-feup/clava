/**
 * Copyright 2016 SPeCS.
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

package pt.up.fe.specs.clang.ast;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.treenode.utils.JsonWriter;

public class JsonUtils {

    private static final FunctionClassMap<ClangNode, String> CLANG_JSON_TRANSLATORS;

    static {
        CLANG_JSON_TRANSLATORS = new FunctionClassMap<>();

        JsonUtils.CLANG_JSON_TRANSLATORS.put(ClangNode.class, ClangNode::jsonContents);
    }

    private static final FunctionClassMap<ClavaNode, String> CLAVA_JSON_TRANSLATORS;

    static {
        CLAVA_JSON_TRANSLATORS = new FunctionClassMap<>();

        JsonUtils.CLAVA_JSON_TRANSLATORS.put(ClavaNode.class, ClavaNode::toJsonContents);
    }

    public static String toJson(ClangNode node) {
        return new JsonWriter<>(JsonUtils.CLANG_JSON_TRANSLATORS).toJson(node);
    }

    public static String toJson(ClavaNode node) {
        return new JsonWriter<>(JsonUtils.CLAVA_JSON_TRANSLATORS).toJson(node);
    }
}
