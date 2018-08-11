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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.suikasoft.jOptions.streamparser.LineStreamWorker;

import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.util.SpecsCheck;
import pt.up.fe.specs.util.SpecsCollections;

public class VisitingChildrenCheck {

    private final List<Set<String>> nodesCurrentlyBeingParsed;
    private final List<List<String>> currentVisitChains;
    private final List<String> topIds;

    public VisitingChildrenCheck() {
        this.nodesCurrentlyBeingParsed = new ArrayList<>();
        this.currentVisitChains = new ArrayList<>();
        topIds = new ArrayList<>();
    }

    public Set<String> getCurrentlyParsedNodes() {
        return getCurrentlyParsedNodes(() -> "Expected at least one element");
    }

    public Set<String> getCurrentlyParsedNodes(Supplier<String> message) {
        SpecsCheck.checkArgument(!nodesCurrentlyBeingParsed.isEmpty(), message);

        return SpecsCollections.last(nodesCurrentlyBeingParsed);
    }

    public List<String> getCurrentVisitChain() {
        SpecsCheck.checkArgument(!currentVisitChains.isEmpty(), () -> "Expected at least one element");

        return SpecsCollections.last(currentVisitChains);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append("Nodes currently being parsed: ").append(nodesCurrentlyBeingParsed).append("\n");
        builder.append("Current visit chains: ").append(currentVisitChains).append("\n");
        builder.append("Top ids: " + topIds);

        return builder.toString();
    }

    // Top node visited, add to stack
    public void topNodeVisitStart(String id) {
        // Add new Set and List
        nodesCurrentlyBeingParsed.add(new HashSet<>());
        currentVisitChains.add(new ArrayList<>());
        topIds.add(id);
    }

    // Top node ends, update
    public void topNodeVisitEnd(String id) {
        // Check set/list are empty when removing

        Set<String> visitedNodes = SpecsCollections.removeLast(nodesCurrentlyBeingParsed);
        SpecsCheck.checkArgument(visitedNodes.isEmpty(), () -> "Expected list to be empty");

        List<String> visitChain = SpecsCollections.removeLast(currentVisitChains);
        SpecsCheck.checkArgument(visitChain.isEmpty(), () -> "Expected set to be empty");

        // Check top id is the same
        String topId = SpecsCollections.removeLast(topIds);
        SpecsCheck.checkArgument(topId.equals(id), () -> "Expected top id to be '" + id + "', but is '" + topId + "");
    }

    // Child visited, update
    public void childNodeVisitStart(String id) {

        Set<String> visitingNodes = getCurrentlyParsedNodes(
                () -> "Does not have a visiting nodes set, when starting child visit to id '" + id
                        + "'. Current status:\n" + toString());
        List<String> visitChain = getCurrentVisitChain();

        if (visitingNodes.contains(id)) {
            ClavaLog.debug("Found possible circular dependency, visiting node '" + id
                    + "' which is already being visited. Current node visit chain: "
                    + visitChain);
        }

        // Add visiting node to set and chain
        visitingNodes.add(id);
        visitChain.add(id);
    }

    // Child ends, update
    public void childNodeVisitEnd(String id) {
        Set<String> visitingNodes = getCurrentlyParsedNodes();
        List<String> visitChain = getCurrentVisitChain();

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

    private static void init(ClangParserData parserData) {
        if (parserData.hasValue(ClangParserData.VISITING_CHILDREN)) {
            return;
        }

        parserData.set(ClangParserData.VISITING_CHILDREN, new VisitingChildrenCheck());
    }

    public static Collection<LineStreamWorker<ClangParserData>> getWorkers() {
        List<LineStreamWorker<ClangParserData>> workers = new ArrayList<>();

        LineStreamWorker<ClangParserData> topNodeStart = LineStreamWorker.newInstance("<Top Visit Start>",
                VisitingChildrenCheck::init,
                (lines, data) -> data.get(ClangParserData.VISITING_CHILDREN).topNodeVisitStart(lines.nextLine()));

        LineStreamWorker<ClangParserData> topNodeEnd = LineStreamWorker.newInstance("<Top Visit End>",
                VisitingChildrenCheck::init,
                // parserData -> SpecsCheck.checkArgument(parserData.hasValue(ClangParserData.VISITING_CHILDREN),
                // () -> "Expected value to exist"),
                (lines, data) -> data.get(ClangParserData.VISITING_CHILDREN).topNodeVisitEnd(lines.nextLine()));

        LineStreamWorker<ClangParserData> visitNodeStart = LineStreamWorker.newInstance("<Visit Start>",
                VisitingChildrenCheck::init,
                (lines, data) -> data.get(ClangParserData.VISITING_CHILDREN).childNodeVisitStart(lines.nextLine()));

        LineStreamWorker<ClangParserData> visitNodeEnd = LineStreamWorker.newInstance("<Visit End>",
                VisitingChildrenCheck::init,
                (lines, data) -> data.get(ClangParserData.VISITING_CHILDREN).childNodeVisitEnd(lines.nextLine()));

        workers.add(topNodeStart);
        workers.add(topNodeEnd);
        workers.add(visitNodeStart);
        workers.add(visitNodeEnd);

        return workers;
    }

}
