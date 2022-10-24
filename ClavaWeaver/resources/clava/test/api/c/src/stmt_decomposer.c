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

	
	return a + c;
}

