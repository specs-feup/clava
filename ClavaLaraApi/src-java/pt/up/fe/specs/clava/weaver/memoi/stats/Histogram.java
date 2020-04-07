/**
 * Copyright 2020 SPeCS.
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

package pt.up.fe.specs.clava.weaver.memoi.stats;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Histogram {

    private Map<Integer, Integer> map;

    public Histogram(List<Integer> meanCounts) {

        map = new TreeMap<>();

        meanCounts.stream()
                .forEach(c -> {
                    map.merge(c, 1, Integer::sum);
                });
    }

    @Override
    public String toString() {

        StringBuilder b = new StringBuilder("== histogram ==\n");

        for (var key : map.keySet()) {
            b.append(key);
            b.append(": ");
            b.append(map.get(key));
            b.append("\n");
        }

        return b.toString();
    }
}
