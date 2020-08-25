package pt.up.fe.specs.tupatcher;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pt.up.fe.specs.tupatcher.parser.TUErrorData;
import pt.up.fe.specs.util.SpecsIo;

/**
 *  Copyright 2020 SPeCS.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */

/**
 * 
 * @author Pedro Galvao
 *
 */
public class TUPatcherUtils {
    

    static final List<String> primitiveTypes = Arrays.asList("int", "char", "bool", "void", "long", "unsigned", "struct", "class");
    
    static List<String> getPrimitiveTypes(){
        return primitiveTypes;
    }
    
    public static int nthIndexOf(String str, char ch, int n) {
        int result = 0;
        for (int i=0; i < n; i++) {
            result = str.indexOf(ch, result+1);
        }
        return result;
    }    
    public static int nthIndexOf(String str, String substr, int n) {
        int result = 0;
        for (int i=0; i < n; i++) {
            result = str.indexOf(substr, result+1);
        }
        return result;
    }
    
    public static int locationColumn(String location) {
        int index2 = location.lastIndexOf(':');
        return Integer.parseInt(location.substring(index2+1));
    }    
    public static int locationLine(String location) {
        int index2 = location.lastIndexOf(':');
        int index1 = location.lastIndexOf(':', index2-1);
        return Integer.parseInt(location.substring(index1+1, index2));
    }

    public static String locationFilepath(String location) {
        int index2 = location.lastIndexOf(':');
        int index1 = location.lastIndexOf(':', index2-1);
        return location.substring(0,index1);
    }
    public static int locationIndex(String location, String source) {
        int column = locationColumn(location);
        int line = locationLine(location);
        return nthIndexOf(source, '\n', line-1) + column - 1;
    }
    
    public static String getTokenFromLocation(String location) {
        String filepath = locationFilepath(location); 
        String source = SpecsIo.read(filepath);
        int n = locationIndex(location, source);
        char ch = source.charAt(n);
        String token = "";
        while (!(Character.isLetterOrDigit(ch) || ch=='_')) {
            n++;
            if (n > source.length()) return token;
            ch = source.charAt(n);
        }
        while (Character.isLetterOrDigit(ch) || ch=='_')  {
            token += ch;
            n++;
            ch = source.charAt(n);
        }
        return token;
        
    }
    public static String getTokenBeforeLocation(String location) {
        String filepath = locationFilepath(location); 
        String source = SpecsIo.read(filepath);
        int n = locationIndex(location, source);
        char ch = source.charAt(n);
        String token = "";
        while (!(Character.isLetterOrDigit(ch) || ch=='_')) {
            n--;
            if (n > source.length()) return token;
            ch = source.charAt(n);
        }
        while (Character.isLetterOrDigit(ch) || ch=='_') {
            n--;
            if (n > source.length()) return token;
            ch = source.charAt(n);
        }
        n++;
        ch = source.charAt(n);
        while (Character.isLetterOrDigit(ch) || ch=='_')  {
            token += ch;
            n++;
            ch = source.charAt(n);
        }
        return token;
        
    }
    
    public static boolean isFunctionCall(String location) {
        String filepath = locationFilepath(location); 
        String source = SpecsIo.read(filepath);
        int n = locationIndex(location, source);
        char ch = source.charAt(n);
        String token = "";
        while (!(Character.isLetterOrDigit(ch) || ch=='_')) {
            n++;
            if (n > source.length()) return false;
            ch = source.charAt(n);
        }
        while (Character.isLetterOrDigit(ch) || ch=='_') {
            token += ch;
            n++;
            ch = source.charAt(n);
            if (ch == ':') {
                token = "";
                if (source.charAt(n+1)==':') {
                    n+=2;
                    ch = source.charAt(n);
                }
                else {
                    return false;
                }
            }
            if (ch == '[') {
                for (; ch != ']'; n++) {
                    ch = source.charAt(n);                    
                }
            }
            if (ch == ' ') {
                for (; ch == ' '; n++) {
                    ch = source.charAt(n);                    
                }
            }
            if (ch == '(') return true;
        }
        
        return false;
    }
    
    public static int getNumArguments(String location) {
        
        String filepath = locationFilepath(location); 
        String source = SpecsIo.read(filepath);
        int n = locationIndex(location, source);
        char ch = source.charAt(n);
        int counter = 0;
        while (ch!='(') {
            n--;
            if (n < 0) return -1;
            ch = source.charAt(n);
        }
        n++;
        ch = source.charAt(n);
        if (ch==')') return 0;
        counter++;
        while (ch !=')') {
            n++;
            ch = source.charAt(n);
            if (ch == ',') {
                counter++;
            }
        }
        return counter;
        
    }
    
