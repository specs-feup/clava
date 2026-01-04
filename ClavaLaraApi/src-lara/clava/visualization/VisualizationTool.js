import GenericVisualizationTool from 'lara-js/api/visualization/GenericVisualizationTool.js';
import ClavaAstConverter from './ClavaAstConverter.js';
export class VisualizationTool extends GenericVisualizationTool {
    astConverter = new ClavaAstConverter();
    static instance = new VisualizationTool();
    static getInstance() {
        return this.instance;
    }
    getAstConverter() {
        return this.astConverter;
    }
}
export default VisualizationTool.getInstance();
//# sourceMappingURL=VisualizationTool.js.map