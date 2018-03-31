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

package pt.up.fe.specs.clava.ast.expr.data;

import java.util.List;

import pt.up.fe.specs.clava.ast.expr.enums.LambdaCaptureDefault;
import pt.up.fe.specs.clava.ast.expr.enums.LambdaCaptureKind;

public class LambdaExprData {

    private final boolean isGenericLambda;
    private final boolean isMutable;
    private final boolean hasExplicitParameters;
    private final boolean hasExplicitResultType;
    private final LambdaCaptureDefault captureDefault;
    private final List<LambdaCaptureKind> captureKinds;

    public LambdaExprData(boolean isGenericLambda, boolean isMutable, boolean hasExplicitParameters,
            boolean hasExplicitResultType, LambdaCaptureDefault captureDefault, List<LambdaCaptureKind> captureKinds) {

        this.isGenericLambda = isGenericLambda;
        this.isMutable = isMutable;
        this.hasExplicitParameters = hasExplicitParameters;
        this.hasExplicitResultType = hasExplicitResultType;
        this.captureDefault = captureDefault;
        this.captureKinds = captureKinds;
    }

    public boolean isGenericLambda() {
        return isGenericLambda;
    }

    public boolean isMutable() {
        return isMutable;
    }

    public boolean isHasExplicitParameters() {
        return hasExplicitParameters;
    }

    public boolean isHasExplicitResultType() {
        return hasExplicitResultType;
    }

    public LambdaCaptureDefault getCaptureDefault() {
        return captureDefault;
    }

    public List<LambdaCaptureKind> getCaptureKinds() {
        return captureKinds;
    }

    @Override
    public String toString() {
        StringBuilder string = new StringBuilder();

        string.append("isGenericLambda: ").append(isGenericLambda).append("\n");
        string.append("isMutable: ").append(isMutable).append("\n");
        string.append("hasExplicitParameters: ").append(hasExplicitParameters).append("\n");
        string.append("hasExplicitResultType: ").append(hasExplicitResultType).append("\n");
        string.append("captureDefault: ").append(captureDefault).append("\n");
        string.append("captureKinds: ").append(captureKinds).append("\n");

        return string.toString();
    }
}
