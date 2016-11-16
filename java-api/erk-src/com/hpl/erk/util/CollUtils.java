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

package com.hpl.erk.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import com.hpl.erk.adt.IdentityHashSet;
import com.hpl.erk.func.NullaryFunc;
import com.hpl.erk.func.Predicate;
import com.hpl.erk.func.UnaryFunc;
import com.hpl.erk.func.UnaryFuncToBoolean;
import com.hpl.erk.func.UnaryFuncToDouble;
import com.hpl.erk.func.UnaryFuncToInt;
import com.hpl.erk.func.UnaryFuncToLong;
import com.hpl.erk.iter.Iteration;
import com.hpl.erk.permutations.Permutation;

public class CollUtils {
  public static <T> T[] arrayFrom(Collection<? extends T> coll, Class<T> elementClass) {
    if (coll == null) {
      return ArrayUtils.no(elementClass);
    }
    final int n = coll.size();
    if (elementClass == Object.class) {
      @SuppressWarnings("unchecked")
      T[] array = (T[]) new Object[n];
      return coll.toArray(array);
    }
    T[] array = ArrayUtils.newArray(elementClass, n);
    return coll.toArray(array);
  }
  
  public static <T extends Comparable<? super T>> T[] sortedArrayFrom(Collection<? extends T> coll, Class<T> elementClass) {
    T[] array = arrayFrom(coll, elementClass);
    if (coll instanceof SortedSet) {
      return array;
    }
    Arrays.sort(array);
    return array;
  }
  
  public static <T> T[] sortedArrayFrom(SortedSet<? extends T> set, Class<T> elementClass) {
    return arrayFrom(set, elementClass);
  }
  
  public static <T> T[] sortedArrayFrom(Collection<? extends T> coll, Class<T> elementClass, Comparator<? super T> cptr) {
    T[] array = arrayFrom(coll, elementClass);
    Arrays.sort(array, cptr);
    return array;
  }
  
  public static <T,M> M[] mappedArrayFrom(Collection<? extends T> coll, UnaryFunc<? super T, ? extends M> func, Class<? extends M> elementClass) {
    if (coll == null) {
      return ArrayUtils.no(elementClass);
    }
    M[] array = ArrayUtils.newArray(elementClass, coll.size());
    for (Iteration<T> iter : Iteration.over(coll)) {
      array[iter.index()] = func.call(iter.current());
    }
    return array;
  }
  public static <T> int[] mappedArrayFrom(Collection<? extends T> coll, UnaryFuncToInt<? super T> func) {
    if (coll == null) {
      return new int[0];
    }
    int[] array = new int[coll.size()];
    for (Iteration<T> iter : Iteration.over(coll)) {
      array[iter.index()] = func.primCall(iter.current());
    }
    return array;
  }
  public static <T> long[] mappedArrayFrom(Collection<? extends T> coll, UnaryFuncToLong<? super T> func) {
    if (coll == null) {
      return new long[0];
    }
    long [] array = new long[coll.size()];
    for (Iteration<T> iter : Iteration.over(coll)) {
      array[iter.index()] = func.primCall(iter.current());
    }
    return array;
  }
  public static <T> double[] mappedArrayFrom(Collection<? extends T> coll, UnaryFuncToDouble<? super T> func) {
    if (coll == null) {
      return new double[0];
    }
    double[] array = new double[coll.size()];
    for (Iteration<T> iter : Iteration.over(coll)) {
      array[iter.index()] = func.primCall(iter.current());
    }
    return array;
  }
  public static <T> boolean[] mappedArrayFrom(Collection<? extends T> coll, UnaryFuncToBoolean<? super T> func) {
    if (coll == null) {
      return new boolean[0];
    }
    boolean[] array = new boolean[coll.size()];
    for (Iteration<T> iter : Iteration.over(coll)) {
      array[iter.index()] = func.primCall(iter.current());
    }
    return array;
  }
  
  public static <V> boolean contains(Collection<? super V> coll, V val) {
    if (coll == null) {
      return false;
    }
    return coll.contains(val);
  }
  
  public static int sizeOf(Collection<?> coll) {
    return coll == null ? 0 : coll.size();
  }
  
  public static boolean isEmpty(Collection<?> coll) {
    return coll == null || coll.isEmpty();
  }
  
  public static <V,C extends Collection<? super V>> C addTo(C coll, V val, NullaryFunc<? extends C> creator) {
    if (coll == null) {
      coll = creator.call();
    }
    coll.add(val);
    return coll;
  }
  
