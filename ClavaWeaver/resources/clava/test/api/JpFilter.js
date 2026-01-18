import JpFilter from "@specs-feup/lara/api/lara/util/JpFilter.js";
import Query from "@specs-feup/lara/api/weaver/Query.js";
import Weaver from "@specs-feup/lara/api/weaver/Weaver.js";

function convert($jps) {
    let string = "";

    let first = true;
    for (const $jp of $jps) {
        if (first) {
            first = false;
        } else {
            string += ", ";
        }

        string += $jp.name;
    }

    return string;
}

function jpName($jp) {
    return $jp.name;
}

const $records = [];
for (const $record of Query.search("record")) {
    $records.push($record);
}

const filter1 = new JpFilter({ name: "A" });
console.log(convert(filter1.filter($records)));

const filter2 = new JpFilter({ name: /A/ });
console.log(convert(filter2.filter($records)));

const filter3 = new JpFilter({ name: /A/, kind: "class" });
console.log(convert(filter3.filter($records)));

const filter4 = new JpFilter({ name: /A/ });
console.log(convert(filter4.filter($records)));

const filter5 = new JpFilter({
    name: /A/,
    line: function (line) {
        return line > 7;
    },
});
console.log(convert(filter5.filter($records)));

const $jps = Query.search("record", { name: /A/ })
    .search("field", { name: "x" })
    .get();

for (const $selected of $jps) {
    console.log("Select function: " + $selected.code);
}

// Test search inclusive
const $foo2 = Query.search("function", { name: "foo2" }).first();
const $secondFoo2 = Query.searchFromInclusive($foo2, "function", {
    name: "foo2",
}).first();
console.log("Search inclusive: " + $secondFoo2.name);

// Test retrieving default attribute of join point
console.log(
    "Default attribute of 'function': " + Weaver.getDefaultAttribute("function")
);
console.log(
    "Search with default: " + Query.search("function", "foo2").first().name
);
