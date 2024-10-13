import Format from "@specs-feup/clava/api/clava/Format.js";
import Query from "@specs-feup/lara/api/weaver/Query.js";

const Nodes = [];
let curr_node_ID = -1;

const Edges = [];

const level = [];
let curr_level_index = -1;

let sub_graph = "";
let sub_graph_index = -1;

const function_list = [];

function add_edges(src, dst) {
    const edge = "ID" + src + "->ID" + dst;
    Edges.push(edge);
}

function increase_curr_level_index(type) {
    type = typeof type !== "undefined" ? type : "not defined";
    curr_level_index++;
    level.push({ Type: type, Nodes: [], firstNode: -1, lastNode: -1 });

    if (
        level[curr_level_index].Type == "function" ||
        level[curr_level_index].Type == "ForStmt" ||
        level[curr_level_index].Type == "WhileStmt"
    ) {
        sub_graph_index++;
        sub_graph += "\nsubgraph cluster_" + sub_graph_index + "{\n";
    }
}

function decrease_curr_level_index() {
    if (
        level[curr_level_index].Type == "function" ||
        level[curr_level_index].Type == "ForStmt" ||
        level[curr_level_index].Type == "WhileStmt"
    ) {
        sub_graph += "\n}\n";
    }

    level.pop();
    curr_level_index--;
}

/**************************************************************
 *
 *                          MAIN
 *
 **************************************************************/

for (const $function of Query.search("file").search("function")) {
    // Using same color to be able to generate the same code for the unit test
    const str_color = "#AAAAAA";
    function_list.push({ Name: $function.name, node_ID: -1, color: str_color });
}

for (const $function of Query.search("function", "dijkstra")) {
    add_funtion_to_graph($function);
}

print_DOT();

/********************************************/
function add_funtion_to_graph($func) {
    increase_curr_level_index("function");

    // add current $func
    add_node("function", $func.name);

    // update node_ID for function in function_list
    const obj = SearchStruct(function_list, "Name", $func.name);
    obj[0]["node_ID"] = curr_node_ID;

    for (const $childStmt of $func.body.stmts) {
        add_stmt_to_graph($childStmt);
    }

    decrease_curr_level_index();
}

/********************************************/
function add_stmt_to_graph($stmt) {
    switch ($stmt.astName) {
        case "ForStmt":
            addNode_ForStmt($stmt);
            break;
        case "WhileStmt":
            addNode_ForStmt($stmt);
            break;
        case "WrapperStmt":
            // Do nothing ... it is a Comment
            break;
        case "DeclStmt":
            addNode_DeclStmt($stmt);
            break;
        case "ExprStmt":
            addNode_ExprStmt($stmt); // should be same as DeclStmt ???
            break;
        case "IfStmt":
            addNode_IfStmt($stmt);
            break;
        case "ReturnStmt":
            addNode_ReturnStmt($stmt);
            break;

        default:
            console.log(
                "ERROR : not defined . . . for stmt.astName = " + $stmt.astName
            );
    }
}

/********************************************/
function addNode_IfStmt($stmt) {
    const label = "<then> then | <if> IF #" + $stmt.line + "|<else> else";
    add_node("IfStmt", label);

    //    add then stmt
    if ($stmt.then != null) {
        increase_curr_level_index("IfStmtThen");

        for (const $childStmt of $stmt.then.stmts) {
            add_stmt_to_graph($childStmt);
        }

        add_edges(
            level[curr_level_index - 1].lastNode + ":then",
            level[curr_level_index].firstNode
        );
        add_edges(
            level[curr_level_index].lastNode,
            level[curr_level_index - 1].lastNode + ":then"
        );
        decrease_curr_level_index();
    }

    //    add else stmt
    if ($stmt.else != null) {
        increase_curr_level_index("IfStmtElse");

        for (const $childStmt of $stmt.else.stmts) {
            add_stmt_to_graph($childStmt);
        }

        add_edges(
            level[curr_level_index - 1].lastNode + ":else",
            level[curr_level_index].firstNode
        );
        add_edges(
            level[curr_level_index].lastNode,
            level[curr_level_index - 1].lastNode + ":else"
        );
        decrease_curr_level_index();
    }
}

/********************************************/
function addNode_ReturnStmt($stmt) {
    const label = "Return";
    add_node("ReturnStmt", label);
}

/********************************************/
function addNode_DeclStmt($stmt) {
    const label = "{ " + $stmt.astName + " #" + $stmt.line + "}";
    add_node("DeclStmt", label);
}

/********************************************/
function addNode_ExprStmt($stmt) {
    const funcCalls = checkforFuncCalls($stmt);
    if (funcCalls.length > 0) {
        let label = '<<table border="0">';
        for (const func_name of funcCalls) {
            const find_obj = SearchStruct(function_list, "Name", func_name);
            const color = find_obj[0]["color"];
            label +=
                '<TR><TD BGCOLOR="' + color + '">' + func_name + "</TD></TR>";
        }
        label += "</table>>";
        add_node("FuncCall", label, funcCalls);
    } else {
        const label = "{ " + $stmt.astName + " #" + $stmt.line + "}";
        add_node("ExprStmt", label);
    }
}

