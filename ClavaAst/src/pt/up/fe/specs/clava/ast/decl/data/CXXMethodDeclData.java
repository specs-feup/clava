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

package pt.up.fe.specs.clava.ast.decl.data;

// TODO: Extract to its own file
public class CXXMethodDeclData {
    private final String namespace;
    private final String record;
    private final String recordId;

    public CXXMethodDeclData(String namespace, String record, String recordId) {
        this.namespace = namespace;
        this.record = record;
        this.recordId = recordId;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getRecord() {
        return record;
    }

    public String getRecordId() {
        return recordId;
    }

    @Override
    public String toString() {
        StringBuilder string = new StringBuilder();

        string.append("Namespace: " + namespace);
        string.append(", Record: " + record);
        string.append(", Record Id: " + recordId);

        return string.toString();
    }
}