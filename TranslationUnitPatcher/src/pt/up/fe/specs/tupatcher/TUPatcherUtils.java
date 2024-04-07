package pt.up.fe.specs.tupatcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pt.up.fe.specs.util.SpecsIo;

/**
 * Copyright 2020 SPeCS.
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

/**
 * Functions to help extract information from error messages and source code.
 * 
 * @author Pedro Galvao
 *
 */
public class TUPatcherUtils {

    private static final String PATCHED_SUFFIX = "_patched";

    public interface CharFunction {
        boolean run(char ch);
    }

    public static class StringInt {
        public String str;
        public int number;
    }

    static final CharFunction isTokenChar = (char ch) -> Character.isLetterOrDigit(ch) || ch == '_';
    static final CharFunction isNotTokenChar = (char ch) -> !isTokenChar.run(ch);

    /**
     * List of primitive types in c/c++
     */
    static final List<String> primitiveTypes = Arrays.asList("int", "char", "bool", "void", "long", "unsigned",
            "struct", "class");

    /**
     * List of operators used in c/c++
     */
    static final List<String> operators;
    static {
        String[] temp = { "+", "-", "*", "&", "/", "%", "^", "|", "~", "!",
                "=", "<", ">", "+=", "-=", "*=", "/=", "%=",
                "^=", "&=", "|=", "<<", ">>", ">>=", "<<=",
                "==", "!=", "<=", ">=", "<=>", "&&", "||",
                "++", "--", "->*", "->" };
        operators = Arrays.asList(temp);
    }

    static List<String> getPrimitiveTypes() {
        return primitiveTypes;
    }

    public static int nthIndexOf(String str, char ch, int n) {
        int result = -1;
        for (int i = 0; i < n; i++) {
            result = str.indexOf(ch, result + 1);
        }
        return result;
    }

    public static int nthIndexOf(String str, String substr, int n) {
        int result = 0;
        for (int i = 0; i < n; i++) {
            result = str.indexOf(substr, result + 1);
        }
        return result;
    }

