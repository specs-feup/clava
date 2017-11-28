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

package pt.up.fe.specs.clava.ast.extra.data;

import java.util.HashMap;
import java.util.Map;

public class IdNormalizer {

    private Map<String, String> idsAlias;

    public IdNormalizer() {
        this.idsAlias = new HashMap<>();
    }

    public void addAlias(Map<String, String> idsAlias) {
        this.idsAlias.putAll(idsAlias);
    }

    public String normalize(String id) {
        String unaliasedId = idsAlias.get(id);

        return unaliasedId != null ? unaliasedId : id;
    }
}
