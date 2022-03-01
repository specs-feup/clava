void iterationsExpr() {
	
	// 10
	for(int a=0; a<10; a++) {
	}
	
	// 11
	for(int a=0; a<=10; a++) {
	}	
	
	// 9
	for(int a=1; a<10; a++) {
	}	

	// 11
	for(int a = 10; a >= 0; a--) {
	}		

	// 10
	for(int a = 0; 10 > a; a++) {
	}		
	
	// 11
	for(int a = 0; 10 >= a; a++) {
	}		

	// 10
	for(int a=0; (a) < 10; a++) {
	}
	
	// 6
	for(float a = 0.0; a < 10.0; a+=1.5) {
	}

	// 5
	for(int a=1; a<11; a+=2) {
	}
	
	// 6
	for(int a=1; a<=11; a+=2) {
	}

	// ((end - 1) - start + 1) / 1
	int start;
	int end;
	for(int a = end - 1; a >= start; a--) {
	}	

	// 10
	int a1;
	for(a1=0; a1<10; a1++) {
	}	
}


void headerInsert1() {
	for(int i=0; i<10; i++);
}

void headerInsert2() {
	int i;
	for(i=0; i<10; i++);
}

int main() {

	for(int a=0; a<10; a++) {
		for(int b=0; b<10; b++) {
			for(int c=0; c<10; c++) {
				
			}
		}
	}



	for(;;){
		
	}

	double a = 0.5;
	int i;
	
	for(i=0; i<10; i++) {
		a += 0.1;
	}

	for(int j=0; j<10; j++) {
		a += 0.1;
	}

	i = 0;
	for(; i<10; i++) {
		a += 0.1;
	}

	for(; ; i++) {
		a += 0.1;
	}

	i = 20;
	while(i > 10) {
		i--;
	}
	
	if(i > 10) {
		for(int a=0; a<10; a++) {
			for(int b=0; b<10; b++) {
			}
		}
	} else {
		for(int a=0; a<10; a++) {
			if(i < 5) {
				for(int b=0; b<10; b++) {
				}
			}

		}
	}
	
	
	
	return 0;
}