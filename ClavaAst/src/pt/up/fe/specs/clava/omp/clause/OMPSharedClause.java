/**
 * Copyright 2016 SPeCS.
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

package pt.up.fe.specs.clava.omp.clause;

import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author joao bispo
 * @deprecated
 */
@Deprecated
public class OMPSharedClause implements OMPClause {

    private final List<String> variables;

    public OMPSharedClause(List<String> variables) {
        this.variables = variables;
    }

    public List<String> getVariables() {
        return variables;
    }

    /**
     * Generates code as in OpenMP 4.5:<br>
     * shared(list)
     */
    @Override
    public String getCode() {
        return "shared(" +
                variables.stream().collect(Collectors.joining(", "))
                + ")";
    }

}
