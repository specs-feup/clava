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

int main() {
	int x = 2 + 3;
	foo(1, 2);

	B b;
	b.a.foo2();
}