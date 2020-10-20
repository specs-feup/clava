class Box {
   public:
	int age = 0;
	int getAge(void);
 
  
};

// Member functions definitions
int Box::getAge(void) {
	Box box;
	age = 3;
   return age + box.age + box.getAge();
}
