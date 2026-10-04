template<class T> struct Holder { template<class U> U convert(U value); };
template<class X> template<class V> V Holder<X>::convert(V value) { return V{value}; }
int use() { Holder<int> h; return h.convert(1); }
template<class... Ts> struct PackHolder { int count(); };
template<class... Xs> int PackHolder<Xs...>::count() { return sizeof...(Xs); }
template<int N> struct SizedHolder { int count(); };
template<int M> int SizedHolder<M>::count() { return M; }
int use_pack() { PackHolder<int, char> p; SizedHolder<3> s; return p.count() + s.count(); }
namespace nested {
template<class T, int N> struct Outer { template<class U> struct Inner { void method(); }; };
template<class X, int M> template<class Y> void Outer<X, M>::Inner<Y>::method() {}
}
void use_nested() { nested::Outer<int, 2>::Inner<char> value; value.method(); }
namespace mixed {
template<class T, class U> struct Outer;
template<class T, class U> struct Outer<T*, U> { template<class V> struct Inner { void method(); }; };
template<class X, class Y> template<class Z> void Outer<X*, Y>::Inner<Z>::method() {}
}
void use_mixed() { mixed::Outer<int*, char>::Inner<double> value; value.method(); }
