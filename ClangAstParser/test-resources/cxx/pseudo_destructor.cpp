typedef int T;
void f(int *p) {
  p->T::~T();
}

template<typename T>
void destroy(T* ptr) {
  ptr->T::~T();
}
