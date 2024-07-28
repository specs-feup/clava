import ToolJoinPoint from "lara-js/api/visualization/public/js/ToolJoinPoint.js";
import { AccessSpecifier, AdjustedType, Body, BoolLiteral, Break, Call, Case, Class, Comment, Continue, Decl, Declarator, DeclStmt, DeleteExpr, EnumDecl, EnumeratorDecl, Expression, ExprStmt, FileJp, FloatLiteral, FunctionJp, GotoStmt, If, Include, IntLiteral, Literal, Loop, NamedDecl, Omp, Pragma, Program, RecordJp, ReturnStmt, Scope, Statement, Switch, Tag, Type, TypedefDecl, Vardecl, Varref, WrapperStmt } from "../Joinpoints.js";
import Clava from "../clava/Clava.js";
import { addIdentation, escapeHtml, getNodeCodeTags, getSyntaxHighlightTags } from "lara-js/api/visualization/AstConverterUtils.js";
/**
 * @brief Clava specialization of GenericAstConverter.
 */
export default class ClavaAstConverter {
    updateAst() {
        Clava.rebuild();
    }
    /**
     * @brief Returns the code of the given node.
     * @details If the node has no code, returns undefined, instead of raising an error.
     *
     * @param node Node
     * @returns The code of the node, or undefined if it has no code
     */
    getCode(node) {
        // TODO: When hasCode implementation is ready, replace this body with the following line:
        // return node.hasCode ? node.code.trim() : undefined
        let code;
        try {
            code = node.code.trim();
        }
        catch (e) {
            console.error(`Could not get code of node '${node.joinPointType}': ${e}`);
            code = undefined;
        }
        return code;
    }
    /**
     * @brief Returns the information of the given join point, according to its type.
     *
     * @param jp Join point
     * @returns The information of the join point
     */
    getJoinPointInfo(jp) {
        const info = {
            'AST ID': jp.astId,
            'AST name': jp.astName,
        };
        if (jp instanceof FileJp) {
            Object.assign(info, {
                'File name': jp.name,
                'Source folder': jp.sourceFoldername,
                'Relative path': jp.relativeFilepath,
                'Is C++': jp.isCxx,
                'Is header file': jp.isHeader,
                'Has main': jp.hasMain
            });
        }
        if (jp instanceof Include) {
            Object.assign(info, {
                'Include name': jp.name,
            });
        }
        if (jp instanceof NamedDecl) {
            Object.assign(info, {
                'Name': jp.name,
            });
        }
        if (jp instanceof Pragma) {
            Object.assign(info, {
                'Pragma name': jp.name,
            });
        }
        if (jp instanceof Program) {
            Object.assign(info, {
                'Program name': jp.name,
                'Standard': jp.standard,
            });
        }
        if (jp instanceof Tag) {
            Object.assign(info, {
                'Pragma ID': jp.id,
            });
        }
        if (jp instanceof Type) {
            Object.assign(info, {
                'Constant?': jp.constant,
                'Is builtin': jp.isBuiltin,
                'Has sugar': jp.hasSugar,
                'Type kind': jp.kind
            });
        }
        if (jp instanceof Varref) {
            Object.assign(info, {
                'Name': jp.name,
            });
        }
        if (jp instanceof WrapperStmt) {
            Object.assign(info, {
                'Wrapper type': jp.kind,
            });
        }
        if (jp instanceof AdjustedType) {
            Object.assign(info, {
                'Original type': jp.originalType,
            });
        }
        if (jp instanceof BoolLiteral) {
            Object.assign(info, {
                'Value': jp.value.toString(),
            });
        }
        if (jp instanceof Call) {
            Object.assign(info, {
                'Function name': jp.name,
            });
        }
        if (jp instanceof Class) {
            Object.assign(info, {
                'Is interface': jp.isInterface,
            });
        }
        if (jp instanceof FloatLiteral) {
            Object.assign(info, {
                'Value': jp.value.toString(),
            });
        }
        if (jp instanceof IntLiteral) {
            Object.assign(info, {
                'Value': jp.value.toString(),
            });
        }
        if (jp instanceof Loop) {
            Object.assign(info, {
                'Loop kind': jp.kind,
            });
        }
        if (jp instanceof Omp) {
            Object.assign(info, {
                'Omp kind': jp.kind,
            });
        }
        return info;
    }
    getToolAst(root) {
        let nextId = 0;
        const toToolJoinPoint = (jp) => {
            return new ToolJoinPoint((nextId++).toString(), jp.joinPointType, this.getCode(jp), jp.filepath, this.getJoinPointInfo(jp), jp.children.map(child => toToolJoinPoint(child)));
        };
        return toToolJoinPoint(root);
    }
    /**
     * @brief Converts the given join point to a CodeNode.
     * @details This function assigns ids to the nodes in pre-order, and it MUST
     * be the same as the order used for getToolAst.
     *
     * @param jp Join point
     * @returns The CodeNode representation of the join point
     */
    toCodeNode(jp) {
        let nextId = 0;
        const toCodeNode = (jp) => {
            return {
                jp: jp,
                id: (nextId++).toString(),
                code: this.getCode(jp),
                children: jp.children.map(child => toCodeNode(child)),
            };
        };
        return toCodeNode(jp);
    }
    /**
     * @brief Sorts the given code nodes by location.
     * @details This function uses the join points' line and column, instead of
     * location attribute. If a node does not have a location (line and code),
     * it is placed before all the others.
     *
     * @param codeNodes Array of code nodes
     * @returns Reference to the original (sorted) array
     */
    sortByLocation(codeNodes) {
        return codeNodes.sort((node1, node2) => {
            if (node1.jp.line === node2.jp.line)
                return (node1.jp.column ?? -1) - (node2.jp.column ?? -1);
            return (node1.jp.line ?? -1) - (node2.jp.line ?? -1);
        });
    }
    /**
     * @brief Applies refinements and corrections to the node code and order of
     * the children.
     * @details This is necessary because some nodes do not have its designated
     * code equal to the matching code in the container. For example, the code
     * inside code blocks do not have its indentation.
     *
     * @param node The node code
     * @param indentation The current indentation to use
     * @returns Reference to the original (refined) node
     */
    refineCode(node, indentation = 0) {
        if (node.code)
            node.code = addIdentation(node.code, indentation);
        this.sortByLocation(node.children);
        if (node.jp instanceof Loop) {
            node.children
                .filter(child => child.jp instanceof ExprStmt)
                .forEach(child => child.code = child.code.slice(0, -1)); // Remove semicolon from expression statements inside loop parentheses
        }
        if (node.jp instanceof DeclStmt) {
            node.children
                .slice(1)
                .forEach(child => {
                child.code = child.code.match(/^(?:\S+\s+)(\S.*)$/)[1]; // Remove type from variable declarations
            });
        }
        for (const child of node.children) {
            const newIndentation = (node.jp instanceof Scope || node.jp instanceof Class) ? indentation + 1 : indentation;
            this.refineCode(child, newIndentation);
        }
        if (node.jp instanceof Body && node.jp.naked) {
            const match = node.code.match(/^([^\/]*\S)\s*(\/\/.*)$/);
            if (match) {
                const [, statement, comment] = match;
                node.code = statement + '  ' + comment; // Fix space between statement and inline comment in naked body
            }
        }
        if (node.children.length >= 1 && node.children[0].jp.astName === 'TagDeclVars') {
            const tagDeclVars = node.children[0];
            const typedef = tagDeclVars.children[0];
            if (typedef.jp instanceof TypedefDecl) {
                const [, code1, code2] = typedef.code.match(/^(.*\S)\s+(\S+)$/);
                tagDeclVars.code = typedef.code = code1;
                const newChild = {
                    jp: tagDeclVars.jp,
                    id: tagDeclVars.id,
                    code: code2,
                    children: [{
                            jp: typedef.jp,
                            id: typedef.id,
                            code: code2,
                            children: [],
                        }],
                };
                node.children.push(newChild);
            } // Assign typedef code to TagDeclVars and split into two children
        }
        if (node.jp instanceof Program) {
            node.children = node.children.map(file => ({ jp: node.jp, id: node.id, code: file.code, children: [file] }));
        } // Divide program code into its files
        return node;
    }
    /**
     * @brief Performs C/C++ syntax hioverlighting on the given code.
     * @details This is done by inserting the appropriate span tags around the
     * code to be highlighted, using the HTML code as a string
     *
     * @param code The code to be highlighted, with the code of its children
     * already linked and syntax highlighted
     * @param node The node that the code belongs to
     * @returns The syntax highlighted code
     */
    syntaxHighlight(code, node) {
        if (code === undefined)
            return undefined;
        if (node.jp.astName === "StringLiteral") {
            const [openingTag, closingTag] = getSyntaxHighlightTags('string');
            return openingTag + code + closingTag;
        }
        if (node.jp instanceof Literal) {
            const [openingTag, closingTag] = getSyntaxHighlightTags('literal');
            return openingTag + code + closingTag;
        }
        const [openingTag, closingTag] = getSyntaxHighlightTags('comment');
        if (node.jp instanceof Comment)
            return openingTag + code + closingTag;
        code = code.replaceAll(/(?<!>)(\/\/.*)/g, `${openingTag}$1${closingTag}`); // Highlight unhighlighted single-line comments
        code = code.replaceAll(/(?<!>)(\/\*.*?\*\/)/g, `${openingTag}$1${closingTag}`); // Highlight unhighlighted multi-line comments
        // We know that a piece of code is not highlighted if it is not preceded by a '>' (from the opening span tag)
        if (node.jp instanceof Declarator || node.jp instanceof EnumeratorDecl) {
            const [openingTag, closingTag] = getSyntaxHighlightTags('type');
            const escapedName = node.jp.name?.replace(/\[[^\]]*]$/, ""); // Remove array suffix from variable name
            const regex = new RegExp(`\\s*[&*]*\\s*` + (escapedName ? `\\b${escapedName}\\b` : "$")); // Match the declaration name (if one exists) with the '&' and '*' prefixes
            const namePos = code.search(regex);
            return namePos <= 0 ? code : openingTag + code.slice(0, namePos) + closingTag + code.slice(namePos);
        }
        if (node.jp instanceof Statement || node.jp instanceof Decl || node.jp instanceof Expression) {
            const [openingTag, closingTag] = getSyntaxHighlightTags('keyword');
            if (node.jp instanceof Switch || node.jp instanceof Break || node.jp instanceof Case
                || node.jp instanceof Continue || node.jp instanceof GotoStmt || node.jp instanceof ReturnStmt
                || node.jp instanceof EnumDecl || node.jp instanceof AccessSpecifier || node.jp instanceof DeleteExpr
                || ["FunctionTemplateDecl", "TemplateTypeParmDecl", "NamespaceDecl"].includes(node.jp.astName)) {
                return code.replace(/^(\w+)\b/, `${openingTag}$1${closingTag}`); // Highlight first word
            }
            if (node.jp instanceof If) {
                const elseMatch = code.match(/^(([^/]|\/[^/*]|\/\/.*|\/\*([^*]|\*[^/])*\*\/)*?)(?<!>)\belse\b/); // Match first unhighlighted 'else' that is not inside a comment (single or multi-line)
                // Note that the function is meant to be called recursively, so the 'else' of child ifs are already highlighted
                if (elseMatch) {
                    const elsePos = elseMatch[1].length;
                    return openingTag + 'if' + closingTag + code.slice(2, elsePos) + openingTag + 'else' + closingTag + code.slice(elsePos + 4);
                }
                else {
                    return openingTag + 'if' + closingTag + code.slice(2);
                }
            }
            if (node.jp instanceof Loop) {
                if (node.jp.kind == 'dowhile') {
                    const whilePos = code.match(/^(([^/]|\/[^/*]|\/\/.*|\/\*([^*]|\*[^/])*\*\/)*?)(?<!>)\bwhile\b/)[1].length; // Match first unhighlighted 'while' that is not inside a comment (single or multi-line)
                    // Same logic as the 'else' keyword
                    return openingTag + 'do' + closingTag + code.slice(2, whilePos) + openingTag + 'while' + closingTag + code.slice(whilePos + 5);
                }
                else {
                    return code.replace(/^(\w+)\b/, `${openingTag}$1${closingTag}`); // Highlight first word
                }
            }
            if (node.jp instanceof TypedefDecl && node.code.startsWith('typedef')) { // The code of a TypedefDecl can be divided, and the second part does not have the keyword
                return openingTag + 'typedef' + closingTag + code.slice(7);
            }
            if (node.jp instanceof RecordJp) {
                return code.replace(/(class(?!=)|struct)/, `${openingTag}$1${closingTag}`); // Highlight 'class' or 'struct' in declaration
            }
            if (node.jp instanceof Include || node.jp instanceof Pragma) {
                return code.replace(/^(#\w+)\b/, `${openingTag}$1${closingTag}`); // Highlight the directive
            }
        }
        return code;
    }
    /**
     * @brief Links the nodes of the given AST to their respective portion of code.
     *
     * @param root The root node of the AST
     * @param outerCode The outer code, which should contain the code of all the
     * nodes
     * @param outerCodeStart The start index of the outer code section to be used
     * @param outerCodeEnd The end index of the outer code section to be used
     * @returns Array with three elements: the start index of the node code in the
     * outer code, the end index of the node code in the outer code, and the
     * linked code.
     */
    linkCodeToAstNodes(root, outerCode, outerCodeStart, outerCodeEnd) {
        const nodeCode = root.code;
        if (!nodeCode || !outerCode)
            return [outerCodeStart, outerCodeStart, ""]; // Return empty string if the node has no code
        const nodeCodeHtml = escapeHtml(nodeCode);
        const innerCodeStart = outerCode.indexOf(nodeCodeHtml, outerCodeStart);
        const innerCodeEnd = innerCodeStart + nodeCodeHtml.length;
        if (innerCodeStart === -1 || innerCodeEnd > outerCodeEnd) {
            console.warn(`Code of node "${root.jp.joinPointType}" not found in code container: "${nodeCodeHtml}"`);
            return [outerCodeStart, outerCodeStart, ""]; // Return empty string if the node code is not found
        }
        const [openingTag, closingTag] = getNodeCodeTags(root.id);
        let newCode = '';
        let newCodeIndex = innerCodeStart;
        if (root.jp instanceof Vardecl) {
            newCodeIndex = outerCode.slice(innerCodeStart, innerCodeEnd).search(/[=;]/) + innerCodeStart + 1;
            newCode += outerCode.slice(innerCodeStart, newCodeIndex);
        } // Ignore variable type and name in declaration
        if (root.jp instanceof FunctionJp) {
            newCodeIndex = outerCode.indexOf('(', innerCodeStart) + 1;
            newCode += outerCode.slice(innerCodeStart, newCodeIndex);
        } // Ignore function return type and name in declaration
        for (const child of root.children) {
            const [childCodeStart, childCodeEnd, childCode] = this.linkCodeToAstNodes(child, outerCode, newCodeIndex, innerCodeEnd);
            newCode += outerCode.slice(newCodeIndex, childCodeStart) + childCode; // Add portion behind children that is not matched and the linked child code
            newCodeIndex = childCodeEnd;
        }
        newCode += outerCode.slice(newCodeIndex, innerCodeEnd); // Add the remaining unmatched code
        newCode = this.syntaxHighlight(newCode, root);
        return [innerCodeStart, innerCodeEnd, openingTag + newCode + closingTag];
    }
    getPrettyHtmlCode(root) {
        const rootCodeNode = this.toCodeNode(root);
        this.refineCode(rootCodeNode);
        if (root instanceof Program) {
            return Object.fromEntries(rootCodeNode.children.map(child => {
                const file = child.children[0];
                const fileJp = file.jp;
                const filepath = fileJp.filepath;
                const fileCode = child.code; // same as file.code!
                const fileHtmlCode = escapeHtml(fileCode);
                const fileLinkedHtmlCode = this.linkCodeToAstNodes(child, fileHtmlCode, 0, fileHtmlCode.length)[2];
                return [filepath, fileLinkedHtmlCode];
            })); // Associate code with each file
        }
        else {
            const filepath = root.filepath;
            const code = rootCodeNode.code;
            const htmlCode = code ? escapeHtml(code) : '';
            const linkedHtmlCode = this.linkCodeToAstNodes(rootCodeNode, htmlCode, 0, htmlCode.length)[2];
            return { [filepath]: linkedHtmlCode };
        }
    }
}
//# sourceMappingURL=ClavaAstConverter.js.map