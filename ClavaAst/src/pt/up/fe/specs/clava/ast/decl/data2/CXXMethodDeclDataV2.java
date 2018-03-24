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

public class CXXMethodDeclDataV2 extends FunctionDeclDataV2 {

    private final String recordId;

    public CXXMethodDeclDataV2(String recordId, FunctionDeclDataV2 data) {
        super(data);

        this.recordId = recordId;
    }

    public CXXMethodDeclDataV2(CXXMethodDeclDataV2 data) {
        this(data.recordId, data);
    }

    @Override
    public CXXMethodDeclDataV2 copy() {
        return new CXXMethodDeclDataV2(this);
    }

    @Override
    public String toString() {
        return toString(super.toString(), "recordId: " + recordId);
    }

    public String getRecordId() {
        return recordId;
    }
}
