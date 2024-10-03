import clava.Format;

var Nodes = [];
var curr_node_ID = -1;

var Edges = [];

var level = [];
var curr_level_index = -1;

var sub_graph = '';
var sub_graph_index = -1;

var function_list = [];

function add_edges(src,dst)
{
    var edge = 'ID' + src + '->ID' + dst;
    Edges.push(edge);
}

function increase_curr_level_index(type)
{
    type = (typeof type !== 'undefined') ?  type : 'not defined';
    curr_level_index++;
    level.push({Type:type, Nodes : [], firstNode:-1, lastNode: -1});
    
    if (level[curr_level_index].Type == 'function' ||
        level[curr_level_index].Type == 'ForStmt' ||
        level[curr_level_index].Type == 'WhileStmt')
    {
        sub_graph_index ++;
        sub_graph += '\nsubgraph cluster_' + sub_graph_index + '{\n';
    }
    
}

function decrease_curr_level_index()
{
    if (level[curr_level_index].Type == 'function' ||
        level[curr_level_index].Type == 'ForStmt' ||
        level[curr_level_index].Type == 'WhileStmt')
    {
        sub_graph += '\n}\n';
    }
    
    level.pop();
    curr_level_index--;
}

/**************************************************************
 * 
 *                          MAIN
 * 
**************************************************************/
aspectdef my_test
    
    
    select program end
    apply
        //$program.exec rebuild();
    end
    
    select program.file.function end
    apply
        //var str_color = '#'+(Math.random()*0xFFFFFF<<0).toString(16);
        //var str_color = '#'+Math.floor(Math.random()*16777215).toString(16);
		// Using same color to be able to generate the same code for the unit test
		var str_color = '#AAAAAA';
        function_list.push({ Name:$function.name, node_ID:-1, color:str_color});
        //println($function.name);
    end
    
    //select function end
    select function{'dijkstra'} end
    apply
        call add_funtion_to_graph($function);
    end
    
    //add_func_calls_dependency();
    print_DOT();
    
end

/********************************************/
aspectdef add_funtion_to_graph
    input   $func   end

    increase_curr_level_index('function');
    
    // add current $func
    add_node('function',$func.name);


    // update node_ID for function in function_list
    var obj = SearchStruct(function_list,'Name',$func.name);
    obj[0]['node_ID'] = curr_node_ID;    
    

    select $func.body.childStmt end
    apply
        call add_stmt_to_graph($childStmt);
        //println($childStmt.astName);
    end

    decrease_curr_level_index();
end

/********************************************/
aspectdef add_stmt_to_graph
    input   $stmt   end



    switch ($stmt.astName)
    {
       case 'ForStmt':
           call addNode_ForStmt($stmt);
           break;
       case 'WhileStmt':
           call addNode_ForStmt($stmt);
           break;
       case 'WrapperStmt':
           // Do nothing ... it is a Comment
           break;
       case 'DeclStmt':
           call addNode_DeclStmt($stmt);
           break;
       case 'ExprStmt':
           call addNode_ExprStmt($stmt); // should be same as DeclStmt ???
           break;
       case 'IfStmt':
           call addNode_IfStmt($stmt);
           break;
       case 'ReturnStmt':
           call addNode_ReturnStmt($stmt);
           break;
           
       default:
           println('ERROR : not defined . . . for stmt.astName = '+ $stmt.astName);
    }
    
    
end


