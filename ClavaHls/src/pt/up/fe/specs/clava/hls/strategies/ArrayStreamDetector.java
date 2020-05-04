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
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowGraph;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowParam;
import pt.up.fe.specs.clava.hls.ClavaHLS;
import pt.up.fe.specs.clava.hls.directives.HLSStream;

public class ArrayStreamDetector extends RestructuringStrategy {
    private HashMap<String, Boolean> isStream = new HashMap<>();

    public ArrayStreamDetector(DataFlowGraph dfg) {
	super(dfg);
	for (DataFlowParam param : dfg.getParams()) {
	    if (param.isArray())
		isStream.put(param.getName(), false);
	}

    }

    @Override
    public void analyze() {
	for (String variable : isStream.keySet()) {
	    isStream.put(variable, true);
	}
    }

    @Override
    public void apply() {
	ClavaNode node = dfg.getFirstStmt();

	for (String variable : isStream.keySet()) {
	    if (isStream.get(variable)) {
		HLSStream directive = new HLSStream(variable);
		insertDirective(node, directive);
		ClavaHLS.log("declaring parameter array " + variable + " as stream");
	    }
	}
    }
}
