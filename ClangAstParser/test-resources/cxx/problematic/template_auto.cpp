template <int X, int Y>
class product {
    public:
        static const int value = X + product<X, Y-1>::value;
};

template <int X>
class product<X, 0> {
    public:
        static const int value = 0;
};

int x = product<5, 3>::value;

template<typename T>
auto foo(T t) -> decltype(t + t) {
    return t;
}

template<class... Args>  
void f(Args... args) {  
    auto x = [args...] { return g(args...); };  
    x();  
}
