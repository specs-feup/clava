#include "clone.h"
#include <iostream>

void foo_in_header() {
	std::cout << "foo_in_header" << std::endl;
}

void test::TestClass::foobar() {
    return;
}

int foo(int ne);

int foo(int ne = 0)  {
    return ne + 2;
}

int bar(int ne = 0) try {
    return ne + 2;
} catch (const std::exception& e) {
    return 0;
}

int main() {

    std::cout << foo(4) << std::endl;

    return 0;
}
