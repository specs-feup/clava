#include "templates.h"

#include <iostream>
#include <list>


struct MAtom {
	int a;
};

std::list<MAtom>::iterator it_atom1;

template <typename t = float>
inline t Distance2(const t a[3], const t b[3])
{
	return (a[0] - b[0]) * (a[0] - b[0]) +
				 (a[1] - b[1]) * (a[1] - b[1]) +
				 (a[2] - b[2]) * (a[2] - b[2]);
}

// TODO: TypeParser not supporting some nodes
/*
template <template <typename> class m>
struct Monad {
    template <typename a>
    static m<a> mreturn(const a&);

    template <typename a, typename b>
    static m<b> mbind(const m<a>&, m<b>(*)(const a&));
};
*/

// TODO: Add support for code in the page http://en.cppreference.com/w/cpp/language/function_template
/*
template<typename T>
void f(T s)
{
    std::cout << s << '\n';
}
 
template void f<double>(double); // instantiates f<double>(double)
template void f<>(char); // instantiates f<char>(char), template argument deduced
template void f(int); // instantiates f<int>(int), template argument deduced
*/

int main() {
		
	return 0;
}