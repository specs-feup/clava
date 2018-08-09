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

import java.util.List;
import java.util.Set;

import org.suikasoft.jOptions.streamparser.LineStreamWorker;

import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.util.utilities.LineStream;

/**
 * Checks when visits to the nodes start, for circular dependency detection.
 * 
 * @author JoaoBispo
 *
 */
public class VisitEndParser implements LineStreamWorker<ClangParserData> {

    private static final String PARSER_ID = "<Visit End>";

    @Override
    public String getId() {
        return PARSER_ID;
    }

    @Override
    public void init(ClangParserData data) {

    }

    @Override
    public void apply(LineStream lineStream, ClangParserData data) {
        Set<String> visitingNodes = data.get(ClangParserData.NODES_CURRENTLY_BEING_PARSED);
        List<String> visitChain = data.get(ClangParserData.CURRENT_NODE_VISIT_CHAIN);

        // Id of the node that ended a visit
        String id = lineStream.nextLine();

        if (!visitingNodes.contains(id)) {
            ClavaLog.debug("Expected visiting nodes to contain id '" + id + "': " + visitingNodes);
            // throw new RuntimeException("Expected visiting nodes to contain id '" + id + "': " + visitingNodes);
        }

        if (!visitChain.get(visitChain.size() - 1).equals(id)) {
            ClavaLog.debug("Expected last element of visit chain to be '" + id + "': " + visitChain);
            // throw new RuntimeException("Expected last element of visit chain to be '" + id + "': " + visitChain);
        }

        // Remove from visiting nodes set and chain
        visitingNodes.remove(id);
        visitChain.remove(visitChain.size() - 1);

    }
}
