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
 * Special pragma that stores information that can be easily accessed from LARA <br>
 * E.g. #pragma clava data aString: 'string', aNumber: 10
 * 
 * @author jbispo
 *
 */
public class ClavaDataPragma extends Pragma {

    private static final String PRAGMA_NAME = "clava";
    private static final String PRAGMA_CLAVA_TYPE = "data";

    /// DATAKEYS BEGIN

    public final static DataKey<String> DATA_CONTENT = KeyFactory.string("dataContent");

    /// DATAKEYS END

    public ClavaDataPragma(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    @Override
    public String getFullContent() {
        return PRAGMA_NAME + " " + PRAGMA_CLAVA_TYPE + " " + get(DATA_CONTENT);
    }

    @Override
    public void setFullContent(String fullContent) {
        String newContent = fullContent;

        newContent = newContent.trim();

        if (newContent.startsWith(PRAGMA_NAME)) {
            newContent = newContent.substring(LARA_MARKER_PREFIX.length());
        }

        set(MARKER_ID, newContent);
        // this.markerId = newContent;
    }

    @Override
    public String getData() {
        return get(DATA_CONTENT);
        // return markerId;
    }

    @Override
    public String getCode() {
        // Only produce code if there is data
        var data = get(DATA_CONTENT);

        if (data.isBlank()) {
            return "";
        }

        return "#pragma " + PRAGMA_NAME + " " + PRAGMA_CLAVA_TYPE + " " + data;
    }
}
