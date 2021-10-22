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

void noData() {
	for(int i=0; i<0; i++) {
	}
}

void insertPreservesPragma() {

	#pragma clava data a:30
	for(int i=0; i<0; i++) {
	}
}

#pragma another pragma
#pragma clava data a:40
// Hello
#pragma yet other pragma
void insertPreservesPragma2() {
	
}

void insertWithoutPragma() {
	for(int i=0; i<0; i++) {
	
	}
}

void updatePragma() {

	#pragma clava data a:100
	for(int i=0; i<0; i++) {
	
	}
}


int main() {
    
    foo();
	noData();
  
	return 0;

}