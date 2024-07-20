import { registerSourceCode } from "lara-js/jest/jestHelpers";
import Clava from "../../../api/clava/Clava";
import Query from "lara-js/api/weaver/Query";
import ophistory from "./History";
import { FunctionJp, Loop, ReturnStmt, Vardecl } from "../../Joinpoints";
import ClavaJoinPoints from "../../../api/clava/ClavaJoinPoints";

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


describe("History of Transformations", () => {
    registerSourceCode(code);
  
    it("Initial code, insert before and rollback code comparison", () => {
        const a: string = Clava.getProgram().code;
  
        const loopStmt = Query.search(Loop).get().at(0);
        loopStmt?.insertBefore(loopStmt.deepCopy());
  
        const b: string = Clava.getProgram().code;
  
        ophistory.rollback();
        const c: string = Clava.getProgram().code;
  
        expect(a).toEqual(c);
        expect(b).not.toEqual(c);
    });

    it("Initial code, insert after and rollback code comparison", () => {
        const a: string = Clava.getProgram().code;
  
        const loopStmt = Query.search(Loop).get().at(0);
        loopStmt?.insertAfter(loopStmt.deepCopy());
  
        const b: string = Clava.getProgram().code;
  
        ophistory.rollback();
        const c: string = Clava.getProgram().code;
  
        expect(a).toEqual(c);
        expect(b).not.toEqual(c);
    });

    it("Initial code, replace singular string and rollback code comparison", () => {
        const a: string = Clava.getProgram().code;
    
        const loopStmt = Query.search(Loop).get().at(0);
        loopStmt?.replaceWith("aaaaa"); 
    
        const b: string = Clava.getProgram().code;
        
        ophistory.rollback();
        const c: string = Clava.getProgram().code;
        
        expect(a).toEqual(c);
        expect(b).not.toEqual(c);
    });

    it("Initial code, replace singular joinpoint and rollback code comparison", () => {
        const a: string = Clava.getProgram().code;
    
        const loopStmt = Query.search(Loop).get().at(0);
        const returnStmt = Query.search(ReturnStmt).first();
        if (returnStmt !== undefined){
            loopStmt?.replaceWith(returnStmt.deepCopy()); 
        }
    
        const b: string = Clava.getProgram().code;
        
        ophistory.rollback();
        const c: string = Clava.getProgram().code;
        
        expect(a).toEqual(c);
        expect(b).not.toEqual(c);
    });

    it("Initial code, replace multiple joinpoints and rollback code comparison", () => {
        // TODO: fix error regarding Joinpoint[] overload not found
        /*const a: string = Clava.getProgram().code;
    
        const loopStmt = Query.search(Loop).get().at(0);
        const returnStmt = Query.search(ReturnStmt).first();
        if (returnStmt !== undefined){
            loopStmt?.replaceWith(returnStmt.deepCopy()); 
        }
    
        const b: string = Clava.getProgram().code;
        
        ophistory.rollback();
        const c: string = Clava.getProgram().code;
        
        expect(a).toEqual(c);
        expect(b).not.toEqual(c);*/
    });

    it("Initial code, replace multiple strings and rollback code comparison", () => {
        const a: string = Clava.getProgram().code;
    
        const loopStmt = Query.search(Loop).get().at(0);
        loopStmt?.replaceWithStrings(["aaaa", "bbbb", "cccc"]);
    
        const b: string = Clava.getProgram().code;
        
        ophistory.rollback();
        const c: string = Clava.getProgram().code;
        
        expect(a).toEqual(c);
        expect(b).not.toEqual(c);
    });

    it("Initial code, comment joinpoint and rollback code comparison", () => {
        const a: string = Clava.getProgram().code;
    
        const loopStmt = Query.search(Loop).get().at(0);
        loopStmt?.toComment();
    
        const b: string = Clava.getProgram().code;
        
        ophistory.rollback();
        const c: string = Clava.getProgram().code;
        
        expect(a).toEqual(c);
        expect(b).not.toEqual(c);
    });

    it("Initial code, replace first child and rollback code comparison", () => {
        const a: string = Clava.getProgram().code;
    
        const func = Query.search(FunctionJp).first();
        const returnStmt = Query.search(ReturnStmt).first();
        if (returnStmt !== undefined){
            func?.body.setFirstChild(returnStmt);
        }
    
        const b: string = Clava.getProgram().code;
        
        ophistory.rollback();
        const c: string = Clava.getProgram().code;
        
        expect(a).toEqual(c);
        expect(b).not.toEqual(c);
    });

    it("Initial code, set first child and rollback code comparison", () => {
        const a: string = Clava.getProgram().code;
    
        const testFunc = Query.search(FunctionJp).get().at(1);
        const returnStmt = Query.search(ReturnStmt).first();
        if (returnStmt !== undefined){
            testFunc?.body.setFirstChild(returnStmt);
        }
    
        const b: string = Clava.getProgram().code;
        
        ophistory.rollback();
        const c: string = Clava.getProgram().code;
        
        expect(a).toEqual(c);
        expect(b).not.toEqual(c);
    });

    it("Initial code, replace last child and rollback code comparison", () => {
        const a: string = Clava.getProgram().code;
    
        const func = Query.search(FunctionJp).first();
        const returnStmt = Query.search(ReturnStmt).first();
        if (returnStmt !== undefined){
            func?.body.setLastChild(returnStmt);
        }
    
        const b: string = Clava.getProgram().code;
        
        ophistory.rollback();
        const c: string = Clava.getProgram().code;
        
        expect(a).toEqual(c);
        expect(b).not.toEqual(c);
    });

    it("Initial code, set last child and rollback code comparison", () => {
        const a: string = Clava.getProgram().code;
    
        const testFunc = Query.search(FunctionJp).get().at(1);
        const returnStmt = Query.search(ReturnStmt).first();
        if (returnStmt !== undefined){
            testFunc?.body.setLastChild(returnStmt);
        }
    
        const b: string = Clava.getProgram().code;
        
        ophistory.rollback();
        const c: string = Clava.getProgram().code;
        
        expect(a).toEqual(c);
        expect(b).not.toEqual(c);
    });

    it("Initial code, remove children and rollback code comparison", () => {
        const a: string = Clava.getProgram().code;
    
        const func = Query.search(FunctionJp).first();
        func?.body.removeChildren();
    
        const b: string = Clava.getProgram().code;
        
        ophistory.rollback();
        const c: string = Clava.getProgram().code;
        
        expect(a).toEqual(c);
        expect(b).not.toEqual(c);
    });

    it("Change type and rollback comparison", () => {
        const a: string = Clava.getProgram().code;        
        
        const vd = Query.search(Vardecl).first();
        vd?.setType(ClavaJoinPoints.type("test"));

        const b: string = Clava.getProgram().code;
        
        ophistory.rollback();
        const c: string = Clava.getProgram().code;

        expect(a).toEqual(c);
        expect(a).not.toEqual(b);
    });
});

