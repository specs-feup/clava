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

package pt.up.fe.specs.clava.ast.attr.data;

public class DummyAttributeData extends AttributeData {

    private final String classname;

    public DummyAttributeData(String classname, AttributeData data) {
        super(data);

        this.classname = classname;
    }

    public DummyAttributeData(DummyAttributeData data) {
        this(data.classname, data);
    }

    public String getClassname() {
        return classname;
    }
}
