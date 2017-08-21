#include <iostream>

long double operator""_E(long double);
long double operator""_a(long double);
int operator""_p(unsigned long long);


int main() {
	
	std::cout << 123.456e-67 << '\n'
		<< .1E4f << '\n'
		<< 58. << '\n'
		<< 4e2 << '\n';
			
	double a0 = 123.4e-5-43.5+3;
	double a1 = 1e10+1e-5L;
	double a2 = 1.-1.e-2;
	double a3 = 3.14+.1f-0.1e-1L;
	double a4 = 0x1ffp10-0X0p-1;
	double a5 = 0x1.p0-0xf.p-1;
	double a6 = 0x0.123p-1-0xa.bp10l;
	// Since C++14
	double singleQuotes = 231.523'5;

	auto y = 1.0_a+2.0;
	auto z = 1.0_E +2.0;
	auto u = 1_p +2;
	
	return 0;
}