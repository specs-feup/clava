import ToolJoinPoint from "lara-js/api/visualization/public/js/ToolJoinPoint.js";
import { AdjustedType, Body, BoolLiteral, Call, Class, Comment, Decl, DeclStmt, ExprStmt, FileJp, FloatLiteral, Include, IntLiteral, Literal, Loop, Marker, NamedDecl, Omp, Pragma, Program, Scope, Tag, Type, TypedefDecl, Vardecl, Varref, WrapperStmt } from "../Joinpoints.js";
export default class ClavaAstConverter {
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
                'Loop ID': jp.id,
                'Loop kind': jp.kind,
            });
        }
        if (jp instanceof Marker) {
            Object.assign(info, {
                'Marker ID': jp.id,
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
            return new ToolJoinPoint((nextId++).toString(), jp.joinPointType, jp.filename, this.getJoinPointInfo(jp), jp.children.map(child => toToolJoinPoint(child)));
        };
        return toToolJoinPoint(root);
    }
    toCodeNode(jp) {
        let nextId = 0;
        const toCodeNode = (jp) => {
            return {
                jp: jp,
                id: (nextId++).toString(),
                code: jp.code.trim(),
                children: jp.children.map(child => toCodeNode(child)),
            };
        };
        return toCodeNode(jp);
    }
    addIdentation(code, indentation) {
        return code.split('\n').map((line, i) => i > 0 ? '   '.repeat(indentation) + line : line).join('\n');
    }
    sortByLocation(codeNodes) {
        return codeNodes.sort((node1, node2) => {
            if (node1.jp.line === node2.jp.line)
                return (node1.jp.column ?? -1) - (node2.jp.column ?? -1);
            return (node1.jp.line ?? -1) - (node2.jp.line ?? -1);
        });
    }
    refineCode(node, indentation = 0) {
        node.code = this.addIdentation(node.code, indentation);
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
                child.code = child.code.match(/^(?:\S+\s+)(\S.*)$/)[1];
            }); // Remove type from variable declarations
        }
        for (const child of node.children) {
            const newIndentation = (node.jp instanceof Scope || node.jp instanceof Class) ? indentation + 1 : indentation;
            this.refineCode(child, newIndentation);
        }
        if (node.jp instanceof Body && node.jp.naked) {
            const match = node.code.match(/^([^\/]*\S)\s*(\/\/.*)$/);
            if (match) {
                const [, statement, comment] = match;
                node.code = statement + '  ' + comment;
            }
        } // Fix space between statement and inline comment in naked body
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
    escapeHtml(text) {
        const specialCharMap = {
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
        };
        return text.replace(/[&<>]/g, (match) => specialCharMap[match]);
    }
    getSpanTags(...attrs) {
        return [`<span ${attrs.join(' ')}>`, '</span>'];
    }
    getNodeCodeTags(nodeId) {
        return this.getSpanTags('class="node-code"', `data-node-id="${nodeId}"`);
    }
    syntaxHighlight(code, node) {
        if (node.jp.astName === "StringLiteral") {
            const [openingTag, closingTag] = this.getSpanTags('class="string"');
            return openingTag + code + closingTag;
        }
        if (node.jp instanceof Literal) {
            const [openingTag, closingTag] = this.getSpanTags('class="literal"');
            return openingTag + code + closingTag;
        }
        if (node.jp instanceof Comment) {
            const [openingTag, closingTag] = this.getSpanTags('class="comment"');
            return openingTag + code + closingTag;
        }
        if (node.jp instanceof NamedDecl) {
            if (node.jp instanceof Vardecl && node.jp.leftJp instanceof Vardecl)
                return code; // Ignore variable declarations without type
            const [openingTag, closingTag] = this.getSpanTags('class="type"');
            return code.replace(/^(\S*)(\s.*)$/s, `${openingTag}$1${closingTag}$2`);
        }
        if (node.jp instanceof Decl) {
            const [openingTag, closingTag] = this.getSpanTags('class="keyword"');
            return code.replace(/^(\S*)(\s.*)$/s, `${openingTag}$1${closingTag}$2`);
        }
        return code;
    }
    linkCodeToAstNodes(root, outerCode, outerCodeStart, outerCodeEnd) {
        const nodeCode = root.code;
        const nodeCodeHtml = this.escapeHtml(nodeCode);
        const innerCodeStart = outerCode.indexOf(nodeCodeHtml, outerCodeStart);
        const innerCodeEnd = innerCodeStart + nodeCodeHtml.length;
        if (innerCodeStart === -1 || innerCodeEnd > outerCodeEnd) {
            console.warn(`Code of node "${root.jp.joinPointType}" not found in code container: "${nodeCodeHtml}"`);
            return [outerCodeStart, outerCodeStart, ""];
        }
        const [openingTag, closingTag] = this.getNodeCodeTags(root.id);
        let newCode = '';
        let newCodeIndex = innerCodeStart;
        if (root.jp instanceof Vardecl) {
            newCodeIndex = outerCode.slice(innerCodeStart, innerCodeEnd).search(/[=;]/) + innerCodeStart + 1;
            newCode += outerCode.slice(innerCodeStart, newCodeIndex);
        } // Ignore variable type and name in declaration
        for (const child of root.children) {
            const [childCodeStart, childCodeEnd, childCode] = this.linkCodeToAstNodes(child, outerCode, newCodeIndex, innerCodeEnd);
            newCode += outerCode.slice(newCodeIndex, childCodeStart) + childCode;
            newCodeIndex = childCodeEnd;
        }
        newCode += outerCode.slice(newCodeIndex, innerCodeEnd);
        newCode = this.syntaxHighlight(newCode, root);
        return [innerCodeStart, innerCodeEnd, openingTag + newCode + closingTag];
    }
    getPrettyHtmlCode(root) {
        const rootCodeNode = this.toCodeNode(root);
        this.refineCode(rootCodeNode);
        if (root instanceof Program) {
            return Object.fromEntries(rootCodeNode.children.map(child => {
                const file = child.children[0];
                const filename = file.jp.name;
                const fileCode = child.code; // same as file.code
                const fileHtmlCode = this.escapeHtml(fileCode);
                const fileLinkedHtmlCode = this.linkCodeToAstNodes(child, fileHtmlCode, 0, fileHtmlCode.length)[2];
                return [filename, fileLinkedHtmlCode];
            }));
        }
        else {
            const filename = root.filename;
            const code = rootCodeNode.code;
            const htmlCode = this.escapeHtml(code);
            const linkedHtmlCode = this.linkCodeToAstNodes(rootCodeNode, htmlCode, 0, htmlCode.length)[2];
            return { [filename]: linkedHtmlCode };
        }
    }
}
//# sourceMappingURL=ClavaAstConverter.js.map