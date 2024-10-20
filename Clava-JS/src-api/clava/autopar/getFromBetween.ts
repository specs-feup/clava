export default class getFromBetween {
    results: (string | false)[] = [];
    string = "";

    getFromBetween(sub1: string, sub2: string) {
        if (this.string.indexOf(sub1) < 0 || this.string.indexOf(sub2) < 0)
            return false;
        const SP = this.string.indexOf(sub1) + sub1.length;
        const string1 = this.string.substr(0, SP);
        const string2 = this.string.substr(SP);
        const TP = string1.length + string2.indexOf(sub2);
        return this.string.substring(SP, TP);
    }

    removeFromBetween(sub1: string, sub2: string) {
        if (this.string.indexOf(sub1) < 0 || this.string.indexOf(sub2) < 0)
            return false;
        const removal = sub1 + this.getFromBetween(sub1, sub2) + sub2;
        this.string = this.string.replace(removal, "");
    }

    getAllResults(sub1: string, sub2: string) {
        // first check to see if we do have both substrings
        if (this.string.indexOf(sub1) < 0 || this.string.indexOf(sub2) < 0)
            return;

        // find one result
        const result = this.getFromBetween(sub1, sub2);
        // push it to the results array
        this.results.push(result);
        // remove the most recently found one from the string
        this.removeFromBetween(sub1, sub2);

        // if there's more substrings
        if (this.string.indexOf(sub1) > -1 && this.string.indexOf(sub2) > -1) {
            this.getAllResults(sub1, sub2);
        } else return;
    }

    get(string: string, sub1: string, sub2: string) {
        this.results = [];
        this.string = string;
        this.getAllResults(sub1, sub2);
        return this.results;
    }
}
