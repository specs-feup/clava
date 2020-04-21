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

package pt.up.fe.specs.clava.analysis.flow.control;

import pt.up.fe.specs.clava.analysis.flow.FlowEdge;

public class BasicBlockEdge extends FlowEdge {
    private BasicBlockEdgeType type;

    public BasicBlockEdge(BasicBlockNode source, BasicBlockNode dest, BasicBlockEdgeType type) {
	super(source, dest);
	this.type = type;
    }

    public static BasicBlockEdge newTrueEdge(BasicBlockNode source, BasicBlockNode dest) {
	return new BasicBlockEdge(source, dest, BasicBlockEdgeType.TRUE);
    }

    public static BasicBlockEdge newFalseEdge(BasicBlockNode source, BasicBlockNode dest) {
	return new BasicBlockEdge(source, dest, BasicBlockEdgeType.FALSE);
    }

    public static BasicBlockEdge newLoopEdge(BasicBlockNode source, BasicBlockNode dest) {
	return new BasicBlockEdge(source, dest, BasicBlockEdgeType.LOOP);
    }

    public static BasicBlockEdge newNoLoopEdge(BasicBlockNode source, BasicBlockNode dest) {
	return new BasicBlockEdge(source, dest, BasicBlockEdgeType.NOLOOP);
    }

    public static BasicBlockEdge newEdge(BasicBlockNode source, BasicBlockNode dest) {
	return new BasicBlockEdge(source, dest, BasicBlockEdgeType.UNCONDITIONAL);
    }

    @Override
    public String toDot() {

	String ARROW = "->";
	String SPACE = " ";
	String NL = "\n";
	String QUOTE = "\"";

	StringBuilder b = new StringBuilder();

	b.append(getSource().getName());
	b.append(SPACE);
	b.append(ARROW);
	b.append(SPACE);
	b.append(getDest().getName());
	b.append(SPACE);
	b.append("[label=");
	b.append(QUOTE);
	b.append(type.getLabel());
	b.append(QUOTE);
	b.append("]");
	b.append(NL);

	return b.toString();
    }

    public BasicBlockEdgeType getType() {
	return type;
    }
}
