
void f() {

	A &a = A::e();
}
void f2() {
/*dependência cíclica*/
	A &a = A::e();
}

/*void f3() {
//dependência cíclica
	A a = A();
	B &b = a.create_b(); //no viable conversion
	A &a2 = b.create_a();
	a2.create_b();
}

bool f4() {
	A a = A();
	B b = B();
	return b==a; //invalid operands to binary expression
}*/
