struct sockaddr {
	int addr;
};

class NetKlass {
  public:
     NetKlass() : m_sockAddr() {}
  private:
     struct sockaddr m_sockAddr;
};

