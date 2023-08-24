/**
 * Copyright 2017 SPeCS.
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

import java.util.Arrays;
import java.util.List;

import pt.up.fe.specs.clava.language.Standard;
import pt.up.fe.specs.clava.weaver.memoi.MemoiCodeGen;
import pt.up.fe.specs.clava.weaver.memoi.MemoiReport;
import pt.up.fe.specs.clava.weaver.memoi.MemoiReportsMap;
import pt.up.fe.specs.clava.weaver.memoi.MergedMemoiReport;
import pt.up.fe.specs.clava.weaver.util.ClavaPetit;
import pt.up.fe.specs.util.providers.ResourceProvider;

public class ClavaLaraApis {

    private static final List<ResourceProvider> CLAVA_LARA_API = ResourceProvider
            .getResourcesFromEnum(ClavaApiJsResource.class, LaraWeaverApiResource.class, LaraApiResource.class);

    private static final List<Class<?>> CLAVA_IMPORTABLE_CLASSES = Arrays.asList(Standard.class, OpenCLTemplates.class,
            ClavaPetit.class, MemoiReport.class, MemoiReportsMap.class, MergedMemoiReport.class,
            MemoiCodeGen.class, MathExtraApiTools.class);

    public static List<ResourceProvider> getApis() {
        return CLAVA_LARA_API;
    }

    public static List<Class<?>> getImportableClasses() {
        return CLAVA_IMPORTABLE_CLASSES;
    }

}
