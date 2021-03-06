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

package com.hpl.erk.iter;

import java.util.Iterator;

import com.hpl.erk.func.Predicate;
import com.hpl.erk.func.UnaryFunc;

/**
 * A variant of {@link Iteration} in which elements of the base {@link Iterable} are subject to a {@link Transformer} to 
 * obtain their final value.  Instances are typically constructed 
 * by calling {@link Iteration#over(Iterable, Transformer)} or {@link Iteration#over(Object[], Transformer)}.
 * See documentation for {@link Iteration} for details.  
 * 
 * @param <X> The type of the base {@link Iterable} (i.e., {@link #baseCurrent()}).
 * @param <T> The type of the result of calling the {@link Transformer} on elements of the base Iterable (i.e., {@link #current()}). 
 */
public class MappedIteration<X,T> extends IterationBase<X, T, Iterable<? extends X>> implements Iterable<MappedIteration<X,T>> {
  private MappedIteration(Iterable<? extends X> base,
                          UnaryFunc<? super X, ? extends T> transformer) {
    super(base, transformer);
  }

  @Override
  public Iterator<MappedIteration<X,T>> iterator() {
    return subtypeIterator(this);
  }
  
  @Override
  protected MappedIteration<X,T> filter(Predicate<? super T> filter) {
    super.filter(filter);
    return this;
  }
  @Override
  protected MappedIteration<X,T> baseFilter(Predicate<? super X> filter) {
    super.baseFilter(filter);
    return this;
  }
  
  public static <X,T> MappedIteration<X,T> over(Iterable<? extends X> base, UnaryFunc<? super X, ? extends T> transformer) {
    return new MappedIteration<>(base, transformer);
  }
  

}
