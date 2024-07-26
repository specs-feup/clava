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

describe("Transformation History: Operations", () => {
    registerSourceCode(code);
    ophistory.checkpoint();
  
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

    it("Initial code, detach first child and rollback code comparison", () => {
        const a: string = Clava.getProgram().code;
    
        const func = Query.search(FunctionJp).first();
        func?.body.firstChild.detach();
    
        const b: string = Clava.getProgram().code;
        
        ophistory.rollback();
        const c: string = Clava.getProgram().code;
        
        expect(a).toEqual(c);
        expect(b).not.toEqual(c);
    });

    it("Initial code, detach only child and rollback code comparison", () => {
        const a: string = Clava.getProgram().code;
    
        const func = Query.search(FunctionJp).first();
        func?.body.detach();
    
        const b: string = Clava.getProgram().code;
        
        ophistory.rollback();
        const c: string = Clava.getProgram().code;
        
        expect(a).toEqual(c);
        expect(b).not.toEqual(c);
    });

    it("Initial code, detach last child and rollback code comparison", () => {
        const a: string = Clava.getProgram().code;
    
        const func = Query.search(FunctionJp).first();
        func?.body.lastChild.detach();
    
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

    it("Initial code, set inline comment and rollback code comparison", () => {
        const a = Clava.getProgram().code;
    
        const loopStmt = Query.search(Loop).get().at(0);
        loopStmt?.setInlineComments("aaaaa");
    
        const b = Clava.getProgram().code;
    
        ophistory.rollback();
        const c = Clava.getProgram().code;
    
        expect(a).toEqual(c);
        expect(b).not.toEqual(c);
    });

    it("Initial code, set inline comments and rollback code comparison", () => {
        const a = Clava.getProgram().code;

        const loopStmt = Query.search(Loop).get().at(0);
        loopStmt?.setInlineComments(["aaaaa", "bbbbb"]);

        const b = Clava.getProgram().code;

        ophistory.rollback();
        const c = Clava.getProgram().code;

        expect(a).toEqual(c);
        expect(b).not.toEqual(c);
    });

    it("Initial code, set inline comments and rollback code comparison", () => {
        const a = Clava.getProgram().code;

        const loopStmt = Query.search(Loop).get().at(0);
        
        loopStmt?.setInlineComments(["aaaaa", "bbbbb"]);
        const b = Clava.getProgram().code;

        loopStmt?.setInlineComments([""]);
        const c = Clava.getProgram().code;

        loopStmt?.setInlineComments("ccccc");
        const d = Clava.getProgram().code;

        loopStmt?.setInlineComments("");
        const e = Clava.getProgram().code;

        ophistory.rollback();
        const f = Clava.getProgram().code;

        ophistory.rollback();
        const g = Clava.getProgram().code;

        ophistory.rollback();
        const h = Clava.getProgram().code;

        ophistory.rollback();
        const i = Clava.getProgram().code;

        expect(a).toEqual(i);
        expect(a).toEqual(e);
        expect(b).toEqual(h);
        expect(c).toEqual(g);
        expect(d).toEqual(f);
        expect(a).not.toEqual(b);
        expect(a).not.toEqual(c);
        expect(a).not.toEqual(d);
        expect(b).not.toEqual(c);
        expect(b).not.toEqual(d);
        expect(c).not.toEqual(d);
  });

    it("Initial code, set id value and rollback value comparison", () => {
        const key = "id";
        const loopStmt = Query.search(Loop).get().at(0);
        const a = loopStmt?.getValue(key);
        loopStmt?.setValue(key, {});

        const b = loopStmt?.getValue(key);

        ophistory.rollback();
        const c = loopStmt?.getValue(key);

        expect(a).toEqual(c);
        expect(b).not.toEqual(c);
    });
});
