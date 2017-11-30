#include <stdbool.h>

bool fooBool();

bool fooBool() {
	return true;
}

int main() {
	
	_Bool aBool = 1;

	bool a = fooBool();
	
	return 0;
}