/********************************************/
function addNode_ForStmt($stmt) {
    increase_curr_level_index($stmt.astName);

    const label =
        "<loop_body> loop | <loop>" +
        $stmt.kind +
        " For #" +
        $stmt.line +
        "|<else> else";

    add_node($stmt.astName, $stmt.kind.toUpperCase() + " #" + $stmt.line);

    if (level[curr_level_index - 1].lastNode != -1)
        add_edges(level[curr_level_index - 1].lastNode, curr_node_ID);
    else level[curr_level_index - 1].firstNode = curr_node_ID;

    level[curr_level_index - 1].lastNode = curr_node_ID;

    for (const $childStmt of $stmt.body.stmts) {
        add_stmt_to_graph($childStmt);
    }

    add_edges(
        level[curr_level_index].lastNode,
        level[curr_level_index].firstNode
    );
    decrease_curr_level_index();
}

/********************************************/
function checkforFuncCalls($stmt) {
    const calls = [];

    for (const $call of Query.searchFrom($stmt, "call")) {
        if (SearchStruct(function_list, "Name", $call.name).length == 1) {
            calls.push($call.name);
        }
    }

    return calls;
}

/* >>>>>>>>>>>>>>>>  Graph Functions <<<<<<<<<<<<<<<<<<<<<<*/
function add_node(type, label, func_calls) {
    func_calls = typeof func_calls !== "undefined" ? func_calls : [];

    const node_ID = ++curr_node_ID;

    const node_obj = {
        Node_ID: node_ID,
        Type: type,
        Label: label,
        Func_Calls: func_calls,
        Shape: return_node_shape(type),
    };
    Nodes.push(node_obj);

    sub_graph += "ID" + node_ID + " ";

    if (level[curr_level_index].firstNode == -1) {
        level[curr_level_index].firstNode = curr_node_ID;
    } else {
        add_edges(level[curr_level_index].lastNode, curr_node_ID);
    }
    level[curr_level_index].lastNode = curr_node_ID;
    level[curr_level_index].Nodes.push(curr_node_ID);
}

function add_func_calls_dependency() {
    for (const obj of Nodes)
        for (const func_call_Name of obj["Func_Calls"]) {
            const func_call_node = SearchStruct(
                function_list,
                "Name",
                func_call_Name
            );
            obj["Child_Node_ID"].push(func_call_node[0]["node_ID"]);
        }
}

function return_node_shape(type) {
    switch (type) {
        case "function":
            return ",shape = Msquare ";
        case "FuncCall":
            return ",shape = Mrecord";
        case "ForStmt":
            return ",shape = doubleoctagon";
        case "WhileStmt":
            return ",shape = doubleoctagon";
        case "DeclStmt":
            return "";
        case "ExprStmt":
            return "";
        case "IfStmt":
            return ",shape = Mrecord";
        case "ReturnStmt":
            return ",shape = Msquare ";
        default:
            console.log(
                "return_node_shape ERROR : type " + type + " not defined . . ."
            );
    }
}

function SearchStruct(structObj, filedName, value) {
    return structObj.filter(function (obj) {
        return obj[filedName] == value;
    });
}

function update_node(node_ID, filedName, value) {
    const obj = SearchStruct(Nodes, "Node_ID", node_ID);
    if (obj.length == 1) {
        if (filedName == "Child_Node_ID") obj[0][filedName].push(value);
        else obj[0][filedName] = value;
    } else if (obj.length > 1)
        console.log(
            "update_node func :ERROR . . . multiple node" +
                " with same Node_ID = " +
                node_ID +
                " exist !!!"
        );
    else
        console.log(
            "update_node func : ERROR . . . Node_ID = " +
                node_ID +
                " not exist !!!"
        );
}

function return_node_attribute(node_ID, filedName) {
    const obj = SearchStruct(Nodes, "Node_ID", node_ID);
    if (obj.length == 1) {
        return obj[0][filedName];
    } else if (obj.length > 1)
        console.log(
            "return_node_attribute func :ERROR . . . multiple node" +
                " with same Node_ID = " +
                node_ID +
                " exist !!!"
        );
    else
        console.log(
            "return_node_attribute func : ERROR . . . Node_ID = " +
                node_ID +
                " not exist !!!"
        );
}

function print_DOT() {
    console.log("digraph my_DOT{");
    // print nodes attributes
    console.log("\t node [shape=record]");
    for (const obj of Nodes) {
        let str = "\t\tID" + obj["Node_ID"];

        if (obj["Type"] != "FuncCall")
            str += ' [label="' + Format.escape(obj["Label"]) + '"';
        else str += " [label=" + obj["Label"];

        str += obj["Shape"];

        if (obj["Type"] == "function") {
            const find_obj = SearchStruct(function_list, "Name", obj["Label"]);
            str += ',style=filled, fillcolor="' + find_obj[0]["color"] + '"';
        }

        str += "]";
        console.log(str);
    }

    console.log();

    for (const edge of Edges) {
        console.log("\t\t" + edge);
    }

    console.log("\n");
    // classified nodes into subgraphs
    console.log(sub_graph);
    console.log("}");
}
