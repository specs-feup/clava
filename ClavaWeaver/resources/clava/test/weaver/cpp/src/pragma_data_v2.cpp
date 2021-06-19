double bar() {
    return 1.0;
}

double foo() {
    double a = 0;
    
	#pragma clava data a:20, b:"a string"
    for(int i=0; i<1000; i++) {
        a += bar();
    }
	
	for(int i=0; i<1000; i++) {
        a += bar();
    }
    
    return a;
}

void noData() {
	for(int i=0; i<0; i++) {
	}
}

int main() {
    
    foo();
	noData();
  
	return 0;

}