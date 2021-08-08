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

package pt.up.fe.specs.clang.parsers.data;

import java.util.ArrayList;
import java.util.List;

import org.suikasoft.jOptions.Interfaces.DataStore;
import org.suikasoft.jOptions.streamparser.LineStreamParsers;

import pt.up.fe.specs.clang.codeparser.ClangParserData;
import pt.up.fe.specs.clang.parsers.NodeDataParser;
import pt.up.fe.specs.clava.ast.stmt.AsmStmt;
import pt.up.fe.specs.clava.ast.stmt.AttributedStmt;
import pt.up.fe.specs.clava.ast.stmt.GCCAsmStmt;
import pt.up.fe.specs.clava.ast.stmt.GotoStmt;
import pt.up.fe.specs.clava.ast.stmt.LabelStmt;
import pt.up.fe.specs.clava.ast.stmt.MSAsmStmt;
import pt.up.fe.specs.clava.ast.stmt.data.AsmInput;
import pt.up.fe.specs.clava.ast.stmt.data.AsmOutput;
import pt.up.fe.specs.util.utilities.LineStream;

/**
 * ClavaData parsers for Stmt nodes.
 * 
 * 
 * @author JoaoBispo
 *
 */
public class StmtDataParser {

    public static DataStore parseStmtData(LineStream lines, ClangParserData dataStore) {
        return NodeDataParser.parseNodeData(lines, dataStore);
    }

    public static DataStore parseLabelStmtData(LineStream lines, ClangParserData dataStore) {
        DataStore data = parseStmtData(lines, dataStore);

        data.add(LabelStmt.LABEL, lines.nextLine());

        return data;
    }

    public static DataStore parseGotoStmtData(LineStream lines, ClangParserData dataStore) {
        DataStore data = parseStmtData(lines, dataStore);

        dataStore.getClavaNodes().queueSetNode(data, GotoStmt.LABEL, lines.nextLine());

        return data;
    }

    public static DataStore parseAttributedStmtData(LineStream lines, ClangParserData dataStore) {
        DataStore data = parseStmtData(lines, dataStore);

        dataStore.getClavaNodes().queueSetNodeList(data, AttributedStmt.STMT_ATTRIBUTES,
                LineStreamParsers.stringList(lines));

        return data;
    }

    public static DataStore parseAsmStmtData(LineStream lines, ClangParserData dataStore) {
        DataStore data = parseStmtData(lines, dataStore);

        data.add(AsmStmt.IS_SIMPLE, LineStreamParsers.oneOrZero(lines));
        data.add(AsmStmt.IS_VOLATILE, LineStreamParsers.oneOrZero(lines));

        int numClobers = LineStreamParsers.integer(lines);
        List<String> clobbers = new ArrayList<>();
        for (int i = 0; i < numClobers; i++) {
            clobbers.add(lines.nextLine());
        }
        data.add(AsmStmt.CLOBBERS, clobbers);

        int numOutputs = LineStreamParsers.integer(lines);
        List<AsmOutput> outputs = new ArrayList<>();
        for (int i = 0; i < numOutputs; i++) {
            var output = new AsmOutput();
            outputs.add(output);

            dataStore.getClavaNodes().queueSetNode(output, AsmOutput.EXPR, lines.nextLine());
            output.set(AsmOutput.CONSTRAINT, lines.nextLine());
            output.set(AsmOutput.IS_PLUS_CONSTRAINT, LineStreamParsers.oneOrZero(lines.nextLine()));
        }
        data.add(AsmStmt.OUTPUTS, outputs);

        int numInputs = LineStreamParsers.integer(lines);
        List<AsmInput> inputs = new ArrayList<>();
        for (int i = 0; i < numInputs; i++) {
            var input = new AsmInput();
            inputs.add(input);

            dataStore.getClavaNodes().queueSetNode(input, AsmInput.EXPR, lines.nextLine());
            input.set(AsmInput.CONSTRAINT, lines.nextLine());
        }
        data.add(AsmStmt.INPUTS, inputs);

        return data;
    }

    public static DataStore parseGCCAsmStmtData(LineStream lines, ClangParserData dataStore) {
        DataStore data = parseAsmStmtData(lines, dataStore);

        data.add(GCCAsmStmt.ASM_STRING, lines.nextLine());

        return data;
    }

    public static DataStore parseMSAsmStmtData(LineStream lines, ClangParserData dataStore) {
        DataStore data = parseAsmStmtData(lines, dataStore);

        data.add(MSAsmStmt.ASM_STRING, lines.nextLine());

        return data;
    }
}
