import { LaraJoinPoint } from "lara-js/api/LaraJoinPoint.js";
import GenericAstConverter from "lara-js/api/visualization/GenericAstConverter.js";
import ToolJoinPoint from "lara-js/api/visualization/public/js/ToolJoinPoint.js";
import { Joinpoint } from "../Joinpoints.js";

export default class ClavaAstConverter implements GenericAstConverter {
  private sortByLocation(jps: Joinpoint[]): Joinpoint[] {
    return jps.sort((jp1, jp2) => jp1.location.localeCompare(jp2.location, 'en', { numeric: true }));
  }

  private toToolJoinPoint(jp: LaraJoinPoint): ToolJoinPoint {
    const clavaJp = jp as Joinpoint;
    const sortedChildren = this.sortByLocation(clavaJp.children.slice());
    
    return new ToolJoinPoint(
      clavaJp.astId,
      clavaJp.joinPointType,
      clavaJp.code,
      sortedChildren.map(child => this.toToolJoinPoint(child)),
    );
  }

  private addIdentation(code: string, indentation: number): string {
    return code.split('\n').map((line, i) => i > 0 ? '   '.repeat(indentation) + line : line).join('\n');
  }

  private refineAstCode(root: ToolJoinPoint, indentation: number = 0): ToolJoinPoint {
    root.code = this.addIdentation(root.code.trim(), indentation);

    const children = root.children;
    if (root.type == 'loop') {
      children
        .filter(child => child.type === 'exprStmt')
        .forEach(child => child.code = child.code.slice(0, -1));	// Remove semicolon from expression statements inside loop parentheses
    }

    if (root.type == 'declStmt') {
      root.children
        .slice(1)
        .forEach(child => {
          child.code = child.code.match(/(?:\S+\s+)(\S.*)/)![1];
        });  // Remove type from variable declarations
    }


    for (const child of root.children) {
      this.refineAstCode(child, ['body', 'class'].includes(root.type) ? indentation + 1 : indentation);
    }

    return root;
  }

  public getToolAst(root: LaraJoinPoint): ToolJoinPoint {
    return this.refineAstCode(this.toToolJoinPoint(root));
  }

  public getPrettyHtmlCode(): string {
    return '';
  }
}