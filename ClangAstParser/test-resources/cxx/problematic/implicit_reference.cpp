int main() {
    int x;
    
    auto func = [&x]() { x = 1; };
    func();
    
    return x;
}