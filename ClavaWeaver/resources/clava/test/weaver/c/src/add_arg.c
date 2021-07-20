int foo();

int foo() {
	return 0;
}

int foo();

int bar(char* str);

int bar(char* str) {
	return 0;
}



int main() {
	
	foo();
	
	bar("bar");
	
	return 0;
}
