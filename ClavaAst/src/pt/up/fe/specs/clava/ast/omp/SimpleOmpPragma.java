/**
 * Copyright 2017 SPeCS.
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

import pt.up.fe.specs.clava.ClavaNodeInfo;

/**
 * E.g., #pragma omp declare target
 * 
 * @author pedro
 *
 */
public class SimpleOmpPragma extends OmpPragma {

    public SimpleOmpPragma(OmpDirectiveKind directiveKind, ClavaNodeInfo info) {
        super(directiveKind, info);
    }

    @Override
    public String getFullContent() {
        StringBuilder fullContent = new StringBuilder();

        fullContent.append("omp ");
        fullContent.append(getDirectiveKind().getString());

        return fullContent.toString();
    }

    @Override
    protected SimpleOmpPragma copyPrivate() {
        return new SimpleOmpPragma(getDirectiveKind(), getInfo());
    }

}
