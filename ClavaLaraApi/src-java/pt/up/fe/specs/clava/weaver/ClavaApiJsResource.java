
/**
 * Copyright 2023 SPeCS.
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

package pt.up.fe.specs.clava.weaver;

import org.lara.interpreter.weaver.utils.LaraResourceProvider;

/**
 * This file has been automatically generated.
 * 
 * @author Joao Bispo, Luis Sousa
 *
 */
public enum ClavaApiJsResource implements LaraResourceProvider {

    JOINPOINTS_JS("Joinpoints.js"),
    CLAVA_JS("clava/Clava.js"),
    CLAVACODE_JS("clava/ClavaCode.js"),
    CLAVAJAVATYPES_JS("clava/ClavaJavaTypes.js"),
    CLAVAJOINPOINTS_JS("clava/ClavaJoinPoints.js"),
    CLAVATYPE_JS("clava/ClavaType.js"),
    FORMAT_JS("clava/Format.js"),
    MATHEXTRA_JS("clava/MathExtra.js"),
    ANALYSER_JS("clava/analysis/Analyser.js"),
    ANALYSERRESULT_JS("clava/analysis/AnalyserResult.js"),
    CHECKBASEDANALYSER_JS("clava/analysis/CheckBasedAnalyser.js"),
    CHECKRESULT_JS("clava/analysis/CheckResult.js"),
    CHECKER_JS("clava/analysis/Checker.js"),
    FIX_JS("clava/analysis/Fix.js"),
    MESSAGEGENERATOR_JS("clava/analysis/MessageGenerator.js"),
    RESULTFORMATMANAGER_JS("clava/analysis/ResultFormatManager.js"),
    RESULTLIST_JS("clava/analysis/ResultList.js"),
    STRCPYCHECKER_JS("clava/analysis/checkers/StrcpyChecker.js"),
    HDF5_JS("clava/hdf5/Hdf5.js"),
    HLSANALYSIS_JS("clava/hls/HLSAnalysis.js"),
    MATHANALYSIS_JS("clava/hls/MathAnalysis.js"),
    MATHHINFO_JS("clava/hls/MathHInfo.js"),
    TRACEINSTRUMENTATION_JS("clava/hls/TraceInstrumentation.js"),
    CLAVADATASTORE_JS("clava/util/ClavaDataStore.js"),
    CORE_JS("core.js"),
    ENERGY_JS("lara/code/Energy.js"),
    LOGGER_JS("lara/code/Logger.js"),
    TIMER_JS("lara/code/Timer.js");

    private final String resource;

    private static final String WEAVER_PACKAGE = "clava/";

    /**
     * @param resource
     */
    private ClavaApiJsResource (String resource) {
      this.resource = WEAVER_PACKAGE + getSeparatorChar() + resource;
    }

    /* (non-Javadoc)
     * @see org.suikasoft.SharedLibrary.Interfaces.ResourceProvider#getResource()
     */
    @Override
    public String getOriginalResource() {
        return resource;
    }

}
