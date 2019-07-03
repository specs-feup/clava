void numStatements() {
	// A comment, does not count
	int a = 0, b = 0;
	
	#pragma does not count
	for(int i=0; i<10; i++) {
		a++;
	}
	
	if(a) {
		b = 1;
	} else {
		b = 2;
	}
}