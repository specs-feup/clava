#include <string>
#include <iostream>

class Empty {
	~Empty();
};

class ExplicitSeparate {
public:
	~ExplicitSeparate();
};

ExplicitSeparate::~ExplicitSeparate() {}

class PublicEmpty {
public:
	~PublicEmpty();
};

class EmptyDefault {
	~EmptyDefault() = default;
};

class Int {
	int x;
	~Int();
};

class IntDefault {
	int x;
	~IntDefault() = default;
};

class Complicated {
	std::string str;
	~Complicated() = default;
};

class ComplicatedExplicit {
	std::string str;
	~ComplicatedExplicit();
};

ComplicatedExplicit::~ComplicatedExplicit() {}

class Custom {
	~Custom() {
		std::cout << "delete" << std::endl;
	}
};

class Custom2 {
	~Custom2() {
		throw "abc";
	}
};

class Deleted {
	~Deleted() = delete;
};

class Virtual {
public:
	virtual ~Virtual() = 0;
};

class Virtual2 {
public:
	virtual ~Virtual2();
};
/*
class Override : public Virtual {
	~Override() override {}
};*/
