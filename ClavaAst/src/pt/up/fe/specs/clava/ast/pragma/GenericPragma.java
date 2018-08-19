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

package pt.up.fe.specs.clava.ast.pragma;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;

public class GenericPragma extends Pragma {

    /// DATAKEYS BEGIN

    public final static DataKey<List<String>> CONTENT = KeyFactory.generic("content",
            (List<String>) new ArrayList<String>());

    /// DATAKEYS END

    public GenericPragma(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    // private List<String> content;

    // /**
    // * @deprecated
    // * @param content
    // * @param info
    // */
    // @Deprecated
    // public GenericPragma(List<String> content, ClavaNodeInfo info) {
    // this(content, info, Collections.emptyList());
    // }
    //
    // /**
    // * @deprecated
    // * @param content
    // * @param info
    // * @param children
    // */
    // @Deprecated
    // private GenericPragma(List<String> content, ClavaNodeInfo info, Collection<? extends ClavaNode> children) {
    // super(info, children);
    //
    // this.content = content;
    // }
    //
    // @Override
    // protected ClavaNode copyPrivate() {
    // return new GenericPragma(new ArrayList<>(content), getInfo(), Collections.emptyList());
    // }

    @Override
    public String getCode() {
        return get(CONTENT).stream().collect(Collectors.joining("\\" + ln(), "#pragma ", ""));
    }

    @Override
    public String getFullContent() {
        return get(CONTENT).stream().collect(Collectors.joining(" "));
    }

    @Override
    public void setFullContent(String fullContent) {
        set(CONTENT, Arrays.asList(fullContent));
        // this.content = Arrays.asList(fullContent);
    }

}
