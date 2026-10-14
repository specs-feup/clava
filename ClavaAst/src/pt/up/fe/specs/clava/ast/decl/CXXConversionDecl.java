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
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clava.ast.decl;

import java.util.Collection;

import org.suikasoft.jOptions.DataStore.DataClass;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.type.Type;

/**
 * Represents a C++ conversion function within a class.
 * 
 * @author JBispo
 *
 */
public class CXXConversionDecl extends CXXConversionDeclBase {

    public CXXConversionDecl(DataStore data, java.util.Collection<? extends ClavaNode> children, CXXConversionDeclBase.Values values) {
        this(data, children);
        setWireValues(values);
    }

    public CXXConversionDecl(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);

        // Fix DECL_NAME
        // set(DECL_NAME, "operator " + getWireConversionType().getCode());
    }

    // @Override
    // public String getDeclName() {
    // return buildDeclName(this);
    // }

    public static String buildDeclName(DataClass<?> data) {
        return "operator " + data.get(CONVERSION_TYPE).getCode();
    }

    @Override
    public String getCode() {
        return getCode(false);
    }
}
