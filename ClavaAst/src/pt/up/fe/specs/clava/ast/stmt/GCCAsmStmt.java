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

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;

/**
 * Represents a GCC inline-assembly statement extension.
 * 
 * @author JBispo
 *
 */
public class GCCAsmStmt extends GCCAsmStmtBase {

    public GCCAsmStmt(DataStore data, java.util.Collection<? extends ClavaNode> children, GCCAsmStmtBase.Values values) {
        this(data, children);
        setWireValues(values);
    }

    public GCCAsmStmt(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    @Override
    public String getCode() {
        var code = new StringBuilder();

        code.append("__asm__");

        if (getWireIsVolatile()) {
            code.append(" __volatile__");
        }

        if (Boolean.TRUE.equals(getWireIsInline())) {
            code.append(" __inline__");
        }

        if (Boolean.TRUE.equals(getWireIsGoto())) {
            code.append(" goto");
        }

        code.append("(\"");
        code.append(escapeStringLiteral(getWireAsmString()));
        code.append("\"");

        // Outputs
        var outputs = getWireOutputs().stream().map(output -> output.getCode())
                .collect(Collectors.joining(", "));

        var inputs = getWireInputs().stream().map(input -> input.getCode())
                .collect(Collectors.joining(", "));

        var clobbers = getWireClobbers().stream()
                .map(clobber -> "\"" + clobber + "\"")
                .collect(Collectors.joining(", "));

        var labels = String.join(", ", getWireLabels());
        var hasLabels = Boolean.TRUE.equals(getWireIsGoto()) || !labels.isEmpty();

        boolean hasExtendedSyntax = !Boolean.TRUE.equals(getWireIsSimple())
                || !outputs.isEmpty() || !inputs.isEmpty() || !clobbers.isEmpty() || hasLabels;

        if (hasExtendedSyntax) {
            code.append("\n   :").append(outputs);
        }

        if (!inputs.isEmpty() || !clobbers.isEmpty() || hasLabels) {
            code.append("\n   :").append(inputs);
        }

        if (!clobbers.isEmpty() || hasLabels) {
            code.append("\n   :").append(clobbers);
        }

        if (hasLabels) {
            code.append("\n   :").append(labels);
        }

        code.append(");");

        return code.toString();
    }

    private static String escapeStringLiteral(String value) {
        var escaped = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20 || character == 0x7f) {
                        // A fixed three-digit escape cannot absorb a following
                        // octal digit, unlike a variable-width hexadecimal escape.
                        escaped.append('\\')
                                .append((char) ('0' + ((character >>> 6) & 0x7)))
                                .append((char) ('0' + ((character >>> 3) & 0x7)))
                                .append((char) ('0' + (character & 0x7)));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.toString();
    }
}
