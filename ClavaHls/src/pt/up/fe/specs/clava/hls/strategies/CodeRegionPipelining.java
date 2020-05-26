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

import java.util.HashMap;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.analysis.flow.data.DFGUtils;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowGraph;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowNode;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowParam;
import pt.up.fe.specs.clava.hls.ClavaHLS;
import pt.up.fe.specs.clava.hls.directives.HLSArrayPartition;
import pt.up.fe.specs.clava.hls.directives.HLSArrayPartition.PartitionType;
import pt.up.fe.specs.clava.hls.directives.HLSPipeline;
import pt.up.fe.specs.clava.hls.heuristics.PipelineHeuristic;

public class CodeRegionPipelining extends RestructuringStrategy {
    private HashMap<DataFlowNode, Integer> toPipeline;
    private HashMap<DataFlowNode, Integer> unrolledLoops;
    private boolean pipelineFunction = false;

    public CodeRegionPipelining(DataFlowGraph dfg, HashMap<DataFlowNode, Integer> unrolledLoops) {
	super(dfg);
	toPipeline = new HashMap<>();
	this.unrolledLoops = unrolledLoops;
    }

    @Override
    public void analyze() {
	this.unrolledLoops.forEach((k, v) -> {
	    if (v == Integer.MAX_VALUE && toPipeline.get(k) == null) {
		DataFlowNode loop = DFGUtils.getLoopOfLoop(k);
		if (!loop.equals(k)) {
		    int II = PipelineHeuristic.calculate(loop);
		    if (II != 0)
			toPipeline.put(loop, II);
		} else {
		    if (DFGUtils.getTopLoopCount(dfg) == 1) {
			// pipelineFunction = true;
		    }
		}
	    }
	});
    }

    @Override
    public void apply() {

	if (pipelineFunction) {
	    HLSPipeline directive = new HLSPipeline();
	    insertDirective(dfg.getFirstStmt(), directive);
	    ClavaHLS.log("pipelining the whole function");
	} else {
	    toPipeline.forEach((k, v) -> {
		StringBuilder sb = new StringBuilder("pipelining body of loop \"").append(k.getLabel())
			.append("\" with ");
		HLSPipeline directive = new HLSPipeline();
		if (v != Integer.MAX_VALUE) {
		    directive.setII(v);
		    sb.append("II = ").append(v);
		} else
		    sb.append(" undetermined II");
		insertDirective(k.getStmt(), directive);
		ClavaHLS.log(sb.toString());
	    });
	}
	ClavaNode firstStmt = dfg.getFirstStmt();
	for (DataFlowParam param : dfg.getParams()) {
	    if (param.isArray() && !param.isStream()) {
		int factor = (param.getMaxSize() != 0) ? param.getMaxSize() / 4 : 2;
		HLSArrayPartition directive = new HLSArrayPartition(PartitionType.CYCLIC, param.getName(), factor);
		directive.setDim(param.getDim());
		this.insertDirective(firstStmt, directive);
	    }

	}
    }

}
