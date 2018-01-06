/**
 * Copyright 2016 SPeCS.
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

package pt.up.fe.specs.clang.clavaparser.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.ClavaParserUtils;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.decl.data.InitializationStyle;
import pt.up.fe.specs.clava.ast.expr.data.ValueKind;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.language.CastKind;
import pt.up.fe.specs.util.SpecsEnums;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.enums.EnumHelper;
import pt.up.fe.specs.util.parsing.arguments.ArgumentsParser;
import pt.up.fe.specs.util.parsing.arguments.Gluer;
import pt.up.fe.specs.util.providers.StringProvider;
import pt.up.fe.specs.util.stringparser.ParserResult;
import pt.up.fe.specs.util.stringparser.StringParsers;
import pt.up.fe.specs.util.utilities.StringSlice;

/**
 * TODO: Check what methods can be moved to SpecsUtils StringParsers.
 * 
 * @author JoaoBispo
 *
 */
public class ClangGenericParsers {

    private static final Pattern REGEX_WINDOWS_PATH = Pattern.compile("^[a-zA-Z]:.*");

    /**
     * Clears the StringSlice, for debugging/development purposes.
     * 
     * @param string
     * @return
     */
    public static ParserResult<String> clear(StringSlice string) {
        return new ParserResult<>(new StringSlice(""), string.toString());
    }

    public static ParserResult<String> parseParenthesis(StringSlice string) {
        return StringParsers.parseNested(string, '(', ')');
    }

    public static ParserResult<List<Type>> parseClangTypeList(StringSlice string, ClangNode node,
            Map<String, Type> typesMap) {

        ParserResult<Type> type = parseClangType(string, node, typesMap);

        return new ParserResult<>(type.getModifiedString(), Arrays.asList(type.getResult()));
    }

    public static ParserResult<Type> parseClangTypeLiteral(StringSlice string) {
        // Get type information
        ParserResult<List<String>> result = parsePrimesSeparatedByString(string, ":");

        Preconditions.checkArgument(!result.getResult().isEmpty(), "Expected at least one type");

        return new ParserResult<>(result.getModifiedString(), ClavaNodeFactory.literalType(result.getResult().get(0)));
    }

    public static ParserResult<Type> parseClangType(StringSlice string, ClangNode node,
            Map<String, Type> typesMap) {

        return parseClangType(string, node.getInfo(), node.getExtendedId(), typesMap);
    }

    public static ParserResult<Type> parseClangType(StringSlice string, ClavaNodeInfo info, String extendedId,
            Map<String, Type> typesMap) {

        // Remove type information
        // ParserResult<List<String>> result = parsePrimesSeparatedByString(string, ":");
        ParserResult<List<String>> result = dropClangType(string);

        // Return type in the table
        Type type = typesMap.get(extendedId);

        // If null, use NullType
        type = type != null ? type : ClavaNodeFactory.nullType(info);

        return new ParserResult<>(result.getModifiedString(), type);
    }

    public static ParserResult<List<String>> dropClangType(StringSlice string) {
        // Remove type information
        return parsePrimesSeparatedByString(string, ":");
    }

    public static ParserResult<Optional<Type>> parseClangTypeTry(StringSlice string, ClangNode node,
            Map<String, Type> typesMap) {

        // Check if string starts with primes
        if (!string.isEmpty() && (string.charAt(0) != '\'')) {
            return new ParserResult<>(string, Optional.empty());
        }

        ParserResult<Type> result = parseClangType(string, node, typesMap);

        return new ParserResult<>(result.getModifiedString(), Optional.of(result.getResult()));
    }

