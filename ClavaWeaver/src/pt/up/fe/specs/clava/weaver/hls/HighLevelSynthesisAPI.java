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

package pt.up.fe.specs.clava.weaver.hls;

import java.io.File;

import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.analysis.flow.data.DataFlowGraph;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.hls.ClavaHLS;
import pt.up.fe.specs.clava.hls.ClavaHLSOptions;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.joinpoints.CxxFunction;

/**
 * Middleman class to establish an API between LARA and the Java HLS module
 * 
 * @author Tiago
 *
 */
public class HighLevelSynthesisAPI {
    /**
     * Applies all generic strategies to a given function
     * 
     * @param cxxFun  the function to analyze
     * @param options JSON with HLS options
     */
    public static void applyGenericStrategies(CxxFunction cxxFun, String options) {
	ClavaLog.info("HLS API: applying all generic strategies to function " + cxxFun.getNameImpl());
	ClavaHLS hls = createHLS(cxxFun);
	ClavaHLSOptions op = new ClavaHLSOptions();
	hls.applyGenericStrategies(op);
    }

    /**
     * Applies function inlining using the given function as the starting point
     * 
     * @param cxxFun the function to analyze
     * @param B      function inlining control parameter (defines whether the
     *               inlining should be more permissive/aggressive)
     */
    public static void applyFunctionInlining(CxxFunction cxxFun, String B) {
	ClavaLog.info("HLS API: applying function inlining to function " + cxxFun.getNameImpl());
	ClavaHLS hls = createHLS(cxxFun);
	try {
	    double b = Double.parseDouble(B);
	    if (b <= 0.0) {
		ClavaLog.warning("HLS API: parameter B of function inlining must be larger than 0.0, aborting...");
		return;
	    }
	    hls.applyFunctionInlining(b);
	} catch (NumberFormatException e) {
	    ClavaLog.warning("HLS API: unable to parse parameter B of function inlining, aborting...");
	    return;
	}
    }

    /**
     * Applies array streaming to the given function
     * 
     * @param cxxFun the function to analyze
     */
    public static void applyArrayStreaming(CxxFunction cxxFun) {
	ClavaLog.info("HLS API: applying array streaming to function " + cxxFun.getNameImpl());
	ClavaHLS hls = createHLS(cxxFun);
	hls.applyArrayStreaming();
    }

    /**
     * Applies the loop strategies to a given function
     * 
     * @param cxxFun the function to analyze
     */
    public static void applyLoopStrategies(CxxFunction cxxFun, String P) {
	ClavaLog.info("HLS API: applying loop strategies to function " + cxxFun.getNameImpl());
	ClavaHLS hls = createHLS(cxxFun);

	try {
	    int p = Integer.parseInt(P);
	    if (p <= 0) {
		ClavaLog.warning("HLS API: provided partitioning factor " + p + " is smaller than 1, aborting...");
		return;
	    }
	    hls.applyLoopStrategies(p);
	} catch (NumberFormatException e) {
	    ClavaLog.warning("HLS API: unable to parse partitioning factor, aborting...");
	}
    }

    /**
     * Applies the load/stores strategy to a given function
     * 
     * @param cxxFun the function to analyze
     * @param N      the load/stores parameter (positive integer)
     */
    public static void applyLoadStoresStrategy(CxxFunction cxxFun, String N) {
	ClavaLog.info("HLS API: applying load/stores strategy to function " + cxxFun.getNameImpl());
	ClavaHLS hls = createHLS(cxxFun);
	try {
	    int n = Integer.parseInt(N);
	    if (n <= 0) {
		ClavaLog.warning("HLS API: provided load/stores value " + n + " is smaller than 1, aborting...");
		return;
	    }
	    hls.applyLoadStoresStrategy(n);
	} catch (NumberFormatException e) {
	    ClavaLog.warning("HLS API: unable to parse load/stores parameter, aborting...");
	}
    }

    public static boolean canBeInstrumented(CxxFunction cxxFun) {
	ClavaLog.info("HLS API: applying trace verification to a function");
	ClavaHLS hls = createHLS(cxxFun);
	boolean can = hls.canBeInstrumented();
	CxxWeaver.getCxxWeaver().getScriptEngine().toJs(can);
	return can;
    }

    /**
     * Creates a ClavaHLS instance for a given function
     * 
     * @param cxxFun the function to use on HLS analysis
     * @return
     */
    private static ClavaHLS createHLS(CxxFunction cxxFun) {
	FunctionDecl fun = cxxFun.getNode();
	DataFlowGraph dfg = new DataFlowGraph(fun);
	File fld = CxxWeaver.getCxxWeaver().getWeavingFolder();
	ClavaHLS hls = new ClavaHLS(dfg, fld);
	return hls;
    }
}
