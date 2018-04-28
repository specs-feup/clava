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

/**
 * @deprecated
 * @author JoaoBispo
 *
 */
@Deprecated
public class DummyAttributeData extends AttributeData {

    private String classname;

    /**
     * Full constructor.
     * 
     * @param classname
     * @param data
     */
    public DummyAttributeData(String classname, AttributeData data) {
        super(data);

        this.classname = classname;
    }

    /**
     * Copy construtor.
     * 
     * @param data
     */
    public DummyAttributeData(DummyAttributeData data) {
        this(data.classname, data);
    }

    /**
     * Empty constructor.
     */
    public DummyAttributeData() {
        this(null, new AttributeData());
    }

    public DummyAttributeData setData(DummyAttributeData data) {
        this.classname = data.classname;

        return this;
    }

    public String getClassname() {
        return classname;
    }
}
