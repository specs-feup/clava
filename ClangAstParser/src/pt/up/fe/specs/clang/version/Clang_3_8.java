/**
 * Copyright 2018 SPeCS.
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

package pt.up.fe.specs.clang.version;

import pt.up.fe.specs.clang.streamparserv2.ClassesService;
import pt.up.fe.specs.clang.streamparserv2.CustomClassnameMapper;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.ClavaData;
import pt.up.fe.specs.clava.ast.attr.AlignedExprAttr;
import pt.up.fe.specs.clava.ast.attr.AlignedTypeAttr;
import pt.up.fe.specs.clava.ast.attr.data.AlignedExprAttrData;
import pt.up.fe.specs.clava.ast.attr.data.AlignedTypeAttrData;
import pt.up.fe.specs.util.exceptions.CaseNotDefinedException;

public class Clang_3_8 {

    private static final CustomClassnameMapper CLASSNAME_MAPPER_3_8;
    static {
        CLASSNAME_MAPPER_3_8 = new CustomClassnameMapper();
        CLASSNAME_MAPPER_3_8.add("AlignedAttr", Clang_3_8::alignedAttrMapper);
    }

    private static final ClassesService CLASSES_SERVICE_3_8 = new ClassesService(CLASSNAME_MAPPER_3_8);

    public static ClassesService getClassesService() {
        return CLASSES_SERVICE_3_8;
    }

    private static Class<? extends ClavaNode> alignedAttrMapper(ClavaData data) {
        if (data instanceof AlignedExprAttrData) {
            return AlignedExprAttr.class;
        }

        if (data instanceof AlignedTypeAttrData) {
            return AlignedTypeAttr.class;
        }

        throw new CaseNotDefinedException(data.getClass());
    }
}
