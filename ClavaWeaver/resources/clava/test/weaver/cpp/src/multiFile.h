#include <vector>

struct A {
	int x;
	int y;
};

template< typename T >
void template_foo(const std::vector<T>& A);

template< typename T >
void template_foo(const std::vector<T>& A) {
    
}

template< typename T >
void template_foo_2(const std::vector<T>& A);

template< typename T >
void template_foo_2(const std::vector<T>& A) {
    
}


template< typename T >
void template_foo_3(const std::vector<T>& A);

template< typename T >
void template_foo_3(const std::vector<T>& A) {
    
}