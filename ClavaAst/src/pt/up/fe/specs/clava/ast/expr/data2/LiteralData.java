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

package pt.up.fe.specs.clava.ast.expr.data2;

/**
 * @deprecated
 * @author JoaoBispo
 *
 */
@Deprecated
public class LiteralData extends ExprDataV2 {

    private String sourceLiteral;

    public LiteralData(String sourceLiteral, ExprDataV2 data) {
        super(data);

        this.sourceLiteral = sourceLiteral;
    }

    public LiteralData(LiteralData data) {
        this(data.sourceLiteral, data);
    }

    public String getSourceLiteral() {
        return sourceLiteral;
    }

    @Override
    public String toString() {
        return toString(super.toString(), "sourceLiteral: " + sourceLiteral);
    }

}
