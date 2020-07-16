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

package pt.up.fe.specs.clava.parsing.snippet.rules;

import java.util.Iterator;
import java.util.Optional;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.SourceRange;
import pt.up.fe.specs.clava.ast.comment.InlineComment;
import pt.up.fe.specs.clava.context.ClavaContext;
import pt.up.fe.specs.clava.parsing.snippet.TextParserRule;

public class InlineCommentRule implements TextParserRule {

    @Override
    public Optional<ClavaNode> apply(String filepath, String line, int lineNumber, Iterator<String> iterator,
            ClavaContext context) {

        // Check if line contains '//'
        int commentIndex = line.indexOf("//");
        if (commentIndex == -1) {
            return Optional.empty();
        }

        // Check if there is text before the comment
        boolean isStmtComment = line.substring(0, commentIndex).trim().isEmpty();

        String commentText = line.substring(commentIndex + "//".length());
        int startCol = commentIndex;
        int endCol = line.length();

        SourceRange loc = new SourceRange(filepath, lineNumber, startCol, lineNumber, endCol);
        // ClavaNodeInfo info = new ClavaNodeInfo(null, loc);
        InlineComment comment = context.getFactory().inlineComment(commentText, isStmtComment);
        comment.set(ClavaNode.LOCATION, loc);

        // System.out.println("INLINE:" + comment.getCode());
        return Optional.of(comment);
    }

}
