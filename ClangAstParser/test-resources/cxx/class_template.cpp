#include "class_template.h"

FastStack<double> fd; 

template<typename T>
struct X : FastStack<T> // "B<T>" is dependent on T
{
	using typename FastStack<T>::i;

    typename T::A* pa; // "T::A" is dependent on T
                       // (see below for the meaning of this use of "typename")
    void f(FastStack<T>* pb) {
        static int i = FastStack<T>::i; // "B<T>::i" is dependent on T
        pb->j++; // "pb->j" is dependent on T
    }
};