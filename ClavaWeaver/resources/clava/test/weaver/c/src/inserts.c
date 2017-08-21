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


int main() {
	return 0;
}