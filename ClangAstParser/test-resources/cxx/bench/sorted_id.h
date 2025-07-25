
#ifndef ROUTING_SORTEDID_H
#define ROUTING_SORTEDID_H


#include <utility>
#include <vector>
#include <algorithm>
#include <iostream>
#include <memory>

namespace Routing {

    namespace StoreMap{
        template<typename K, typename T>
        class SortedId {
            //typedef std::pair<K, T> std::pair<K, T>;
        public:

			SortedId(const std::vector<std::pair<K, T>> &data) :
                    data(new std::vector<std::pair<K, T>>(data)) {

                this->Sort();
            }
			

            SortedId(std::vector<std::pair<K, T>> &&data, bool isSorted = false) :
                    data(new std::vector<std::pair<K, T>>(std::move(data))) {
                this->Sort();
            }

            inline int DataSize(void) { return this->data->size(); };

            inline bool ValueExist(int value) const {
                try {
                    this->GetData(value);
                }
                catch (std::logic_error &e){
                    return false;
                }

                return true;
            };

            const T GetData(K key) const {
                unsigned long low = 0;
                unsigned long high = this->data->size() - 1;
                unsigned long mid;

                while ((*this->data)[high].first != (*this->data)[low].first &&
                       key >= (*this->data)[low].first &&
                       key <= (*this->data)[high].first) {

                    mid = low + (key -
                            (*this->data)[low].first) * ((high - low) /
                            ((*this->data)[high].first - (*this->data)[low].first));

                    if ((*this->data)[mid].first < key)
                        low = mid + 1;
                    else if (key < (*this->data)[mid].first)
                        high = mid - 1;
                    else {
                        return (*this->data)[mid].second;
                    }
                }

                if (key == (*this->data)[low].first) {
                    return (*this->data)[low].second;
                } else {
                    std::cerr << "Data for key " << key << " not found.\n";
                    throw std::logic_error(to_string(key));
                }

            }

            void Print() {
                for (const auto &p: *data) {
                    std::cout << p << std::endl;
                }
            }

        private:
            std::unique_ptr<std::vector<std::pair<K, T>>> data;

            void Sort(){
                std::sort(this->data->begin(), this->data->end(),
                          [](const std::pair<K, T> &first, const std::pair<K, T> &second) -> bool {
                              return first.first < second.first;
                          });
            }

        };
    }
}

#endif //ROUTING_SORTEDID_H
