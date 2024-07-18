import ToolJoinPoint from "lara-js/api/visualization/public/js/ToolJoinPoint.js";
export default class ClavaAstConverter {
    sortByLocation(jps) {
        return jps.sort((jp1, jp2) => jp1.location.localeCompare(jp2.location, 'en', { numeric: true }));
    }
    toToolJoinPoint(jp) {
        const clavaJp = jp;
        const sortedChildren = this.sortByLocation(clavaJp.children.slice());
        return new ToolJoinPoint(clavaJp.astId, clavaJp.joinPointType, clavaJp.code, sortedChildren.map(child => this.toToolJoinPoint(child)));
    }
    refineJoinPoint(jp) {
    }
    getToolAst(root) {
        return this.toToolJoinPoint(root);
    }
    getPrettyHtmlCode() {
        return '';
    }
}
//# sourceMappingURL=ClavaAstConverter.js.map