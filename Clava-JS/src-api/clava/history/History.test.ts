import { registerSourceCode } from "lara-js/jest/jestHelpers";
import Clava from "../../../api/clava/Clava";
import Query from "lara-js/api/weaver/Query";
import ophistory from "./History";
import { Loop } from "../../Joinpoints";

const code: string = `void func() {
    for (int i = 0; i < 1; i++){
        i++;
    }
    for (int i = 0; i < 2; i++){
        i++;
    }
}

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
});

