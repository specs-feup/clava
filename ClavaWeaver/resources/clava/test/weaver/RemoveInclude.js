import Query from "@specs-feup/lara/api/weaver/Query.js";

//select file end
for (const $include of Query.search("include")) {
    if ($include.name === "remove_include_1.h") {
        $include.detach();
    }
}

for (const $file of Query.search("file")) {
    $file.addInclude("remove_include_2.h");
}

for (const $file of Query.search("file", "remove_include.c")) {
    console.log($file.code);
}
