struct Base
{
    virtual int g();
    virtual ~Base() {}
};
 
struct A : Base
{
    // OK: declares three member virtual functions, two of them pure
    virtual int f() = 0, g() override = 0, h();
 
    // OK: destructor can be pure too
    ~A() = 0;
 
};

class B : A
{
    virtual int b() = 0;
	virtual int h() override = 0;
};

class IObserver {
	public:
		virtual ~IObserver(){};
		virtual void Update() = 0;
};

class ISubject {
	public:
		virtual ~ISubject(){};
		virtual void Attach(IObserver *observer) = 0;
		virtual void Detach(IObserver *observer) = 0;
		virtual void Notify() = 0;
};