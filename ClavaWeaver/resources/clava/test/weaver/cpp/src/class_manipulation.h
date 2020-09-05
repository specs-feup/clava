

class originalClass {


public:

    void foo(int n);

};


class originalClass2 {


public:

    int foo2(int n);

};


class originalClass3 {

	typedef int intType;

public:

    int foo3(originalClass3::intType n);

};


class originalClass4 {

public:

	int a;

};