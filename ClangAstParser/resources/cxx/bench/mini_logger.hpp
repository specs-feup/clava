#include <cstdio>
#include <fstream>
#include <iomanip>
#include <string>

struct my_logger_t {
    std::filebuf log_file;
    std::ostream out_stream{nullptr};

    my_logger_t(const std::string& file_name) {
        log_file.open(file_name, std::ios::out);
        out_stream.rdbuf(&log_file);
    }

    ~my_logger_t() { log_file.close(); }

    template <class... Ys>
    inline void operator()(const Ys... fields) {
        print(fields...);
        endl();
    }

   private:
    template <class T, class... Ys>
    inline void print(const T what, const Ys... remainder) {
        print(what);
        print<Ys...>(remainder...);
    }

    template <class T>
    inline void print(const T what) {
        out_stream << std::left << std::setw(20) << std::setfill(' ') << what;
    }

    inline void endl() { out_stream << std::endl; }
};