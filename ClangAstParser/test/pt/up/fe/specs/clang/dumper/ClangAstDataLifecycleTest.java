/**
 * Copyright 2026 SPeCS.
 *
 * Licensed under the Apache License, Version 2.0.
 */

package pt.up.fe.specs.clang.dumper;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clang.parsers.ClavaNodes;
import pt.up.fe.specs.clang.parsers.util.PragmasLocations;
import pt.up.fe.specs.clava.Include;
import pt.up.fe.specs.clava.ast.extra.data.Language;
import pt.up.fe.specs.clava.context.ClavaContext;

class ClangAstDataLifecycleTest {

    @Test
    void releaseParserStateClearsAllLookupCollectionsAndCanBeRepeated() {
        ClangAstData data = new ClangAstData();
        ClavaContext context = new ClavaContext();
        ClavaNodes nodes = new ClavaNodes(context.getFactory());
        nodes.getNodes().put("node", nodes.nullNode("nullptr_expr"));
        nodes.queueAction(() -> { });
        data.set(ClangAstData.CLAVA_NODES, nodes);

        Map<String, DataStore> nodeData = new HashMap<>();
        nodeData.put("node", DataStore.newInstance("node"));
        data.set(ClangAstData.NODE_DATA, nodeData);

        Map<String, List<String>> children = new HashMap<>();
        children.put("node", List.of("child"));
        data.set(ClangAstData.VISITED_CHILDREN, children);

        Map<String, String> filenames = new LinkedHashMap<>();
        filenames.put("node", "source.cpp");
        data.set(ClangAstData.ID_TO_FILENAME_MAP, filenames);
        data.set(ClangAstData.INCLUDES, new ArrayList<>(List.of(new Include("header.h", true))));
        data.set(ClangAstData.TOP_LEVEL_DECL_IDS, new HashSet<>(Set.of("node")));
        data.set(ClangAstData.TOP_LEVEL_TYPE_IDS, new HashSet<>(Set.of("node")));
        data.set(ClangAstData.TOP_LEVEL_ATTR_IDS, new HashSet<>(Set.of("node")));

        Map<File, Language> languages = new HashMap<>();
        languages.put(new File("source.cpp"), null);
        data.set(ClangAstData.FILE_LANGUAGE_DATA, languages);
        PragmasLocations pragmas = new PragmasLocations();
        pragmas.addPragmaLocation(new File("source.cpp"), 1, 1);
        data.set(ClangAstData.PRAGMAS_LOCATIONS, pragmas);

        data.releaseParserState();
        data.releaseParserState();

        assertTrue(nodes.getNodes().isEmpty());
        assertTrue(nodes.getQueuedActions().isEmpty());
        assertTrue(nodeData.isEmpty());
        assertTrue(children.isEmpty());
        assertTrue(filenames.isEmpty());
        assertTrue(data.get(ClangAstData.INCLUDES).isEmpty());
        assertTrue(data.get(ClangAstData.TOP_LEVEL_DECL_IDS).isEmpty());
        assertTrue(data.get(ClangAstData.TOP_LEVEL_TYPE_IDS).isEmpty());
        assertTrue(data.get(ClangAstData.TOP_LEVEL_ATTR_IDS).isEmpty());
        assertTrue(languages.isEmpty());
        assertTrue(pragmas.toString().equals("{}"));
    }
}
