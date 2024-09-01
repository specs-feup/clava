<h1 align="center">Clava History API</h1>

<h2>Table of Contents</h2>  

- [Features](#features)
  - [State of Implementation](#state-of-implementation)
- [Usage](#usage)
    - [Importing the History API](#importing-the-history-api)
    - [Enabling the History recording](#enabling-the-history-recording)
    - [Performing Rollbacks](#performing-rollbacks)
    - [Returning to the Last Checkpoint](#returning-to-the-last-checkpoint)
    - [Stopping the History Recording](#stopping-the-history-recording)
  - [Example](#example)

## Features

- **History system** capable of storing operations made to the Joinpoints and the Abstract Syntax Tree (AST) based upon the Event System
- **Operation** wrappers for 12 possible transformations (more info on the table below)
- **Checkpoints** and **rollbacks** by executing reverse operations and returning the AST to a previous state
- **Unit tests** for all of the code, ensuring 100% coverage on all instances

### State of Implementation

| Joinpoint Action | State of Implementation |
| :---: | :--- |
| copy | Irrelevant |
| dataClear | Irrelevant |
| deepCopy | Irrelevant |
| detach | Implemented |
| insertAfter | Implemented |
| insertBefore | Implemented |
| messageToUser | Irrelevant |
| removeChildren | Implemented |
| replaceWith | Implemented |
| setData | Irrelevant |
| setFirstChild | Implemented |
| setInlineComments | Implemented |
| setLastChild | Implemented |
| setType | Implemented |
| setUserField | Irrelevant |
| setValue | Implemented |
| toComment | Implemented |

## Usage

In order to use the History API we have to perform the following steps:

#### Importing the History API

If you want to use the History API, you should start by importing the operation history's instance with:

```javascript
import ophistory from "clava-js/api/clava/history/History.js"
```
This ```ophistory``` is the singleton instance of the Operation History that is used as interface with the API.

#### Enabling the History recording

By default, the History API is not enabled as to not consume unnecessary resources, therefore, if you want to use the History API you can either:
```javascript 
// Start the operation history recording
ophistory.start()

// Create a checkpoint for the script
// This implicitly enables the history recording if needed
ophistory.checkpoint()
```

#### Performing Rollbacks

After enabling the History recording, the transformations are automatically saved to the history without having to change anything in the script. When you need to rollback to a previous state, for example, when an exception is thrown, just call:
```javascript 
// Single rollback operation
// By default, undoes the last transformation
ophistory.rollback()

// Multiple rollback operation
// Undoes the minimum between the value passed
// or the number of operations stored since the last checkpoint
ophistory.rollback(73)
```

#### Returning to the Last Checkpoint

The History system API assumes that when checkpoints are created, the AST is in a safe and stable state, therefore, operations are only saved up to the last checkpoint created. However, when we want to rollback until the last checkpoint state, instead of calling rollback, we can use:
```javascript 
// Undoes all the operations stored since the last checkpoint
ophistory.returnToLastCheckpoint()
```

#### Stopping the History Recording

Just like we can start recording the history on a certain point of the program, the API also allows the history recording to be stopped when necessary. Note that this will clear all the saved operations and effectivelly clear any checkpoints created. On the other hand, this also allows the history to only be used for certain parts of the script.
```javascript 
// Clears any operations saved and stops the recording
ophistory.stop()
```

### Example

```javascript 
import ophistory from "clava-js/api/clava/history/History.js"

/* Execute some queries */
const $loops = Query.search(Loop).get();

ophistory.checkpoint()

/* Transform the AST */
for (const $loop of $loops) {
    $loop.setInlineComments("This is a loop statement")
}

ophistory.rollback(5)

/* Perform some checks */
if ($loops[0].inlineComments.length > 0){
    console.log("More than 5 loops detected")
}

ophistory.returnToLastCheckpoint() 

ophistory.stop()

/* Run some "safe" operations */
for (const $loop of $loops) {
    console.log($loop.code)
}

ophistory.start()

/* Perform some operations */
try {
    for (const $loop of $loops) { 
        $loop.detach() 
    }
}
catch (error) {
    ophistory.returnToLastCheckpoint()
    console.log("Error caused rollback: ", error)
}
```