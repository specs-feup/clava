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

package pt.up.fe.specs.clang.clava.lara;

import java.util.Collection;
import java.util.Collections;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.pragma.Pragma;

public class LaraMarkerPragma extends Pragma {

    private static final String LARA_MARKER_PREFIX = "lara marker ";

    private String markerId;

    public LaraMarkerPragma(String markerId, ClavaNodeInfo info) {
        this(markerId, info, Collections.emptyList());
    }

    private LaraMarkerPragma(String markerId, ClavaNodeInfo info, Collection<? extends ClavaNode> children) {
        super(info, children);

        this.markerId = markerId;
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new LaraMarkerPragma(markerId, getInfo(), Collections.emptyList());
    }

    @Override
    public String getFullContent() {
        return LARA_MARKER_PREFIX + markerId;
    }

    @Override
    public void setFullContent(String fullContent) {
        String newContent = fullContent;

        if (newContent.startsWith(LARA_MARKER_PREFIX)) {
            newContent = newContent.substring(LARA_MARKER_PREFIX.length());
        }

        this.markerId = newContent;
    }

    public String getMarkerId() {
        return markerId;
    }

    @Override
    public String getCode() {
        return "#pragma lara marker " + markerId;
    }
}
