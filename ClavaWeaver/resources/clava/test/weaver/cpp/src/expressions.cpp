#include "classA.h"

class temp{
public:
  int id;
  char c;
};

struct testStruct{
    int a;
    double d;
};

double analyse(){
    struct testStruct ts = {1,2.0};
    ts.a = 20;
    ts.d = 12.2;
    
    ts.a = 20;
    (&ts)->a = 20;
    
    struct testStruct * ts2;
    ts2->a = 20;
    (*ts2).a = 20;
    
    int a;
    a = 12;
    temp classVar;
    classVar.id = 1;
    
    
    int x, y;
    
    x = 20;
    y = x + 2;
	
	return 0.0;
}

double bar() {
    return 1.0;
}

double foo() {
    double a = 0;
    
    for(int i=0; i<1000; i++) {
        a += bar();
    }
    
    return a;
}

int main() {
    
    foo();
	
	// New expressions
	A *a1 = new A();
	A *a2 = new A(20);
	A *a3 = new A[3];
	
	// Delete expressions
	delete a1;
	delete a2;
	delete[] a3;
	
    return 0;
}