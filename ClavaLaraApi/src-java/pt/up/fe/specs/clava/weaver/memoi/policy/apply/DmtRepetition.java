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

package pt.up.fe.specs.clava.weaver.memoi.policy.apply;

import java.util.function.Predicate;

import pt.up.fe.specs.clava.weaver.memoi.DirectMappedTable;

public class DmtRepetition {

    public static Predicate<DirectMappedTable> OVER_25_PCT = dmt -> overXPct(dmt, 25);
    public static Predicate<DirectMappedTable> OVER_50_PCT = dmt -> overXPct(dmt, 50);
    public static Predicate<DirectMappedTable> OVER_75_PCT = dmt -> overXPct(dmt, 75);
    public static Predicate<DirectMappedTable> OVER_90_PCT = dmt -> overXPct(dmt, 90);

    public static boolean overXPct(DirectMappedTable dmt, double pct) {

        return dmt.getStats().getCallCoverage() > pct;
    }
}
