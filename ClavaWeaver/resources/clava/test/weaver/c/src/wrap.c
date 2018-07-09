#include "wrap.h"
#include <stdio.h>

int foo(int a) {
	return a + 1;
}

int fooNoDecl(int a) {
	return a + 10;
}

void voidFoo(int *a) {
	(*a)++;
}


int main() {


	foo(10);
	fooNoDecl(20);

	int a = 100;
	voidFoo(&a);
	
	printf("Hello");
}