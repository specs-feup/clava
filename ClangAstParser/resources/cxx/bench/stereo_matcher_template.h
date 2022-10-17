using namespace std;

#ifdef WIN32
#include <typeinfo.h>
#else
#include <typeinfo>
#define __max(a,b)  (((a) > (b)) ? (a) : (b))
#define __min(a,b)  (((a) < (b)) ? (a) : (b))
#endif

class CImage {
   
   public:
   CImage();
   CImage(type_info const & ti, int bandSize);
};

//  Strongly typed image
template <class T>
class CImageOf : public CImage {
   
   public:
   CImageOf<T>();
};

//  These are defined inline so user-defined image types can be supported:
template <class T>
inline CImageOf<T>::CImageOf() : CImage(typeid(T), sizeof(T)) {
}
