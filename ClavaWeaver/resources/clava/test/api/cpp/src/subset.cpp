#include <string.h>
#include <fstream>

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
