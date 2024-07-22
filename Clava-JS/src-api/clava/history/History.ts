import { Operation } from "./Operations.js"

class OperationHistory {
  private operations: Operation[];
  private locked: boolean;

  constructor() {
    this.operations = [];
    this.locked = false;
  }

  private lock() {
    this.locked = true;
  }

  private unlock() {
    this.locked = false;
  }

  newOperation(operation: Operation) {
    if (!this.locked) {
      this.operations.push(operation);
    }
  }

  rollback(n: number = 1) {
    if (n > 0){
      while (n--){
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
    }
  }

  checkpoint() {
    this.operations.length = 0;
  }

  returnToLastCheckpoint() {
    while (this.operations.length > 0) {
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
  }
}

const ophistory = new OperationHistory();

export default ophistory;