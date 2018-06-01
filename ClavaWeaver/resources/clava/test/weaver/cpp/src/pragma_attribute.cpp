int foo(int a);
int foo2(int a);

int main() {
	
	int acc = 0;
	
	_Pragma("clava attribute init(int i=(10)) isParallel")
	for(int i=0; i<10; i++) {
		#pragma clava attribute select(call) name(foo2)
		#pragma clava attribute select(call.arg)
		acc += foo(20);
	}
}
