int foo() {
	return 1;
}

int main() {
	
	for(int i=0; i<10; i+=foo()) {
			
	}
	
	if(foo()) {
	}
	else if(foo()) {
	}


	return foo();
}