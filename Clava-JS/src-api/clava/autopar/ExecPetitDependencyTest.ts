/**************************************************************
 *
 *                       ExecPetitDependencyTest
 *
 **************************************************************/
import ClavaJavaTypes from "@specs-feup/clava/api/clava/ClavaJavaTypes.js";
import { FunctionJp, Loop } from "../../Joinpoints.js";
import GetLoopIndex from "./GetLoopIndex.js";
import Clava from "../Clava.js";
import Io from "@specs-feup/lara/api/lara/Io.js";
import Strings from "@specs-feup/lara/api/lara/Strings.js";
import { allReplace } from "./allReplace.js";
import Add_msgError from "./Add_msgError.js";
import { LoopOmpAttributes } from "./checkForOpenMPCanonicalForm.js";

export default function ExecPetitDependencyTest($ForStmt: Loop) {
    const loopindex = GetLoopIndex($ForStmt);
    if (LoopOmpAttributes[loopindex].msgError?.length !== 0) return;

    LoopOmpAttributes[loopindex].petitInputFileAddress =
        Clava.getWeavingFolder() +
        "/Petitdeploop#" +
        $ForStmt.line +
        "[" +
        ($ForStmt.getAstAncestor("FunctionDecl") as FunctionJp).name +
        "]" +
        ".t";

    LoopOmpAttributes[loopindex].petitOutputFileAddress =
        Clava.getWeavingFolder() +
        "/Petitdeploop#" +
        $ForStmt.line +
        "[" +
        ($ForStmt.getAstAncestor("FunctionDecl") as FunctionJp).name +
        "]" +
        "_output.t";

    const petit_input_file = [];
    for (const element of LoopOmpAttributes[loopindex].ForStmtToPetit)
        petit_input_file.push(
            element.str
        );

    Io.writeFile(
        LoopOmpAttributes[loopindex].petitInputFileAddress,
        petit_input_file.join("\n")
    );

    const petitArgs = [
        "-Fc",
        "-g",
        "-s",
        "-4",
        LoopOmpAttributes[loopindex].petitInputFileAddress,
        "-R" + LoopOmpAttributes[loopindex].petitOutputFileAddress,
    ];

    // RUN petit dependency test
    const printToConsole = false;
    const timeoutSeconds = 60;
    let consoleOutput = ClavaJavaTypes.ClavaPetit.execute(
        petitArgs,
        Clava.getWeavingFolder() + "/",
        printToConsole,
        timeoutSeconds
    );

    if (consoleOutput.length === 0) {
        LoopOmpAttributes[loopindex].PetitFoundDependency = [];

        const PetitOutputDependencyFile = Strings.asLines(
            Io.readFile(LoopOmpAttributes[loopindex].petitOutputFileAddress)
        );
        Io.appendFile(
            LoopOmpAttributes[loopindex].petitInputFileAddress,
            "\n\n!" + Array(100).join("-") + "\n"
        );
        Io.appendFile(
            LoopOmpAttributes[loopindex].petitInputFileAddress,
            "!" + Array(10).join("\t") + " Petit Output Dependency " + "\n!-\n"
        );

        const nosolvedepvarName = [];
        if (PetitOutputDependencyFile) {
            for (const element of PetitOutputDependencyFile) {
                const outputLine = allReplace(element, {
                    ":": "",
                    "-->": "",
                    "\\[": "",
                    "\\]": "",
                })
                    .split(" ")
                    .filter(function (n) {
                        return n != "";
                    })
                    .join(" ")
                    .split(" ");

                const varName = Object.keys(
                    LoopOmpAttributes[loopindex].petit_arrays
                ).filter(function (key) {
                    return (
                        LoopOmpAttributes[loopindex].petit_arrays[key].name ===
                        outputLine[2].split("(")[0]
                    );
                })[0];

                const depObj = {
                    depType: outputLine[0],
                    src: Number(outputLine[1]),
                    dst: Number(outputLine[3]),
                    ArrayName: outputLine[2].split("(")[0],
                    varName: varName,
                    depVector: allReplace(outputLine[5], {
                        "\\(": "",
                        "\\)": "",
                    }).split(","),
                    subscriptNumber: outputLine[2].split("(")[1].split(",")
                        .length,
                    depStatus: outputLine[6],
                    src_usge: outputLine[2],
                    dst_usge: outputLine[4],

                    parentlooprank_src:
                        LoopOmpAttributes[loopindex].ForStmtToPetit[
                            Number(outputLine[1]) - 1
                        ].parentlooprank,
                    parentlooprank_dst:
                        LoopOmpAttributes[loopindex].ForStmtToPetit[
                            Number(outputLine[3]) - 1
                        ].parentlooprank,

                    IsdependentCurrentloop_src:
                        LoopOmpAttributes[loopindex].ForStmtToPetit[
                            Number(outputLine[1]) - 1
                        ].IsdependentCurrentloop,
                    IsdependentInnerloop_src:
                        LoopOmpAttributes[loopindex].ForStmtToPetit[
                            Number(outputLine[1]) - 1
                        ].IsdependentInnerloop,
                    IsdependentOuterloop_src:
                        LoopOmpAttributes[loopindex].ForStmtToPetit[
                            Number(outputLine[1]) - 1
                        ].IsdependentOuterloop,

                    IsdependentCurrentloop_dst:
                        LoopOmpAttributes[loopindex].ForStmtToPetit[
                            Number(outputLine[3]) - 1
                        ].IsdependentCurrentloop,
                    IsdependentInnerloop_dst:
                        LoopOmpAttributes[loopindex].ForStmtToPetit[
                            Number(outputLine[3]) - 1
                        ].IsdependentInnerloop,
                    IsdependentOuterloop_dst:
                        LoopOmpAttributes[loopindex].ForStmtToPetit[
                            Number(outputLine[3]) - 1
                        ].IsdependentOuterloop,

                    varref_line_src:
                        LoopOmpAttributes[loopindex].ForStmtToPetit[
                            Number(outputLine[1]) - 1
                        ].line,
                    varref_line_dst:
                        LoopOmpAttributes[loopindex].ForStmtToPetit[
                            Number(outputLine[3]) - 1
                        ].line,

                    cannotbesolved: false,
                    canbeignored: false,
                };

                if (
                    outputLine[5].indexOf("#") !== -1 ||
                    allReplace(String(depObj.depVector.join("")), {
                        "\\+": "",
                        "0": "",
                        "\\*": "",
                    }).length !== 0
                ) {
                    if (nosolvedepvarName.indexOf(varName) === -1) {
                        nosolvedepvarName.push(varName);
                    }
                }

                if (
                    depObj.subscriptNumber === depObj.depVector.length &&
                    (depObj.IsdependentInnerloop_src ||
                        depObj.IsdependentCurrentloop_src) &&
                    allReplace(String(depObj.depVector.join("")), {
                        "\\+": "",
                        "0": "",
                    }).length === 0 &&
                    depObj.depVector[0] === "0"
                ) {
                    depObj.canbeignored = true;
                }

                if (
                    (depObj.depVector[0] === "0" &&
                        depObj.src_usge === depObj.dst_usge) ||
                    (depObj.src > depObj.dst &&
                        depObj.subscriptNumber === depObj.depVector.length &&
                        depObj.parentlooprank_dst.indexOf(
                            depObj.parentlooprank_src + "_"
                        ) === -1) // add new
                ) {
                    depObj.canbeignored = true;
                }

                if (
                    depObj.IsdependentCurrentloop_src === false ||
                    depObj.IsdependentCurrentloop_dst === false
                )
                    depObj.canbeignored = false;

                LoopOmpAttributes[loopindex].PetitFoundDependency.push(depObj);

                Io.appendFile(
                    LoopOmpAttributes[loopindex].petitInputFileAddress,
                    "!-\t" +
                        element +
                        (depObj.canbeignored === true
                            ? "\t canbeignored = " + depObj.canbeignored
                            : "\t" + Array(21).join(" ")) +
                        (nosolvedepvarName.indexOf(depObj.varName) !== -1
                            ? "\t cannotbesolved = true"
                            : "") +
                        "\n"
                );
            }
        }

        for (const depObj of LoopOmpAttributes[loopindex].PetitFoundDependency)
            if (nosolvedepvarName.indexOf(depObj.varName) !== -1)
                depObj.cannotbesolved = true;

        Io.appendFile(
            LoopOmpAttributes[loopindex].petitInputFileAddress,
            "!" + Array(100).join("-") + "\n"
        );
    } else {
        // Check if console output has path to petit executable, cut it so that the message is platform independent
        const petitIndex = consoleOutput.indexOf("petit:");
        if (petitIndex !== -1) {
            consoleOutput = consoleOutput.substring(petitIndex);
        }
        Add_msgError(
            LoopOmpAttributes,
            $ForStmt,
            "consoleOutput " + consoleOutput
        );
    }
}
