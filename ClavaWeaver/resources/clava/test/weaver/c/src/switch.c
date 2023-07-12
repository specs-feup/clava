int foo1(int a) {
	
	
	switch(a) {
		case 0:
		return 1;
		case 1:
		return 2;
		default:
		return 3;
	}
	
	
}

int foo2(int a) {
	
	switch(a) {
		case 0:
		case 1 ... 2:
			a = 0;
			return 1;	
	}
	
}