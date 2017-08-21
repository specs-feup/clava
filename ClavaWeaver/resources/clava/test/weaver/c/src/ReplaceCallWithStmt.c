int foo() {	
	return 2;
}

float* fooPointer(float* aPointer) {
	return aPointer;
}

typedef double aType;

aType fooTypedef() {
	return 2.0;
}

int main() {
	float* aFloatPointer;
	
	foo();
	fooPointer(aFloatPointer);
	fooTypedef();
}