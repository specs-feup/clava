# LARA Visualization Tool

Clava integration of LARA's web tool for visualization and analysis of the AST and its source code.

For more details, see the [LARA Framework repository](https://github.com/specs-feup/lara-framework).

## Usage

To launch or update the visualization tool, execute the following statements:

```js
import VisualizationTool from "clava-js/api/visualization/VisualizationTool.js";

await VisualizationTool.visualize();
```

Once ready, Clava will provide the URL that should be opened in the browser to access the web interface. The function can also change the AST root and URL domain and port.

Other properties will allow the user to know other important information from the server:

```js
VisualizationTool.isLaunched;  // true if the server is running
VisualizationTool.url;         // URL where the server is running
VisualizationTool.port;        // port to which the server is listening
VisualizationTool.hostname;    // hostname to which the server is listening
```

For more details, refer to the `GenericVisualizationTool` documentation, from [LARA](https://github.com/specs-feup/lara-framework).