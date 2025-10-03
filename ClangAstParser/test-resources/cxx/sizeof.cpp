/** Example based on http://en.cppreference.com/w/cpp/language/sizeof **/

#include <iostream>
 
struct Empty {};
struct Base { int a; };
struct Derived : Base { int b; };
struct Bit { unsigned bit: 1; };
 
int main()
{
    Empty e;
    Derived d;
    Base& b = d;
    Bit bit;
    int a[10];

	// empty class
	//sizeof e;
	
    std::cout << "size of empty class: "              << sizeof e          << '\n';
    std::cout << "size of pointer : "                 << sizeof &e         << '\n';
//            << "size of function: "                 << sizeof(void())    << '\n'  // error
//            << "size of incomplete type: "          << sizeof(int[])     << '\n'  // error
//            << "size of bit field: "                << sizeof bit.bit    << '\n'  // error
    std::cout << "size of array of 10 int: "          << sizeof(int[10])   << '\n';
    std::cout << "size of array of 10 int (2): "      << sizeof a          << '\n';
    std::cout << "length of array of 10 int: "        << ((sizeof a) / (sizeof *a)) << '\n';
    std::cout << "length of array of 10 int (2): "    << ((sizeof a) / (sizeof a[0])) << '\n';
    std::cout << "size of the Derived: "              << sizeof d          << '\n';
    std::cout << "size of the Derived through Base: " << sizeof b          << '\n';
 }