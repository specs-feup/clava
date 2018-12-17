#include <math.h>


void fooStmtBeforeAfter() {
	int a = 0;
}

void fooStmtReplace() {
	int a = 0;
}

void fooBodyBeforeAfter() {
	int a = 0;
}

void fooBodyReplace() {
	int a = 0;
	int b = 1;
}

void fooBodyEmptyBeforeAfter() {
}

void fooBodyEmptyReplace() {
}

void fooCallBeforeAfter() {
	double a = sqrt(10.0);
}

void fooCallReplace() {
	double a = sqrt(20.0);
}

void fooBeforeAfter() {
}

void fooReplace() {
}

void callsInsideFor() {
	int a = 0;
	for(int i=sqrt(4); i<sqrt(20); i++) {
		a += sqrt(i);
	}
}


int main() {
	return 0;
}