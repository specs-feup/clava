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
    addIdentation(code, indentation) {
        return code.split('\n').map((line, i) => i > 0 ? '   '.repeat(indentation) + line : line).join('\n');
    }
    refineAstCode(root, indentation = 0) {
        root.code = this.addIdentation(root.code.trim(), indentation);
        const children = root.children;
        if (root.type == 'loop') {
            children
                .filter(child => child.type === 'exprStmt')
                .forEach(child => child.code = child.code.slice(0, -1)); // Remove semicolon from expression statements inside loop parentheses
        }
        if (root.type == 'declStmt') {
            root.children
                .slice(1)
                .forEach(child => {
                child.code = child.code.match(/(?:\S+\s+)(\S.*)/)[1];
            }); // Remove type from variable declarations
        }
        for (const child of root.children) {
            this.refineAstCode(child, ['body', 'class'].includes(root.type) ? indentation + 1 : indentation);
        }
        return root;
    }
    getToolAst(root) {
        return this.refineAstCode(this.toToolJoinPoint(root));
    }
    getPrettyHtmlCode() {
        return '';
    }
}
//# sourceMappingURL=ClavaAstConverter.js.map