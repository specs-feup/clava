#include "mpi_scatter_gather.h"

void bar() {
	// Do something parallel
}


void foo() {
	
	for(int i=0; i<1000; i++) {
        bar();
    }

}


int main() {
	
	foo();
	
}