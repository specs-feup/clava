/**
 * Copyright 2020 SPeCS.
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

package pt.up.fe.specs.clava.ast.stmt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodes;

/** Represents a Microsoft-style inline assembly statement. */
public class MSAsmStmt extends AsmStmt {

    public static final DataKey<String> ASM_STRING = KeyFactory.string("asmString");

    public MSAsmStmt(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    @Override
    public String getCode() {
        var code = new StringBuilder();

        code.append("__asm");
        code.append("{");

        String asmBody = stripCommonIndent(get(ASM_STRING));
        if (!asmBody.isEmpty()) {
            String formattedBody = asmBody.lines()
                    .map(line -> getTab() + line)
                    .collect(Collectors.joining(ClavaNodes.ln()));
            code.append(ClavaNodes.ln()).append(formattedBody).append(ClavaNodes.ln());
        } else {
            code.append(ClavaNodes.ln());
        }

        code.append("}");
        return code.toString();
    }

    private static String stripCommonIndent(String source) {
        var lines = new ArrayList<>(source.lines().toList());
        while (!lines.isEmpty() && lines.get(0).isBlank()) {
            lines.remove(0);
        }
        while (!lines.isEmpty() && lines.get(lines.size() - 1).isBlank()) {
            lines.remove(lines.size() - 1);
        }
        if (lines.isEmpty()) {
            return "";
        }

        int commonIndent = lines.stream()
                .filter(line -> !line.isBlank())
                .mapToInt(MSAsmStmt::leadingWhitespace)
                .min()
                .orElse(0);

        return lines.stream()
                .map(line -> line.isBlank() ? "" : line.substring(Math.min(commonIndent, leadingWhitespace(line))))
                .collect(Collectors.joining(ClavaNodes.ln()));
    }

    private static int leadingWhitespace(String line) {
        int index = 0;
        while (index < line.length() && Character.isWhitespace(line.charAt(index))) {
            index++;
        }
        return index;
    }
}
