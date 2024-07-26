import { Operation } from "./Operations.js"

class OperationHistory {
  private operations: Operation[];
  private locked: boolean;

  constructor() {
    this.operations = [];
    this.locked = true;
  }

  private lock() {
    this.locked = true;
  }

  private unlock() {
    this.locked = false;
  }

  private undo() {
    const op = this.operations.pop();
    if (op !== undefined) {
      try {
        this.lock();
        op.undo();
      } catch (error) {
        console.error("Failed to undo operation:", error);
      } finally {
        this.unlock();
      }
    }
  }

  start() {
    this.operations.length = 0;
    this.unlock();
  }

  stop() {
    this.lock();
    this.operations.length = 0;
  }

  newOperation(operation: Operation) {
    if (!this.locked) {
      this.operations.push(operation);
    }
  }

  rollback(n: number = 1) {
    if (n > 0){
      while (n--){
        this.undo();
      }
    }
  }

  checkpoint() {
    this.operations.length = 0;
    this.unlock();
  }

  returnToLastCheckpoint() {
    while (this.operations.length > 0) {
      this.undo();
    }
  }
}

const ophistory = new OperationHistory();

export default ophistory;