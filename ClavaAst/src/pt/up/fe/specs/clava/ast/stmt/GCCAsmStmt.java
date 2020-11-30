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
 * specific language governing permissions and limitations under the License. under the License.
 */

package pt.up.fe.specs.clava.ast.stmt;

import java.util.Collection;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;

/**
 * Represents a GCC inline-assembly statement extension.
 * 
 * @author JBispo
 *
 */
public class GCCAsmStmt extends AsmStmt {

    public static final DataKey<String> ASM_STRING = KeyFactory.string("asmString");

    public GCCAsmStmt(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    @Override
    public String getCode() {
        var code = new StringBuilder();

        code.append("__asm__");

        if (get(IS_VOLATILE)) {
            code.append(" __volatile__");
        }

        code.append("(\"");
        code.append(get(ASM_STRING));
        code.append("\"");

        // Outputs
        var outputs = get(OUTPUTS).stream().map(output -> output.getCode())
                .collect(Collectors.joining(", "));

        if (!outputs.isEmpty()) {
            code.append("\n   :").append(outputs);
        }

        // Inputs
        var inputs = get(INPUTS).stream().map(input -> input.getCode())
                .collect(Collectors.joining(", "));

        if (!inputs.isEmpty()) {
            code.append("\n   :").append(inputs);
        }

        // Cobblers
        var cobblers = get(CLOBBERS).stream().collect(Collectors.joining(", "));

        if (!cobblers.isEmpty()) {
            code.append("\n   :").append(cobblers);
        }

        code.append(");");

        return code.toString();
    }
}
