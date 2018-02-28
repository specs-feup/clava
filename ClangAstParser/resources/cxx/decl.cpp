#include <vector>
#include <string>

int nullGlobal = NULL;
int nullGlobal2 = NULL + 1;

class A {

    void defaultInitDecl(int* a = nullptr);
	void defaultInitImpl(int* a);
	void defaultInitImpl(int* a, int* b = nullptr);
};

void A::defaultInitDecl(int* a) {}
void A::defaultInitImpl(int* a = nullptr) {}
void A::defaultInitImpl(int* a = nullptr, int* b) {}


void foo1(int a);
void foo1(int a = 2);
void foo1(int a) {}

std::vector<std::string> foo() {
	std::vector<std::string> aString;
	
	return aString;
}

const std::string& fooRef(const std::string& aString) {	
	return aString;
}

static int a;

int main() {
	
	
	int a;
	int b, c=0, d;
	/** Full comment */
	const int e=2, f=3;
	std::vector<int> v1, v2;

	{
		int aa, bb;
	}

	auto fooResult = foo();
    auto fooRefResult = fooRef("Hello");


	std::vector<int> t = std::vector<int> {};
   	t = std::vector<int> {};
	t = {0};
   	t = {};
	std::vector<int> v = {};

	
	return 0;
}