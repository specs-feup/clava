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

package pt.up.fe.specs.clang.parsers.clavadata;

import pt.up.fe.specs.clang.parsers.ClavaDataParser;
import pt.up.fe.specs.clang.parsers.GeneralParsers;
import pt.up.fe.specs.clava.ast.ClavaData;
import pt.up.fe.specs.clava.ast.lang.AttributeData;
import pt.up.fe.specs.clava.ast.lang.AttributeKind;
import pt.up.fe.specs.util.utilities.LineStream;

public class AttrDataParser {

    public static AttributeData parseAttributeData(LineStream lines) {

        ClavaData clavaData = ClavaDataParser.parseClavaData(lines);

        AttributeKind kind = GeneralParsers.enumFromName(AttributeKind.getHelper(), lines);
        boolean isImplicit = GeneralParsers.parseOneOrZero(lines);
        boolean isInherited = GeneralParsers.parseOneOrZero(lines);
        boolean isLateParsed = GeneralParsers.parseOneOrZero(lines);
        boolean isPackExpansion = GeneralParsers.parseOneOrZero(lines);

        return new AttributeData(kind, isImplicit, isInherited, isLateParsed, isPackExpansion, clavaData);
    }

}
