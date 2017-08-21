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

package pt.up.fe.specs.clava.ast.comment;

import java.util.Collections;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;

public class TextComment extends InlineContentComment {

    private final String text;

    public TextComment(String text, ClavaNodeInfo nodeInfo) {
        super(nodeInfo, Collections.emptyList());

        this.text = text;
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new TextComment(text, getInfo());
    }

    @Override
    public String getCode() {
        return text;
    }

}
