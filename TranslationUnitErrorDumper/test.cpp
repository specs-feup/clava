//rr
void sttrructt(){
	struct ooo omoo = {1,2,3};
	struct ooo2 omoo2 = {1,2,3};
	//e
	/*ee*/
	ooo3 omoo3 = {1,2,3};
}

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
}*/

bool f4() {
	//tests with binary operators
	Aww a = Aww();
	Bww b = Bww();
	a.f();
	a += b;
	a = a + b;
	Dww d;
	a = d.dd;
	Cww c;
	c += d;
	return a == b;
}

A f5() {
	vvvv = ee + 4;
	return vvvv;
}
UJ f8(Aee ee){
	switch (ee){
		case EE:
			return 0;
		default:
			return 1;
	}
}
/*
int f6()
{
	return m[0][0];
}

int f7(Matrix m)
{
	return m[0][0];
}
*/
