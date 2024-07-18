import { LaraJoinPoint } from "lara-js/api/LaraJoinPoint.js";
import GenericAstConverter from "lara-js/api/visualization/GenericAstConverter.js";
import ToolJoinPoint from "lara-js/api/visualization/public/js/ToolJoinPoint.js";
import { Body, Joinpoint } from "../Joinpoints.js";
import Clava from "../clava/Clava.js";

type CodeNode = {
  jp: Joinpoint;
  code: string;
  children: CodeNode[];
};

export default class ClavaAstConverter implements GenericAstConverter {
  public getToolAst(root: LaraJoinPoint): ToolJoinPoint {
    const clavaJp = root as Joinpoint;
    
    return new ToolJoinPoint(
      clavaJp.astId,
      clavaJp.joinPointType,
      clavaJp.children.map(child => this.getToolAst(child)),
    );
  }

  private sortByLocation(codeNodes: CodeNode[]): CodeNode[] {
    return codeNodes.sort((node1, node2) => node1.jp.location.localeCompare(node2.jp.location, 'en', { numeric: true }));
  }

  private toCodeNode(jp: Joinpoint): CodeNode {
    return {
      jp: jp,
      code: jp.code.trim(),
      children: jp.children.map(child => this.toCodeNode(child)),
    };
  }

  private addIdentation(code: string, indentation: number): string {
    return code.split('\n').map((line, i) => i > 0 ? '   '.repeat(indentation) + line : line).join('\n');
  }

  private refineCode(node: CodeNode, indentation: number = 0): CodeNode {
    node.code = this.addIdentation(node.code, indentation);
    this.sortByLocation(node.children);

    if (node.jp.joinPointType == 'loop') {
      node.children
        .filter(child => child.jp.joinPointType === 'exprStmt')
        .forEach(child => child.code = child.code.slice(0, -1));	// Remove semicolon from expression statements inside loop parentheses
    }

    if (node.jp.joinPointType == 'declStmt') {
      node.children
        .slice(1)
        .forEach(child => {
          child.code = child.code.match(/^(?:\S+\s+)(\S.*)$/)![1];
        });  // Remove type from variable declarations
    }

    for (const child of node.children) {
      const newIndentation = ['body', 'class'].includes(node.jp.joinPointType) ? indentation + 1 : indentation;
      this.refineCode(child, newIndentation);
    }

    if (node.jp.joinPointType == 'body' && (node.jp as Body).naked) {
      const match = node.code.match(/^([^\/]*\S)\s*(\/\/.*)$/);
      if (match) {
        const [, statement, comment] = match;
        node.code = statement + '  ' + comment;
      }
    }  // Fix space between statement and inline comment in naked body
    
    if (node.children.length >= 1 && node.children[0].jp.astName === 'TagDeclVars') {
      const tagDeclVars = node.children[0];
      const typedef = tagDeclVars.children[0];
      if (typedef.jp.joinPointType === 'typedefDecl') {
        const [, code1, code2] = typedef.code.match(/^(.*\S)\s+(\S+)$/)!;
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
      }  // Assign typedef code to TagDeclVars and split into two children
    }

    return node;
  }

  private escapeHtml(text: string): string {
    const specialCharMap: { [char: string]: string } = {
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
    };
    
    return text.replace(/[&<>]/g, (match) => specialCharMap[match]);
  }

  private getSpanTags(attrs: string[]): string[] {
    return [`<span ${attrs.join(' ')}>`, '</span>'];
  }

  private getNodeCodeTags(nodeId: string): string[] {
    return this.getSpanTags(['class="node-code"', `data-node-id="${nodeId}"`]);
  }

  private linkCodeToAstNodes(root: CodeNode, outerCode: string, outerCodeStart: number, outerCodeEnd: number): any[] {
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

  public getPrettyHtmlCode(): string {
    const root = Clava.getProgram();
    const rootCodeNode = this.toCodeNode(root);
    this.refineCode(rootCodeNode);

    let code = rootCodeNode.code;
    code = this.escapeHtml(code);
    code = this.linkCodeToAstNodes(rootCodeNode, code, 0, code.length)[2];
    return code;
  }
}