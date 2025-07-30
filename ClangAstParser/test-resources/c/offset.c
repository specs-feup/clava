#include <stddef.h>

struct IndexInfo {
    long long timeCreated;
    signed char isDirectMapped;
    unsigned int numOfParts;
};

struct S {
  float f;
  double d;
};

struct T {
  int i;
  struct S s[10];
};


int main() {
	
	int offset = offsetof(struct T, s[2].d);
	int offset2 = offsetof(struct IndexInfo, timeCreated);
	
	return 0;
}