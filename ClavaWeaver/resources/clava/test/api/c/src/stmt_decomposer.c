void bar1() {
	// Do nothing
}

void bar2(int a) {
	// Do nothing
}

int bar3() {
	return 0;
}

int foo(int a) {
	int b=1, c=2;
	
	switch(a) {
		// In C, declarations are now allowed after labels
		case 0:
			c = a + b * 2;
			break;
		default:
			c = 0;
	}
	
	
	some_label:
		c = a * b / 2;

	
	// Call to void function
	bar1();
	
	// Call to void function that contains an expression as arguments
	bar2(1+2+3);	

	// Call to function that does not return void, but does not use result
	bar3();
	
	return a + c;
}

