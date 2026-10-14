template<class T> struct Owner;
class Target {
    template<class T> friend void Owner<T>::get() noexcept(sizeof(R"(
    text
)") > 0);
};
