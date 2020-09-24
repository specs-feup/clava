package pt.up.fe.specs.clava.weaver.memoi.policy.apply;

import java.util.List;
import java.util.function.Predicate;

import pt.up.fe.specs.clava.weaver.memoi.MergedMemoiReport;

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

public class ApplyIfNotEmpty implements Predicate<MergedMemoiReport>, java.io.Serializable {

    private static final long serialVersionUID = 4039863189233044476L;

    @Override
    public boolean test(MergedMemoiReport t) {

        List<Integer> reportElements = t.getElements();

        return reportElements.stream().anyMatch(e -> e != 0.0);
    }
}
