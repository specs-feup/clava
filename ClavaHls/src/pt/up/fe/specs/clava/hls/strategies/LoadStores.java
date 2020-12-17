/**
 *  Copyright 2020 SPeCS.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

package pt.up.fe.specs.clava.hls.strategies;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.analysis.flow.data.DFGUtils;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowGraph;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowNode;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowNodeType;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowParam;
import pt.up.fe.specs.clava.hls.ClavaHLS;
import pt.up.fe.specs.clava.hls.directives.HLSArrayPartition;
import pt.up.fe.specs.clava.hls.directives.HLSArrayPartition.PartitionType;
import pt.up.fe.specs.clava.hls.directives.HLSPipeline;
import pt.up.fe.specs.clava.hls.directives.HLSUnroll;

public class LoadStores extends RestructuringStrategy {
    private boolean isSimpleLoop = false;
    private int lsFactor;
    public static int DEFAULT_LS = 32;

    public LoadStores(DataFlowGraph dfg, int lsFactor) {
	super(dfg);
	this.lsFactor = lsFactor;
    }

    @Override
    public void analyze() {
	ClavaHLS.log("checking if the function is a simple loop...");
	this.isSimpleLoop = false;
	if (DFGUtils.getAllNodesOfType(dfg, DataFlowNodeType.LOOP).size() != 1) {
	    return;
	}
	DataFlowNode loop = DFGUtils.getAllNodesOfType(dfg, DataFlowNodeType.LOOP).get(0);
	if (loop.getOutNodes().size() != 1) {
	    return;
	}
	this.isSimpleLoop = true;
    }

    @Override
    public void apply() {
	if (isSimpleLoop) {
	    ClavaNode loopNode = DFGUtils.getAllNodesOfType(dfg, DataFlowNodeType.LOOP).get(0).getClavaNode();
	    ClavaNode funNode = dfg.getFirstStmt();

	    // Partitioning arrays
	    for (DataFlowParam p : dfg.getParams()) {
		if (p.isArray()) {
		    var part = new HLSArrayPartition(PartitionType.CYCLIC, p.getName(), lsFactor);
		    insertDirective(funNode, part);
		}
	    }

	    // Unroll loop
	    HLSUnroll unroll = new HLSUnroll();
	    unroll.setFactor(lsFactor);
	    insertDirective(loopNode, unroll);

	    // Pipeline loop
	    HLSPipeline pip = new HLSPipeline();
	    insertDirective(loopNode, pip);
	}
    }

    public int getLsFactor() {
	return lsFactor;
    }

    public void setLsFactor(int lsFactor) {
	this.lsFactor = lsFactor;
    }

    public boolean isSimpleLoop() {
	return isSimpleLoop;
    }

    public void setSimpleLoop(boolean isSimpleLoop) {
	this.isSimpleLoop = isSimpleLoop;
    }

}
