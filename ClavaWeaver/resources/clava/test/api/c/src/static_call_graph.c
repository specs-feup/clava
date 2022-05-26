void bar1() {

}

// Only declaration, no definition
void foo3();

void foo2() {
	foo3();
}

void foo1() {
	foo2();
	bar1();
}



void rec() {
	rec();
}

int main() {

	foo1();
	bar1();
	rec();
	foo1();
}