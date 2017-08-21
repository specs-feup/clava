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

package pt.up.fe.specs.clava.ast.omp;

import java.util.Arrays;
import java.util.Collections;

import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.clava.omp.OMPDirective;

/**
 * Base class for representing an OpenMP directive.
 *
 * @author João Bispo
 *
 * @deprecated
 */
@Deprecated
abstract class OMPExecutableDirective extends Stmt {

    private final OMPDirective directive;

    protected OMPExecutableDirective(OMPDirective directive, ClavaNodeInfo info, Stmt associatedStmt) {
        super(info, Arrays.asList(associatedStmt));

        this.directive = directive;
    }

    /**
     * Constructor for copy() method.
     *
     * @param directive
     * @param info
     */
    protected OMPExecutableDirective(OMPDirective directive, ClavaNodeInfo info) {
        super(info, Collections.emptyList());

        this.directive = directive;
    }

    public OMPDirective getDirective() {
        return directive;
    }

    public Stmt getAssociatedStmt() {
        return getChild(Stmt.class, 0);
    }

    protected abstract String getDirectiveCode();

    @Override
    public String getCode() {
        StringBuilder code = new StringBuilder();

        // Add pragma
        code.append(getDirectiveCode()).append(ln());
        // Add associated statement
        code.append(getAssociatedStmt().getCode());

        return code.toString();
    }
}
