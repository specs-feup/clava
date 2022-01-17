/**
 * Copyright 2013 SuikaSoft.
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

package pt.up.fe.specs.antarex.clava;

import org.lara.interpreter.weaver.utils.LaraResourceProvider;

/**
 * @author Joao Bispo
 *
 */
public enum LaraAntarexApiResource implements LaraResourceProvider {
    TEST("_Test.lara"),

    // Examon
    EXAMON("examon/Examon.lara"),
    EXAMON_ASPECTS("examon/ExamonAspects.lara"),

    // Inline
    INLINE_ASPECTS("inline/inlineAspects.lara"),
    INLINE_FUNCS("inline/inlineFuncs.lara"),

    // LIBVC
    LIBVC("libvc/LibVC.lara"),
    LIBVC_ASPECTS("libvc/_internal/LibVCAspects.lara"),

    // mARGOt codegen
    MARGOT_CODE_GEN("margot/codegen/MargotCodeGen.lara"),
    MARGOT_CODE_GEN_ASPECTS("margot/codegen/_internal/MargotCodeGenAspects.lara"),
    MARGOT_CODE_GEN_STRINGS("margot/codegen/_internal/MargotStringsGen.lara"),

    // mARGOt config
    MARGOT_CONFIG_BLOCK("margot/config/MargotBlock.lara"),
    MARGOT_CONFIG("margot/config/MargotConfig.lara"),
    MARGOT_CONFIG_DATA_FEATURE("margot/config/MargotDataFeature.lara"),
    MARGOT_CONFIG_DOMAIN("margot/config/MargotEnergyDomain.lara"),
    MARGOT_CONFIG_KNOB("margot/config/MargotKnob.lara"),
    MARGOT_CONFIG_STATE("margot/config/MargotState.lara"),

    MARGOT_CONFIG_CUSTOM_MONITOR("margot/config/monitor/MargotCustomMonitor.lara"),
    MARGOT_CONFIG_ENERGY_MONITOR("margot/config/monitor/MargotEnergyMonitor.lara"),
    MARGOT_CONFIG_MONITOR("margot/config/monitor/MargotMonitor.lara"),
    MARGOT_CONFIG_THROUGHPUT_MONITOR("margot/config/monitor/MargotThroughputMonitor.lara"),
    MARGOT_CONFIG_TIME_MONITOR("margot/config/monitor/MargotTimeMonitor.lara"),

    // mARGOt DSE
    MARGOT_DSE_INFO("margot/dse/MargotDseInfo.lara"),
    MARGOT_DSE_METRIC("margot/dse/metric/MargotMetric.lara"),

    // Memoi
    MEMOIZATION("memoi/Memoization.lara"),
    MEMOIZATION_AUTO_ASPECTS("memoi/MemoizationAutoAspects.lara"),
    MEMOIZATION_AUTO_FUNCS("memoi/MemoizationAutoFuncs.lara"),
    MEMOIZATION_C("memoi/MemoizationC.lara"),
    MEMOIZATION_CXX("memoi/MemoizationCXX.lara"),
    MEMOIZATION_LIB_FUNCS("memoi/MemoizationLibFuncs.lara"),
    MEMOIZATION_MATH("memoi/MemoizationMath.lara"),

    // MultiVersioning
    MULTI_POINTERS("multi/MultiVersionPointers.lara"),
    MULTI_POINTERS_ASPECTS("multi/MultiVersionPointersAspects.lara"),

    // Precision
    CUSTOM_PRECISION("precision/CustomPrecision.lara"),
    CUSTOM_PRECISION_FUNC("precision/CustomPrecisionFunc.lara"),
    REW_TYPES("precision/rewTypes.lara"),
    REW_TYPES_FUNC("precision/rewTypesFunc.lara"),

    // Split
    EXTRACT_CODE_ASPECTS("split/extractCodeAspects.lara"),
    EXTRACT_CODE_FUNCS("split/extractCodeFuncs.lara"),
    SPLIT_DECLARATIONS("split/splitDeclarations.lara"),
    SPLIT_LOOP_ASPECTS("split/splitLoopAspects.lara"),
    SPLIT_LOOP_FUNCS("split/splitLoopFuncs.lara"),

    // Utils
    IDENT_REFERENCES("utils/IdentReferences.lara"),
    LOW_LEVEL_FUNCS("utils/lowLevelFuncs.lara"),
    MANGLING("utils/mangling.lara"),
    MESSAGES("utils/messages.lara"),
    SYSFILE("utils/sysfile.lara");
    // MISC_ASPECTS("utils/miscAspects.lara"),
    // MISC_FUNCS("utils/miscFuncs.lara");

    private final String resource;

    private static final String WEAVER_PACKAGE = "clava/";
    // This is the prefix that will appear in the LARA import, WEAVER_PACKAGE will be ignored
    // E.g., import antarex.Test;
    private static final String BASE_PACKAGE = "antarex/";

    /**
     * @param resource
     */
    private LaraAntarexApiResource(String resource) {
        this.resource = WEAVER_PACKAGE + getSeparatorChar() + BASE_PACKAGE + resource;
    }

    /* (non-Javadoc)
     * @see org.suikasoft.SharedLibrary.Interfaces.ResourceProvider#getResource()
     */
    @Override
    public String getOriginalResource() {
        return resource;
    }

}
