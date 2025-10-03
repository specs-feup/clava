int main() {
	int test = 10;
	
	// Simple while
	while(test) {
		test--;
	}
	
	// Simple empty while
	while(test) {
		
	}
		
	// Declaration inside while
	while(int a = test) {
		test--;
	}
	
	// Declaration inside empty while
	while(int a = test) {

	}
	
	while(true);
	
	int i = 2;
    do { // compound statement is the loop body
		i++;
	} while (i < 9);
	
	i = 2;
    do { 
	} while (i++ < 9);
	
	do; while(true);
	
	return 0;
}