// Based on the challenges in https://github.com/trailofbits/cb-multios/

#include <stdlib.h>

void bitBlaster() {
	((int (*)())0)();
	((int (*)())NULL)();
	((int (*)())nullptr)();	
	((int (*)())(3 + 4 - 7))();	
}

