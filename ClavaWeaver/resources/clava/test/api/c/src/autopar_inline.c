int bar() {
	int a = 0;

	for(int i=0; i<10; i++) {
		a++;
	}	
	
	return a;
}

int foo() {

	int a = 0;

	for(int i=0; i<10; i++) {
		// Variable that may already after inline renaming
		int i_1 = 1;
		
		a+= bar();
	}
	
	return a;
}


