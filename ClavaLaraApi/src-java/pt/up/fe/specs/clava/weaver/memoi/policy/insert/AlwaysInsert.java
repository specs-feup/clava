package pt.up.fe.specs.clava.weaver.memoi.policy.insert;

import java.util.function.Predicate;

import pt.up.fe.specs.clava.weaver.memoi.MergedMemoiEntry;

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

/**
 * Policies for inserting specific entries into the table.
 * 
 * @author pedro
 *
 */
public class AlwaysInsert implements Predicate<MergedMemoiEntry>, java.io.Serializable {

    private static final long serialVersionUID = 645571394428346494L;

    public AlwaysInsert() {
    }

    @Override
    public boolean test(MergedMemoiEntry t) {
        return true;
    }
}