    /**
     * Receives a string starting with "'{element}'({separator}'{element}')*", returns a list with the elements, without
     * the primes.
     * 
     * <p>
     * Trims the string after processing.
     * 
     * @param string
     * @param separator
     * @return
     */
    public static ParserResult<List<String>> parsePrimesSeparatedByString(StringSlice string, String separator) {
        List<String> elements = new ArrayList<>();

        if (string.isEmpty()) {
            return new ParserResult<>(string, elements);
        }

        // Check that String starts with a '
        if (!string.startsWith("'")) {
            throw new RuntimeException("Given string does not start with quote ('):" + string);
        }

        // return new ParserResult<>(string, elements);

        // While string starts with a prime (')
        while (string.startsWith("'")) {
            // Get string between primes
            ParserResult<String> primeString = StringParsers.parseNested(string, '\'', '\'');

            // Update string
            string = primeString.getModifiedString();
            elements.add(primeString.getResult());

            // If there is not a separator, with a prime following it, return
            if (!string.startsWith(separator + "'")) {
                // Trim string
                string = string;
                return new ParserResult<>(string, elements);
            }

            // Remove separator from string
            string = string.substring(separator.length());
        }

        throw new RuntimeException("Should not arrive here, current string: '" + string + "'");
        /*
        	ParserResult<List<String>> primesResult;
        	// While the result of parsing primes is not empty
        	while (!(primesResult = parsePrimes(string)).getResult().isEmpty()) {
        	    // Update string
        	    string = primesResult.getModifiedString();
        
        	    // System.out.println("NEW STRING:" + string);
        	    // System.out.println("RESULTS:" + primesResult.getResult());
        
        	    // Add result
        	    Preconditions.checkArgument(primesResult.getResult().size() == 1, "Expected only one element");
        	    elements.add(primesResult.getResult().get(0));
        
        	    // Remove separator
        	    if (string.startsWith(separator)) {
        		string = string.substring(separator.length());
        	    }
        	    // Check that there is no more primes
        	    else {
        		Preconditions.checkArgument(!string.startsWith("'"), "Did not expect a prime here:" + string);
        	    }
        	}
        
        	return new ParserResult<>(string, elements);
        	*/
    }

    /**
     * Receives a string starting with "(line|col):{number}(:{number})? and ending with a whitespace
     * 
     * @param string
     * @return
     */
    public static ParserResult<String> parseLocation(StringSlice string) {
        String location = "";

        Optional<Integer> pathOffsetIndex = testPath(string);

        // Windows path
        if (pathOffsetIndex.isPresent()) {
            int offset = pathOffsetIndex.get();

            // Fetch the next colon, it should mark line/col specification
            int colonIndex = string.substring(offset).indexOf(':');

            if (colonIndex == -1) {
                throw new RuntimeException("Was expecting a colon:" + string.substring(offset));
            }

            // Use space as separator after colon
            int spaceIndex = string.substring(offset + colonIndex).indexOf(' ');

            if (spaceIndex == -1) {
                throw new RuntimeException("Was expecting a space after the colon:" + string.substring(offset));
            }

            location = string.substring(0, offset + colonIndex + spaceIndex).toString();
            string = string.substring(offset + colonIndex + spaceIndex);

            return new ParserResult<>(string, location);
        }

        if (string.startsWith("col") || string.startsWith("line")) {
            // Get last index
            int endIndex = string.indexOf(' ');
            if (endIndex == -1) {
                // endIndex = string.length() - 1;
                endIndex = string.length();
            }

            // Check if there are colons inside
            if (string.indexOf(':') == -1) {
                throw new RuntimeException("Expected a colon (:) in string '" + string + "'");
            }

            location = string.substring(0, endIndex).toString();

            // Update slice
            string = string.substring(endIndex);
        }

        return new ParserResult<>(string, location);
    }

    /**
     * Returns the index after any possible colons in the path.
     * 
     * @param string
     * @return
     */
    private static Optional<Integer> testPath(StringSlice string) {
        // Linux path
        if (string.startsWith("/")) {
            return Optional.of(0);
            // throw new RuntimeException("Parsing of Linux paths not done yet, current path:" + testString);
        }

        // Windows path
        if (ClangGenericParsers.REGEX_WINDOWS_PATH.matcher(string.toString()).matches()) {
            return Optional.of(2);
        }

        return Optional.empty();
    }

    /**
     * 
     * @param string
     * @return the remaining of the string in the parser
     */
    public static ParserResult<String> parseRemaining(StringSlice string) {
        String rem = string.toString();
        string = string.substring(rem.length());

        return new ParserResult<>(string, rem);
    }

    /*
    private static ParserResult<Optional<String>> parseWordTry(StringSlice string) {
        // Check if first character is an alphabetic character
        if (!string.isEmpty() && !Character.isLetter(string.charAt(0))) {
            return new ParserResult<>(string, Optional.empty());
        }
    
        ParserResult<String> result = StringParsers.parseWord(string);
    
        return new ParserResult<>(result.getModifiedString(), Optional.of(result.getResult()));
    }
    */

