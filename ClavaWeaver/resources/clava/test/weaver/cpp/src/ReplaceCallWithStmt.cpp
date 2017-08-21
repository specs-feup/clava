#include <vector>
#include <string>

std::vector<std::string> foo() {
	std::vector<std::string> aString;
	
	return aString;
}

const std::string& fooRef(const std::string& aString) {	
	return aString;
}

int main() {
	foo();
	fooRef("Hello");
}