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

package pt.up.fe.specs.clang.parsersv2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.Include;
import pt.up.fe.specs.clava.ast.ClavaData;

public interface ClangParserKeys {

    /**
     * Currently parsed ClavaNode instances.
     */
    DataKey<Map<String, ClavaNode>> CLAVA_NODES = KeyFactory.generic("clang_parser_stream_clava_nodes",
            new HashMap<>());

    /**
     * Currently parsed ClavaData instances.
     */
    DataKey<Map<String, ClavaData>> CLAVA_DATA = KeyFactory.generic("clang_parser_stream_clava_data",
            new HashMap<String, ClavaData>());

    DataKey<Map<String, List<String>>> VISITED_CHILDREN = KeyFactory.generic("clang_parser_stream_visited_children",
            new HashMap<>());

    DataKey<Map<String, String>> ID_TO_FILENAME_MAP = KeyFactory.generic("clang_parser_stream_id_to_filename_map",
            new HashMap<>());

    DataKey<List<Include>> INCLUDES = KeyFactory.generic("clang_parser_stream_includes", new ArrayList<>());

    DataKey<Set<String>> TOP_LEVEL_DECL_IDS = KeyFactory.generic("clang_parser_stream_top_level_decl_ids",
            new HashSet<>());

    DataKey<Set<String>> TOP_LEVEL_TYPE_IDS = KeyFactory.generic("clang_parser_stream_top_level_type_ids",
            new HashSet<>());

    DataKey<Set<String>> TOP_LEVEL_ATTR_IDS = KeyFactory.generic("clang_parser_stream_top_level_attr_ids",
            new HashSet<>());

    /**
     * Enables debug prints.
     */
    DataKey<Boolean> DEBUG = KeyFactory.bool("clang_parser_stream_debug");

    static ClavaNode getNode(DataStore dataStore, String nodeId) {
        ClavaNode node = dataStore.get(CLAVA_NODES).get(nodeId);
        Preconditions.checkNotNull(node, "Could not find ClavaNode with id '" + nodeId + "'");
        return node;
    }

}