/********************************************/
aspectdef addNode_IfStmt
    input   $stmt   end

    var label = '<then> then | <if> IF #'+ $stmt.line + '|<else> else';
    add_node('IfStmt', label);

    //    add then stmt
    if ($stmt.then != null)
        increase_curr_level_index('IfStmtThen');
        
    select $stmt.then.childStmt end
    apply
        call add_stmt_to_graph($childStmt);
    end
       
    if ($stmt.then != null)
    {
        add_edges(level[curr_level_index-1].lastNode + ':then',
            level[curr_level_index].firstNode);
        add_edges(level[curr_level_index].lastNode,
            level[curr_level_index-1].lastNode + ':then');
        decrease_curr_level_index();
    }
    
    
    //    add else stmt
    if ($stmt.else != null)
        increase_curr_level_index('IfStmtElse');
    
    
    select $stmt.else.childStmt end
    apply
        call add_stmt_to_graph($childStmt);
    end
       
    if ($stmt.else != null)
    {
        add_edges(level[curr_level_index-1].lastNode + ':else',
            level[curr_level_index].firstNode);
        add_edges(level[curr_level_index].lastNode,
            level[curr_level_index-1].lastNode + ':else');
        decrease_curr_level_index();
    }
        
    
end
/********************************************/
aspectdef addNode_ReturnStmt
    input   $stmt   end

    var label = 'Return';
    add_node('ReturnStmt',label);
end

/********************************************/
aspectdef addNode_DeclStmt
    input   $stmt   end

    var label = '{ '+ $stmt.astName + ' #'+ $stmt.line + '}';
    add_node('DeclStmt',label);
    
end
/********************************************/
aspectdef addNode_ExprStmt
    input   $stmt   end

    call FuncCalls : checkforFuncCalls($stmt);
    if(FuncCalls.calls.length > 0)
    {
        var label = '<<table border=\"0\">';
        for (var func_name of FuncCalls.calls)
        {
            var find_obj = SearchStruct(function_list,'Name',func_name);
            var color = find_obj[0]['color'];
            label += '<TR><TD BGCOLOR=\"' + color +'\">'+
                    func_name + '</TD></TR>';
        }
        label += '</table>>';
        add_node('FuncCall',label, FuncCalls.calls);
    }
    else
    {
        var label = '{ '+ $stmt.astName + ' #'+ $stmt.line + '}';
        add_node('ExprStmt',label);
    }
    
end
/********************************************/
aspectdef addNode_ForStmt
    input   $stmt   end

    increase_curr_level_index($stmt.astName);
    
    var label = '<loop_body> loop | <loop>' + $stmt.kind + 
            ' For #'+ $stmt.line + '|<else> else';
    
    add_node($stmt.astName,$stmt.kind.toUpperCase() + ' #' + $stmt.line );
    
    if (level[curr_level_index-1].lastNode != -1)
        add_edges(level[curr_level_index-1].lastNode, curr_node_ID);
    else
        level[curr_level_index-1].firstNode = curr_node_ID;

    level[curr_level_index-1].lastNode = curr_node_ID;

    select $stmt.body.childStmt end
    apply
        call add_stmt_to_graph($childStmt);
    end
    
    add_edges(level[curr_level_index].lastNode,
                level[curr_level_index].firstNode);
    decrease_curr_level_index();

end

/********************************************/
aspectdef checkforFuncCalls
    input   $stmt   end
    output calls end
    
    this.calls = [];
    select $stmt.call end
    apply
        if (SearchStruct(function_list,'Name',$call.name).length == 1)
        {
            this.calls.push($call.name);
        }
    end    
end
/* >>>>>>>>>>>>>>>>  Graph Functions <<<<<<<<<<<<<<<<<<<<<<*/
function add_node(type,label,func_calls)
{
    func_calls = (typeof func_calls !== 'undefined') ?  func_calls : [];
    var add_edge_opt = (typeof add_edge_opt !== 'undefined') ?  add_edge_opt : null;

    var node_ID = ++curr_node_ID;
    
    var node_obj = {
        Node_ID : node_ID,
        Type : type,
        Label : label,
        Func_Calls : func_calls,
        Shape : return_node_shape(type),
    };
    Nodes.push(node_obj);
    
    sub_graph += 'ID' + node_ID + ' ';
    
    if (level[curr_level_index].firstNode == -1)
    {
        level[curr_level_index].firstNode = curr_node_ID;
    }
    else
    {
        add_edges(level[curr_level_index].lastNode,curr_node_ID);
    }
    level[curr_level_index].lastNode = curr_node_ID;
    level[curr_level_index].Nodes.push(curr_node_ID);
}

