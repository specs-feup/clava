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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pt.up.fe.specs.clava.ast.decl.ParmVarDecl;

public class DataFlowParam {
    private String name;
    private String type;
    private boolean isArray = false;
    private int maxSize = 0;
    private int dim = 1;
    private boolean isStream = false;

    public DataFlowParam(String name, String type, boolean isArray) {
	this.name = name;
	this.type = type;
	this.isArray = isArray;
    }

    public DataFlowParam(ParmVarDecl paramNode) {
	name = paramNode.getDeclName();
	isArray = paramNode.getTypeCode().contains("[");
	if (isArray) {
	    type = paramNode.getTypeCode().substring(0, paramNode.getTypeCode().indexOf('['));
	    Pattern p = Pattern.compile("-?\\d+");
	    Matcher m = p.matcher(paramNode.getTypeCode());
	    int max = 0;
	    int dim = 0;
	    while (m.find()) {
		String n = m.group();
		int num = Integer.parseUnsignedInt(n);
		if (num > max)
		    max = num;
		dim++;
	    }
	    this.maxSize = max;
	    this.dim = dim;
	} else
	    type = paramNode.getTypeCode();
    }

    public String getName() {
	return name;
    }

    public void setName(String name) {
	this.name = name;
    }

    public String getType() {
	return type;
    }

    public void setType(String type) {
	this.type = type;
    }

    public boolean isArray() {
	return isArray;
    }

    public void setArray(boolean isArray) {
	this.isArray = isArray;
    }

    @Override
    public String toString() {
	String s = "Param: ";
	s += type + " " + name;
	s += (!isArray) ? ", scalar" : ", array";
	return s;
    }

    public int getMaxSize() {
	return maxSize;
    }

    public int getDim() {
	return dim;
    }

    public boolean isStream() {
	return isStream;
    }

    public void setStream(boolean isStream) {
	this.isStream = isStream;
    }
}
