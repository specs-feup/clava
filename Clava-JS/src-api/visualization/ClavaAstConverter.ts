import { LaraJoinPoint } from "lara-js/api/LaraJoinPoint.js";
import GenericAstConverter from "lara-js/api/visualization/GenericAstConverter.js";
import ToolJoinPoint from "lara-js/api/visualization/public/js/ToolJoinPoint.js";
import { Joinpoint } from "../Joinpoints.js";
import Clava from "../clava/Clava.js";

type CodeMap = { [nodeId: string]: string };

export default class ClavaAstConverter implements GenericAstConverter {
  public getToolAst(root: LaraJoinPoint): ToolJoinPoint {
    const clavaJp = root as Joinpoint;
    
    return new ToolJoinPoint(
      clavaJp.astId,
      clavaJp.joinPointType,
      clavaJp.code,
      clavaJp.children.map(child => this.getToolAst(child)),
    );
  }

  private toCodeMap(jp: Joinpoint, codeMap: CodeMap): CodeMap {
    codeMap[jp.astId] = jp.code.trim();
    jp.children.forEach(child => this.toCodeMap(child, codeMap));
    return codeMap;
  }

  private addIdentation(code: string, indentation: number): string {
    return code.split('\n').map((line, i) => i > 0 ? '   '.repeat(indentation) + line : line).join('\n');
  }

  private refineCodeMap(root: Joinpoint, codeMap: CodeMap, indentation: number = 0): CodeMap {
    codeMap[root.astId] = this.addIdentation(codeMap[root.astId], indentation);

    if (root.joinPointType == 'loop') {
      root.children
        .filter(child => child.joinPointType === 'exprStmt')
        .forEach(child => codeMap[child.astId] = codeMap[child.astId].slice(0, -1));	// Remove semicolon from expression statements inside loop parentheses
    }

    if (root.joinPointType == 'declStmt') {
      root.children
        .slice(1)
        .forEach(child => {
          codeMap[child.astId] = codeMap[child.astId].match(/(?:\S+\s+)(\S.*)/)![1];
        });  // Remove type from variable declarations
    }

    for (const child of root.children) {
      const newIndentation = ['body', 'class'].includes(root.joinPointType) ? indentation + 1 : indentation;
      this.refineCodeMap(child, codeMap, newIndentation);
    }

    return codeMap;
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

  private sortByLocation(jps: Joinpoint[]): Joinpoint[] {
    return jps.sort((jp1, jp2) => jp1.location.localeCompare(jp2.location, 'en', { numeric: true }));
  }

  private linkCodeToAstNodes(root: Joinpoint, codeMap: CodeMap, outerCode: string, outerCodeStart: number, outerCodeEnd: number): any[] {
    const nodeCode = codeMap[root.astId];
    const nodeCodeHtml = this.escapeHtml(nodeCode);
    const innerCodeStart = outerCode.indexOf(nodeCodeHtml, outerCodeStart);
    const innerCodeEnd = innerCodeStart + nodeCodeHtml.length;
    if (innerCodeStart === -1 || innerCodeEnd > outerCodeEnd) {
      console.warn(`Code of node "${root.joinPointType}" not found in code container: "${nodeCodeHtml}"`);
      return [outerCodeStart, outerCodeStart, ""];
    }
    if (root.joinPointType === 'varref' && nodeCode === 'i') {
      console.warn(outerCode.slice(outerCodeStart, outerCodeEnd), innerCodeStart)
    }

    const [openingTag, closingTag] = this.getNodeCodeTags(root.astId);

    let newCode = openingTag;
    let newCodeIndex = innerCodeStart;

    const sortedChildren = this.sortByLocation(root.children.slice());
    for (const child of sortedChildren) {
      const [childCodeStart, childCodeEnd, childCode] = this.linkCodeToAstNodes(child, codeMap, outerCode, newCodeIndex, innerCodeEnd);
      newCode += outerCode.slice(newCodeIndex, childCodeStart) + childCode;
      newCodeIndex = childCodeEnd;
    }
    newCode += outerCode.slice(newCodeIndex, innerCodeEnd) + closingTag;

    return [innerCodeStart, innerCodeEnd, newCode];
  }

  public getPrettyHtmlCode(): string {
    const root = Clava.getProgram();
    const codeMap: CodeMap = {};
    this.toCodeMap(root, codeMap);
    this.refineCodeMap(root, codeMap);

    let code = codeMap[root.astId];
    code = this.escapeHtml(code);
    code = this.linkCodeToAstNodes(root, codeMap, code, 0, code.length)[2];
    return code;
  }
}