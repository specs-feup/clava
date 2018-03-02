#include <typeinfo>


int main() {



	// TypeOf
	int x1;         /* Plain old int variable. */
	typeof(x1) y;   /* Same type as x. Plain old int variable. */	

	int *px;
	typeof(*px) py;
	
	int* x2;
    __typeof__(*x2 + *x2) y2;
	
	// TypeId
	//auto x3 = typeid(int).name();
	
	return 0;
}