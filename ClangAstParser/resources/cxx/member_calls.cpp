#include <fstream> 
#include <string>

void foo(std::string fileName) {
	
	std::ifstream xclbinFile(fileName, std::ifstream::binary);
	xclbinFile.seekg(0, xclbinFile.end);
	size_t size = xclbinFile.tellg();    
	xclbinFile.seekg(0, xclbinFile.beg);
}
