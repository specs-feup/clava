import ToolJoinPoint from "lara-js/api/visualization/public/js/ToolJoinPoint.js";
import { AdjustedType, BoolLiteral, Call, Class, FileJp, FloatLiteral, Include, IntLiteral, Loop, Marker, NamedDecl, Omp, Pragma, Program, Tag, Type, TypedefDecl, Varref, WrapperStmt } from "../Joinpoints.js";
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
        const clavaJp = root;
        return new ToolJoinPoint(clavaJp.astId, clavaJp.joinPointType, this.getJoinPointInfo(clavaJp), clavaJp.children.map(child => this.getToolAst(child)));
    }
    toCodeNode(jp) {
        return {
            jp: jp,
            code: jp.code.trim(),
            children: jp.children.map(child => this.toCodeNode(child)),
        };
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
        if (node.jp.joinPointType == 'loop') {
            node.children
                .filter(child => child.jp.joinPointType === 'exprStmt')
                .forEach(child => child.code = child.code.slice(0, -1)); // Remove semicolon from expression statements inside loop parentheses
        }
        if (node.jp.joinPointType == 'declStmt') {
            node.children
                .slice(1)
                .forEach(child => {
                child.code = child.code.match(/^(?:\S+\s+)(\S.*)$/)[1];
            }); // Remove type from variable declarations
        }
        for (const child of node.children) {
            const newIndentation = ['body', 'class'].includes(node.jp.joinPointType) ? indentation + 1 : indentation;
            this.refineCode(child, newIndentation);
        }
        if (node.jp.joinPointType == 'body' && node.jp.naked) {
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
                    code: code2,
                    children: [{
                            jp: typedef.jp,
                            code: code2,
                            children: [],
                        }],
                };
                node.children.push(newChild);
            } // Assign typedef code to TagDeclVars and split into two children
        }
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
    getSpanTags(attrs) {
        return [`<span ${attrs.join(' ')}>`, '</span>'];
    }
    getNodeCodeTags(nodeId) {
        return this.getSpanTags(['class="node-code"', `data-node-id="${nodeId}"`]);
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
        const [openingTag, closingTag] = this.getNodeCodeTags(root.jp.astId);
        let newCode = openingTag;
        let newCodeIndex = innerCodeStart;
        for (const child of root.children) {
            const [childCodeStart, childCodeEnd, childCode] = this.linkCodeToAstNodes(child, outerCode, newCodeIndex, innerCodeEnd);
            newCode += outerCode.slice(newCodeIndex, childCodeStart) + childCode;
            newCodeIndex = childCodeEnd;
        }
        newCode += outerCode.slice(newCodeIndex, innerCodeEnd) + closingTag;
        return [innerCodeStart, innerCodeEnd, newCode];
    }
    getPrettyHtmlCode(root) {
        const rootCodeNode = this.toCodeNode(root);
        this.refineCode(rootCodeNode);
        let code = rootCodeNode.code;
        code = this.escapeHtml(code);
        code = this.linkCodeToAstNodes(rootCodeNode, code, 0, code.length)[2];
        return code;
    }
}
//# sourceMappingURL=ClavaAstConverter.js.map