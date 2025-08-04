#include <stdbool.h>

int main() {

	const char c1[] = (const char []){"abc"}; 

	int *p1 = (int[]){2, 4}; // creates an unnamed static array of type int[2]
                        // initializes the array to the values {2, 4}
                        // creates pointer p to point at the first element of the array
	
}
