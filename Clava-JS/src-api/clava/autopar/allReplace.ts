/**************************************************************
 *
 *                       allReplace
 *
 **************************************************************/
export function allReplace(str: string, obj: Record<string, any>) {
    let retStr = str;
    for (const x in obj) {
        retStr = retStr.replace(new RegExp(x, "g"), obj[x]);
    }
    return retStr;
}

export function getBetweenBrackets(str: string) {
    const matchlist = [];
    for (const match of str.matchAll(/\[(.*?)\]/g)) {
        matchlist.push(match);
    }
    return matchlist;
}

export function moveBracketsToEnd(str: string, arraylist: string[]) {
    let index = 0;
    let matchlist = str;
    while (true) {
        const posSTBracket = str.indexOf("[", index);
        if (posSTBracket === -1) break;

        let posSTspace = str.lastIndexOf(" ", posSTBracket);
        if (posSTspace === -1) {
            posSTspace = 0;
        }

        const posFTBracket = str.indexOf("]", posSTBracket);
        let posFTspace = str.indexOf(" ", posFTBracket);
        if (posFTspace === -1) {
            posFTspace = str.length;
        }

        const substr = str.substring(posSTspace, posFTspace);
        const betweenBracketslist = getBetweenBrackets(substr);
        let newsubstr = substr.replace(/\[.*?\]/g, "");

        arraylist.push(
            newsubstr +
                "(" +
                Array(betweenBracketslist.length + 1)
                    .join("#")
                    .split("")
                    .join(",") +
                ")"
        );

        newsubstr = newsubstr + "(" + betweenBracketslist.join(",") + ")";
        matchlist = matchlist.replace(substr, newsubstr);
        index = posFTspace;
    }

    return matchlist;
}

export function normalizeVarName(Varname: string) {
    //return Varname.replace(/\[.*?\]/g, "");
    return Varname.replace(/\[.*\]/g, "");
}

export function normalizeVarName2(Varname: string) {
    return Varname.replace(/\[.*?\]/g, "-");
}

export function moveBracketsToEnd3(
    str: string,
    arraylistdic: Record<string, { name: string; size: string }>
) {
    if (str.indexOf("[") === -1) return str;

    let filteredstr = allReplace(str, { "\t": " " }) + " ";
    let outputstr = str;

    while (true) {
        if (outputstr.indexOf("[") === -1) break;

        const indexST = filteredstr.lastIndexOf(" ", filteredstr.indexOf("["));
        let indexFT = filteredstr.indexOf("]", indexST + 1);
        while (indexFT < filteredstr.length) {
            if (
                filteredstr.indexOf("[", indexFT) !== -1 &&
                filteredstr
                    .substring(indexFT, filteredstr.indexOf("[", indexFT))
                    .indexOf(" ") === -1
            )
                indexFT = filteredstr.indexOf("]", indexFT + 1);
            else break;
        }
        indexFT = filteredstr.indexOf(" ", indexFT);

        const substr = filteredstr.substring(indexST + 1, indexFT);
        const arrayName = normalizeVarName(substr);
        const betweenBracketslist = getBetweenBrackets(substr);

        const len = Object.keys(arraylistdic).length + 1;
        if (arraylistdic[arrayName] === undefined) {
            arraylistdic[arrayName] = {
                name: "A_" + len,
                size:
                    "(0:9999" +
                    Array(betweenBracketslist.length).join(",0:9999") +
                    ")",
            };
        }
        const newsubstr =
            arraylistdic[arrayName].name +
            "(" +
            betweenBracketslist.join(",") +
            ")";

        outputstr = outputstr.replace(substr, newsubstr);
        filteredstr = filteredstr.replace(substr, "");
    }

    return outputstr;
}

export function moveBracketsToEnd2(
    str: string,
    arraylistdic: Record<string, { name: string; size: string }>
) {
    let index = 0;
    let newCode = str;
    while (true) {
        const posSTBracket = str.indexOf("[", index);
        if (posSTBracket === -1) break;

        let posSTspace = str.lastIndexOf(" ", posSTBracket);
        if (posSTspace === -1) posSTspace = 0;

        const posFTBracket = str.indexOf("]", posSTBracket);
        let posFTspace = str.indexOf(" ", posFTBracket);
        if (posFTspace === -1) posFTspace = str.length;

        const substr = str.substring(posSTspace, posFTspace);

        const betweenBracketslist = getBetweenBrackets(substr);
        let newsubstr = substr.replace(/\[.*?\]/g, "");

        newsubstr = newsubstr + "(" + betweenBracketslist.join(",") + ")";

        let arrayName = newsubstr.split("(")[0];
        arrayName = arrayName.trim();

        const len = Object.keys(arraylistdic).length + 1;
        if (arraylistdic[arrayName] === undefined) {
            arraylistdic[arrayName] = {
                name: "A_" + len,
                size:
                    "(0:9999" +
                    Array(betweenBracketslist.length).join(",0:9999") +
                    ")",
            };
        }
        newsubstr = newsubstr.replace(arrayName, arraylistdic[arrayName].name);

        newCode = str.replace(substr, newsubstr);
        index = posFTspace;
    }

    return newCode;
}
