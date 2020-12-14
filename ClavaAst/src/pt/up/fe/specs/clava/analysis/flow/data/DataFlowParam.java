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

package pt.up.fe.specs.clava.analysis.flow.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pt.up.fe.specs.clava.ast.decl.ParmVarDecl;

/**
 * Class that holds information about a function parameter for the DFG
 * 
 * @author Tiago
 *
 */
public class DataFlowParam {
    private String name;
    private String type;
    private boolean isArray = false;
    private ArrayList<Integer> dim = new ArrayList<>();
    private boolean isStream = false;
    private int dataTypeSize;
    public String[] commonTypes = { "char", "short", "int", "long", "float", "double" };
    public HashMap<String, Integer> typeSizes = new HashMap<>() {
	{
	    put("char", 1);
	    put("short", 2);
	    put("int", 4);
	    put("long", 8);
	    put("float", 4);
	    put("double", 8);
	}
    };

    /**
     * Checks whether the parameter type is a type supported by the DFG. Filters
     * things like "unsigned short" into being just "short", since the
     * signed/unsigned information does not matter
     * 
     * @param type
     * @return the simplified name of the type if it is supported, or null otherwise
     */
    private String filterType(String type) {
	String[] tokens = type.split(" ");
	for (String s : tokens) {
	    if (Arrays.asList(commonTypes).contains(s))
		return s;
	}
	return null;
    }

    /**
     * Constructor from a Clava parameter node
     * 
     * @param paramNode
     */
    public DataFlowParam(ParmVarDecl paramNode) {
	name = paramNode.getDeclName();
	isArray = paramNode.getTypeCode().contains("[");
	if (isArray) {
	    type = paramNode.getTypeCode().substring(0, paramNode.getTypeCode().indexOf('['));
	    Pattern p = Pattern.compile("-?\\d+");
	    Matcher m = p.matcher(paramNode.getTypeCode());
	    while (m.find()) {
		String n = m.group();
		int num = Integer.parseUnsignedInt(n);
		dim.add(num);
	    }
	} else
	    type = paramNode.getTypeCode();

	this.type = filterType(this.type).strip();
	this.dataTypeSize = (typeSizes.get(this.type) != null) ? typeSizes.get(this.type) : 4;
    }

    /**
     * Gets the name of the parameter
     * 
     * @return
     */
    public String getName() {
	return name;
    }

    /**
     * Gets the simplified type of the parameter
     * 
     * @return
     */
    public String getType() {
	return type;
    }

    /**
     * Checks if the parameter is an array
     * 
     * @return true if it is an array, false otherwise
     */
    public boolean isArray() {
	return isArray;
    }

    @Override
    public String toString() {
	StringBuilder sb = new StringBuilder();
	sb.append(this.name);
	for (Integer i : this.dim) {
	    sb.append("[").append(i).append("]");
	}
	sb.append(": ").append(this.type).append(" (").append(this.dataTypeSize * 8).append("-bit)");
	return sb.toString();
    }

    /**
     * Gets the dimension of the parameter, if it is an array
     * 
     * @return The dimension if it is an array, 0 if it is a variable
     */
    public ArrayList<Integer> getDim() {
	return dim;
    }

    /**
     * Checks whether the parameter can be considered a stream. Relevant only for
     * HLS
     * 
     * @return true if it can be a stream, false otherwise
     */
    public boolean isStream() {
	return isStream;
    }

    /**
     * Sets the parameter as being a stream. Relevant only for HLS
     *
     * @param isStream
     */
    public void setStream(boolean isStream) {
	this.isStream = isStream;
    }

    /**
     * Gets the size of the data type, in bytes
     * 
     * @return the size of the data type in bytes
     */
    public int getDataTypeSize() {
	return dataTypeSize;
    }
}