    public static ArrayList<String> getArguments(String location) {
        ArrayList<String> result = new ArrayList<>();
        String filepath = locationFilepath(location); 
        String source = SpecsIo.read(filepath);
        int n = locationIndex(location, source);
        char ch = source.charAt(n);
        int counter = 0;
        while (ch != '(') {
            n--;
            if (n < 0) return null;
            ch = source.charAt(n);
        }
        n++;
        ch = source.charAt(n);
        if (ch==')') return result;
        counter++;
        String token = "";
        while (ch !=')') {
            n++;
            ch = source.charAt(n);
            if (!(Character.isLetterOrDigit(ch) || ch=='_')) {
                //System.out.println("token: "+token);
                result.add(token);
                token = new String();
            }
            else {
                token += ch;
            }
        }
        return result;
        
    }
    
    
    public static String getTypeFromDeclaration(String location) {
        //esta função não funciona em todos os casos
        String filepath = locationFilepath(location); 
        String source = SpecsIo.read(filepath);
        int n = locationIndex(location, source);
        char ch = source.charAt(n);
        String token = "";
        while (!(ch==';' || ch=='{')) {
            n--;
            if (n < 0) return null;
            ch = source.charAt(n);
            if (ch == '/' && source.charAt(n-1)=='*') {
                break;
            }
        }
        while (!(Character.isLetterOrDigit(ch) || ch=='_')) {
            n++;
            if (n > source.length()) return null;
            ch = source.charAt(n);
        }
        while (Character.isLetterOrDigit(ch) || ch=='_') {            
            token += ch;
            n++;
            ch = source.charAt(n);
        }
        return token;
    }    
    public static String getTypeFromStructDeclaration(String location) {
        //esta função não funciona em todos os casos
        String filepath = locationFilepath(location); 
        String source = SpecsIo.read(filepath);
        int n = locationIndex(location, source);
        char ch = source.charAt(n);
        String token = "";
        while (!(ch=='{')) {
            n--;
            if (n < 0) return null;
            ch = source.charAt(n);
            if (ch == '/' && source.charAt(n-1)=='*') {
                n+=2;
                ch = source.charAt(n);
                break;
            }
        }
        n--;
        ch = source.charAt(n);
        while (!(ch==';' || ch=='{')) {
            n--;
            if (n < 0) return null;
            ch = source.charAt(n);
            //System.out.println(ch);
            if (ch == '/' && source.charAt(n-1)=='*') {
                n++;
                ch = source.charAt(n);
                break;
            }
        }
        while (!(Character.isLetterOrDigit(ch) || ch=='_')) {
            n++;
            if (n > source.length()) return null;
            ch = source.charAt(n);
            //System.out.println(ch);
        }
        while (Character.isLetterOrDigit(ch) || ch=='_') {            
            token += ch;
            n++;
            ch = source.charAt(n);
            //System.out.println(ch);
        }
        if (token.equals("struct")) {
            token = "";
            n++;
            ch = source.charAt(n);
            while (Character.isLetterOrDigit(ch) || ch=='_') {            
                token += ch;
                n++;
                ch = source.charAt(n);
            }
        }
        return token;
    }    
    
    
    public static ArrayList<String> extractFromParenthesis(String message) {
        ArrayList<String> result = new ArrayList<String>();
        int index1 = nthIndexOf(message, '(', 1);
        int index2 = nthIndexOf(message, ')', 2);
        int n=0;
        while (index1>0) {
            result.add(message.substring(index1+1, index2));
            n += 2;
            index1 = nthIndexOf(message, '(', n+1);
            index2 = nthIndexOf(message, ')', n+2);
        }
        return result;
    }
    
    public static ArrayList<String> extractFromSingleQuotes(String message) {
        ArrayList<String> result = new ArrayList<String>();
        int index1 = nthIndexOf(message, '\'', 1);
        int index2 = nthIndexOf(message, '\'', 2);
        int n=0;
        while (index1>0) {
            result.add(message.substring(index1+1, index2));
            n += 2;
            index1 = nthIndexOf(message, '\'', n+1);
            index2 = nthIndexOf(message, '\'', n+2);
        }
        return result;
    }
    
    public static ArrayList<String> getTypesFromMessage(String message) {
        ArrayList<String> types = extractFromSingleQuotes(message);
        if (message.contains("did you mean")) {
            types.remove(types.size()-1);
        }
        if (message.contains("redefinition of")) {
            types.remove(0);
        }
        String toTypeName;
        String fromTypeName;
        if (types.size()==2) {
            toTypeName = types.get(0);
            fromTypeName = types.get(1);
        }
        else if (types.size()==4) {
            toTypeName = types.get(0);
            fromTypeName = types.get(2);
        }
        else {
            if (nthIndexOf(message, "\'", 3) > message.indexOf("aka")) {
                toTypeName = types.get(0);
                fromTypeName = types.get(2);
            }
            else {
                toTypeName = types.get(0);
                fromTypeName = types.get(1);
            }            
        }
        ArrayList<String> result = new ArrayList<>();
        result.add(toTypeName);
        result.add(fromTypeName);
        return result;
    }
    public static ArrayList<String> getAkaFromMessage(String message) {
        ArrayList<String> result = new ArrayList<>();
        ArrayList<String> types = extractFromSingleQuotes(message);
        int indexAka1 = message.indexOf("aka");
        int indexAka2 = message.indexOf("aka", indexAka1+1);
        if (indexAka1 < 0) {
            result.add("");
            result.add("");
            return result;
        }
        else {
            if (indexAka2 > 0) {
                String aka1 = extractFromSingleQuotes(message.substring(indexAka1)).get(0);
                String aka2 = extractFromSingleQuotes(message.substring(indexAka2)).get(0);
                result.add(aka1);
                result.add(aka2);
                return result;
            }
            else {
                if (nthIndexOf(message,'\'',3) < indexAka1) {
                    result.add("");
                    String aka = extractFromSingleQuotes(message.substring(indexAka1)).get(0);
                    result.add(aka);
                }
                else {
                    String aka = extractFromSingleQuotes(message.substring(indexAka1)).get(0);
                    result.add(aka);
                    result.add("");
                }
                return result;        
            }
        }
    }
    
    
    public static String removeBracketsFromType(String typeName) {
        if (typeName.contains("[")) {
            String fix = typeName.substring(0, typeName.indexOf('['));
            fix += "*";
            typeName = fix;
        }
        return typeName;
    }
    

    public static String getTypeName(String qualtype) {
        return removeBracketsFromType(qualtype).replace("struct ", "").replace("class ", "").replace("*", "").replace("&","").replace(" ","");
    }
}
