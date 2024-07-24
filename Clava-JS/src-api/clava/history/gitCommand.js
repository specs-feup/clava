import ProcessExecutor from "../../../../../lara-framework/Lara-JS/api/lara/util/ProcessExecutor.js";
import { fileURLToPath } from 'url';
import { dirname } from "path";

// Function to get current directory
function getCurrentDir() {
    const __filename = fileURLToPath(import.meta.url);
    return dirname(__filename);
}

// Function to execute git command
function executeGitCommand(command){

    const executor = new ProcessExecutor;
    executor.setWorkingDir(getCurrentDir());

    //Executes the command
    executor.execute(command)

    //Checks the return value and prints the error/log
    if (executor.getReturnValue() !== 0){
        console.error("Error executing git command: " , executor.getStdErr());
        return false;
    } else {
        console.log("Git command executed with sucess: " , executor.getStdOut());
        return true;
    }

}

// Function to run git commands before push
function runPrePushCommands(commands) {
    for (const command of commands) {
        if (!executeGitCommand(command)) {
            return false;
        }
    }
    return true;
}

// Function to commit the changes to git
function gitCommand(commitMessage){

    const prePushCommands = [
        ['git', 'add', '.'],
        ['git', 'commit', '-m', `'${commitMessage.replace(/'/g, "'\\''")}'`]
    ];

    if (!runPrePushCommands(prePushCommands)) {
        return;
    }

    // Try to push the changes
    if (!executeGitCommand(['git', 'push'])) {
        // If the push fails, reset the last commit
        executeGitCommand(['git', 'reset', '--hard', 'HEAD^']);
    }
}

export { gitCommand };