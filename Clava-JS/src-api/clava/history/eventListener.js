import EventEmitter from "events";
import * as fs from "fs";
import path from "path";
import { fileURLToPath } from "url";
import Query from "lara-js/api/weaver/Query.js";
import { gitCommand } from "./gitCommand.js";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const eventListener = new EventEmitter();

const historyDir = path.resolve(__dirname, 'history');
const idxFile = path.join(historyDir, 'index.txt');

// Function to get the current index from the index file
function getCurrentIdx() {
  if (fs.existsSync(idxFile)) {
    return parseInt(fs.readFileSync(idxFile), 10);
  } else {
    return 0;
  }
}

// Function to increment the index and save it to the index file
function incrementIdx(idx) {
  fs.writeFileSync(idxFile, (idx + 1).toString());
}

// Event listener for storing the AST state
eventListener.on('storeAST', () => {
  const idx = getCurrentIdx();
  console.log(`Waypoint ${idx}`);

  // Checks if the folder 'history' exists, else, creates
  if (!fs.existsSync(historyDir)) {
    console.log(`Directory ${historyDir} does not exist. Creating...`);
    fs.mkdirSync(historyDir);
  } else {
    console.log(`Directory ${historyDir} exists.`);
  }

  // Define the file path for saving the AST state
  const files = Query.root().children;
  const fileList = [];
  files.forEach(file => {
    const filePath = path.join(historyDir, `${path.basename(file.filename, '.cpp')}_${idx}.cpp`);
    console.log(`Writing to file ${filePath}`);
    fs.writeFileSync(filePath, file.code);

    fileList.push({ name: path.basename(file.filename) });

    if (fs.existsSync(filePath)) {
      console.log(`File ${filePath} created successfully.`);
    } else {
      console.log(`Failed to create file ${filePath}.`);
    }
  });

  // Save the file list as JSON
  const fileListPath = path.join(historyDir, `waypoint_${idx}_files.json`);
  console.log(`Writing file list to ${fileListPath}`);
  fs.writeFileSync(fileListPath, JSON.stringify(fileList, null, 2));

  // Capture stack trace to obtain the file\line
  const stack = new Error().stack.split('\n')[2].trim();
  const commitMessage = `Automatic commit - Waypoint ${idx} - Triggered by: ${stack}`;

  // Increment the index
  incrementIdx(idx);
  
  // Execute the git command with the commit message
  gitCommand(commitMessage);
});

export default eventListener;
