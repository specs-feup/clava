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

package pt.up.fe.specs.clava.ast.extra;

import java.util.Collections;
import java.util.List;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.ClavaData;

public class UnsupportedNode extends ClavaNode {

    private final String classname;

    public UnsupportedNode(String classname, ClavaData data, List<ClavaNode> children) {
        super(data, Collections.emptyList());
        // super(ClavaNodeInfo.undefinedInfo(), children);

        this.classname = classname;
        // setData(data);
    }

    // @Override
    // protected ClavaNode copyPrivate() {
    // return new UnsupportedNode(classname, ClavaData.copy(getData()), Collections.emptyList());
    // }

    public String getClassname() {
        return classname;
    }

    @Override
    public String toContentString() {
        return toContentString("[" + classname + "]", super.toContentString());
    }

}
