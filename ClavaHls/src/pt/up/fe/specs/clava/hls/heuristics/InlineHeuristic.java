/**
 *  Copyright 2020 SPeCS.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

package pt.up.fe.specs.clava.hls.heuristics;

import pt.up.fe.specs.clava.hls.ClavaHLS;

public class InlineHeuristic {
    public static double DEFAULT_B = 2;

    public static boolean calculate(long callFreq, long funCost, long mainCost, double n) {
	ClavaHLS.log("callFreq = " + callFreq + ", funCost = " + funCost + ", mainCost = " + mainCost + "N = " + n);
	long x = callFreq * funCost;
	boolean res = mainCost > (x / n);
	ClavaHLS.log(mainCost + " > (" + callFreq + "*" + funCost + ") / " + n + " -> " + res);
	return res;
    }
}
