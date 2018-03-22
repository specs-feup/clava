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

package pt.up.fe.specs.clava.ast.decl.data2;

import pt.up.fe.specs.util.exceptions.NotImplementedException;

public abstract class ClavaData {

    protected String toString(String superToString, String thisToString) {

        // Use bridge if there is content and a suffix
        String bridge = !thisToString.isEmpty() && !superToString.isEmpty() ? ", " : "";

        return superToString + bridge + thisToString;
    }

    public String getId() {
        throw new NotImplementedException(this);
    }

    /**
     * Makes a deep copy of this object.
     * 
     * @return
     */
    abstract ClavaData copy();
}
