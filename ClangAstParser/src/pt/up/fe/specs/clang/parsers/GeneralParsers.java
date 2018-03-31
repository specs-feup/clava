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

package pt.up.fe.specs.clang.parsers;

import pt.up.fe.specs.util.enums.EnumHelper;
import pt.up.fe.specs.util.providers.StringProvider;
import pt.up.fe.specs.util.utilities.LineStream;

/**
 *
 * Utility methods for parsing general-purpose information (e.g., a boolean, an enum) from a LineStream.
 *
 * @author JoaoBispo
 *
 */
public class GeneralParsers {

    public static boolean parseOneOrZero(String aBoolean) {
        if (aBoolean.equals("1")) {
            return true;
        }

        if (aBoolean.equals("0")) {
            return false;
        }

        throw new RuntimeException("Unexpected value: " + aBoolean);
    }

    public static boolean parseOneOrZero(LineStream lines) {
        return parseOneOrZero(lines.nextLine());
    }

    public static int parseInt(LineStream lines) {
        return Integer.parseInt(lines.nextLine());
    }

    public static <T extends Enum<T> & StringProvider> T enumFromInt(EnumHelper<T> helper,
            LineStream lines) {

        return helper.valueOf(parseInt(lines));
    }

}
