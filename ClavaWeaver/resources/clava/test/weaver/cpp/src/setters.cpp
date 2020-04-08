#include <chrono>

void testSetQualifiedName() {
	auto a = std::chrono::high_resolution_clock::now();
}

void testIf() {
	int a = 0;
	if(a == 0) {
		a = 1;
	} else {
		a = 2;
	}

}

int testFunctionType(double a) {
	return 0;
}

int main() {
	return 0;
}
