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

  private refineJoinPoint(jp: ToolJoinPoint) {
    
  }

  public getToolAst(root: LaraJoinPoint): ToolJoinPoint {
    return this.toToolJoinPoint(root);
  }

  public getPrettyHtmlCode(): string {
    return '';
  }
}