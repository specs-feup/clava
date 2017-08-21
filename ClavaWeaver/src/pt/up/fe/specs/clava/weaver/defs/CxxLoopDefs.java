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

package pt.up.fe.specs.clava.weaver.defs;

import org.lara.interpreter.utils.DefMap;

import pt.up.fe.specs.clava.weaver.joinpoints.CxxLoop;

public class CxxLoopDefs {

    private static final DefMap<CxxLoop> DEF_MAP;
    static {
        DEF_MAP = new DefMap<>(CxxLoop.class);
        DEF_MAP.addBool("isParallel", CxxLoopDefs::defIsParallel);
        DEF_MAP.addInteger("iterations", CxxLoopDefs::defIterations);
    }

    public static DefMap<CxxLoop> getDefMap() {
        return DEF_MAP;
    }

    private static void defIsParallel(CxxLoop jp, Boolean value) {
        jp.getNode().setParallel(value);
    }

    private static void defIterations(CxxLoop jp, Integer iterations) {
        jp.getNode().setIterations(iterations);
    }

}
