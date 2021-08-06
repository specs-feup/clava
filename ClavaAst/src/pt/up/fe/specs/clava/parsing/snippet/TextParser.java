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

package pt.up.fe.specs.clava.parsing.snippet;

import static pt.up.fe.specs.clava.context.ClavaContext.FACTORY;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodes;
import pt.up.fe.specs.clava.SourceRange;
import pt.up.fe.specs.clava.ast.comment.InlineComment;
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.decl.DummyDecl;
import pt.up.fe.specs.clava.ast.decl.ParmVarDecl;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.ast.stmt.CompoundStmt;
import pt.up.fe.specs.clava.ast.stmt.DummyStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.clava.context.ClavaContext;
import pt.up.fe.specs.clava.context.ClavaFactory;
import pt.up.fe.specs.clava.parsing.snippet.rules.InlineCommentRule;
import pt.up.fe.specs.clava.parsing.snippet.rules.MultiLineCommentRule;
import pt.up.fe.specs.util.SpecsCheck;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.treenode.transform.TransformQueue;
import pt.up.fe.specs.util.utilities.LineStream;

/**
 * Parses text elements from C/C++ files, such as comments and pragmas.
 * 
 * @author JoaoBispo
 *
 */
public class TextParser {

    private static final List<TextParserRule> RULES = Arrays.asList(
            new InlineCommentRule(), new MultiLineCommentRule());

    private final ClavaContext context;

    public TextParser(ClavaContext context) {

        this.context = context;
    }

    private ClavaFactory getFactory() {
        return context.get(ClavaContext.FACTORY);
    }

    /**
     * Adds text elements to the App tree.
     * 
     * @param app
     */
    public void addElements(App app) {
        for (TranslationUnit tu : app.getTranslationUnits()) {
            if (!tu.hasChildren()) {
                continue;
            }

            addElements(tu);
        }
    }

    public void addElements(TranslationUnit tu) {

        // Collect elements from the tree
        TextElements textElements = parseElements(tu.getFile());

        addElements(tu, textElements);
    }

    public void addElements(TranslationUnit tu, TextElements textElements) {

        // TranslationUnit path
        String tuFilepath = tu.getFile().getPath();

        // Add associated inline comments before adding guards
        addAssociatedInlineComments(tu, textElements.associatedInlineComments);

        TransformQueue<ClavaNode> queue = new TransformQueue<>("TextParser Queue");

        // Add temporary guard nodes to translation unit and all CompoundStmt, to simplify insertion algorithm
        List<ClavaNode> guardNodes = insertGuardNodes(tu);

        Iterator<ClavaNode> iterator = getIterator(tu);

        // First node
        Optional<ClavaNode> currentNodeTry = next(iterator);

        Preconditions.checkArgument(currentNodeTry.isPresent(), "After adding guards, this should not be possible");
        ClavaNode currentNode = currentNodeTry.get();

        boolean hasNodes = true;

        // Insert all text elements
        for (ClavaNode textElement : textElements.getStandaloneElements()) {
            int textStartLine = textElement.getLocation().getStartLine();

            // Get node that has a line number greater than the text element
            while (hasNodes && textStartLine >= currentNode.getLocation().getStartLine(tuFilepath)) {

                // If no more nodes, stop
                Optional<ClavaNode> nextNodeTry = next(iterator);

                if (!nextNodeTry.isPresent()) {
                    hasNodes = false;
                    break;
                }

                currentNode = nextNodeTry.get();
            }

            // Check if should insert text element as Stmt
            Optional<Stmt> statement = ClavaNodes.getStatement(currentNode);
            if (statement.isPresent()) {
                textElement = ClavaNodes.toStmt(textElement);
            }

            ClavaNode insertionPoint = statement.isPresent() ? statement.get() : currentNode;

            // If node is inside a StmtWithCondition, insert TextElement before StmtWithCondition
            Stmt parentConditionStmt = insertionPoint.getStmtWithConditionAncestor().orElse(null);
            if (parentConditionStmt != null) {
                queue.moveBefore(parentConditionStmt, textElement);
                continue;
            }

            // If current node is an empty CompoundStmt, add as child
            if (currentNode instanceof CompoundStmt && !currentNode.hasChildren()) {
                queue.addChild(insertionPoint, textElement);
                continue;
            }

            // If current node has start line greater than text element, insert before
            if (textStartLine <= insertionPoint.getLocation().getStartLine(tuFilepath)) {
                queue.moveBefore(insertionPoint, textElement);
                continue;
            }

            SpecsLogs.info("Could not insert comment: " + textElement.getCode());
        }

        // Remove guard nodes
        guardNodes.stream().forEach(guardNode -> queue.delete(guardNode));

        // Apply transformations
        queue.apply();
    }

