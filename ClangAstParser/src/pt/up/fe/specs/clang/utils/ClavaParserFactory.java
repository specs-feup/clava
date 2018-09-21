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

package pt.up.fe.specs.clang.utils;

import java.util.Collections;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clang.clava.lara.LaraMarkerPragma;
import pt.up.fe.specs.clang.clava.lara.LaraTagPragma;
import pt.up.fe.specs.clava.context.ClavaContext;
import pt.up.fe.specs.clava.context.ClavaFactory;

public class ClavaParserFactory extends ClavaFactory {

    public ClavaParserFactory(ClavaContext context) {
        super(context);
    }

    /// PRAGMAS

    public LaraMarkerPragma laraMarkerPragma(String markedId) {
        DataStore data = newDataStore(LaraMarkerPragma.class)
                .set(LaraMarkerPragma.MARKER_ID, markedId);

        return new LaraMarkerPragma(data, Collections.emptyList());
    }

    public LaraTagPragma laraTagPragma(String tagId) {
        DataStore data = newDataStore(LaraTagPragma.class)
                .set(LaraTagPragma.TAG_ID, tagId);

        return new LaraTagPragma(data, Collections.emptyList());
    }
}
