template< class T >
class ATemplatedKlass 
{
public:
  ATemplatedKlass()
  {
  }

  // a reference operator
  operator T*() const { return reinterpret_cast< T* >( this ); }
};