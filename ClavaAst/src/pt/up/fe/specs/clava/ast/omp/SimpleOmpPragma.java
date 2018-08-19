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

/**
 * E.g., #pragma omp declare target
 * 
 * @author pedro
 *
 */
public class SimpleOmpPragma extends OmpPragma {

    /// DATAKEYS BEGIN

    public final static DataKey<String> CUSTOM_CONTENT = KeyFactory.string("customContent");

    /// DATAKEYS END

    public SimpleOmpPragma(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    // private String customContent;

    // public SimpleOmpPragma(OmpDirectiveKind directiveKind, ClavaNodeInfo info) {
    // this(null, directiveKind, info);
    // }

    // private SimpleOmpPragma(String customContent, OmpDirectiveKind directiveKind, ClavaNodeInfo info) {
    // super(directiveKind, info);
    //
    // this.customContent = null;
    // }

    @Override
    public String getFullContent() {
        // Give priority to custom content
        if (hasValue(CUSTOM_CONTENT)) {
            return get(CUSTOM_CONTENT);
        }
        // if (customContent != null) {
        // return customContent;
        // }

        StringBuilder fullContent = new StringBuilder();

        fullContent.append("omp ");
        fullContent.append(getDirectiveKind().getString());

        return fullContent.toString();
    }

    // @Override
    // protected SimpleOmpPragma copyPrivate() {
    // return new SimpleOmpPragma(customContent, getDirectiveKind(), getInfo());
    // }

    @Override
    public void setFullContent(String fullContent) {
        set(CUSTOM_CONTENT, fullContent);
        // this.customContent = fullContent;
    }

}
