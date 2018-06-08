double bar() {
    return 1.0;
}

double foo() {
    double a = 0;
    
	#pragma clava firstPragma
	#pragma omp parallel for
	//#pragma clava data a:20, b:"a string", c:{d:10, e:function(){return 10;}}
	#pragma clava data a:20, b:"a string"
	#pragma clava lastPragma
    for(int i=0; i<1000; i++) {
        a += bar();
    }
    
    return a;
}

int main() {
    
    foo();
  
	return 0;

}