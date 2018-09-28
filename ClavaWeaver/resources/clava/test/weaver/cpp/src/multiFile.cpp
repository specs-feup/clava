#include <multiFile.h>


int main() {

	A a;

	std::vector<double> vector_double;
    std::vector<int> vector_int;
    
    template_foo(vector_double);
    template_foo(vector_int);
	
	template_foo_2(vector_double);
    template_foo_2(vector_int);
	
	template_foo_3(vector_double);
    template_foo_3(vector_int);
}