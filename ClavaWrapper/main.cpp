#include <iostream>
#include <sstream>
#include <iomanip>
#include <stdlib.h>
#include <unistd.h>
#include <libgearman/gearman.h>
#include <string.h>

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

std::string json_to_string(const std::string &s) {
    std::ostringstream o;
    for (auto c = s.cbegin(); c != s.cend(); c++) {
    	if(*c == '\\') {
    	    c++;
    	    if(*c == 'n') {
    	    	 o << '\n';
    	    } 
    	    else if(*c == 'u') {
    	         std::stringstream ss;
    	         ss << std::hex; 
    	         for(int i=0; i<4; i++) {
    	             c++;
                     ss << *c;
    	         }
    	         
    	         unsigned int unicodeChar;
    	         ss >> unicodeChar;
    	         o << (unsigned char) unicodeChar;
    	    }
    	    else {
    	        o << '\\';
    	        o << *c;
    	    }
    	} else {
    	     o << *c;
    	}     
    }
    
    return o.str();
}

void launchServer() {
    // Get current folder

    const int MAX_PATH_LEGTH = 4096;
    char currentExe[MAX_PATH_LEGTH];

    readlink("/proc/self/exe", currentExe, sizeof(currentExe));
    
    std::string currentExeStr(currentExe);
    std::string currentFolder = currentExeStr.substr(0, currentExeStr.find_last_of("/"));
    ///proc/self/exe 
    
    // Build command
    std::string cmd = currentFolder;
    cmd += "/clava -server &";
    std::cout << "Launching " << cmd << std::endl;
    system(&cmd[0]);
}

int main(int argc, char** argv) {

    // To check if the server is running
    // Taken from here: https://stackoverflow.com/a/13230341
    //(echo status ; sleep 0.1) | nc 127.0.0.1 4730 -w 1

    //launchServer();

    gearman_client_st *client= gearman_client_create(NULL);

    // Connect to server
    gearman_return_t ret = gearman_client_add_server(client, "localhost", 4733);
    if (gearman_failed(ret))
    {
        return -1;
    }




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
    
    //std::cout << "Command: " << jsonArgs << std::endl;
    

    gearman_argument_t value= gearman_argument_make(0, 0, &jsonArgs[0], strlen(&jsonArgs[0]));

    gearman_task_st *task= gearman_execute(client,
                                           "weaver", strlen("weaver"),  // function
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
        std::string resultStr(gearman_result_value(result));
        std::cout << json_to_string(resultStr) << std::endl;
    }

    gearman_client_free(client);


    return 0;
}
