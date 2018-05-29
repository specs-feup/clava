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

    // MPI
    MPI_UTILS("mpi/MpiScatterGatherLoop.lara"),

    // OpenCL
    OPENCL_CALL("opencl/OpenCLCall.lara"),
    OPENCL_CALL_VARIABLES("opencl/OpenCLCallVariables.lara"),
    OPENCL_KERNEL_REPLACER("opencl/KernelReplacer.lara"),
    OPENCL_KERNEL_REPLACER_AUTO("opencl/KernelReplacerAuto.lara"),

    // Clava utils
    CLAVA_DATA_STORE("util/ClavaDataStore.lara"),
    SINGLE_FILE("util/SingleFile.lara"),

    // Static objects

    CLAVA("Clava.lara"),
    CLAVA_ASPECTS("ClavaAspects.lara"),
    CLAVA_CODE("ClavaCode.lara"),
    CLAVA_JOIN_POINTS("ClavaJoinPoints.lara");

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
