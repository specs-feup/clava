#include <iostream>
//#include <limits>

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
	//double a7 = std::numeric_limits<double>::infinity();
	// Since C++14, current parser does not correctly support single quote
	double singleQuotes = 231.523'5;
	double singleQuotes2 = 2'3'1.5;

	auto y = 1.0_a+2.0;
	auto z = 1.0_E +2.0;
	auto u = 1_p +2;
	
	// Constants from NAS benchmark
	double xcrref[5];
	xcrref[0] = 0.1125590409344e+03;
    xcrref[1] = 0.1180007595731e+02;
    xcrref[2] = 0.2710329767846e+02;
    xcrref[3] = 0.2469174937669e+02;
    xcrref[4] = 0.2638427874317e+03;
	
	double xceref[5];
    xceref[0] = 0.4419655736008e+01;
    xceref[1] = 0.4638531260002e+00;
    xceref[2] = 0.1011551749967e+01;
    xceref[3] = 0.9235878729944e+00;
    xceref[4] = 0.1018045837718e+02;

	auto stringLiteral = "A string literal with escape characters: \n \t";
	auto wideStringLiteral = L"Wide literal: ç";
	auto utf8StringLiteral = u8"UTF-8 literal: ç";
	auto utf16StringLiteral = u"UTF-16 literal: ç";
	auto rawStringLiteral = R"(splot "-" u 1:2:3 with points ls 3 title "Ligand"\)";
	auto rawWideStringLiteral = LR"("Wide" Raw String)";
	auto rawUtf8StringLiteral = u8R"("UTF-8" Raw String)";
	auto rawUtf16StringLiteral = uR"("UTF-16" Raw String)";
	//auto rawUtf32StringLiteral = UR"("UTF-32" Raw String)";
	return 0;
}