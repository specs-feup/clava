#include "constructor.h"

#include <string>

struct X {
	X(int i);
};
struct Y {
	explicit Y(int i);
};

int main() {
	X x1 = 1;
	X x2 = X(2);
	Y y = Y(3);
	
  std::string s0 ("Initial string");
	
  // constructors for std::string
  std::string s1;
  std::string s2 (s0);
  std::string s3 (s0, 8, 3);
  std::string s4 ("A character sequence");
  std::string s5 ("Another character sequence", 12);
  std::string s6a (10, 'x');
  std::string s6b (10, 42);      // 42 is the ASCII code for '*'
  std::string s7 (s0.begin(), s0.begin()+7);
  
}