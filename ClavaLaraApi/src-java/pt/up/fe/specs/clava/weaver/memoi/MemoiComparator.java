package pt.up.fe.specs.clava.weaver.memoi;
import java.util.List;

/**
 * Copyright 2019 SPeCS.
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

@FunctionalInterface
public interface MemoiComparator {

    final static MemoiComparator MEAN = (List<Integer> c1, List<Integer> c2, int t) -> {

        double mean1 = mean(c1, t);
        double mean2 = mean(c2, t);

        if (mean1 > mean2) {
            return 1;
        } else if (mean1 < mean2) {
            return -1;
        } else {
            return 0;
        }
    };

    private static double mean(List<Integer> c1, int t) {

        return c1.stream().reduce(0, Integer::sum) / t;
    }

    /**
     * Compares to counts merged lists.
     * 
     * @param c1
     *            the first list
     * @param c2
     *            the second list
     * @param total
     *            the total number of elements
     * 
     * @return 1 if c1 > c2, -1 if c1 < c2, and 0 otherwise
     */
    int compare(List<Integer> c1, List<Integer> c2, int total);
}
