#include <vector>

class Box {
   
};

int main() {

	std::vector<int> t = std::vector<int> {};
   	t = {};
	std::vector<int> v = {};
	Box b = {};

	
	
	// allocates memory for a class
   Box * box = new Box;
   // allocates memory for a class, with nothrow
   Box * box2 = new (std::nothrow) Box;
   // allocates and constructs five objects (nothrow):
   std::vector<int> * p2 = new (std::nothrow) std::vector<int>[5];
	
	return 0;
}

