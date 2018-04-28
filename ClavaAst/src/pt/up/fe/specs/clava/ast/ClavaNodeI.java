/**
 * Copyright 2018 SPeCS.
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

package pt.up.fe.specs.clava.ast;

import java.util.ArrayList;
import java.util.List;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;

import pt.up.fe.specs.clava.SourceRange;
import pt.up.fe.specs.clava.ast.comment.InlineComment;

public interface ClavaNodeI {
    /**
     * Id of the node.
     */
    static DataKey<String> ID = KeyFactory.string("id");

    /**
     * The kind of ClavaNode class this DataStore is associated with. TODO: Not sure if is necessary.
     */
    // static DataKey<Class<? extends ClavaNode>> NODE_CLASS = KeyFactory.generic("nodeClass", ClavaNode.class);

    /**
     * Location of this node. Might not be available (e.g., type nodes).
     */
    static DataKey<SourceRange> LOCATION = KeyFactory.object("location", SourceRange.class);

    /**
     * If this node is part of a macro.
     */
    static DataKey<Boolean> IS_MACRO = KeyFactory.bool("isMacro");

    static DataKey<SourceRange> SPELLING_LOCATION = KeyFactory.object("spellingLocation",
            SourceRange.class);

    static DataKey<List<InlineComment>> INLINE_COMMENTS = KeyFactory.generic("inlineComments", new ArrayList<>());
}
