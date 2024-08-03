import { registerSourceCode } from "lara-js/jest/jestHelpers";
import Clava from "../Clava";
import { FunctionJp, Joinpoint, Loop, ReturnStmt } from "../../Joinpoints";
import Query from "lara-js/api/weaver/Query";
import ophistory from "./History";
import {jest} from '@jest/globals'


const code: string = `void func() {
    for (int i = 0; i < 1; i++){
        i++;
    }
    for (int i = 0; i < 2; i++){
        i++;
    }
}

void test() {}

int main(int argc, char *argv[]) {
    func();
    return 0;
}
`;

describe("Transformation History: Multiple operations", () => {
    registerSourceCode(code);
    ophistory.checkpoint();
  
    it("Inserts and detaches code comparison", () => {
        const a: string = Clava.getProgram().code;
  
        const loopStmt1 = Query.search(Loop).get().at(0) as Joinpoint;
        const loopStmt2 = Query.search(Loop).get().at(1) as Joinpoint;
        loopStmt1.insertBefore(loopStmt2.deepCopy());
        loopStmt2.insertAfter(loopStmt1.deepCopy());

        loopStmt1.detach();
        loopStmt2.detach();
  
        const b: string = Clava.getProgram().code;

        ophistory.rollback(4);
        const c: string = Clava.getProgram().code;
  
        expect(a).toEqual(c);
        expect(b).not.toEqual(c);
    });

    it("Replaces and detach code comparison", () => {
        const a: string = Clava.getProgram().code;
  
        const loopStmt1 = Query.search(Loop).get().at(0) as Joinpoint;
        const returnStmt = Query.search(ReturnStmt).get().at(0) as Joinpoint;
        
        let cur = loopStmt1.replaceWith(returnStmt.deepCopy());
        cur = cur.replaceWith("aaaaa");
        cur = cur.replaceWithStrings(["aaaaa", "bbbbb", "ccccc"]);
        cur = cur.toComment();
        cur.detach();
  
        const b: string = Clava.getProgram().code;

        ophistory.rollback(5);
        const c: string = Clava.getProgram().code;
  
        expect(a).toEqual(c);
        expect(b).not.toEqual(c);
    });

    it("Children set and removes code comparison", () => {
        const a: string = Clava.getProgram().code;
  
        const loopStmt1 = Query.search(Loop).get().at(0) as Joinpoint;
        const testFunc = Query.search(FunctionJp).get().at(1) as FunctionJp;
        const returnStmt = Query.search(ReturnStmt).get().at(0) as Joinpoint;
        
        testFunc.body.setFirstChild(loopStmt1.deepCopy());
        testFunc.body.setLastChild(returnStmt.deepCopy());
        testFunc.body.setFirstChild(loopStmt1.deepCopy());
        testFunc.body.setLastChild(returnStmt.deepCopy());
  
        const b: string = Clava.getProgram().code;

        ophistory.rollback(4);
        const c: string = Clava.getProgram().code;
  
        expect(a).toEqual(c);
        expect(b).not.toEqual(c);
    });

    it("Log an error message on undo operation (single rollback)", () => {
        const errorSpy = jest.spyOn(global.console, "error")
            .mockImplementation(() => {});
        
        const loopStmt1 = Query.search(Loop).get().at(0) as Joinpoint;
        
        loopStmt1.replaceWith("aaaa");
        loopStmt1.detach();
  
        ophistory.rollback(2);

        expect(errorSpy).toHaveBeenCalledTimes(1);
    
        errorSpy.mockRestore();
    });

    it("Checkpoints code comparison", () => {
        
        const loopStmt1 = Query.search(Loop).get().at(0) as Joinpoint;
        const loopStmt2 = Query.search(Loop).get().at(1) as Joinpoint;
        loopStmt1.insertBefore(loopStmt2.deepCopy());
        loopStmt2.insertAfter(loopStmt1.deepCopy());
        
        ophistory.checkpoint();
        const a: string = Clava.getProgram().code;

        loopStmt1.detach();
        loopStmt2.detach();
  
        const b: string = Clava.getProgram().code;

        ophistory.returnToLastCheckpoint();
        const c: string = Clava.getProgram().code;
  
        expect(a).toEqual(c);
        expect(b).not.toEqual(c);
    });

    it("Log an error message on undo operation (checkpoint rollback)", () => {
        const errorSpy = jest.spyOn(global.console, "error")
            .mockImplementation(() => {});
        
        const loopStmt1 = Query.search(Loop).get().at(0) as Joinpoint;
        
        ophistory.checkpoint();
        loopStmt1.replaceWith("aaaa");
        loopStmt1.detach();
  
        ophistory.returnToLastCheckpoint();

        expect(errorSpy).toHaveBeenCalledTimes(1);
    
        errorSpy.mockRestore();
    });

    it("Start and stop history recording", () => {
        ophistory.stop();

        const a: string = Clava.getProgram().code;
        
        const loopStmt1 = Query.search(Loop).get().at(0) as Joinpoint;
        loopStmt1.replaceWith("aaaa");
        
        const b: string = Clava.getProgram().code;

        ophistory.start();

        const returnStmt = Query.search(Loop).get().at(0) as Joinpoint;
        const comment = returnStmt.toComment();
        ophistory.checkpoint();
        const c: string = Clava.getProgram().code;
        
        comment.detach();
        const d: string = Clava.getProgram().code;

        ophistory.returnToLastCheckpoint();
        const e: string = Clava.getProgram().code;

        expect(c).toEqual(e);
        expect(a).not.toEqual(b);
        expect(a).not.toEqual(c);
        expect(a).not.toEqual(d);
        expect(b).not.toEqual(c);
        expect(b).not.toEqual(d);
        expect(c).not.toEqual(d);
    });

});