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
    return 0;
}