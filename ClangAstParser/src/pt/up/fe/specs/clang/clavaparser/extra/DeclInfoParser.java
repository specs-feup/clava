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

package pt.up.fe.specs.clang.clavaparser.extra;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.utils.ClangGenericParsers;
import pt.up.fe.specs.clava.ast.type.tag.DeclRef;
import pt.up.fe.specs.util.stringparser.StringParser;

public class DeclInfoParser {

    public DeclRef parse(ClangNode node) {

	// Examples:
	//
	// 'CSVReader'

	StringParser parser = new StringParser(node.getContent());

	String declName = node.getName();
	// String address = node.getAddressTry().orElse(null);
	String declId = node.getExtendedId();
	String declType = parser.apply(ClangGenericParsers::parsePrimes);

	parser.checkEmpty();

	return new DeclRef(declName, declId, declType);
    }

}
