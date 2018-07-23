#include "inline_utils.h"

int main() {
	int* a;
	*a = 3;
	
	// foo_no_return
	foo_no_return(a);
	// foo_no_return (x2)
	foo_no_return(a);
	
	int i;
	double* j;
	int k[3][2][2];

	// foo_with_args
	foo_with_args(i, j, k[1]);
	
	double array4d[2][3][5][5];
	
	// foo_with_array
	foo_with_array(array4d, i);

	
	// foo_with_return
	//foo_with_return(*a);
	
	// foo_with_return with call inside expression
	int returnTest = 2 + foo_with_return(3);
	
	// var shadowing
	int shadowingResult = foo_with_var_shadowing();
	
	// call function with array parameter using pointer argument
	foo_with_1darray_input((int *) k[0][0]);
	foo_with_2darray_input((int (*)[2]) k[0]);
	
	const double atom1[3] = {1, 2, 3};
	const double atom2[3] = {4, 5, 6};
	double distResult = Distance2(atom1, atom2);
	
	int x_2 = 3;
	double v[2][1][x_2][5];
	double x_result = inputInCast(x_2, 1, v);
}