template<class... Args> int count(Args... args) {
    return sizeof...(Args) + sizeof...(args);
}
int main() { return count(1, 2.0); }
