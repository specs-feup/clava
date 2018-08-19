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

import java.util.Collection;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;

public class OmpLiteralPragma extends OmpPragma {

    /// DATAKEYS BEGIN

    public final static DataKey<String> CUSTOM_CONTENT = KeyFactory.string("customContent");

    /// DATAKEYS END

    public OmpLiteralPragma(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    // private String customContent;

    // public OmpLiteralPragma(OmpDirectiveKind directiveKind, String content, ClavaNodeInfo info) {
    // super(directiveKind, info);
    //
    // this.customContent = content;
    // }

    @Override
    public String getFullContent() {
        // return customContent;
        return get(CUSTOM_CONTENT);
    }

    // @Override
    // protected ClavaNode copyPrivate() {
    // return new OmpLiteralPragma(getDirectiveKind(), customContent, getInfo());
    // }

    @Override
    public void setFullContent(String fullContent) {
        set(CUSTOM_CONTENT, fullContent);
        // this.customContent = fullContent;
    }

}
