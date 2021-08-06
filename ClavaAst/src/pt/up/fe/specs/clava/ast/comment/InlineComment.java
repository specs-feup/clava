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

import java.util.Collection;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;

public class InlineComment extends Comment {

    /// DATAKEYS BEGIN

    public final static DataKey<String> TEXT = KeyFactory.string("text");

    public final static DataKey<Boolean> IS_STMT_COMMENT = KeyFactory.bool("isStmtComment");

    /// DATAKEYS END

    public InlineComment(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    @Override
    public String getCode() {
        return "//" + get(TEXT);
    }

    @Override
    public String getText() {
        return get(TEXT);
    }

    @Override
    public void setText(String text) {
        set(TEXT, text);
    }

    /**
     * 
     * @return true, if the inline comment appears alone in the statement.
     */
    public boolean isStmtComment() {
        return get(IS_STMT_COMMENT);
    }

}
