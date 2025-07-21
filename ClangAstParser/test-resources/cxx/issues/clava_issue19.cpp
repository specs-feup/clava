#include <iostream>

template <typename T, std::size_t N>
constexpr std::size_t countof(T const (&)[N]) noexcept
{
 return N;
}

static const std::size_t values[] = {42, 76, 16, 11, 31};
   
int main(int argc, char *argv[]){
   for(std::size_t i = 0; i < countof(values); ++i)
      std::cout << values[i] << std::endl;
   return 0;
}
