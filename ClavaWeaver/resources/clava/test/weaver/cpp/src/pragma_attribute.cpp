int foo();
int foo2();

int main() {
	
	int acc = 0;
	
	#pragma clava attribute initValue((10)) isParallel
	for(int i=0; i<10; i++) {
		#pragma clava attribute select(call) name(foo2)
		acc += foo();
	}
}
