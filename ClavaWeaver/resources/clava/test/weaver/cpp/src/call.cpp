#include <stdio.h>

int foo(int a, int b) {
	return a + b;
}

class A {
	public:
	int foo2();
};

int A::foo2() {
	return -1;
}

class B {
	
	public:
	A a;
};

void functionDecl();

void functionDef() {
	printf("functionDef\n");
}

void functionDeclAndDef();

void functionDeclAndDef() {
	printf("functionDeclAndDef\n");
}

void functionDefAndDecl() {
	printf("functionDefAndDecl\n");
}

void functionDefAndDecl();


int main() {
	int x = 2 + 3;
	foo(1, 2);

	B b;
	b.a.foo2();
	
	functionDecl();
	functionDef();
	functionDeclAndDef();
	functionDefAndDecl();
	printf(".function test");
}