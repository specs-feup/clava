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
import java.util.List;

import org.suikasoft.jOptions.streamparser.LineStreamWorker;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.astlineparser.AstParser;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.utilities.LineStream;

public class SystemHeadersClangNodes implements LineStreamWorker<ClangParserData> {

    private static final String PARSER_ID = "<System Header Node Dump Begin>";
    private static final String END_ID = "<System Header Node Dump End>";

    @Override
    public String getId() {
        return PARSER_ID;
    }

    @Override
    public void init(ClangParserData data) {
        data.set(ClangParserData.SYSTEM_HEADERS_CLANG_NODES, new HashMap<>());
    }

    @Override
    public void apply(LineStream lineStream, ClangParserData data) {
        // First line is id
        String nodeId = lineStream.nextLine();

        // Extract id suffix
        String idSuffix = nodeId.substring(nodeId.lastIndexOf('_') + 1, nodeId.length());

        // Read lines until END_ID appears
        StringBuilder builder = new StringBuilder();
        String nextLine;
        while (!(nextLine = lineStream.nextLine()).equals(END_ID)) {
            builder.append(nextLine).append("\n");
        }

        // Parse Clang output
        List<ClangNode> clangDump = new AstParser(null, idSuffix).parse(SpecsIo.toInputStream(builder.toString()));

        // Make sure it has one node
        Preconditions.checkArgument(clangDump.size() == 1, "Expected a single node, has " + clangDump.size());

        ClangNode clangNode = clangDump.get(0);
        clangNode.getInfo().setId(nodeId);

        data.get(ClangParserData.SYSTEM_HEADERS_CLANG_NODES).put(nodeId, clangNode);

    }

}
