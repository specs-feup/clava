#include <iostream>
#include <vector>
#include <stdexcept>
 
struct A {
    int n;
    A(int n = 0): n(n) { 
		std::cout << "A(" << n << ") constructed successfully\n"; 
	}
    
	~A() {
		std::cout << "A(" << n << ") destroyed\n"; 
	}
};
 
int foo() {
    throw std::runtime_error("error");
}
 
struct B {
    A a1, a2, a3;
    B() try : a1(1), a2(foo()), a3(3) {
        std::cout << "B constructed successfully\n";
    } catch(...) {
    	std::cout << "B::B() exiting with exception\n";
    }
    ~B() { std::cout << "B destroyed\n"; }
};
 
struct C : A, B {
    C() try {
        std::cout << "C::C() completed successfully\n";
    } catch(...) {
        std::cout << "C::C() exiting with exception\n";
    }
    ~C() { std::cout << "C destroyed\n"; }
}; 
 
int main() {
    try {
        std::cout << "Throwing an integer exception...\n";
        throw 42;
    } catch (int i) {
        std::cout << " the integer exception was caught, with value: " << i << '\n';
    }
 
    try {
        std::cout << "Creating a vector of size 5... \n";
        std::vector<int> v(5);
        std::cout << "Accessing the 11th element of the vector...\n";
        std::cout << v.at(10); // vector::at() throws std::out_of_range
    } catch (const std::exception& e) { // caught by reference to base
        std::cout << " a standard exception was caught, with message '"
                  << e.what() << "'\n";
		throw;
    }
 
	try {
		
	} catch (const std::exception& e) {
		// will be executed if f() throws std::runtime_error
	} catch (const std::runtime_error& e) {
		// dead code!
	} catch (...) {
		
	}
}