laraImport("weaver.Query")

const $pragmaStartEnd = Query.search("function", "targetNodesStartEnd").search("pragma").first()
$pragmaStartEnd.getTargetNodes("end").forEach(node => println(node.line))

const $pragmaStart = Query.search("function", "targetNodesStart").search("pragma").first()
$pragmaStart.getTargetNodes().forEach(node => println(node.line))

const $pragmaStartEndWithoutEnd = Query.search("function", "targetNodesStartEndWithoutEnd").search("pragma").first()
$pragmaStartEndWithoutEnd.getTargetNodes("end").forEach(node => println(node.line))