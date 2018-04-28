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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import pt.up.fe.specs.util.collections.MultiMap;
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

    public static long parseLong(LineStream lines) {
        return Long.parseLong(lines.nextLine());
    }

    /*
    public static <T extends Enum<T> & StringProvider> T enumFromInt(EnumHelper<T> helper, T defaultValue,
            LineStream lines) {
    
        int index = parseInt(lines);
    
        if (index >= helper.getSize()) {
            return defaultValue;
        }
    
        return helper.valueOf(index);
    }
    */

    public static <T extends Enum<T> & StringProvider> T enumFromInt(EnumHelper<T> helper,
            LineStream lines) {

        return helper.valueOf(parseInt(lines));
    }

    public static <T extends Enum<T> & StringProvider> T enumFromName(EnumHelper<T> helper,
            LineStream lines) {

        return helper.fromName(lines.nextLine());
    }

    public static <T extends Enum<T> & StringProvider> T enumFromValue(EnumHelper<T> helper,
            LineStream lines) {

        String value = lines.nextLine();
        return helper.valueOf(value);
    }

    /**
     * First line represents the number of enums to parse, one in each succeding line.
     * 
     * @param helper
     * @param lines
     * @return
     */
    public static <T extends Enum<T> & StringProvider> List<T> enumListFromName(EnumHelper<T> helper,
            LineStream lines) {

        int numEnums = parseInt(lines);
        List<T> enums = new ArrayList<>(numEnums);

        for (int i = 0; i < numEnums; i++) {
            enums.add(enumFromName(helper, lines));
        }

        return enums;
    }

    public static <K> void checkDuplicate(String id, K key, Object value, Map<K, ?> map) {
        Object currentObject = map.get(key);
        if (currentObject != null) {
            throw new RuntimeException("Duplicate value for id '" + id + "', key '" + key + "'");
        }

    }

    public static <K> void checkDuplicate(String id, K key, Set<K> set) {
        if (set.contains(key)) {
            throw new RuntimeException("Duplicate value for id '" + id + "', key '" + key + "'");
        }
    }

    public static void parseStringMap(String id, LineStream linestream, Map<String, String> stringMap) {
        String key = linestream.nextLine();
        String value = linestream.nextLine();

        GeneralParsers.checkDuplicate(id, key, value, stringMap);
        stringMap.put(key, value);
    }

    /**
     * Overload that sets 'checkDuplicate' to true.
     * 
     * @param id
     * @param linestream
     * @param stringSet
     */
    public static void parseStringSet(String id, LineStream linestream, Set<String> stringSet) {
        parseStringSet(id, linestream, stringSet, true);
    }

    public static void parseStringSet(String id, LineStream linestream, Set<String> stringSet, boolean checkDuplicate) {
        String key = linestream.nextLine();

        if (checkDuplicate) {
            GeneralParsers.checkDuplicate(id, key, stringSet);
        }

        stringSet.add(key);
    }

    /**
     * Overload which uses the second line as value.
     * 
     * @param lines
     * @param map
     */
    public static void parseMultiMap(LineStream lines, MultiMap<String, String> map) {
        parseMultiMap(lines, map, string -> string);
    }

    /**
     * Reads two lines from LineStream, the first is the key, the second is the value. Applies the given decoder to the
     * value.
     * 
     * @param lines
     * @param map
     * @param decoder
     */
    public static <T> void parseMultiMap(LineStream lines, MultiMap<String, T> map, Function<String, T> decoder) {
        String key = lines.nextLine();
        map.put(key, decoder.apply(lines.nextLine()));
    }

    /**
     * First line represents the number of elements of the list.
     * 
     * @param lines
     * @return
     */
    public static List<String> parseStringList(LineStream lines) {
        int numElements = parseInt(lines);

        List<String> strings = new ArrayList<>(numElements);
        for (int i = 0; i < numElements; i++) {
            strings.add(lines.nextLine());
        }

        return strings;
    }

    /*
    public static List<AttributeData> parseAttributes(LineStream lines) {
        int numAttrs = parseInt(lines);
    
        List<AttributeData> attributes = new ArrayList<>(numAttrs);
        for (int i = 0; i < numAttrs; i++) {
            AttributeKind kind = GeneralParsers.enumFromName(AttributeKind.getHelper(), lines);
            boolean isImplicit = parseOneOrZero(lines);
            boolean isInherited = parseOneOrZero(lines);
            boolean isLateParsed = parseOneOrZero(lines);
            boolean isPackExpansion = parseOneOrZero(lines);
    
            // attributes.add(new AttributeData(kind, isImplicit, isInherited, isLateParsed, isPackExpansion));
        }
    
        return attributes;
    }
    */

}
