/**
 * Copyright 2015 SPeCS.
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

package pt.up.fe.specs.clava.viewer;

import pt.up.fe.specs.clava.viewer.codeprinters.ClangAstViewer;
import pt.up.fe.specs.clava.viewer.codeprinters.ClavaAstViewer;

public enum DemoMode {
    CLANG("Clang AST", new ClangAstViewer()),
    // CLAVA("Clava AST", new ClavaAstViewer(false, false)),
    CLAVA_AST("Clava AST", new ClavaAstViewer(false)),
    CLAVA_CPP("C/C++", new ClavaAstViewer(true));

    private final String label;
    private final CodeViewer printer;

    private DemoMode(String label, CodeViewer printer) {
        this.label = label;
        this.printer = printer;
    }

    public String getLabel() {
        return label;
    }

    public CodeViewer getPrinter() {
        return printer;
    }

}
