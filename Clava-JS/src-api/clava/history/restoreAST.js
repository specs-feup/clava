import * as fs from "fs";
import path from "path";
import { fileURLToPath } from "url";
import Clava from "../Clava.js";
import Query from "lara-js/api/weaver/Query.js";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const historyDir = path.resolve(__dirname, 'history');

// Function to restore the AST to a previous state
function restoreAST(idx) {
    const fileListPath = path.join(historyDir, `waypoint_${idx}_files.json`);

    if (!fs.existsSync(fileListPath)) {
      throw new Error(`Waypoint ${idx} does not exist or no source files found.`);
    }
  
  const fileList = JSON.parse(fs.readFileSync(fileListPath));

  // Clean the current AST
  Query.root().removeChildren();

  // Iterate over each file in the list and restore it
  fileList.forEach(file => {
    const filePath = path.join(historyDir, `${path.basename(file.name, '.cpp')}_${idx}.cpp`);
    if (!fs.existsSync(filePath)) {
      throw new Error(`File ${filePath} does not exist.`);
    }

    const code = fs.readFileSync(filePath);
    const tempFilePath = path.join(historyDir, `temp_${path.basename(file.name, '.cpp')}_${idx}.cpp`);
    fs.writeFileSync(tempFilePath, code);

    Clava.addExistingFile(tempFilePath);
    Clava.rebuild();

    fs.unlinkSync(tempFilePath);

    const originalFilePath = path.resolve(__dirname, '../../../../../', file.name);
    fs.writeFileSync(originalFilePath, code);

    console.log(`Restored ${file.name} from ${filePath}`);
  });

  console.log(`AST restored to waypoint ${idx}`);
}

export default restoreAST;