    /**
     * Makes sure the string has the given prefix at the beginning.
     * 
     * @param string
     * @param prefix
     * @return
     */
    public static ParserResult<Boolean> ensurePrefix(StringSlice string, String prefix) {
        // Save the string in case we need to throw an exception
        String originalString = string.toString();

        ParserResult<Boolean> result = checkStringStarts(string, prefix);

        if (result.getResult()) {
            return result;
        }

        throw new RuntimeException(
                "Expected to find the prefix '" + prefix + "' at the beginning of '" + originalString + "'");
    }

    /**
     * Makes sure the string has the given string at the beginning, separated by a whitespace, or is the complete string
     * if no whitespace is found.
     * 
     * @param string
     * @param word
     * @return
     */
    public static ParserResult<Boolean> ensureWord(StringSlice string, String word) {
        // Save the string in case we need to throw an exception
        String originalString = string.toString();

        ParserResult<Boolean> result = checkWord(string, word);

        if (result.getResult()) {
            return result;
        }

        throw new RuntimeException(
                "Expected to find the word '" + word + "' at the beginning of '" + originalString + "'");
    }

    /**
     * Checks if starts with the given string, separated by a whitespace or if there is no whitespace, until the end of
     * the string.
     * 
     * @param string
     * @param string
     * @return
     */
    public static ParserResult<Boolean> checkWord(StringSlice string, String word) {
        int endIndex = string.indexOf(' ');
        if (endIndex == -1) {
            endIndex = string.length();
        }

        boolean hasWord = string.substring(0, endIndex).equalsString(word);
        if (!hasWord) {
            return new ParserResult<>(string, false);
        }

        string = string.substring(endIndex);

        return new ParserResult<>(string, true);
    }

    /**
     * Checks if ends with the given string, separated by a whitespace or if there is no whitespace, considers the whole
     * string.
     * 
     * @param string
     * @param string
     * @return
     */
    public static ParserResult<Boolean> checkLastString(StringSlice string, String word) {
        // TODO: Using String because StringSlice.lastIndexOf is not implemented
        String workString = string.toString();
        int startIndex = workString.lastIndexOf(' ');
        if (startIndex == -1) {
            startIndex = 0;
        } else {
            startIndex = startIndex + 1;
        }

        boolean hasWord = workString.substring(startIndex, workString.length()).equals(word);
        if (!hasWord) {
            return new ParserResult<>(string, false);
        }

        string = new StringSlice(workString.substring(0, startIndex));

        return new ParserResult<>(string, true);
    }

    /**
     * Returns true if the string starts with the given prefix, removes it from parsing.
     * 
     * <p>
     * Helper method which enables case-sensitiveness by default.
     * 
     * @param string
     * @param prefix
     * @return
     */
    public static ParserResult<Boolean> checkStringStarts(StringSlice string, String prefix) {
        return checkStringStarts(string, prefix, true);
    }

    /**
     * Returns true if the string starts with the given prefix, removes it from parsing.
     * 
     * @param string
     * @param prefix
     * @param caseSensitive
     * @return
     */
    public static ParserResult<Boolean> checkStringStarts(StringSlice string, String prefix, boolean caseSensitive) {

        boolean startsWith = caseSensitive ? string.startsWith(prefix)
                : string.toString().toLowerCase().startsWith(prefix.toLowerCase());
        /*        
        if(caseSensitive) {
            string.startsWith(prefix)
        } else {
            string.toString().toLowerCase().startsWith(prefix.toLowerCase())
        }
         */
        // if (string.startsWith(prefix)) {
        if (startsWith) {
            string = string.substring(prefix.length());
            return new ParserResult<>(string, true);
        }

        return new ParserResult<>(string, false);
    }

    public static ParserResult<Boolean> ensureStringStarts(StringSlice string, String prefix) {
        ParserResult<Boolean> result = checkStringStarts(string, prefix);
        if (result.getResult()) {
            return result;
        }

        throw new RuntimeException("Expected string to start with '" + prefix + "', instead is '" + string + "'");
    }

    public static ParserResult<Boolean> checkStringEnds(StringSlice string, String suffix) {

        if (string.endsWith(suffix)) {
            string = string.substring(0, string.length() - suffix.length());
            return new ParserResult<>(string, true);
        }

        return new ParserResult<>(string, false);
    }

