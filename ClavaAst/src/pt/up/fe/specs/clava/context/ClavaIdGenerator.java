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

package pt.up.fe.specs.clava.context;

import pt.up.fe.specs.util.collections.AccumulatorMap;

public class ClavaIdGenerator {

    // private static final String TYPE_ID_PREFIX = "type_";
    // private static final String DECL_ID_PREFIX = "decl_";

    private final AccumulatorMap<String> acc;

    public ClavaIdGenerator() {
        this.acc = new AccumulatorMap<>();
    }

    public String next(String prefix) {
        Integer suffixValue = acc.add(prefix);

        return prefix + suffixValue;
    }

    /*
    public String nextTypeId() {
        return next(TYPE_ID_PREFIX);
    }
    
    public String nextDeclId() {
        return next(DECL_ID_PREFIX);
    }
    */
}
