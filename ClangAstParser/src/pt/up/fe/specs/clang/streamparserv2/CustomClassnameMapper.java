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

package pt.up.fe.specs.clang.streamparserv2;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;

public class CustomClassnameMapper {

    private final Map<String, Function<DataStore, Class<? extends ClavaNode>>> customMaps;

    public CustomClassnameMapper() {
        this.customMaps = new HashMap<>();
    }

    public Class<? extends ClavaNode> getClass(String classname, DataStore data) {
        if (!customMaps.containsKey(classname)) {
            return null;
        }

        return customMaps.get(classname).apply(data);
    }

    public CustomClassnameMapper add(String classname, Function<DataStore, Class<? extends ClavaNode>> mapper) {

        customMaps.put(classname, mapper);

        return this;
    }

}