function add_func_calls_dependency()
{
    for(var obj of Nodes)
    for(var func_call_Name of obj['Func_Calls'])
    {
        var func_call_node = SearchStruct(function_list,'Name',func_call_Name);
        obj['Child_Node_ID'].push(func_call_node[0]['node_ID']);
    }
    
}

function return_node_shape(type)
{
    switch (type)
    {
       case 'function':
           return ',shape = Msquare ';
       case 'FuncCall':
           return ',shape = Mrecord';
       case 'ForStmt':
           return ',shape = doubleoctagon';
       case 'WhileStmt':
           return ',shape = doubleoctagon';
       case 'DeclStmt':
           return '';
       case 'ExprStmt':
           return '';
       case 'IfStmt':
           return ',shape = Mrecord';
       case 'ReturnStmt':
           return ',shape = Msquare ';
       default:
           println('return_node_shape ERROR : type '+ type + 
                    ' not defined . . .');
    }    
}

function SearchStruct(structObj, filedName, value )
{
    return structObj.filter(function( obj ){return obj[filedName] ==value;});
}

function update_node(node_ID,filedName, value)
{
    var obj = SearchStruct(Nodes,'Node_ID',node_ID);
    if (obj.length == 1)
    {
        if (filedName=='Child_Node_ID')
            obj[0][filedName].push(value);
        else
            obj[0][filedName] = value;
    }
    else if (obj.length > 1)
        println('update_node func :ERROR . . . multiple node' +
         ' with same Node_ID = ' + node_ID + ' exist !!!');
    else
        println('update_node func : ERROR . . . Node_ID = '+ 
            node_ID +' not exist !!!');
}

function return_node_attribute(node_ID,filedName)
{
    var obj = SearchStruct(Nodes,'Node_ID',node_ID);
    if (obj.length == 1)
    {
        return obj[0][filedName];
    }
    else if (obj.length > 1)
        println('return_node_attribute func :ERROR . . . multiple node' +
         ' with same Node_ID = ' + node_ID + ' exist !!!');
    else
        println('return_node_attribute func : ERROR . . . Node_ID = '+ 
            node_ID +' not exist !!!');

}




function print_DOT()
{
    println('digraph my_DOT{');
    // print nodes attributes
    //println('\t node [margin=0,width=0,height=0,shape=record]');
    println('\t node [shape=record]');
    for(var obj of Nodes)
    {
        var str = '\t\tID' + obj['Node_ID'];
        
        if (obj['Type'] != 'FuncCall')
            str += ' [label=\"' + Format.escape(obj['Label']) + '\"';
        else
            str += ' [label=' + obj['Label'];
            
        str +=  obj['Shape'] ;
        
        if (obj['Type'] == 'function')
        {
            var find_obj = SearchStruct(function_list,'Name',obj['Label']);
            str += ',style=filled, fillcolor=\"'+ find_obj[0]['color']+'\"';
        }

        str += ']';
        println(str);
    }
    
    println();
    // print data dependency between nodes
    /*
    for(var obj of Nodes)
    {
        var child_num = obj['Child_Node_ID'].length;
        if (child_num> 0)
        for( var child_id of obj['Child_Node_ID'])
        {
            var str = '\t\tID' + obj['Node_ID'];
            str += '->ID' + child_id;
            println(str);
        }
    }
    */
    for(var edge of Edges)
    {
        println('\t\t' + edge);
    }
    
    
    println('\n');
    // classified nodes into subgraphs
    println(sub_graph);
    /*
    for(var i=0;i<=sub_graph_index;i++ )
    {
        println('\tsubgraph cluster_' + i + '{');
        println('\t\t' + sub_graph[i].toString() + ';');
        println('\t}');
    }
    */
    
    println('}');
}