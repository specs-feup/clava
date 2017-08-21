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

public class InlineComment extends Comment {

    private final String text;
    private final boolean isStmtComment;

    public InlineComment(String text, boolean isStmtComment, ClavaNodeInfo nodeInfo) {
        super(nodeInfo, Collections.emptyList());

        this.text = text;
        this.isStmtComment = isStmtComment;
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new InlineComment(text, isStmtComment, getInfo());
    }

    @Override
    public String getCode() {
        return "//" + text;
    }

    @Override
    public String getText() {
        return text;
    }

    /**
     * 
     * @return true, if the inline comment appears alone in the statement.
     */
    public boolean isStmtComment() {
        return isStmtComment;
    }

}