    public static ParserResult<Boolean> checkStringEndsStrict(StringSlice string, String suffix) {
        ParserResult<Boolean> result = checkStringEnds(string, suffix);
        if (result.getResult()) {
            return result;
        }

        throw new RuntimeException("Expected string to end with '" + suffix + "', instead is '" + string + "'");
    }

    /**
     * 
     * @param string
     * @return true if the string starts with '->', false if it starts with '.', throws an exception otherwise
     */
    public static ParserResult<Boolean> checkArrow(StringSlice string) {
        if (string.startsWith("->")) {
            string = string.substring("->".length());
            return new ParserResult<>(string, true);
        }

        if (string.startsWith(".")) {
            string = string.substring(".".length());
            return new ParserResult<>(string, false);
        }

        throw new RuntimeException("Expected string to start with either -> or .");
    }

    /**
     * Helper method which returns RVALUE as default, if not present an value kind in the string.
     * 
     * @param string
     * @return
     */
    public static ParserResult<ValueKind> parseValueKind(StringSlice string) {
        ParserResult<Optional<ValueKind>> result = parseValueKindTry(string);

        return new ParserResult<>(result.getModifiedString(), result.getResult().orElse(ValueKind.R_VALUE));
    }

    /**
     * Helper method which returns NO_INIT as default, if not present an value kind in the string.
     * 
     * @param string
     * @return
     */
    public static ParserResult<InitializationStyle> parseInitializationStyle(StringSlice string) {

        ParserResult<Optional<InitializationStyle>> result = parseInitializationStyleTry(string);

        return new ParserResult<>(result.getModifiedString(), result.getResult().orElse(InitializationStyle.NO_INIT));
    }

    /**
     * Parses a CastKind included inside brackets, e.g., <NO_OP>.
     * 
     * @param string
     * @return
     */
    public static ParserResult<CastKind> parseCastKind(StringSlice string) {

        ParserResult<String> nestedResult = StringParsers.parseNested(string, '<', '>',
                ClangGenericParsers::kindEndPredicate);

        string = nestedResult.getModifiedString();
        String castKindString = nestedResult.getResult();

        int whitespaceIndex = castKindString.indexOf(' ');
        if (whitespaceIndex != -1) {
            // Drop BasePath
            SpecsLogs.msgLib("[ParseCastKind] Discarding base path:" + castKindString.substring(whitespaceIndex));
            castKindString = castKindString.substring(0, whitespaceIndex);
        }

        CastKind castKind = CastKind.getHelper().valueOf(castKindString);

        return new ParserResult<>(string, castKind);
    }

    /**
     * When parsing '<' '>', there is a special case of '->' that should not be signaled as the end.
     * 
     * @param string
     * @param endIndex
     * @return
     */
    private static Boolean kindEndPredicate(StringSlice string, Integer endIndex) {
        // Check if previous character is a '-' and current character is a '>'
        if (endIndex != 0 && string.charAt(endIndex - 1) == '-' && string.charAt(endIndex) == '>') {
            return false;
        }

        // Normal check
        return string.charAt(endIndex) == '>';
    }

    /**
     * Parses a value kind, that can be separated by a space.
     * 
     * @param string
     * @return
     */
    public static ParserResult<Optional<ValueKind>> parseValueKindTry(StringSlice string) {
        // Get string to parse
        int endIndex = string.indexOf(' ');

        // If no space found, consider the entire string
        if (endIndex == -1) {
            endIndex = string.length();
        }

        Optional<ValueKind> valueKind = ClavaParserUtils.parseValueKind(string.substring(0, endIndex).toString());

        // Update string if value present
        if (valueKind.isPresent()) {
            // Update string
            string = string.substring(endIndex);
        }

        return new ParserResult<>(string, valueKind);

    }

    /**
     * Parses an initialization kind, that can be separated by a space.
     * 
     * @param string
     * @return
     */
    public static ParserResult<Optional<InitializationStyle>> parseInitializationStyleTry(StringSlice string) {
        // Get string to parse
        int endIndex = string.indexOf(' ');

        // If no space found, consider the entire string
        if (endIndex == -1) {
            endIndex = string.length();
        }

        Optional<InitializationStyle> valueKind = ClavaParserUtils
                .parseInitializationStyle(string.substring(0, endIndex).toString());

        // Update string if value present
        if (valueKind.isPresent()) {
            // Update string
            string = string.substring(endIndex);
        }

        return new ParserResult<>(string, valueKind);

    }