    /**
     * Number of occurrences of a char in a string
     */
    public static int countChar(char ch, String str) {
        int counter = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == ch)
                counter++;
        }
        return counter;
    }

    /**
     * Number of column of source code location
     * 
     * @param location
     *            - location in source code in the format <filepath>:<line-number>:<column-number>
     */
    public static int locationColumn(String location) {
        int index2 = location.lastIndexOf(':');
        return Integer.parseInt(location.substring(index2 + 1));
    }

    /**
     * Number of the line of source code location
     * 
     * @param location
     *            - location in source code in the format <filepath>:<line-number>:<column-number>
     */
    public static int locationLine(String location) {
        int index2 = location.lastIndexOf(':');
        int index1 = location.lastIndexOf(':', index2 - 1);
        return Integer.parseInt(location.substring(index1 + 1, index2));
    }

    /**
     * Filepath in source code location
     * 
     * @param location
     *            - location in source code in the format <filepath>:<line-number>:<column-number>
     */
    public static String locationFilepath(String location) {
        int index2 = location.lastIndexOf(':');
        int index1 = location.lastIndexOf(':', index2 - 1);
        return location.substring(0, index1);
    }

    /**
     * For a given location in the source code find the index of the character in this location
     * 
     * @param location
     *            - location in source code in the format <filepath>:<line-number>:<column-number>
     */
    public static int locationIndex(String location, String source) {
        int column = locationColumn(location);
        int line = locationLine(location);
        return nthIndexOf(source, '\n', line - 1) + column - 1;
    }

    /**
     * Read source code while the condition is true
     * <p>
     * 
     * @param forward
     *            : true to read forward, false to read backwards.
     * @return String with the code read and int indicating position in source where it stopped reading.
     *
     */
    public static StringInt readWhile(String source, int startIndex, CharFunction condition, boolean forward) {
        int n = startIndex;
        StringBuilder token = new StringBuilder();
        StringInt result = new StringInt();
        if (n >= source.length() || n < 0) {
            return result;
        }
        char ch = source.charAt(n);
        while (condition.run(ch)) {
            token.append(ch);
            if (forward) {
                n++;
            } else {
                if (ch == ']') {
                    while (ch != '[') {
                        n--;
                        ch = source.charAt(n);
                    }
                }
                n--;
            }
            if (n >= source.length() || n < 0)
                break;
            ch = source.charAt(n);
        }
        result.str = token.toString();
        result.number = n;
        return result;
    }

    /**
     * Get the token in a given location of source code.
     * 
     * @param location
     *            - location in source code in the format <filepath>:<line-number>:<column-number>
     */
    public static String getTokenFromLocation(String location) {
        String filepath = locationFilepath(location);
        String source = removeComments(SpecsIo.read(filepath));
        StringInt result;
        int n = locationIndex(location, source);
        result = readWhile(source, n, isNotTokenChar, true);
        result = readWhile(source, result.number, isTokenChar, true);
        return result.str;

    }

    /**
     * Same as the function getTokenFromLocation, but assuming that the location indicates some character after the
     * beginning of the token.
     * 
     * @param location
     *            - location in source code in the format <filepath>:<line-number>:<column-number>
     */
    public static String getTokenBeforeLocation(String location) {
        String filepath = locationFilepath(location);
        String source = removeComments(SpecsIo.read(filepath));
        int n = locationIndex(location, source);
        StringInt result = readWhile(source, n, isNotTokenChar, false);
        result = readWhile(source, result.number, isTokenChar, false);
        result = readWhile(source, result.number + 1, isTokenChar, true);
        return result.str;

    }

    /**
     * Check if there is a function call in the given location.
     * 
     * @param location
     *            - location in source code in the format <filepath>:<line-number>:<column-number>
     */
    public static boolean isFunctionCall(String location) {
        String filepath = locationFilepath(location);
        String source = removeComments(SpecsIo.read(filepath));
        int n = locationIndex(location, source);
        StringInt result = readWhile(source, n, isNotTokenChar, true);
        result = readWhile(source, result.number, (char ch) -> (Character.isLetterOrDigit(ch) || ch == '_' || ch == ' ' || ch == ':'), true);
        char ch = source.charAt(result.number);
        return ch == '(';
    }

    /**
     * Get number of arguments in a function call.
     * 
     * @param location
     *            - location in source code in the format <filepath>:<line-number>:<column-number>
     */
    public static int getNumArguments(String location) {
        String filepath = locationFilepath(location);
        String source = removeComments(SpecsIo.read(filepath));
        int n = locationIndex(location, source);
        StringInt si = readWhile(source, n, (char c) -> c != '(', false);
        si = readWhile(source, si.number, (char c) -> c != ')', true);
        return si.str.split(",").length;
    }

    /**
     * Get names of variables used as arguments in a function call.
     *
     * @param location - location in source code in the format <filepath>:<line-number>:<column-number>
     */
    public static List<String> getArguments(String location) {
        String filepath = locationFilepath(location);
        String source = removeComments(SpecsIo.read(filepath));
        int n = locationIndex(location, source);
        StringInt si = readWhile(source, n, (char c) -> c != '(', false);
        si = readWhile(source, si.number, (char c) -> c != ')', true);
        return new ArrayList<>(
                Arrays.asList(si.str.replace("(", "").replace(" ", "").split(",")));
    }

    /**
     * Find the type name in a variable declaration.
     * 
     * @param location
     *            - location in source code in the format <filepath>:<line-number>:<column-number>
     * @return String with type name or empty string if there is no declaration in the location.
     */
    public static String getTypeFromDeclaration(String location) {
        String filepath = locationFilepath(location);
        String source = removeComments(SpecsIo.read(filepath));
        int n = locationIndex(location, source);
        StringInt si = readWhile(source, n, (char c) -> !(c == ';' || c == '{'), false);
        si = readWhile(source, si.number, isNotTokenChar, true);
        if (si.str.matches("^[^a-zA-Z]*[&\\*)]+.*")) {
            // no declaration in this location
            return "";
        }
        si = readWhile(source, si.number, isTokenChar, true);
        return si.str;
    }

    /**
     * Find struct name in a declaration
     * <p>
     * This function should be used when a variable is declared in the following format (or something similar): struct
     * foo bar = {0, 1, 2}; This function only works if the initialization uses curly brackets.
     * 
     * @param location
     *            - location in source code in the format <filepath>:<line-number>:<column-number>
     */
    public static String getTypeFromStructDeclaration(String location) {
        String filepath = locationFilepath(location);
        String source = removeComments(SpecsIo.read(filepath));
        int n = locationIndex(location, source);
        StringInt si = readWhile(source, n, (char c) -> c != '}' && c != ';', true);
        if (source.charAt(si.number) == ';') {
            return "";
        }
        si = readWhile(source, si.number - 1, (char c) -> c != '{', false);
        si = readWhile(source, si.number - 1, (char c) -> !(c == ';' || c == '{'), false);
        si = readWhile(source, si.number + 1, isNotTokenChar, true);
        si = readWhile(source, si.number, isTokenChar, true);
        if (si.str.equals("struct")) {
            si = readWhile(source, si.number, isNotTokenChar, true);
            si = readWhile(source, si.number, isTokenChar, true);
        }
        return si.str;
    }

    /**
     * Extract substrings between parenthesis from error messages
     */
    public static List<String> extractFromParenthesis(String message) {
        List<String> result = new ArrayList<>();
        int index1 = nthIndexOf(message, '(', 1);
        int index2 = nthIndexOf(message, ')', 2);
        int n = 0;
        while (index1 > 0) {
            result.add(message.substring(index1 + 1, index2));
            n += 2;
            index1 = nthIndexOf(message, '(', n + 1);
            index2 = nthIndexOf(message, ')', n + 2);
        }
        return result;
    }

    /**
     * Extract substrings between single quotes from error messages
     */
    public static List<String> extractFromSingleQuotes(String message) {
        List<String> result = new ArrayList<>();
        message = " " + message;
        int index1 = nthIndexOf(message, '\'', 1);
        int index2 = nthIndexOf(message, '\'', 2);
        int n = 0;
        while (index1 > 0) {
            result.add(message.substring(index1 + 1, index2));
            n += 2;
            index1 = nthIndexOf(message, '\'', n + 1);
            index2 = nthIndexOf(message, '\'', n + 2);
        }
        return result;
    }

    /**
     * Extract type names from error messages
     */
    public static List<String> getTypesFromMessage(String message) {
        List<String> types = extractFromSingleQuotes(message);
        if (message.contains("did you mean")) {
            types.remove(types.size() - 1);
        }
        if (message.contains("redefinition of")) {
            types.remove(0);
        }
        String toTypeName;
        String fromTypeName;
        if (types.size() == 2) {
            toTypeName = types.get(0);
            fromTypeName = types.get(1);
        } else if (types.size() == 4) {
            toTypeName = types.get(0);
            fromTypeName = types.get(2);
        } else {
            if (nthIndexOf(message, "'", 3) > message.indexOf("aka")) {
                toTypeName = types.get(0);
                fromTypeName = types.get(2);
            } else {
                toTypeName = types.get(0);
                fromTypeName = types.get(1);
            }
        }
        List<String> result = new ArrayList<>();
        result.add(toTypeName);
        result.add(fromTypeName);
        return result;
    }

    /**
     * Extract "aka" from error messages (if there is any)
     * <p>
     * Example: "member reference base type 'TYPE_PATCH_1872' (aka 'int') is not a structure or union" -> "int" When no
     * "aka" is found for the type, it returns an empty string. The result is always a List with size=2.
     */
    public static List<String> getAkaFromMessage(String message) {
        List<String> result = new ArrayList<>();
        int indexAka1 = message.indexOf("aka");
        int indexAka2 = message.indexOf("aka", indexAka1 + 1);
        if (indexAka1 < 0) {
            result.add("");
            result.add("");
        } else {
            if (indexAka2 > 0) {
                String aka1 = extractFromSingleQuotes(message.substring(indexAka1)).get(0);
                String aka2 = extractFromSingleQuotes(message.substring(indexAka2)).get(0);
                result.add(aka1);
                result.add(aka2);
            } else {
                if (nthIndexOf(message, '\'', 3) < indexAka1) {
                    result.add("");
                    String aka = extractFromSingleQuotes(message.substring(indexAka1)).get(0);
                    result.add(aka);
                } else {
                    String aka = extractFromSingleQuotes(message.substring(indexAka1)).get(0);
                    result.add(aka);
                    result.add("");
                }
            }
        }
        return result;
    }

    /**
     * Remove brackets and substitute with *
     * <p>
     * Example: char[3] -> char*
     */
    public static String removeBracketsFromType(String typeName) {
        if (typeName.contains("[")) {
            String fix = typeName.substring(0, typeName.indexOf('['));
            fix += "*";
            typeName = fix;
        }
        return typeName;
    }

    /**
     * Takes as argument a type with qualifiers and returns the type name without the qualifiers.
     * <p>
     * Example: const foo * -> foo
     * 
     * @param qualtype
     *            - type name with qualifiers
     * @return type name without qualifiers
     */
    public static String getTypeName(String qualtype) {
        return removeBracketsFromType(qualtype).replace("struct ", "").replace("class ", "").replace("*", "")
                .replace("&", "").replace("const ", "").replace("static ", "").replace(" ", "").replace("(void)", "")
                .replace("()", "");
    }

    /**
     * Takes as argument a fragment of source code and detects which operator is found in the first characters.
     */
    public static String extractOperator(String source) {
        if (source.length() >= 3 && operators.contains(source.substring(0, 3))) {
            return source.substring(0, 3);
        } else if (source.length() >= 2 && operators.contains(source.substring(0, 2))) {
            return source.substring(0, 2);
        } else if (!source.isEmpty() && operators.contains(source.substring(0, 1))) {
            return source.substring(0, 1);
        } else {
            throw new RuntimeException("Could not identify operator: " + source);
        }
    }

    /**
     * Identify if an arrow or dot is used after the identifier in the location.
     * 
     * @param location
     *            - location in source code in the format <filepath>:<line-number>:<column-number>
     */
    public static boolean hasArrowOrDot(String location) {
        String filepath = locationFilepath(location);
        String source = removeComments(SpecsIo.read(filepath));
        int n = locationIndex(location, source);
        char ch = source.charAt(n);
        if (!(Character.isLetterOrDigit(ch) || ch == '_')) {
            n++;
            ch = source.charAt(n);
        }
        StringInt si = readWhile(source, n, isTokenChar, true);
        si.number++;
        ch = source.charAt(si.number);
        return ch == '.' || (ch == '-' && source.charAt(n + 1) == '>');
    }

    /**
     * Identify if the token in a given location in source code has an operator after it.
     * <p>
     * This function is useful to check if the identifier is the name of a variable. Example: foo += bar;
     * 
     * @param location
     *            - location in source code in the format <filepath>:<line-number>:<column-number>
     */
    public static boolean usingOperator(String location) {
        String filepath = locationFilepath(location);
        String source = removeComments(SpecsIo.read(filepath));
        int n = locationIndex(location, source);
        char ch = source.charAt(n);
        if (!(Character.isLetterOrDigit(ch) || ch == '_')) {
            n++;
        }
        StringInt si = readWhile(source, n, isTokenChar, true);
        si = readWhile(source, si.number, (char c) -> c == ' ', true);
        ch = source.charAt(si.number);
        String op = String.valueOf(ch);

        if (operators.contains(op) && ch != '*' && ch != '&') {
            return true;
        } else return operators.contains(op + source.charAt(n + 2));
    }

    /**
     * Identify if the token in a given location in source code is used to cast a variable.
     * <p>
     * This function is useful to check if the identifier is the name of a type (not a variable or function). Example: x
     * = (Foo *)y;
     * 
     * @param location
     *            - location in source code in the format <filepath>:<line-number>:<column-number>
     */
    public static boolean isCast(String location) {
        String filepath = locationFilepath(location);
        String source = removeComments(SpecsIo.read(filepath));
        int n = locationIndex(location, source);
        char ch = source.charAt(n);
        if (!(Character.isLetterOrDigit(ch) || ch == '_' || ch == ' ' || ch == '(')) {
            return false;
        }
        StringInt result = readWhile(source, n, (char c) -> c != '(', false);
        if (result.str.contains(")")) {
            return false;
        }
        result = readWhile(source, result.number + 1, (char c) -> c != ')', true);
        if (result.str.matches("^.*[&\\*]+[^a-zA-Z0-9]*")) {
            // if there is char '*' or '&' before the char ')'
            return true;
        } else if (result.str.matches("^.*[a-zA-Z0-9]+[^a-zA-Z0-9]+[a-zA-Z0-9]+.*")) {
            // operation with a variable
            return false;
        } else {
            result = readWhile(source, result.number + 1, (char c) -> c == ' ', true);
            result = readWhile(source, result.number, isNotTokenChar, true);
            return result.str.replace(")", "").isEmpty();
        }

    }

    /**
     * Check if the identifier in the given location is a variable.
     * 
     * @param location
     * @return true if it is possible to assure that the identifier is a variable, false otherwise.
     */
    public static boolean isVariable(String location) {
        if (hasArrowOrDot(location)) {
            System.out.println("arrow");
            return true;
        } else if (usingOperator(location)) {
            System.out.println("op");
            return true;
        } else {
            return false;
        }
    }

    public static boolean isPrimitiveType(String type) {
        return primitiveTypes.contains(getTypeName(type));
    }

    /**
     * Remove all line and block comments in source code.
     * 
     * @param source
     * @return
     */
    public static String removeComments(String source) {
        String[] lines = source.split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains("//")) {
                lines[i] = lines[i].substring(0, lines[i].indexOf("//"));
            }
        }
        source = String.join("\n", lines);
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < source.length(); i++) {
            char ch = source.charAt(i);
            if (ch == '/') {
                i++;
                result.append(' ');
                ch = source.charAt(i);
                if (ch == '*') {
                    i++;
                    result.append(' ');
                    while (!(source.charAt(i) == '*' && source.charAt(i + 1) == '/')) {
                        i++;
                        if (source.charAt(i) == '\n')
                            result.append('\n');
                        else
                            result.append(' ');
                    }
                }
                i++;
                result.append(" ");
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }

    public static String getPatchedFilename(String originalFilename) {
        var extension = SpecsIo.getExtension(originalFilename);
        var name = SpecsIo.removeExtension(originalFilename);

        return name + PATCHED_SUFFIX + "." + extension;
    }

    public static String getPatchedHeaderFilename(String originalFilename) {
        var name = SpecsIo.removeExtension(originalFilename);

        return name + PATCHED_SUFFIX + ".h";
    }
}
