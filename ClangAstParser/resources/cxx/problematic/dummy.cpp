#include <array>


class B {
    
};

class A
{
 public:
 static constexpr int number_of_sizes = 3;
 static constexpr std::array<int, number_of_sizes> SPACE{{512, 256, 128}};
 static constexpr B SPACE2{};

 int a;
 
 void xpto();
};

constexpr std::array<int, A::number_of_sizes> A::SPACE;
constexpr B A::SPACE2;

void A::xpto() {
    
    return;
}
