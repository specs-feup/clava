#include <new>
#include <vector>

class Box {
};


int main() {

	// allocates memory for a primitive typedef
	double * pvalue = new double;

	// allocates memory for a primitive typedef, with nothrow
	double * pvalueNoThrow = new (std::nothrow) double;
	
	// allocates memory for a class
    Box * box = new Box;
	
	// allocates memory for a class, with nothrow
    Box * box2 = new (std::nothrow) Box;
	
	// allocates and constructs five objects:
	double * p1 = new double[5];
	
	// allocates and constructs five objects (nothrow):
    std::vector<int> * p2 = new (std::nothrow) std::vector<int>[5];
	
	// primitive with constructor
	double *dc = new double(34.56);
	
	delete dc;
	delete[] p2;
    delete[] p1;
    delete box2;
    delete box;
    delete pvalueNoThrow;
    delete pvalue;
	
	
	return 0;
}