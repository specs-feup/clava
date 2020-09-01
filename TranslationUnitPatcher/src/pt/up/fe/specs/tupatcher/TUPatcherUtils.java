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
 * 
 * @author Pedro Galvao
 *
 */
public class TUPatcherUtils {
    interface CharFunction {
        boolean run(char ch);
    }

    static class StringInt {
        public String str;
        public int number;
    }

    static CharFunction isTokenChar = (char ch) -> {
        return Character.isLetterOrDigit(ch) || ch == '_';
    };
    static CharFunction isNotTokenChar = (char ch) -> {
        return !isTokenChar.run(ch);
    };

    /**
     * List of primitive types in c/c++
     */
    static final List<String> primitiveTypes = Arrays.asList("int", "char", "bool", "void", "long", "unsigned",
            "struct", "class");

    /**
     * List of operators used in c/c++
     */
    static List<String> operators;
    static {
        String[] temp = { "+", "-", "/", "%", "^", "|", "~", "!",
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
    public static int countChar(char ch, String str) {
        int counter = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i)==ch)
            counter++;
        }
        return counter;
    }

    public static int locationColumn(String location) {
        int index2 = location.lastIndexOf(':');
        return Integer.parseInt(location.substring(index2 + 1));
    }

    public static int locationLine(String location) {
        int index2 = location.lastIndexOf(':');
        int index1 = location.lastIndexOf(':', index2 - 1);
        return Integer.parseInt(location.substring(index1 + 1, index2));
    }

    public static String locationFilepath(String location) {
        int index2 = location.lastIndexOf(':');
        int index1 = location.lastIndexOf(':', index2 - 1);
        return location.substring(0, index1);
    }

    /**
     * For a given location in the source code find the index of the character in this location
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
     * @return String with the code read and int indicating position in source where it stopped reading.
     *
     */
    public static StringInt readWhile(String source, int startIndex, CharFunction condition, boolean forward) {
        int n = startIndex;
        String token = "";
        StringInt result = new StringInt();
        if (n >= source.length() || n < 0) {
            return result;
        }
        char ch = source.charAt(n);
        while (condition.run(ch)) {
            token += ch;
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
        result.str = token;
        result.number = n;
        return result;
    }

    /**
     * Get the token in a given location of source code.
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
     */
    public static boolean isFunctionCall(String location) {
        String filepath = locationFilepath(location);
        String source = removeComments(SpecsIo.read(filepath));
        int n = locationIndex(location, source);
        StringInt result = readWhile(source, n, isNotTokenChar, true);
        result = readWhile(source, result.number, (char ch) -> {
            return (Character.isLetterOrDigit(ch) || ch == '_' || ch == ' ' || ch == ':');
        }, true);
        char ch = source.charAt(result.number);
        if (ch == '(') {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Get number of arguments in a function call.
     */
    public static int getNumArguments(String location) {
        String filepath = locationFilepath(location);
        String source = removeComments(SpecsIo.read(filepath));
        int n = locationIndex(location, source);
        StringInt si = readWhile(source, n, (char c) -> {
            return c != '(';
        }, false);
        si = readWhile(source, si.number, (char c) -> {
            return c != ')';
        }, true);
        int numArgs = si.str.split(",").length;
        return numArgs;
    }

    /**
     * Get names of variables used as arguments in a function call.
     */
    public static ArrayList<String> getArguments(String location) {
        String filepath = locationFilepath(location);
        String source = removeComments(SpecsIo.read(filepath));
        int n = locationIndex(location, source);
        StringInt si = readWhile(source, n, (char c) -> {
            return c != '(';
        }, false);
        si = readWhile(source, si.number, (char c) -> {
            return c != ')';
        }, true);
        ArrayList<String> result = new ArrayList<String>(
                Arrays.asList(si.str.replace("(", "").replace(" ", "").split(",")));
        return result;
    }

    /**
     * Find the type name in a variable declaration.
     */
    public static String getTypeFromDeclaration(String location) {
        String filepath = locationFilepath(location);
        String source = removeComments(SpecsIo.read(filepath));
        int n = locationIndex(location, source);
        StringInt si = readWhile(source, n, (char c) -> {
            return !(c == ';' || c == '{');
        }, false);
        si = readWhile(source, si.number, isNotTokenChar, true);
        si = readWhile(source, si.number, isTokenChar, true);
        return si.str;
    }

    /**
     * Find the struct name when a variable is declared in the following format (or something similar): struct foo bar =
     * {0, 1, 2}; This function only works if the initialization uses curly brackets.
     */
    public static String getTypeFromStructDeclaration(String location) {
        String filepath = locationFilepath(location);
        String source = removeComments(SpecsIo.read(filepath));
        int n = locationIndex(location, source);
        StringInt si = readWhile(source, n, (char c) -> {
            return c != '}';
        }, true);
        si = readWhile(source, si.number - 1, (char c) -> {
            return c != '{';
        }, false);
        si = readWhile(source, si.number - 1, (char c) -> {
            return !(c == ';' || c == '{');
        }, false);
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
    public static ArrayList<String> extractFromParenthesis(String message) {
        ArrayList<String> result = new ArrayList<String>();
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
    public static ArrayList<String> extractFromSingleQuotes(String message) {
        ArrayList<String> result = new ArrayList<String>();
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
     * Extract "aka" from error messages (if there is any) Example: "member reference base type 'TYPE_PATCH_1872' (aka
     * 'int') is not a structure or union" -> "int" When no "aka" is found for the type, it returns an empty string. The
     * result is always an ArrayList with 2 strings.
     */
    public static ArrayList<String> getTypesFromMessage(String message) {
        ArrayList<String> types = extractFromSingleQuotes(message);
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
            if (nthIndexOf(message, "\'", 3) > message.indexOf("aka")) {
                toTypeName = types.get(0);
                fromTypeName = types.get(2);
            } else {
                toTypeName = types.get(0);
                fromTypeName = types.get(1);
            }
        }
        ArrayList<String> result = new ArrayList<>();
        result.add(toTypeName);
        result.add(fromTypeName);
        return result;
    }

    /**
     * Extract "aka" from error messages (if there is any) Example: "member reference base type 'TYPE_PATCH_1872' (aka
     * 'int') is not a structure or union" -> "int" When no "aka" is found for the type, it returns an empty string. The
     * result is always an ArrayList with size=2
     */
    public static ArrayList<String> getAkaFromMessage(String message) {
        ArrayList<String> result = new ArrayList<>();
        int indexAka1 = message.indexOf("aka");
        int indexAka2 = message.indexOf("aka", indexAka1 + 1);
        if (indexAka1 < 0) {
            result.add("");
            result.add("");
            return result;
        } else {
            if (indexAka2 > 0) {
                String aka1 = extractFromSingleQuotes(message.substring(indexAka1)).get(0);
                String aka2 = extractFromSingleQuotes(message.substring(indexAka2)).get(0);
                result.add(aka1);
                result.add(aka2);
                return result;
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
                return result;
            }
        }
    }

    /**
     * remove brackets and substitute with * Example: char[3] -> char*
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
     * Takes as argument a type with qualifiers and returns the type name without the qualifiers. Example: const foo *
     * -> foo
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
        if (operators.contains(source.substring(0, 3))) {
            return source.substring(0, 3);
        } else if (operators.contains(source.substring(0, 2))) {
            return source.substring(0, 2);
        } else if (operators.contains(source.substring(0, 1))) {
            return source.substring(0, 1);
        } else {
            throw new RuntimeException("Could not identify operator");
        }
    }

    /**
     * Identify if an arrow or dot is used after the identifier in the location.
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
        if (ch == '.' || (ch == '-' && source.charAt(n + 1) == '>')) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Identify if the token in a given location in source code has an operator after it. This function is useful to
     * identify if the identifier is the name of a variable. Example: foo += bar;
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
        si = readWhile(source, si.number, (char c) -> {
            return c == ' ';
        }, true);
        ch = source.charAt(si.number);
        String op = String.valueOf(ch);
        if (operators.contains(op) && ch != '*') {
            return true;
        } else if (operators.contains(op + source.charAt(n + 1))) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Identify if the token in a given location in source code is used to cast a variable. This function is useful to
     * identify if the identifier is the name of a type (not a variable or function). Example: x = (Foo *)y;
     */
    public static boolean isCast(String location) {
        String filepath = locationFilepath(location);
        String source = removeComments(SpecsIo.read(filepath));
        int n = locationIndex(location, source);
        char ch = source.charAt(n);
        if (!(Character.isLetterOrDigit(ch) || ch == '_' || ch == ' ' || ch == '(')) {
            return false;
        }
        StringInt result = readWhile(source, n, (char c) -> {
            return c != '(';
        }, false);
        if (result.str.contains(")")) {
            return false;
        }
        result = readWhile(source, result.number + 1, (char c) -> {
            return c != ')';
        }, true);
        result.str += source.charAt(result.number + 1);
        System.out.println("________________result.str_________________");
        System.out.println(result.str);
        if (result.str.matches("^[a-zA-Z0-9&\\s\\*]*$")) {
            System.out.println("before if");
            result = readWhile(source, n, isNotTokenChar, false);
            System.out.println(result.str);
            if (countChar('(', result.str) <= 1) {
                System.out.println("after if");
                result = readWhile(source, result.number, isTokenChar, false);
                System.out.println(result.str);
                result = readWhile(source, result.number+1, isTokenChar, true);
                System.out.println(result.str);
                if (result.str.equals("if") || result.str.equals("while")  || result.str.equals("switch")) {
                    System.out.println("FALsE");
                    return false;
                }
                else {
                    System.out.println("TRUE");
                    return true;
                }
            }
            else return true;
        } else {
            System.out.println("FALSE");
            return false;
        }

    }

    public static boolean isVariable(String location) {
        if (hasArrowOrDot(location)) {
            return true;
        } else if (usingOperator(location)) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean isPrimitiveType(String type) {
        return primitiveTypes.contains(getTypeName(type));
    }

    public static String removeComments(String source) {
        String[] lines = source.split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains("//")) {
                lines[i] = lines[i].substring(0, lines[i].indexOf("//"));
            }
        }
        source = String.join("\n", lines);
        String result = "";
        for (int i = 0; i < source.length(); i++) {
            char ch = source.charAt(i);
            if (ch == '/') {
                i++;
                result += ' ';
                ch = source.charAt(i);
                if (ch == '*') {
                    i++;
                    result += ' ';
                    while (!(source.charAt(i) == '*' && source.charAt(i + 1) == '/')) {
                        i++;
                        if (source.charAt(i) == '\n')
                            result += '\n';
                        else
                            result += ' ';
                    }
                }
                i++;
                result += " ";
            } else {
                result += ch;
            }
        }
        return result;
    }

}
