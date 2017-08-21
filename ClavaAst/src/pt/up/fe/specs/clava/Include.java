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

package pt.up.fe.specs.clava;

import java.io.File;

public class Include {

    private final File sourceFile;
    private final String include;
    private final int line;
    private final boolean isAngled;

    public Include(String include, boolean isAngled) {
	this(null, include, -1, isAngled);
    }

    public Include(File sourceFile, String include, int line, boolean isAngled) {
	this.sourceFile = sourceFile;
	this.include = include;
	this.line = line;
	this.isAngled = isAngled;
    }

    public static Include empty() {
	return new Include(null, null, -1, false);
    }

    public String getInclude() {
	return include;
    }

    public int getLine() {
	return line;
    }

    public File getSourceFile() {
	return sourceFile;
    }

    public boolean isAngled() {
	return isAngled;
    }

    public Include setSourceFile(File sourceFile) {
	return new Include(sourceFile, include, line, isAngled);
    }

    public Include setInclude(String include) {
	return new Include(sourceFile, include, line, isAngled);

    }

    public Include setLine(int line) {
	return new Include(sourceFile, include, line, isAngled);
    }

    public Include setAngled(boolean isAngled) {
	return new Include(sourceFile, include, line, isAngled);
    }

    @Override
    public String toString() {
	StringBuilder builder = new StringBuilder();

	if (isAngled) {
	    builder.append("<");
	}

	builder.append(include);

	if (isAngled) {
	    builder.append(">");
	}

	builder.append(":").append(line);

	return builder.toString();
    }

}
