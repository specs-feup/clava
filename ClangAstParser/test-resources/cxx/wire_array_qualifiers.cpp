template<typename T> struct check;
template<unsigned N> struct check<const char[N]> {};
template<unsigned N> struct check<int *const[N]> {};

void arrays() {
  check<const char[4]> first;
  check<int *const[2]> second;
  const char values[4] = "abc";
  int *const pointers[2] = {nullptr, nullptr};
}