    /**
     * Starts at the end of the string, looking for a delimited by possibly nested symbols 'start' and 'end'.
     * 
     * <p>
     * Example: ("a string <another string>", '<', '>') should return "another string"
     * 
     * @param string
     * @param start
     * @param end
     * @return
     */
    public static ParserResult<String> reverseNested(StringSlice string, char start, char end) {
        Preconditions.checkArgument(!string.isEmpty());

        if (string.charAt(string.length() - 1) != end) {
            return new ParserResult<>(string, "");
        }

        // First character is termination at the end of string
        int counter = 1;
        int startIndex = string.length() - 1;
        while (counter > 0) {
            startIndex--;

            if (string.charAt(startIndex) == start) {
                counter--;
                continue;
            }

            if (string.charAt(startIndex) == end) {
                counter++;
                continue;
            }
        }

        // Return string without separators
        String result = string.substring(startIndex + 1, string.length() - 1).toString();
        // Cut string from parser
        string = string.substring(0, startIndex);

        return new ParserResult<>(string, result);
    }

    /**
     * Receives a string starting with '0x' and interprets the next characters as an hexadecimal number, until there is
     * a whitespace or the string ends.
     * 
     * @param string
     * @return an Integer representing the decoded hexadecimal, or -1 if no hex was found
     */
    public static ParserResult<Long> parseHex(StringSlice string) {
        if (!string.startsWith("0x")) {
            return new ParserResult<>(string, -1l);
        }

        ParserResult<String> result = StringParsers.parseWord(string);

        string = result.getModifiedString();
        String hexString = result.getResult();

        // CHECK: Does it ever enter here?
        if (hexString.isEmpty()) {
            return new ParserResult<>(string, 0l);
        }

        Long hexValue = Long.decode(hexString);

        return new ParserResult<>(string, hexValue);
    }

    /**
     * Receives a string ending with a 'word' starting with '0x' and interprets the next characters as an hexadecimal
     * number, until the string ends.
     * 
     * @param string
     * @return an Integer representing the decoded hexadecimal, or -1 if no hex was found
     */
    public static ParserResult<Long> reverseHex(StringSlice string) {
        int startIndex = string.lastIndexOf(' ');
        if (startIndex == -1) {
            startIndex = 0;
        }

        // StringSlice hexString = string.substring(startIndex + 1, string.length()).trim();
        StringSlice hexString = string.substring(startIndex + 1, string.length());

        if (!hexString.startsWith("0x")) {
            return new ParserResult<>(string, -1l);
        }

        // CHECK: Does it ever enter here?
        if (hexString.isEmpty()) {
            return new ParserResult<>(string.substring(0, startIndex), 0l);
        }

        Long hexValue = Long.decode(hexString.toString());

        return new ParserResult<>(string.substring(0, startIndex), hexValue);
    }

    /**
     * Receives a string and interprets the next characters as an integer number, until there is a whitespace or the
     * string ends.
     * 
     * @param string
     * @return an Integer representing the decoded hexadecimal, or -1 if no hex was found
     */
    public static ParserResult<Integer> parseInt(StringSlice string) {
        return parseDecodedWord(string, intString -> Integer.decode(intString), 0);
    }

    public static <T> ParserResult<T> parseDecodedWord(StringSlice string, Function<String, T> decoder, T emptyValue) {
        ParserResult<String> result = StringParsers.parseWord(string);

        string = result.getModifiedString();
        String value = result.getResult();

        // CHECK: Does it ever enter here?
        if (value.isEmpty()) {
            return new ParserResult<>(string, emptyValue);
        }

        T decodedValue = decoder.apply(value);

        return new ParserResult<>(string, decodedValue);
    }

    /**
     * Helper method which converts the word to upper case (enum values by convention should be uppercase).
     * 
     * @param string
     * @param enumClass
     * @return
     */
    public static <K extends Enum<K>> ParserResult<K> parseEnum(StringSlice string, Class<K> enumClass,
            K defaultValue) {

        return parseEnum(string, enumClass, defaultValue, Collections.emptyMap());
        /*
        ParserResult<Optional<K>> enumTry = checkEnum(string, enumClass, true, Collections.emptyMap());
        
        K result = enumTry.getResult().orElseThrow(() -> new RuntimeException("Could not convert string '"
            + parseWord(string) + "' to enum '" + Arrays.toString(enumClass.getEnumConstants()) + "'"));
        
        return new ParserResult<>(enumTry.getModifiedString(), result);
        */
    }

