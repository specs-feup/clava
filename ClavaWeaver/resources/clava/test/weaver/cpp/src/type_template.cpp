#include <vector>
#include <map>
#include <chrono>


using namespace std;

void foo_no_std() {
  
  // Removing std:: changes tree
  vector<double> W;

  //vector<double> W_nostd;
}

int main ()
{
  std::vector<double> W;
  W[0] = 12.3;
  W[1] = 1.09;
  
  std::map<int, float> map;
  
  // TemplateArgumentExpr, not implemented yet
  auto nano = std::ratio<1, 1000000000>();
  
  return 0;
}


typedef std::vector<double>::const_iterator typedef_to_change;
typedef_to_change type_of_changed_typedef;


typedef std::vector<double>::const_iterator a_typedef;
a_typedef changed_typedef_type;