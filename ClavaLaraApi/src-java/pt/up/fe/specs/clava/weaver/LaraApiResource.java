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

package pt.up.fe.specs.clava.weaver;

import org.lara.interpreter.weaver.utils.LaraResourceProvider;

/**
 * @author Joao Bispo
 *
 */
public enum LaraApiResource implements LaraResourceProvider {

    // AUTOPAR
    ADDITIONAL_CONDITIONS_CHECK("autopar/additionalConditionsCheck.lara"),
    ADD_OPENMP_DIRECTIVES("autopar/AddOpenMPDirectivesForLoop.lara"),
    ADD_PRAGMA_LOOP_INDEX("autopar/AddPragmaLoopIndex.lara"),
    AUTOPAR_UTILS("autopar/AutoParUtils.lara"),
    AUTOPAR_1("autopar/BuildPetitFileInput.lara"),
    AUTOPAR_2("autopar/checkForFunctionCalls.lara"),
    AUTOPAR_3("autopar/checkForInvalidStmts.lara"),
    AUTOPAR_4("autopar/checkForOpenMPCanonicalForm.lara"),
    AUTOPAR_4_1("autopar/CheckForSafeFunctionCall.lara"),
    AUTOPAR_5("autopar/checkvarreReduction.lara"),
    AUTOPAR_6("autopar/ExecPetitDependencyTest.lara"),
    AUTOPAR_7("autopar/FindReductionArrays.lara"),
    AUTOPAR_8("autopar/get_varTypeAccess.lara"),
    AUTOPAR_9("autopar/InlineFunctionCalls.lara"),
    AUTOPAR_9_1("autopar/LoopInductionVariables.lara"),
    AUTOPAR_10("autopar/NormalizedBinaryOp.lara"),
    AUTOPAR_10_1("autopar/OmegaConfig.lara"),
    AUTOPAR_17("autopar/Parallelize.lara"),
    AUTOPAR_11("autopar/ParallelizeLoop.lara"),
    AUTOPAR_12("autopar/RemoveNakedloops.lara"),
    AUTOPAR_12_1("autopar/RemoveOpenMPfromInnerloop.lara"),
    AUTOPAR_12_2("autopar/RunInlineFunctionCalls.lara"),
    AUTOPAR_13("autopar/SetArrayAccessOpenMPscoping.lara"),
    AUTOPAR_14("autopar/SetMemberAccessOpenMPscoping.lara"),
    AUTOPAR_15("autopar/SetVariableAccess.lara"),
    AUTOPAR_16("autopar/SetVarrefOpenMPscoping.lara"),

    // Gprofer
    GPROFER("gprofer/Gprofer.lara"),
    GPROFER_ASPECTS("gprofer/_GproferAspects.lara"),

    // Hdf5
    HDF5("hdf5/Hdf5.lara"),

    // MPI
    MPI_ACCESS_PATTERN("mpi/MpiAccessPattern.lara"),
    MPI_SCATTER_GATHER_LOOP("mpi/MpiScatterGatherLoop.lara"),
    MPI_UTILS("mpi/MpiUtils.lara"),

    // MPI Patterns
    ITERATION_VARIABLE_PATTERN("mpi/patterns/IterationVariablePattern.lara"),
    MPI_ACCESS_PATTERNS("mpi/patterns/MpiAccessPatterns.lara"),
    SCALAR_PATTERN("mpi/patterns/ScalarPattern.lara"),

    // OpenCL
    OPENCL_CALL("opencl/OpenCLCall.lara"),
    OPENCL_CALL_VARIABLES("opencl/OpenCLCallVariables.lara"),
    OPENCL_KERNEL_REPLACER("opencl/KernelReplacer.lara"),
    OPENCL_KERNEL_REPLACER_AUTO("opencl/KernelReplacerAuto.lara"),

    // Memoization
    MEMOI_PROF("memoi/MemoiProf.lara"),
    MEMOI_PROF_HELPER("memoi/_MemoiProfHelper.lara"),
    MEMOI_GEN("memoi/MemoiGen.lara"),
    MEMOI_GEN_HELPER("memoi/_MemoiGenHelper.lara"),

    // Clava utils
    CLAVA_DATA_STORE("util/ClavaDataStore.lara"),
    SINGLE_FILE("util/SingleFile.lara"),

    // Static objects

    CLAVA("Clava.lara"),
    CLAVA_ASPECTS("ClavaAspects.lara"),
    CLAVA_CODE("ClavaCode.lara"),
    CLAVA_JOIN_POINTS("ClavaJoinPoints.lara"),
    CLAVA_TYPE("ClavaType.lara");

    private final String resource;

    private static final String WEAVER_PACKAGE = "clava/";
    private static final String BASE_PACKAGE = "clava/";

    /**
     * @param resource
     */
    private LaraApiResource(String resource) {
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
