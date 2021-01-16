#include <iterator> // reverse_iterator
template<typename Base>
class json_reverse_iterator : public std::reverse_iterator<Base>
{
};