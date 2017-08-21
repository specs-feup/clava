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

package pt.up.fe.specs.clang.clavaparser.comment;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clang.clavaparser.utils.ClangGenericParsers;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.comment.TextComment;
import pt.up.fe.specs.util.stringparser.StringParser;

public class TextCommentParser extends AClangNodeParser<TextComment> {

    public TextCommentParser(ClangConverterTable converter) {
        super(converter);
    }

    @Override
    protected TextComment parse(ClangNode node, StringParser parser) {

        // Not using parser to avoid trimming
        String fullText = parser.toString();
        Preconditions.checkArgument(fullText.startsWith("Text=\""));
        Preconditions.checkArgument(fullText.endsWith("\""));

        String text = fullText.substring("Text=\"".length(), fullText.length() - 1);

        // Drop parser text
        parser.apply(ClangGenericParsers::getString);

        // Check string starts with 'Text="' and ends with '"'
        // boolean startOk = parser.apply(string -> ClangParseWorkers.checkStringStarts(string, "Text=\""));
        // boolean endOk = parser.apply(string -> ClangParseWorkers.checkStringEnds(string, "\""));
        //
        // Preconditions.checkArgument(startOk);
        // Preconditions.checkArgument(endOk);

        // String text = parser.apply(ClangParseWorkers::getString);

        return ClavaNodeFactory.textComment(text, info(node));
    }

}
