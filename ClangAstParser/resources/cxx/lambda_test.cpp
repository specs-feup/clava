// Example code taken from http://en.cppreference.com/w/cpp/language/lambda

#include <vector>
#include <algorithm>



/*
struct X {
    int x, y;
    int operator()(int);
    void f()
    {
        // the context of the following lambda is the member function X::f
        [=]()->int
        {
            return operator()(this->x + y); // X::operator()(this->x + (*this).y)
                                            // this has type X*
        };
    }
};
*/

void func3(std::vector<int>& v) {
  std::for_each(v.begin(), v.end(), [](int) { /* do something here*/ });
}

int main() {
	
	auto x1 = []() { return 0; };
auto x2 = [&](int x) { return x + 1; };
auto x3 = [](int x) -> int { return x; };
auto x4 = [&]() { return [&]() { return 0; }; };
auto x5 = [this]() { return this->x; };
auto x6 = [a, &b]() { return a + b; };
auto x7 = [=]() { return 0; };
auto x8 = [=, &a]() { return 0; };
	
	/*
	// generic lambda, operator() is a template with two parameters
	auto glambda = [](auto a, auto&& b) { return a < b; };
	bool b = glambda(3, 3.14); // ok
	
	// generic lambda, operator() is a template with one parameter
	auto vglambda = [](auto printer) {
		return [=](auto&&... ts) // generic lambda, ts is a parameter pack
		{ 
			printer(std::forward<decltype(ts)>(ts)...);
			return [=] { printer(ts...); }; // nullary lambda (takes no parameters)
		};
	};
	auto p = vglambda([](auto v1, auto v2, auto v3) { std::cout << v1 << v2 << v3; });
	auto q = p(1, 'a', 3.14); // outputs 1a3.14
	q();                      // outputs 1a3.14
	*/
}