    public static <K extends Enum<K>> ParserResult<K> parseEnum(StringSlice string, Class<K> enumClass) {
        return parseEnum(string, enumClass, null, Collections.emptyMap());
    }

    // private static <K extends Enum<K>> ParserResult<K> checkEnum(StringSlice string, Class<K> enumClass, K
    // defaultValue,
    // Map<String, K> customMappings) {
    //
    // return checkEnum(string, enumClass, defaultValue, true, customMappings);
    // }

    public static <K extends Enum<K> & StringProvider> ParserResult<List<K>> parseElements(StringSlice string,
            EnumHelper<K> enumHelper) {

        List<K> parsedElements = new ArrayList<>();

        ParserResult<Optional<K>> element = checkEnum(string, enumHelper);
        while (element.getResult().isPresent()) {
            parsedElements.add(element.getResult().get());

            // Update string
            string = element.getModifiedString();

            // Parse again
            element = checkEnum(string, enumHelper);
        }

        return new ParserResult<>(string, parsedElements);
    }

    /**
     * Helper method which does not use the example value as a default value. Throws exception if the enum is not found.
     * 
     * @param string
     * @param exampleValue
     * @return
     */
    public static <K extends Enum<K> & StringProvider> ParserResult<K> parseEnum(
            StringSlice string, EnumHelper<K> enumHelper) {

        return parseEnum(string, enumHelper, null);
    }

    /**
     * 
     * @param string
     * @param exampleValue
     * @param useAsDefault
     *            if true, uses the given value as the default
     * @return
     */
    public static <K extends Enum<K> & StringProvider> ParserResult<K> parseEnum(
            StringSlice string, EnumHelper<K> enumHelper, K defaultValue) {

        // Try parsing the enum
        ParserResult<Optional<K>> result = checkEnum(string, enumHelper);

        if (result.getResult().isPresent()) {
            return new ParserResult<>(result.getModifiedString(), result.getResult().get());
        }

        // No value found, check if should use the given example value as default
        if (defaultValue != null) {
            return new ParserResult<>(string, defaultValue);
        }

        throw new RuntimeException(
                "Could not convert string '" + StringParsers.parseWord(new StringSlice(string)).getResult()
                        + "' to enum '"
                        + enumHelper.getTranslationMap() + "'");

    }

    /**
     * Helper method which accepts a default value.
     * 
     * @param string
     * @param enumHelper
     * @param defaultValue
     * @return
     */
    public static <K extends Enum<K> & StringProvider> ParserResult<K> checkEnum(
            StringSlice string, EnumHelper<K> enumHelper, K defaultValue) {

        ParserResult<Optional<K>> result = checkEnum(string, enumHelper);
        K value = result.getResult().orElse(defaultValue);
        return new ParserResult<>(result.getModifiedString(), value);
    }

    /**
     * Checks if string starts with a word representing an enumeration of the given example value.
     * 
     * @param string
     * @param exampleValue
     * @return
     */
    public static <K extends Enum<K> & StringProvider> ParserResult<Optional<K>> checkEnum(
            StringSlice string, EnumHelper<K> enumHelper) {

        // Copy StringSlice, in case the function does not found the enum
        ParserResult<String> word = StringParsers.parseWord(new StringSlice(string));

        // Check if there are any custom mappings for the word
        Optional<K> result = enumHelper.valueOfTry(word.getResult());

        // Prepare return value
        StringSlice modifiedString = result.isPresent() ? word.getModifiedString() : string;

        return new ParserResult<>(modifiedString, result);
    }

