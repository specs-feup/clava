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

package pt.up.fe.specs.clava.ast.attr.data;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;

import pt.up.fe.specs.clava.ast.ClavaNodeI;
import pt.up.fe.specs.clava.ast.attr.enums.AttributeKind;

public interface AttributeI extends ClavaNodeI {

    static DataKey<AttributeKind> KIND = KeyFactory.enumeration("attributeKind", AttributeKind.class);

    static DataKey<Boolean> IS_IMPLICIT = KeyFactory.bool("isImplicit");

    static DataKey<Boolean> IS_INHERITED = KeyFactory.bool("isInherited");

    static DataKey<Boolean> IS_LATE_PARSED = KeyFactory.bool("isLateParsed");

    static DataKey<Boolean> IS_PACK_EXPANSION = KeyFactory.bool("isPackExpansion");

}
