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

package pt.up.fe.specs.clava.ast.lara;

import java.util.Collection;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.pragma.Pragma;

public class LaraTagPragma extends Pragma {

    private static final String LARA_TAG_PREFIX = "lara tag ";

    /// DATAKEYS BEGIN

    public final static DataKey<String> TAG_ID = KeyFactory.string("tagId");

    /// DATAKEYS END

    public LaraTagPragma(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    // private String tagId;

    /**
     * @param tagId
     * @param info
     */
    // @Deprecated
    // public LaraTagPragma(String tagId, ClavaNodeInfo info) {
    // this(tagId, info, Collections.emptyList());
    // }

    /**
     * 
     * @param tagId
     * @param info
     * @param children
     */
    // @Deprecated
    // private LaraTagPragma(String tagId, ClavaNodeInfo info, Collection<? extends ClavaNode> children) {
    // super(info, children);
    //
    // this.tagId = tagId;
    // }
    //
    // @Override
    // protected ClavaNode copyPrivate() {
    // return new LaraTagPragma(tagId, getInfo(), Collections.emptyList());
    // }

    @Override
    public String getFullContent() {
        // return LARA_TAG_PREFIX + tagId;
        return LARA_TAG_PREFIX + get(TAG_ID);
    }

    @Override
    public void setFullContent(String fullContent) {
        String newContent = fullContent;

        if (newContent.startsWith(LARA_TAG_PREFIX)) {
            newContent = newContent.substring(LARA_TAG_PREFIX.length());
        }

        set(TAG_ID, newContent);
        // tagId = newContent;
    }

    public String getTagId() {
        return get(TAG_ID);
        // return tagId;
    }

    @Override
    public String getCode() {
        return "#pragma " + getFullContent();
    }
}
