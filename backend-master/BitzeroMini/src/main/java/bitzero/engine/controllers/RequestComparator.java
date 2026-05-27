package bitzero.engine.controllers;

import bitzero.engine.io.IRequest;
import java.util.Comparator;

public class RequestComparator implements Comparator {
     public int compare(IRequest r1, IRequest r2) {
          int res = 0;

          if (r1.getPriority().getValue() < r2.getPriority().getValue()) {
               res = -1;
          } else if (r1.getPriority() == r2.getPriority()) {
               if (r1.getTimeStamp() < r2.getTimeStamp()) {
                    res = -1;
               } else if (r1.getTimeStamp() > r2.getTimeStamp()) {
                    res = 1;
               } else {
                    res = 0;
               }
          } else {
               res = 1;
          }

          return res;
     }

     public int compare(Object obj, Object obj1) {
          return this.compare((IRequest)obj, (IRequest)obj1);
     }
}