    private static Iterator<ClavaNode> getIterator(TranslationUnit tu) {
        String tuFilepath = tu.getLocation().getFilepath();

        Iterator<ClavaNode> iterator = tu.getDescendantsStream()
                .filter(node -> node instanceof Stmt || node instanceof Decl)
                .filter(node -> tuFilepath.equals(node.getLocation().getFilepath()))
                .iterator();

        return iterator;
    }

    private void addAssociatedInlineComments(TranslationUnit tu, List<InlineComment> associatedInlineComments) {

        // If empty, do nothing
        if (associatedInlineComments.isEmpty()) {
            return;
        }

        // Build iterator
        Iterator<ClavaNode> iterator = getIterator(tu);

        // Build NavigableMap
        NavigableMap<Integer, InlineComment> textElements = new TreeMap<>();

        for (InlineComment comment : associatedInlineComments) {
            textElements.put(comment.getLocation().getStartLine(), comment);
        }

        Map<InlineComment, ClavaNode> associatedNodes = new LinkedHashMap<>();
        String tuFilepath = tu.getFile().getPath();

        while (iterator.hasNext()) {
            ClavaNode currentNode = iterator.next();

            // Check that interval is valid
            int startLine = currentNode.getLocation().getStartLine(tuFilepath);
            int endLine = currentNode.getLocation().getEndLine();

            if (startLine == -1 || endLine == -1) {
                continue;
            }

            // End line must be equal or greater than end line
            if (endLine < startLine) {
                continue;
            }

            SortedMap<Integer, InlineComment> comments = textElements.subMap(startLine, true, endLine, true);

            for (InlineComment comment : comments.values()) {
                associatedNodes.put(comment, currentNode);
            }

        }

        // Check if there is any missing comment
        Set<InlineComment> allComments = new HashSet<>(associatedInlineComments);
        allComments.removeAll(associatedNodes.keySet());
        if (!allComments.isEmpty()) {
            String missingComments = allComments.stream()
                    .map(InlineComment::getCode)
                    .collect(Collectors.joining(ClavaNodes.ln()));

            SpecsLogs.msgInfo("Could not associate the following comments:" + missingComments);
        }

        for (InlineComment comment : associatedNodes.keySet()) {
            associatedNodes.get(comment).associateComment(comment);
        }
    }

