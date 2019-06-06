int foo() {
	return 1;
}

int foo2() {
	return 1;
}

int foo3(int a) {
	return a;
}

int main() {
	
	for(int i=0; i<10; i+=foo()) {
			
	}
	
	if(foo()) {
	}
	else if(foo()) {
	}

	if(foo() || foo2()) {
	}

	foo3(foo());

	return foo();
}