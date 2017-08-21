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

package pt.up.fe.specs.clava.ast.type.tag;

public class DeclRef {
    private final String declName;
    private final String declId;
    // private final String declAddress;
    private final String declType;

    public DeclRef(String declName, String declId, String declType) {
        this.declName = declName;
        this.declId = declId;
        this.declType = declType;
    }

    public String getDeclId() {
        return declId;
    }

    public String getDeclName() {
        return declName;
    }

    public String getDeclType() {
        return declType;
    }

    @Override
    public String toString() {
        return "name: " + declName + "; id: " + declId + "; type: " + declType;
    }

}