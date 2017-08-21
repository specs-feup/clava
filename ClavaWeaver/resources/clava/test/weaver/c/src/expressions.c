typedef enum { false, true } logical;

logical timeron;

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
    
    
    int x, y;
    
    x = 20;
    y = x + 2;
}

int main() {
	
	timeron = true;
	
}