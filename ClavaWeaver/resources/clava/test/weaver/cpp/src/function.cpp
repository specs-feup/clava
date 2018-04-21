#include "function.h"

inline int Max(int x, int y) {
   return (x > y)? x : y;
}

int MaxNoInline(int x, int y) {
   return (x > y)? x : y;
}

int declOnly();

int defOnly() {
	return 1;
}

int declAndDef();

int declAndDef() {
	return 2;
}



void caller() {
	declOnly();
	defOnly();
	declAndDef();
	
	A a;
	a.declOnly();
}
