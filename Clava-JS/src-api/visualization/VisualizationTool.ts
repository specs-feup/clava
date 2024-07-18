import GenericAstConverter from 'lara-js/api/visualization/GenericAstConverter.js';
import GenericVisualizationTool from 'lara-js/api/visualization/GenericVisualizationTool.js'
import ClavaAstConverter from './ClavaAstConverter.js';

export class VisualizationTool extends GenericVisualizationTool {
  private joinPointConverter = new ClavaAstConverter();
  private static instance: VisualizationTool = new VisualizationTool();

  public static getInstance(): VisualizationTool {
    return this.instance;
  }

  protected override getAstConverter(): GenericAstConverter {
    return this.joinPointConverter;
  }
}

export default VisualizationTool.getInstance();