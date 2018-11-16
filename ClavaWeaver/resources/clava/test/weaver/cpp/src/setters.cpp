#include <chrono>

void testSetQualifiedName() {
	auto a = std::chrono::high_resolution_clock::now();
}

int main() {
	return 0;
}
