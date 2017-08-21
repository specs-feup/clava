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

package pt.up.fe.specs.clava.ast.decl.data;

import pt.up.fe.specs.util.enums.EnumHelper;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.providers.StringProvider;

/**
 * Storage class specifiers.
 * 
 * @author JoaoBispo
 *
 */
public enum ExceptionType implements StringProvider {
    NONE,
    UNEVALUATED("noexcept-unevaluated"),
    UNINSTANTIATED("noexcept-uninstantiated");

    private static final Lazy<EnumHelper<ExceptionType>> HELPER = EnumHelper
	    .newLazyHelper(ExceptionType.class, NONE);

    public static EnumHelper<ExceptionType> getHelper() {
	return HELPER.get();
    }

    private final String name;

    private ExceptionType() {
	this.name = name().toLowerCase();
    }

    private ExceptionType(String name) {
	this.name = name;
    }

    @Override
    public String getString() {
	return name;
    }
}
