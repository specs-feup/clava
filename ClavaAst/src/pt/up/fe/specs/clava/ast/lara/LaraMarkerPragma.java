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

/**
 * Special pragma that can be used to mark scopes (e.g., #pragma lara marker loop1).
 * 
 * @author jbispo
 *
 */
public class LaraMarkerPragma extends Pragma {

    private static final String LARA_MARKER_PREFIX = "lara marker ";

    /// DATAKEYS BEGIN

    public final static DataKey<String> MARKER_ID = KeyFactory.string("markerId");

    /// DATAKEYS END

    public LaraMarkerPragma(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    // private String markerId;

    /**
     * @param markerId
     * @param info
     */
    // @Deprecated
    // public LaraMarkerPragma(String markerId, ClavaNodeInfo info) {
    // this(markerId, info, Collections.emptyList());
    // }

    /**
     * 
     * @param markerId
     * @param info
     * @param children
     */
    // @Deprecated
    // private LaraMarkerPragma(String markerId, ClavaNodeInfo info, Collection<? extends ClavaNode> children) {
    // super(info, children);
    //
    // this.markerId = markerId;
    // }

    // @Override
    // protected ClavaNode copyPrivate() {
    // return new LaraMarkerPragma(markerId, getInfo(), Collections.emptyList());
    // }

    @Override
    public String getFullContent() {
        return LARA_MARKER_PREFIX + get(MARKER_ID);
    }

    @Override
    public void setFullContent(String fullContent) {
        String newContent = fullContent;

        if (newContent.startsWith(LARA_MARKER_PREFIX)) {
            newContent = newContent.substring(LARA_MARKER_PREFIX.length());
        }

        set(MARKER_ID, newContent);
        // this.markerId = newContent;
    }

    public String getMarkerId() {
        return get(MARKER_ID);
        // return markerId;
    }

    @Override
    public String getCode() {
        return "#pragma lara marker " + getMarkerId();
    }
}
