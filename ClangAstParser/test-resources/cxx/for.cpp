#include <vector>

int main() {
	
	int i;

	// Common for
	for(i=0; i<10; i++) {
	}
	
	// For with variable declaration
	for(int j=0; j<10; j++) {
	}
	
	// Empty for
	for(;;) {
	}
	
	// Only increment
	for(;;i=i+1) {
	}
	
	// Only initialization
	for(i=0;;) {
	}
	
	// Only condition
	for(;i<10;) {
	}
	
	// Init and inc
	for(i=0;;i++) {
	}
	
	// Init and cond
	for(i=0;i<10;) {
	}
	
	// Cond and inc
	for(;i<10;i++) {
	}
	
	for(;;);
	
	std::vector<int> intVector;
	for(auto element : intVector) {
		
	}
	
	for(int ii=0, jj=0; ii<10 && jj<10; ii++, jj++) {
        i+= ii + jj;
    }
	
	// Decl in cond
	for(int ii=0; int jj = ii + 1; ii++) {
        i+= ii;
    }
}