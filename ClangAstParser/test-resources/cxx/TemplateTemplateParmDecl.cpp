template <template <typename> class T> class container { };

template <int N> class foo { };
template <template <int> class T = foo> class container2 { };
