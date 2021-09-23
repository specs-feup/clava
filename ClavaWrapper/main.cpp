#include <iostream>
#include <sstream>
#include <iomanip>
#include <stdlib.h>
#include <unistd.h>
#include <libgearman/gearman.h>

std::string escape_json(const std::string &s) {
    std::ostringstream o;
    for (auto c = s.cbegin(); c != s.cend(); c++) {
        if (*c == '"' || *c == '\\' || ('\x00' <= *c && *c <= '\x1f')) {
            o << "\\u"
              << std::hex << std::setw(4) << std::setfill('0') << (int)*c;
        } else {
            o << *c;
        }
    }
    return o.str();
}

int main(int argc, char** argv) {

    gearman_client_st *client= gearman_client_create(NULL);

    gearman_return_t ret= gearman_client_add_server(client, "localhost", 4830);
    if (gearman_failed(ret))
    {
        return 1;
    }

    gearman_argument_t value= gearman_argument_make(0, 0, full_path, strlen(full_path));

    gearman_task_st *task= gearman_execute(client,
                                           "mb", strlen("mb"),  // function
                                           NULL, 0,  // no un
                                           NULL,
                                           &value, 0);

    if (task == NULL) // If gearman_execute() can return NULL on error
    {
        std::cerr << "Error: " << gearman_client_error(client) << std::endl;
        gearman_client_free(client);
        return 1;
    }

    // Make sure the task was run successfully
    if (gearman_success(gearman_task_return(task)))
    {
        // Make use of value
        gearman_result_st *result= gearman_task_result(task);
        std::cout << "cycles: " << gearman_result_value(result) << std::endl;
    }

    gearman_client_free(client);

    free(full_path);
    //std::cout <<  "Hello, World!" << std::endl;
    return 0;

    // Try to connect to Clava server
    
    // If failed, try launching it
    
    // If failed again, quit



    // Get current folder

    const int MAX_PATH_LEGTH = 4096;
    char currentExe[MAX_PATH_LEGTH];

    readlink("/proc/self/exe", currentExe, sizeof(currentExe));
    
    std::string currentExeStr(currentExe);
    std::string currentFolder = currentExeStr.substr(0, currentExeStr.find_last_of("/"));
    ///proc/self/exe 
    
    // Build command
    std::string cmd = currentFolder;
    cmd += "/clava-server";
    
    // Create json with arguments
    std::string jsonArgs("[");
    for(int i=1; i<argc; i++) {
    	std::string arg(argv[i]);
    	if(i != 1) {
        	jsonArgs += ", ";	
    	}

	jsonArgs += "'";
        jsonArgs += escape_json(arg);
	jsonArgs += "'";        
    }
    jsonArgs += "]";
    
    //system("");
    std::cout << "Command: " << jsonArgs << std::endl;
    std::cout << "Hello, World!" << std::endl;
    return 0;
}
