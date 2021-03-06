/*
 *
 *  Managed Data Structures
 *  Copyright © 2016 Hewlett Packard Enterprise Development Company LP.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  As an exception, the copyright holders of this Library grant you permission
 *  to (i) compile an Application with the Library, and (ii) distribute the 
 *  Application containing code generated by the Library and added to the 
 *  Application during this compilation process under terms of your choice, 
 *  provided you also meet the terms and conditions of the Application license.
 *
 */

package com.hpl.erk.chain;

import com.hpl.erk.func.NullaryFunc;
import com.hpl.erk.func.Predicate;
import com.hpl.erk.func.SourceExhausted;

public class FilterLink<Head, In> extends Link<Head, In, In> {
  protected final NullaryFunc<? extends Predicate<? super In>> creator;
  protected final boolean keep;

  protected FilterLink(Chain<Head, ? extends In> pred,
      NullaryFunc<? extends Predicate<? super In>> creator,
          boolean keep) 
  {
    super(pred);
    this.creator = creator;
    this.keep = keep;
  }

  @Override
  public Flow pipeInto(final Receiver<? super In> sink) {
    final Predicate<? super In> predicate = creator.call();
    if (keep) {
      return pred.pipeInto(new ChainedReceiver<In,In>(sink) {
        @Override
        public boolean receive(In val) {
          if (predicate.test(val)) {
            if (!sink.receive(val)) {
              return false;
            }
          }
          return true;
        }});
    } else {
      return pred.pipeInto(new ChainedReceiver<In,In>(sink) {
        @Override
        public boolean receive(In val) {
          if (!predicate.test(val)) {
            if (!sink.receive(val)) {
              return false;
            }
          }
          return true;
        }});
    }
  }


  @Override
  public Context createContext() {
    final Predicate<? super In> pred = creator.call();
    if (keep) {
      return new Context() {

        @Override
        final public In produce() throws SourceExhausted {
          while (true) {
            In elt;// = source.produce();
            if (pred.test(elt = source.produce())) {
//            In elt = source.produce();
//            if (pred.test(elt) == keep) {
              return elt;
            }
          }
        }
      };
    } else {
      return new Context() {
//        protected final Predicate<? super In> pred = creator.create();

        @Override
        final public In produce() throws SourceExhausted {
          while (true) {
            In elt;// = source.produce();
            if (!pred.test(elt = source.produce())) {
//            In elt = source.produce();
//            if (pred.test(elt) == keep) {
              return elt;
            }
          }
        }
      };
    }
  }

}
