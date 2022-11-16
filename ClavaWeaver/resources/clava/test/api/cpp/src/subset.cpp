#include <string.h>
#include <fstream>
#include <vector>

void strcmpTest() {
	if (strcmp("a", "b") == 0) {
		return;
	}
}

void fstreamTest(const char *fn) {
  std::fstream f(fn, std::fstream::in);

  while (f.good())
  {
    return;
  }	
}

void vectorSizeTest(std::vector<float> &v) {	
  for (int i = 0; i < v.size(); ++i) {
  }			
}