    private List<ClavaNode> insertGuardNodes(TranslationUnit tu) {

        // Guard nodes for the translation unit
        SourceRange dummyStartLoc = new SourceRange(tu.getLocation().getFilepath(), 0, 0, 0, 0);

        DummyDecl startGuard = context.get(FACTORY).dummyDecl("Textparser_StartGuard");
        startGuard.setLocation(dummyStartLoc);

        // Get last end line
        int lastLine = tu.getLocation().getEndLine();
        SpecsCheck.checkArgument(lastLine >= 0, () -> "Expected line to be greater or equal than 0: " + lastLine);
        int endLine = lastLine + 1;

        SourceRange dummyEndLoc = new SourceRange(tu.getLocation().getFilepath(), endLine + 1, 0, endLine + 1, 0);

        DummyDecl endGuard = context.get(FACTORY).dummyDecl("Textparser_EndGuard");
        endGuard.setLocation(dummyEndLoc);

        tu.addChild(0, startGuard);
        tu.addChild(tu.getNumChildren(), endGuard);

        List<ClavaNode> guardNodes = new ArrayList<>();
        guardNodes.add(startGuard);
        guardNodes.add(endGuard);

        TransformQueue<ClavaNode> queue = new TransformQueue<>("Guard Nodes");

        // Add guard nodes to CompoundStmt
        for (CompoundStmt compoundStmt : tu.getDescendants(CompoundStmt.class)) {
            SourceRange compoundStartLoc = new SourceRange(compoundStmt.getLocation().getStart());
            SourceRange compoundEndLoc = new SourceRange(compoundStmt.getLocation().getEnd());

            DummyStmt compoundStartGuard = getFactory().dummyStmt("Compound_StartGuard");
            compoundStartGuard.set(ClavaNode.LOCATION, compoundStartLoc);
            DummyStmt compoundEndGuard = getFactory().dummyStmt("Compound_EndGuard");
            compoundEndGuard.set(ClavaNode.LOCATION, compoundEndLoc);

            guardNodes.add(compoundStartGuard);
            guardNodes.add(compoundEndGuard);

            // If compound statement is a scope, add guards before/after the scope
            if (compoundStmt.isNestedScope()) {
                queue.moveBefore(compoundStmt, compoundStartGuard);
                queue.moveAfter(compoundStmt, compoundEndGuard);
            }
            // Otherwise, add guards inside the body
            // This allows inline comments at the level of ifs/fors/etc to be moved inside the body
            else {
                queue.addChildHead(compoundStmt, compoundStartGuard);
                queue.addChild(compoundStmt, compoundEndGuard);
            }

        }

        // Commit adds
        queue.apply();

        return guardNodes;
    }

    private static Optional<ClavaNode> next(Iterator<ClavaNode> iterator) {
        Optional<ClavaNode> currentNode = Optional.empty();

        // Filter nodes that we do not what to test against
        while ((currentNode = nextInternal(iterator)).isPresent()) {
            ClavaNode node = currentNode.get();

            boolean filteredInstances = node instanceof ParmVarDecl || node instanceof CompoundStmt;

            if (!filteredInstances) {
                return currentNode;
            }

            // Otherwise, ask for next
        }

        return Optional.empty();
    }

    private static Optional<ClavaNode> nextInternal(Iterator<ClavaNode> iterator) {

        if (!iterator.hasNext()) {
            return Optional.empty();
        }

        // TODO: Do we need to check if node is part of the current translation unit?
        // Probably not, since we are limiting nodes to Decl and Stmt

        ClavaNode node = iterator.next();
        return Optional.of(node);

    }

    public TextElements parseElements(File sourceFile) {

        // Separate inline comments associated to a node from the rest
        List<ClavaNode> standaloneElements = new ArrayList<>();
        List<InlineComment> associatedComments = new ArrayList<>();

        String filepath = sourceFile.getAbsolutePath();

        try (LineStream lines = LineStream.newInstance(sourceFile)) {
            Iterator<String> iterator = lines.getIterable().iterator();
            int currentLineNumber = 0;

            // Parse each line, looking for text elements
            while (iterator.hasNext()) {

                // Get line, update line number
                String currentLine = iterator.next();
                currentLineNumber++;

                Optional<ClavaNode> textElement = applyRules(filepath, iterator, currentLine, currentLineNumber,
                        context);

                if (!textElement.isPresent()) {
                    continue;
                }

                // Rule was applied, add element
                ClavaNode node = textElement.get();

                if (node instanceof InlineComment && !((InlineComment) node).isStmtComment()) {
                    associatedComments.add((InlineComment) node);
                } else {
                    standaloneElements.add(node);
                }

                // Adjust number of lines in case the rule parsed more than one line
                currentLineNumber += node.getLocation().getEndLine() - node.getLocation().getStartLine();
            }
        }

        return new TextElements(standaloneElements, associatedComments);
    }

    public static Optional<ClavaNode> applyRules(String filepath, Iterator<String> iterator, String currentLine,
            int currentLineNumber, ClavaContext context) {

        // Apply all rules to the current line
        for (TextParserRule rule : RULES) {
            Optional<ClavaNode> textElement = rule.apply(filepath, currentLine, currentLineNumber, iterator, context);

            // If node is present, return
            if (textElement.isPresent()) {
                return textElement;
            }

        }

        return Optional.empty();
    }

}
