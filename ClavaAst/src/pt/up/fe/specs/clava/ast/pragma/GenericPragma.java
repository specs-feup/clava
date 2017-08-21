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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;

public class GenericPragma extends Pragma {

    private final List<String> content;

    public GenericPragma(List<String> content, ClavaNodeInfo info) {
        this(content, info, Collections.emptyList());
    }

    private GenericPragma(List<String> content, ClavaNodeInfo info, Collection<? extends ClavaNode> children) {
        super(info, children);

        this.content = content;
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new GenericPragma(content, getInfo(), Collections.emptyList());
    }

    @Override
    public String getCode() {
        return content.stream().collect(Collectors.joining("\\" + ln(), "#pragma ", ""));
    }

    @Override
    public String getFullContent() {
        return content.stream().collect(Collectors.joining(" "));
    }

}
