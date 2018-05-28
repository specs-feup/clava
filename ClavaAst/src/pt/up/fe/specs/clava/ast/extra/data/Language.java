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

package pt.up.fe.specs.clava.ast.extra.data;

import org.suikasoft.jOptions.DataStore.DataClass;
import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;

public class Language extends DataClass<Language> {

    /**
     * True if supports '//' comments.
     */
    public static final DataKey<Boolean> LINE_COMMENT = KeyFactory.bool("lineComment");

    /**
     * True if is a superset of C89.
     */
    public static final DataKey<Boolean> C89 = KeyFactory.bool("c89");

    /**
     * True if is a superset of C99.
     */
    public static final DataKey<Boolean> C99 = KeyFactory.bool("c99");

    /**
     * True if is a superset of C11.
     */
    public static final DataKey<Boolean> C11 = KeyFactory.bool("c11");

    /**
     * True if is a C++ variant.
     */
    public static final DataKey<Boolean> C_PLUS_PLUS = KeyFactory.bool("c++");

    /**
     * True if is a C++11 variant (or later).
     */
    public static final DataKey<Boolean> C_PLUS_PLUS_11 = KeyFactory.bool("c++11");

    /**
     * True if is a C++14 variant (or later).
     */
    public static final DataKey<Boolean> C_PLUS_PLUS_14 = KeyFactory.bool("c++14");

    /**
     * True if is a C++17 variant (or later).
     */
    public static final DataKey<Boolean> C_PLUS_PLUS_17 = KeyFactory.bool("c++17");

    /**
     * True if supports digraphs.
     */
    public static final DataKey<Boolean> HAS_DIGRAPHS = KeyFactory.bool("hasDigraphs");

    /**
     * True if includes GNU extensions.
     */
    public static final DataKey<Boolean> IS_GNU = KeyFactory.bool("isGnu");

    /**
     * True if supports hexadecimal float constants.
     */
    public static final DataKey<Boolean> HEX_FLOATS = KeyFactory.bool("hexFloats");

    /**
     * True if allows variables to be typed as int implicitly.
     */
    public static final DataKey<Boolean> IMPLICIT_INT = KeyFactory.bool("implicitInt");

    /**
     * True if is an OpenCL variant.
     */
    public static final DataKey<Boolean> OPEN_CL = KeyFactory.bool("opencl");

    /**
     * True if is an OpenCL variant.
     */
    public static final DataKey<OpenCLVersion> OPEN_CL_VERSION = KeyFactory
            .enumeration("openclVersion", OpenCLVersion.class);

    /**
     * True if supports native half type.
     */
    public static final DataKey<Boolean> NATIVE_HALF_TYPE = KeyFactory.bool("nativeHalfType");

    /**
     * True if is a CUDA unit.
     */
    public static final DataKey<Boolean> CUDA = KeyFactory.bool("cuda");

    /**
     * True if supports the 'bool' keyword.
     */
    public static final DataKey<Boolean> HAS_BOOL = KeyFactory.bool("hasBool");

    /**
     * True if supports the 'half' keyword.
     */
    public static final DataKey<Boolean> HAS_HALF = KeyFactory.bool("hasHalf");

    /**
     * True if supports the 'wchar_t' keyword.
     */
    public static final DataKey<Boolean> HAS_WCHAR = KeyFactory.bool("hasWchar");

}
