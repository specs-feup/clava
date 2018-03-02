#include <typeinfo>


int main() {



	// TypeOf
	int x1;         /* Plain old int variable. */
	typeof(x1) y;   /* Same type as x. Plain old int variable. */	

	int *px;
	typeof(*px) y2;
	
	// TypeId
	//auto x2 = typeid(int).name();
	
	return 0;
}