template<class T> struct Owner;
class Target {
    static void hidden();
    template<class T> friend void Owner<T>::get();
};
template<class T> struct Owner { void get() { Target::hidden(); } };
template<class T> struct Nested {
    template<class U> void convert(U);
    template<class V>
    template<class U>
    friend void Nested<V>::convert(U);
};
