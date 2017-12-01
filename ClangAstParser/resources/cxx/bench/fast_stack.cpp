#include <iostream>

template<typename Type>
class FastStack {
  Type *ptr;
  size_t idx;
  size_t size;

  public:
    FastStack(size_t size) {
      ptr = new Type[size];
      this->size = size;
      this->idx = 0;
      std::cout << "Initialized with " << size << std::endl;
    }

    ~FastStack() {
      if(this->ptr != nullptr) {
        delete[] ptr;
      }
    }

    void Push(Type item) {
      if(this->idx + 1 <= this->size) {
        this->ptr[this->idx++] = item;
      }
    }

    void Print() {
      for(size_t i = 0; i < this->size; i++) {
        std::cout << this->ptr[i] << std::endl;
       }
     }
};

int main(void) {

  FastStack<float> stack(2);

  stack.Push(0.1f);
  stack.Push(0.2f);
  stack.Push(0.3f);

  stack.Print();

  return 0;

}