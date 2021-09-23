#include <iostream>
#include <sstream>
#include <iomanip>
#include <stdlib.h>
#include <unistd.h>

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

int main() {

    const int MAX_PATH_LEGTH = 4096;
    char currentExe[MAX_PATH_LEGTH];

    readlink("/proc/self/exe");
    //system("");

    std::cout << "Hello, World!" << std::endl;
    return 0;
}
