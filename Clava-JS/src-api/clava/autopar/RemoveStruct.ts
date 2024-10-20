/**************************************************************
 *
 *                       RemoveStruct
 *
 **************************************************************/
export default function RemoveStruct(structObj: any, criteria: any) {
    return structObj.filter((obj) => {
        return !Object.keys(criteria).every(function (c) {
            if (typeof obj[c] === "string") {
                return obj[c].indexOf(criteria[c]) > -1;
            } else {
                return obj[c] === criteria[c];
            }
        });
    });
}
