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

    // Try to connect to clava server
    
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
