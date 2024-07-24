import { LaraJoinPoint } from "lara-js/api/LaraJoinPoint.js";
import GenericAstConverter, { FilesCode } from "lara-js/api/visualization/GenericAstConverter.js";
import ToolJoinPoint, { JoinPointInfo } from "lara-js/api/visualization/public/js/ToolJoinPoint.js";
import { AdjustedType, Body, BoolLiteral, Call, Class, DeclStmt, ExprStmt, FileJp, FloatLiteral, Include, IntLiteral, Joinpoint, Loop, Marker, NamedDecl, Omp, Pragma, Program, Scope, Tag, Type, TypedefDecl, Varref, WrapperStmt } from "../Joinpoints.js";

type CodeNode = {
  jp: Joinpoint;
  id: string;
  code: string;
  children: CodeNode[];
};

export default class ClavaAstConverter implements GenericAstConverter {
  private getJoinPointInfo(jp: Joinpoint): JoinPointInfo {
    const info: JoinPointInfo = {
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

  public getToolAst(root: LaraJoinPoint): ToolJoinPoint {
    let nextId = 0;
    const toToolJoinPoint = (jp: Joinpoint): ToolJoinPoint => {
      return new ToolJoinPoint(
        (nextId++).toString(),
        jp.joinPointType,
        jp.filename,
        this.getJoinPointInfo(jp),
        jp.children.map(child => toToolJoinPoint(child)),
      );
    };

    return toToolJoinPoint(root as Joinpoint);
  }

  private toCodeNode(jp: Joinpoint): CodeNode {
    let nextId = 0;
    const toCodeNode = (jp: Joinpoint): CodeNode => {
      return {
        jp: jp,
        id: (nextId++).toString(),
        code: jp.code.trim(),
        children: jp.children.map(child => toCodeNode(child)),
      };
    };

    return toCodeNode(jp);
  }

  private addIdentation(code: string, indentation: number): string {
    return code.split('\n').map((line, i) => i > 0 ? '   '.repeat(indentation) + line : line).join('\n');
  }

  private sortByLocation(codeNodes: CodeNode[]): CodeNode[] {
    return codeNodes.sort((node1, node2) => {
      if (node1.jp.line === node2.jp.line)
        return (node1.jp.column ?? -1) - (node2.jp.column ?? -1);
      return (node1.jp.line ?? -1) - (node2.jp.line ?? -1);
    });
  }

  private refineCode(node: CodeNode, indentation: number = 0): CodeNode {
    node.code = this.addIdentation(node.code, indentation);
    this.sortByLocation(node.children);

    if (node.jp instanceof Loop) {
      node.children
        .filter(child => child.jp instanceof ExprStmt)
        .forEach(child => child.code = child.code.slice(0, -1));	// Remove semicolon from expression statements inside loop parentheses
    }

    if (node.jp instanceof DeclStmt) {
      node.children
        .slice(1)
        .forEach(child => {
          child.code = child.code.match(/^(?:\S+\s+)(\S.*)$/)![1];
        });  // Remove type from variable declarations
    }

    for (const child of node.children) {
      const newIndentation = (node.jp instanceof Scope || node.jp instanceof Class) ? indentation + 1 : indentation;
      this.refineCode(child, newIndentation);
    }

    if (node.jp instanceof Body && (node.jp as Body).naked) {
      const match = node.code.match(/^([^\/]*\S)\s*(\/\/.*)$/);
      if (match) {
        const [, statement, comment] = match;
        node.code = statement + '  ' + comment;
      }
    }  // Fix space between statement and inline comment in naked body
    
    if (node.children.length >= 1 && node.children[0].jp.astName === 'TagDeclVars') {
      const tagDeclVars = node.children[0];
      const typedef = tagDeclVars.children[0];
      if (typedef.jp instanceof TypedefDecl) {
        const [, code1, code2] = typedef.code.match(/^(.*\S)\s+(\S+)$/)!;
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
      }  // Assign typedef code to TagDeclVars and split into two children
    }

    if (node.jp instanceof Program) {
      node.children = node.children.map(file => ({ jp: node.jp, id: node.id, code: file.code, children: [file] }));
    }  // Divide program code into its files

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

  private getSpanTags(...attrs: string[]): string[] {
    return [`<span ${attrs.join(' ')}>`, '</span>'];
  }

  private getNodeCodeTags(nodeId: string): string[] {
    return this.getSpanTags('class="node-code"', `data-node-id="${nodeId}"`);
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

    const [openingTag, closingTag] = this.getNodeCodeTags(root.id);

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

  public getPrettyHtmlCode(root: LaraJoinPoint): FilesCode {
    const rootCodeNode = this.toCodeNode(root as Joinpoint);
    this.refineCode(rootCodeNode);

    if (root instanceof Program) {
      return Object.fromEntries(rootCodeNode.children.map(child => {
        const file = child.children[0];
        const filename = (file.jp as FileJp).name;

        const fileCode = child.code;  // same as file.code
        const fileHtmlCode = this.escapeHtml(fileCode);
        const fileLinkedHtmlCode = this.linkCodeToAstNodes(child, fileHtmlCode, 0, fileHtmlCode.length)[2];
        return [filename, fileLinkedHtmlCode];
      }));
    } else {
      const filename = (root as Joinpoint).filename;
      const code = rootCodeNode.code;
      const htmlCode = this.escapeHtml(code);
      const linkedHtmlCode = this.linkCodeToAstNodes(rootCodeNode, htmlCode, 0, htmlCode.length)[2];

      return { [filename]: linkedHtmlCode };
    }
  }
}