  public static <T> NullaryFunc<ArrayList<T>> arrayListCreator(final int capacity) {
    return new NullaryFunc<ArrayList<T>>() {
      @Override
      public ArrayList<T> call() {
        return new ArrayList<T>(capacity);
      }};
  }
  public static <T> NullaryFunc<ArrayList<T>> arrayListCreator() {
    return new NullaryFunc<ArrayList<T>>() {
      @Override
      public ArrayList<T> call() {
        return new ArrayList<T>();
      }};
  }
  public static <T> NullaryFunc<HashSet<T>> hashSetCreator(final int capacity) {
    return new NullaryFunc<HashSet<T>>() {
      @Override
      public HashSet<T> call() {
        return new HashSet<T>(capacity);
      }};
  }
  public static <T> NullaryFunc<HashSet<T>> hashSetCreator() {
    return new NullaryFunc<HashSet<T>>() {
      @Override
      public HashSet<T> call() {
        return new HashSet<T>();
      }};
  }
  public static <T> NullaryFunc<IdentityHashSet<T>> identityHashSetCreator(final int capacity) {
    return new NullaryFunc<IdentityHashSet<T>>() {
      @Override
      public IdentityHashSet<T> call() {
        return new IdentityHashSet<T>(capacity);
      }};
  }
  public static <T> NullaryFunc<IdentityHashSet<T>> identityHashSetCreator() {
    return new NullaryFunc<IdentityHashSet<T>>() {
      @Override
      public IdentityHashSet<T> call() {
        return new IdentityHashSet<T>();
      }};
  }
  
  public static <T> Iterable<T> maybeNullIterable(Iterable<T> iterable) {
    if (iterable == null) {
      return Collections.emptyList();
    }
    return iterable;
  }
  public static <T> List<T> maybeNullList(List<T> list) {
    if (list == null) {
      return Collections.emptyList();
    }
    return list;
  }
  public static <T> Set<T> maybeNullSet(Set<T> set) {
    if (set == null) {
      return Collections.emptySet();
    }
    return set;
  }
  public static <T> Collection<T> maybeNullCollection(Collection<T> collection) {
    if (collection == null) {
      return Collections.emptySet();
    }
    return collection;
  }
  public static <T extends Comparable<? super T>> List<T> sorted(Collection<? extends T> coll) {
    if (coll == null || coll.isEmpty()) {
      return Collections.emptyList();
    }
    List<T> list = new ArrayList<>(coll);
    Collections.sort(list);
    return list;
  }
  public static <T> List<T> sorted(Collection<? extends T> coll, Comparator<? super T> cptr) {
    if (coll == null || coll.isEmpty()) {
      return Collections.emptyList();
    }
    List<T> list = new ArrayList<>(coll);
    Collections.sort(list, cptr);
    return list;
  }
  public static <T extends Comparable<? super T>> T[] sortedArray(Collection<? extends T> coll, Class<T> clss) {
    T[] array = arrayFrom(coll, clss);
    Arrays.sort(array);
    return array;
  }
  public static <T> T[] sortedArray(Collection<? extends T> coll, Comparator<? super T> cptr, Class<T> clss) {
    T[] array = arrayFrom(coll, clss);
    Arrays.sort(array, cptr);
    return array;
  }
  
  public static <T> Set<T> setOf(Collection<T> coll) {
    return new HashSet<>(coll);
  }
  
  @SafeVarargs
  public static <T> Set<T> setOf(T...ts) {
    return setOf(Arrays.asList(ts));
  }
  
  public static <T> List<T> listOf(Collection<T> coll) {
    return new ArrayList<>(coll);
  }
  
  @SafeVarargs
  public static <T> List<T> listOf(T...ts) {
    return listOf(Arrays.asList(ts));
  }

  public static <X,Y> List<Y> map(Collection<X> coll, UnaryFunc<? super X, ? extends Y> transformer) {
    if (coll == null) {
      return null;
    }
    final List<Y> list = new ArrayList<>(coll.size());
    for (X elt : coll) {
      list.add(transformer.call(elt));
    }
    return list;
  }
  
  public static <T, C extends Iterable<? extends T>> C removeWhen(Predicate<? super T> pred, C coll) {
    if (coll == null) {
      return null;
    }
    Iteration<? extends T> iteration = Iteration.over(coll);
    for (T elt : iteration.values()) {
      if (pred.test(elt)) {
        iteration.removeCurrent();
      }
    }
    return coll;
  }
  public static <T, C extends Iterable<? extends T>> C removeUnless(Predicate<? super T> pred, C coll) {
    if (coll == null) {
      return null;
    }
    Iteration<? extends T> iteration = Iteration.over(coll);
    for (T elt : iteration.values()) {
      if (!pred.test(elt)) {
        iteration.removeCurrent();
      }
    }
    return coll;
  }
  
  public static Predicate<Collection<?>> isEmptyCollection() {
    return new Predicate<Collection<?>>() {
      @Override
      public boolean test(Collection<?> val) {
        return isEmpty(val);
      }
    };
  }
  public static Predicate<Collection<?>> notEmptyCollection() {
    return new Predicate<Collection<?>>() {
      @Override
      public boolean test(Collection<?> val) {
        return !isEmpty(val);
      }
    };
  }
  public static UnaryFunc<Collection<?>, Integer> collectionSize() {
    return new UnaryFunc<Collection<?>, Integer>() {
      @Override
      public Integer call(Collection<?> val) {
        return sizeOf(val);
      }
    };
  }
  public static <T,M> void sortMapped(List<T> list, UnaryFunc<? super T, ? extends M> func, Comparator<? super M> cptr) {
    Permutation.toSorted(list, func, cptr).permute(list);
  }
  public static <T,M extends Comparable<? super M>> void sortMapped(List<T> list, UnaryFunc<? super T, ? extends M> func) {
    Permutation.toSorted(list, func).permute(list);
  }
  
}