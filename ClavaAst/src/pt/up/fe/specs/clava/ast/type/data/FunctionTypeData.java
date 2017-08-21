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

package pt.up.fe.specs.clava.ast.type.data;

import pt.up.fe.specs.clava.ast.type.FunctionType.CallingConv;

public class FunctionTypeData {

    private final boolean hasNoReturn;
    private final boolean producesResult;
    private final boolean hasRegParm;
    private final Integer regParm;
    private final CallingConv callingConv;

    public FunctionTypeData() {
        this(false, false, false, 0, CallingConv.C);
    }

    public FunctionTypeData(boolean hasNoReturn, boolean producesResult, boolean hasRegParm, Integer regParm,
            CallingConv callingConv) {
        this.hasNoReturn = hasNoReturn;
        this.producesResult = producesResult;
        this.hasRegParm = hasRegParm;
        this.regParm = regParm;
        this.callingConv = callingConv;
    }

    public boolean hasNoReturn() {
        return hasNoReturn;
    }

    public boolean producesResult() {
        return producesResult;
    }

    public boolean hasRegParm() {
        return hasRegParm;
    }

    public Integer getRegParm() {
        return regParm;
    }

    public CallingConv getCallingConv() {
        return callingConv;
    }

}
