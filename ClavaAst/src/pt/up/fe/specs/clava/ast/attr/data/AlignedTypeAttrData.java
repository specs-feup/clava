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

import pt.up.fe.specs.clava.ast.type.Type;

public class AlignedTypeAttrData extends AttributeData {

    private String spelling;
    private Type type;

    /**
     * Full constructor.
     * 
     * @param spelling
     * @param type
     * @param data
     */
    public AlignedTypeAttrData(String spelling, Type type, AttributeData data) {
        super(data);

        this.spelling = spelling;
        this.type = type;
    }

    /**
     * Copy constructor.
     * 
     * @param data
     */
    public AlignedTypeAttrData(AlignedTypeAttrData data) {
        super(data);
        setData(data);
    }

    /**
     * Empty constructor.
     * 
     * @param data
     * @return
     */
    public AlignedTypeAttrData() {
        this(null, null, new AttributeData());
    }

    public AlignedTypeAttrData setData(AlignedTypeAttrData data) {
        this.spelling = data.spelling;
        this.type = data.type;

        return this;
    }

    public String getSpelling() {
        return spelling;
    }

    public Type getType() {
        return type;
    }

}
