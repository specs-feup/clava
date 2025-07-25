// Example taken from http://en.cppreference.com/w/cpp/language/namespace_alias

#include <iostream>
 
namespace foo {
    namespace bar {
         namespace baz {
             int qux = 42;
         }
    }
}
 
namespace fbz = foo::bar::baz;
namespace f = foo;
 
int main()
{
    std::cout << fbz::qux << '\n';
}