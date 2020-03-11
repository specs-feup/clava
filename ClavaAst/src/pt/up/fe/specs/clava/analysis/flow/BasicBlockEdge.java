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

package pt.up.fe.specs.clava.analysis.flow;

public class BasicBlockEdge {

    private BasicBlockEdgeType type;
    private BasicBlock source;
    private BasicBlock dest;

    public BasicBlockEdge(BasicBlock source, BasicBlock dest, BasicBlockEdgeType type) {
        this.type = type;
        this.source = source;
        this.dest = dest;
    }

    public static BasicBlockEdge newTrueEdge(BasicBlock source, BasicBlock dest) {
        return new BasicBlockEdge(source, dest, BasicBlockEdgeType.TRUE);
    }

    public static BasicBlockEdge newFalseEdge(BasicBlock source, BasicBlock dest) {
        return new BasicBlockEdge(source, dest, BasicBlockEdgeType.FALSE);
    }

    public static BasicBlockEdge newLoopEdge(BasicBlock source, BasicBlock dest) {
        return new BasicBlockEdge(source, dest, BasicBlockEdgeType.LOOP);
    }

    public static BasicBlockEdge newNoLoopEdge(BasicBlock source, BasicBlock dest) {
        return new BasicBlockEdge(source, dest, BasicBlockEdgeType.NOLOOP);
    }

    public static BasicBlockEdge newEdge(BasicBlock source, BasicBlock dest) {
        return new BasicBlockEdge(source, dest, BasicBlockEdgeType.UNCONDITIONAL);
    }

    public String toDot() {

        String ARROW = "->";
        String SPACE = " ";
        String NL = "\n";
        String QUOTE = "\"";

        StringBuilder b = new StringBuilder();

        b.append(source.getId());
        b.append(SPACE);
        b.append(ARROW);
        b.append(SPACE);
        b.append(dest.getId());
        b.append(SPACE);
        b.append("[label=");
        b.append(QUOTE);
        b.append(type.getLabel());
        b.append(QUOTE);
        b.append("]");
        b.append(NL);

        return b.toString();
    }

    public BasicBlock getDest() {
        return dest;
    }
}
