#include <stdio.h>
#include <math.h>

int foo(int numIter) {

	int acc = 0;
	
    for(int i=0; i<numIter; i++) {
       acc++;
    }
    
	return acc;
}
