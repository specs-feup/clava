#include <stdio.h>
#define N 1011


void foo(double a[N], double b[N], double c[N]) {
	
	{
		// Loop to parallelize
		#pragma foo_loop_once
		for(int i=0; i<N; i++) {
			c[i] = a[i] + b[i];
		}
		
		
		// Inside loop, potentially more than one execution
		for(int j=0; j<10; j++) {
			#pragma foo_loop_not_once
			for(int i=0; i<N; i++) {
				c[i] = a[i] + b[i];
			}
		}

		// Inside if/else, might not execute
		if(a[0] > 0) {
			#pragma foo_loop_not_once
			for(int i=0; i<N; i++) {
				c[i] = a[i] + b[i];
			}
		} else {
			#pragma foo_loop_not_once
			for(int i=0; i<N; i++) {
				c[i] = a[i] + b[i];
			}
		}
	}
	
}

int main(int argc, char **argv) {
		
	double a[N], b[N], c[N];
	
	for(int i=0; i<N; i++) {
		a[i] = i;
		b[i] = i + 1;
	}
	
	foo(a, b, c);
	
	// test output
	double acc = 0;
	for(int i=0; i<N; i++) {
		acc += c[i];
	}
	
	printf("Result: %f\n", acc);
	
	return 0;
}