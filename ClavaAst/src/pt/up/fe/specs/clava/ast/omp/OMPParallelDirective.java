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

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.clava.omp.OMPDirective;

/**
 * @deprecated
 * @author
 *
 */
@Deprecated
public class OMPParallelDirective extends OMPExecutableDirective {

    public OMPParallelDirective(OMPDirective directive, ClavaNodeInfo info, Stmt associatedStmt) {
        super(directive, info, associatedStmt);
    }

    private OMPParallelDirective(OMPDirective directive, ClavaNodeInfo info) {
        super(directive, info);
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new OMPParallelDirective(getDirective(), getInfo());
    }

    @Override
    protected String getDirectiveCode() {
        return getDirective().getCode("parallel");
    }

}
