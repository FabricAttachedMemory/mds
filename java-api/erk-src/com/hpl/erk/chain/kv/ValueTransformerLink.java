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

package com.hpl.erk.chain.kv;

import com.hpl.erk.chain.Flow;
import com.hpl.erk.func.NullaryFunc;
import com.hpl.erk.func.Pair;
import com.hpl.erk.func.SourceExhausted;
import com.hpl.erk.func.UnaryFunc;

public class ValueTransformerLink<Head, K, InV, OutV> extends KeyValLink<Head, K, InV, K, OutV> {
  protected final NullaryFunc<? extends UnaryFunc<? super InV, ? extends OutV>> creator; 

  protected ValueTransformerLink(KeyValChain<Head, ? extends K, ? extends InV> pred,
                                 NullaryFunc<? extends UnaryFunc<? super InV, ? extends OutV>> creator) 
  {
    super(pred);
    this.creator = creator;
  }


  @Override
  public Flow pipeInto(final Receiver<? super K, ? super OutV> sink) {
    return pred.pipeInto(new ChainedReceiver<K, InV, K, OutV>(sink) {
      protected final UnaryFunc<? super InV, ? extends OutV> transformer = creator.call();

      @Override
      public boolean receive(K key, InV value) {
        return sink.receive(key, transformer.call(value));
      }});
  }


  @Override
  public Context createContext() {
    return new Context() {
      protected final UnaryFunc<? super InV, ? extends OutV> transformer = creator.call();

      @Override
      public Pair<K,OutV> produce() throws SourceExhausted {
        Pair<? extends K, ? extends InV> elt = source.produce();
        return new Pair<K,OutV>(elt.key, transformer.call(elt.value));
      }
    };
  }


}