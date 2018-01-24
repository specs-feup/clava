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

package pt.up.fe.specs.clava.ast.decl;

import java.util.Collections;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.Include;
import pt.up.fe.specs.clava.ast.decl.data.DeclData;

public class IncludeDecl extends Decl {

    private final Include include;

    public IncludeDecl(String include, boolean isAngled) {
        this(new Include(include, isAngled), ClavaNodeInfo.undefinedInfo());
    }

    public IncludeDecl(Include include, ClavaNodeInfo info) {
        super(DeclData.empty(), info, Collections.emptyList());

        this.include = include;
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new IncludeDecl(include, getInfo());
    }

    @Override
    public String getCode() {
        StringBuilder builder = new StringBuilder();

        builder.append("#include ");
        builder.append(getFormattedInclude());

        return builder.toString();
    }

    public String getFormattedInclude() {
        if (include.isAngled()) {
            return "<" + include.getInclude() + ">";
        }
        return "\"" + include.getInclude() + "\"";
    }

    /**
     * TODO: change to getName()
     * 
     * @return
     */
    public Include getInclude() {
        return include;
    }

    @Override
    public String toContentString() {
        return super.toContentString() + getFormattedInclude() + " ";
    }

}