    public static <K extends Enum<K>> ParserResult<K> parseEnum(StringSlice string, Class<K> enumClass,
            K defaultValue, Map<String, K> customMappings) {

        // Copy StringSlice, in case the function does not found the enum
        ParserResult<String> word = StringParsers.parseWord(new StringSlice(string));

        // String wordToTest = word.getResult();

        // Convert to upper case if needed
        // if (toUpper) {
        // wordToTest = wordToTest.toUpperCase();
        // }

        // Check if enumeration contains element with the same name as the string
        K anEnum = SpecsEnums.valueOf(enumClass, word.getResult().toUpperCase());
        if (anEnum != null) {
            return new ParserResult<>(word.getModifiedString(), anEnum);
        }

        // Check if there are any custom mappings for the word
        K customMapping = customMappings.get(word.getResult());
        if (customMapping != null) {
            return new ParserResult<>(word.getModifiedString(), customMapping);
        }

        // Check if there is a default value
        // In this case, return the unmodified string
        if (defaultValue != null) {
            return new ParserResult<>(string, defaultValue);
        }

        throw new RuntimeException(
                "Could not convert string '" + StringParsers.parseWord(new StringSlice(string)).getResult()
                        + "' to enum '"
                        + Arrays.toString(enumClass.getEnumConstants()) + "'");

    }

    /**
     * For a string in the format <TYPENAME>::<TYPE>, returns a string with what is before '::', StringSlide with what
     * is after.
     * 
     * 
     * <p>
     * If no '::' is found, returns an empty string.
     * 
     * @param string
     * @return
     */
    public static ParserResult<String> parseTypename(StringSlice string) {
        // Test on the first word
        StringSlice testString = string.getFirstWord();

        int firstIndex = testString.indexOf(':');
        // Return empty string if could not find ':', or if it is the last character
        if (firstIndex == -1 || (firstIndex + 1) == string.length()) {
            return new ParserResult<>(string, "");
        }

        // Return if there is no '::'
        if (string.charAt(firstIndex + 1) != ':') {
            return new ParserResult<>(string, "");
        }

        String typename = string.substring(0, firstIndex).toString();
        // System.out.println("!!STRING:" + string);
        // System.out.println("!!TYPENAME:" + typename);

        // Cut up to '::', return typename
        return new ParserResult<>(string.substring(firstIndex + 2), typename);
    }

    /**
     * For a string in the format <TEMPLATE><<TYPE>>, returns a string with what is before '<', StringSlide with what is
     * inside <>.
     * 
     * 
     * <p>
     * If no '<' is found, returns an empty string.
     * 
     * @param string
     * @return
     */
    public static ParserResult<String> parseTemplate(StringSlice string) {
        StringSlice testString = string.getFirstWord();

        int openIndex = testString.indexOf('<');
        int closeIndex = testString.indexOf('>');

        // Return empty string if could not find '<' or '>' in the first word
        if (openIndex == -1 || closeIndex == -1) {
            return new ParserResult<>(string, "");
        }

        Preconditions.checkArgument(openIndex < closeIndex, "Expected < to appear before > : " + string);

        String templateName = string.substring(0, openIndex).toString();

        // Cut up what is outside of '<>', return templateName
        return new ParserResult<>(string.substring(openIndex + 1, closeIndex), templateName);
    }

    /**
     * 
     * @param string
     * @return a string with all the contents of the StringSlice
     */
    public static ParserResult<String> getString(StringSlice string) {
        String result = string.toString();

        return new ParserResult<>(string.substring(string.length()), result);
    }

    /**
     * Parses a string between primes (e.g., 'a string').
     * 
     * @param string
     * @return
     */
    public static ParserResult<String> parsePrimes(StringSlice string) {
        return StringParsers.parseNested(string, '\'', '\'');
    }

    /**
     * Parses a class name.
     * 
     * <p>
     * Class names can have template declarations (e.g., Foo<Type)>.
     * 
     * @param string
     * @return
     */
    public static ParserResult<String> parseClassName(StringSlice string) {
        List<String> parsedElements = classNameParser().parse(string.toString());
        Preconditions.checkArgument(!parsedElements.isEmpty(), "Expected to find a class name: " + string);

        return StringParsers.parseString(string, parsedElements.get(0));
        // Preconditions.checkArgument(checkStringStarts(string, parsedElements.get(0)), "Expected string to start with
        // '"parsedElements.get(0)+"':" + string);

        // return parseClangType(string, node.getInfo(), node.getExtendedId(), typesMap);
    }

    /**
     * Argument parser that delimits arguments by spaces (' '), and glues them with double quotes ('<>') and escapes
     * single characters with backslash ('\').
     * 
     * @return
     */
    private static ArgumentsParser classNameParser() {
        return new ArgumentsParser(Arrays.asList(" "), Arrays.asList(Gluer.newTag()),
                Arrays.asList(), true);
    }

}
