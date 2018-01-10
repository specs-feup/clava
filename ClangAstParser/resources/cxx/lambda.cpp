/*
class A {

	int x;

	void foo() {
		auto x5 = [this]() { return this->x; };
	}

};
*/
int main() {
	
	int a, b;
	
	auto x1 = []() { return 0; };
	auto x2 = [&](int x) { return x + 1; };
//	auto x3 = [](int x) -> int { return x; };
//	auto x4 = [&]() { return [&]() { return 0; }; };
//	auto x6 = [a, &b]() { return a + b; };
	auto x7 = [=]() { return 0; };
//	auto x8 = [=, &a]() { return 0; };

//	auto x9 = [x] -> decltype(x + x) { return x + x; };
//  auto x10 = [=]() mutable throw() -> int { return 0; }
//  auto x11 = [] { return 0; };
//  auto x12 = [val = 1] { return val; };
//  auto x13 = [&val, =] { return val; };
//  int x14 = [] { return 0; }(); // x is 0
//  auto x15 = []{};
//  auto x16 = [](auto arg1, auto arg2) { return arg1 + arg2; };
/*
auto x17 = [int y] { return y; };
auto x17_1 = new decltype(x17)(0);
std::cout << x17_1->operator()(); << std::endl;
*/
}