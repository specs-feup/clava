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

package pt.up.fe.specs.clang.parsers;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.suikasoft.jOptions.streamparser.LineStreamParserV2;
import org.suikasoft.jOptions.streamparser.LineStreamWorker;

import pt.up.fe.specs.clang.version.Clang_3_8;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.context.ClavaContext;

public class ClangStreamParserV2 {

    private static final Map<String, LineStreamWorker> WORKERS;
    static {
        WORKERS = new HashMap<>();
        addWorker(() -> new ClavaNodeParser(Clang_3_8.getClassesService()));
        addWorker(VisitedChildrenParser::new);
        addWorker(IdToFilenameParser::new);
        addWorker(IncludesParser::new);
        TopLevelNodesParser.getWorkers().forEach(ClangStreamParserV2::addWorker);
        NodeDataParser.getWorkers().forEach(ClangStreamParserV2::addWorker);
    }

    private static void addWorker(Supplier<LineStreamWorker> workerSupplier) {
        addWorker(workerSupplier.get());
    }

    private static void addWorker(LineStreamWorker worker) {
        // Add worker
        WORKERS.put(worker.getId(), worker);
    }

    // public static LineStreamParserV2 newInstance(List<String> arguments) {
    public static LineStreamParserV2 newInstance(ClavaContext context) {
        LineStreamParserV2 streamParser = LineStreamParserV2.newInstance(WORKERS);

        // Create ClavaContext
        // streamParser.getData().add(ClavaNode.CONTEXT, new ClavaContext(arguments));
        streamParser.getData().add(ClavaNode.CONTEXT, context);

        return streamParser;
        // return LineStreamParserV2.newInstance(WORKERS);
        // LineStreamParserV2 lineStreamParser = LineStreamParserV2.newInstance(WORKERS);
        //
        // // Initialize some keys
        // DataStore data = lineStreamParser.getData();
        // data.add(ClangParserKeys.CLAVA_NODES, new HashMap<>());
        // data.add(ClangParserKeys.CLAVA_DATA, new HashMap<>());
        // data.add(ClangParserKeys.VISITED_CHILDREN, new HashMap<>());
        //
        // return lineStreamParser;
    }
}
