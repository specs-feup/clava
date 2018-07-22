#include "inline_utils.h"

void foo_no_return(int* output) {
	int a = 0;
	for(int i=0; i<*output; i++) {
		a += 1;
	}	
	
	*output = a;
}

void foo_with_args(int a, double* b, int c[2][2]) {
	a = 10;
	c[0][0] = 20;
	
	//foo_with_args2(c[1]);
	// c[1][0] = 1
}

void foo_with_args2(int array[2]) {
	array[0] = 1;
}

void foo_with_array(double array[][3][5][5], int size) {
	array[0][0][0][0] = size;	
}

int foo_with_return(int a) {
	return a + a;
}

int foo_with_multiple_returns(int a) {
	if(a >= 0) {
		return a;
	}
	
	return -a;
}


int foo_with_var_shadowing() {
	int i = 0;
	{
		int i = 1;
	}
	
	return i;
}

void foo_with_1darray_input(int array[]) {
	array[0] = 1;
}

void foo_with_2darray_input(int array[2][2]) {
	array[0][0] = 1;
}

double Distance2(const double a[3], const double b[3])
{
  return (a[0] - b[0]) * (a[0] - b[0]) +
         (a[1] - b[1]) * (a[1] - b[1]) +
         (a[2] - b[2]) * (a[2] - b[2]);
}

double inputInCast(int x, int k, double v[][1][x][5]) {
	double (*vx)[x][5] = v[k];
	
	return vx[0][0][0];
}