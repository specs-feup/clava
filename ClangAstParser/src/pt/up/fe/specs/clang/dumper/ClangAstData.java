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

package pt.up.fe.specs.clang.dumper;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.suikasoft.jOptions.DataStore.ADataClass;
import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clang.parsers.ClavaNodes;
import pt.up.fe.specs.clang.parsers.VisitingChildrenCheck;
import pt.up.fe.specs.clang.parsers.util.PragmasLocations;
import pt.up.fe.specs.clava.Include;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.ast.extra.data.Language;
import pt.up.fe.specs.clava.context.ClavaContext;
import pt.up.fe.specs.clava.context.ClavaFactory;

/**
 * Aggregates the data output by the ClangAstDumper binary.
 * 
 * @author JBispo
 *
 */
public class ClangAstData extends ADataClass<ClangAstData> {

    /// DATAKEYS BEGIN

    /**
     * Global object with information about the program.
     * 
     */
    public final static DataKey<ClavaContext> CONTEXT = KeyFactory.object("context", ClavaContext.class);

    /**
     * Currently parsed ClavaNode instances.
     */
    public final static DataKey<ClavaNodes> CLAVA_NODES = KeyFactory.object("clang_parser_stream_clava_nodes",
            ClavaNodes.class);

    public final static DataKey<Map<String, DataStore>> NODE_DATA = KeyFactory.generic("clang_parser_stream_node_data",
            new HashMap<String, DataStore>());

    public final static DataKey<Map<String, List<String>>> VISITED_CHILDREN = KeyFactory.generic(
            "clang_parser_stream_visited_children",
            new HashMap<>());

    public final static DataKey<Map<String, String>> ID_TO_FILENAME_MAP = KeyFactory.generic(
            "clang_parser_stream_id_to_filename_map",
            new HashMap<String, String>());

    public final static DataKey<List<Include>> INCLUDES = KeyFactory.generic("clang_parser_stream_includes",
            new ArrayList<>());

    public final static DataKey<Set<String>> TOP_LEVEL_DECL_IDS = KeyFactory.generic(
            "clang_parser_stream_top_level_decl_ids",
            new HashSet<>());

    public final static DataKey<Set<String>> TOP_LEVEL_TYPE_IDS = KeyFactory.generic(
            "clang_parser_stream_top_level_type_ids",
            new HashSet<>());

    public final static DataKey<Set<String>> TOP_LEVEL_ATTR_IDS = KeyFactory.generic(
            "clang_parser_stream_top_level_attr_ids",
            new HashSet<>());

    public final static DataKey<Map<File, Language>> FILE_LANGUAGE_DATA = KeyFactory
            .generic("clang_parser_stream_file_language_data", () -> new HashMap<>());

    // public final static DataKey<Map<String, ClangNode>> SYSTEM_HEADERS_CLANG_NODES = KeyFactory
    // .generic("clang_parser_stream_system_headers_clang_nodes",
    // (Map<String, ClangNode>) new HashMap<String, ClangNode>());

    public final static DataKey<String> LINES_NOT_PARSED = KeyFactory.string("clang_dumper_parser_warnings");

    public final static DataKey<Set<String>> NODES_CURRENTLY_BEING_PARSED = KeyFactory
            .generic("nodesCurrentlyBeingParsed", (Set<String>) new HashSet<String>());

    public final static DataKey<List<String>> CURRENT_NODE_VISIT_CHAIN = KeyFactory
            .generic("currentNodeVisitChain", (List<String>) new ArrayList<String>());

    public final static DataKey<VisitingChildrenCheck> VISITING_CHILDREN = KeyFactory
            .object("visitingChildren", VisitingChildrenCheck.class);

    public final static DataKey<Map<String, String>> SKIPPED_NODES_MAP = KeyFactory.generic(
            "skippedNodesMap",
            new HashMap<>());

    public final static DataKey<PragmasLocations> PRAGMAS_LOCATIONS = KeyFactory.object("pragmasLocations",
            PragmasLocations.class);

    /**
     * Parsed translation unit.
     */
    public final static DataKey<TranslationUnit> TRANSLATION_UNIT = KeyFactory
            .object("translationUnit", TranslationUnit.class);

    /**
     * Enables debug prints.
     */
    public final static DataKey<Boolean> DEBUG = KeyFactory.bool("clang_parser_stream_debug");

    /**
     * True, if there were errors during parsing of the source code.
     */
    public final static DataKey<Boolean> HAS_ERRORS = KeyFactory.bool("hasErrors");

    /**
     * The errors output
     */
    // public final static DataKey<String> ERROR_OUTPUT = KeyFactory.string("errorOutput");

    /// DATAKEYS END

    // public ClangParserData() {
    // set(NODES_CURRENTLY_BEING_PARSED, new HashSet<>());
    // }

    /**
     * Helper method.
     * 
     * @return
     */
    public ClavaNodes getClavaNodes() {
        return get(CLAVA_NODES);
    }

    public ClavaFactory getFactory() {
        return get(CONTEXT).get(ClavaContext.FACTORY);
    }
}
