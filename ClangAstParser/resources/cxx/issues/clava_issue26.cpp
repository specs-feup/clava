class BaseKlass
{
   public:
      BaseKlass(int x, int y, int z) : m_x(x), m_y(y), m_z(z) {}

   private:
     int m_x;
     int m_y;
     int m_z;
};

class SubKlass :
  public BaseKlass
{

   public:
     SubKlass(int x, int y, int z) :
       BaseKlass(x, y, z)
     {
     }
};