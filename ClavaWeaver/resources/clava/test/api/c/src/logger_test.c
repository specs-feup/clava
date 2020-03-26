double bar() {
    return 1.0;
}

double foo() {
    double a = 0;
    
    for(int i=0; i<1000; i++) {
        a += bar();
    }
    
    return a;
    // Comment after return
}

void testAppend() {
	long int aLong;
	long long aLongLong;
	
	int a;
}

void testAppendJp() {
	int a, b, c = 0;
	
	c = a + b;
}

int main() {
    
    foo();

}