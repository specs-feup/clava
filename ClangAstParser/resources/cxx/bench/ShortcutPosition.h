#ifndef ROUTING_SHORTCUTPOSITION_H
#define ROUTING_SHORTCUTPOSITION_H

#include <ostream>

namespace Routing {

   struct ShortcutPosition {
   public:
       int edgeId;
       int position;
       int count;

       ShortcutPosition() {};

       ShortcutPosition(int edgeId, int position, int count) :
               edgeId(edgeId),
               position(position),
               count(count) { };
   };
}

#endif //ROUTING_SHORTCUTPOSITION_H