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

import java.util.Collection;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.Include;

/**
 * Represents an include directive (e.g., #include <stdio.h>)
 * 
 * @author jbispo
 *
 */
public class IncludeDecl extends Decl {

    /// DATAKEYS BEGIN

    public final static DataKey<Include> INCLUDE = KeyFactory.object("include", Include.class);

    /// DATAKEYS END

    public IncludeDecl(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    // private final Include include;
    //
    // public IncludeDecl(String include, boolean isAngled) {
    // this(new Include(include, isAngled), ClavaNodeInfo.undefinedInfo());
    // }
    //
    // public IncludeDecl(Include include, ClavaNodeInfo info) {
    // super(DeclData.empty(), info, Collections.emptyList());
    //
    // this.include = include;
    // }

    // @Override
    // protected ClavaNode copyPrivate() {
    // return new IncludeDecl(include, getInfo());
    // }

    @Override
    public String getCode() {
        StringBuilder builder = new StringBuilder();

        builder.append("#include ");
        builder.append(getFormattedInclude());

        return builder.toString();
    }

    public String getFormattedInclude() {
        Include include = get(INCLUDE);
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
        return get(INCLUDE);
    }

    // @Override
    // public String toContentString() {
    // return super.toContentString() + getFormattedInclude() + " ";
    // }

}
