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

package pt.up.fe.specs.clava.ast.type;

import java.util.Collection;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;

public class MemberPointerType extends Type {

    public MemberPointerType(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    @Override
    public String getCode(ClavaNode sourceNode, String intermediateCode) {
        // HACK: not sure if this always works, and anyway, should be generating the code from the type information
        var bareType = getBareType();

        if (intermediateCode != null) {
            if (!bareType.endsWith(")()")) {
                // throw new RuntimeException("Expected bare type to end with ')()': " + bareType);
                return bareType + intermediateCode;
            }

            return bareType.substring(0, bareType.length() - 3) + intermediateCode + ")()";
        }

        // System.out.println("MemberPointerType intermediate code: " + intermediateCode);
        // System.out.println("Source node: " + sourceNode);
        // System.out.println("Bare type: " + getBareType());

        // Otherwise, just return super
        return super.getCode(sourceNode, intermediateCode);
    }

}
