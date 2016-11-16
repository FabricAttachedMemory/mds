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

/**
 * 
 */
package com.hpl.erk.adt;

import java.util.ConcurrentModificationException;
import java.util.Iterator;

import cern.colt.function.LongProcedure;


public abstract class AbstractUniformHashMap {
  public static final int DEFAULT_INITIAL_CAPACITY = 16;
  public static final double LOAD_FACTOR = 0.75;
  private static final long EMPTY = 0;
  private static final long DELETED = 1;
//private static final int DOTFREQ = 1024*1024;
  @SuppressWarnings("unused")
  private static final int DOTFREQ = 64*1024;
  protected int capacity;
  private int size = 0;
  private int mask;
  private long[] keys;
  private int threshold;
  private transient volatile int modCount;
  
  protected abstract void removeAtIndex(int index);
  protected abstract void resizeValues(int size);
  
  
  protected abstract class Iter<X> implements Iterator<X> {
    private final int mc = modCount; 
    private final int max = keys.length;
    private int lastI = -1;
    private int nextI = findNext(lastI);

    protected abstract X element(long key, int index);
    
    private int findNext(int i) {
      for (i++; i < max; i++) {
        long key = keys[i];
        if (key != EMPTY && key != DELETED) {
          return i;
        }
      }
      return -1;
    }

    @Override
    public boolean hasNext() {
      return nextI >= 0;
    }


    @Override
    public X next() {
      if (mc != modCount) {
        throw new ConcurrentModificationException();
      }
      long key = keys[nextI];
      lastI = nextI;
      nextI = findNext(nextI);
      return element(key, lastI);
    }


    @Override
    public void remove() {
      if (mc != modCount) {
        throw new ConcurrentModificationException();
      }
      keys[lastI] = DELETED;
      removeAtIndex(lastI);
    }
  }
  
  
  
  private class KeyIterator extends Iter<Long> {
    @Override
    protected Long element(long key, int index) {
      return key;
    }
  }

  public AbstractUniformHashMap(int expected) {
    capacity = roomFor(expected);
    keys = new long[capacity];
    mask = capacity-1;
    threshold = (int)(capacity*LOAD_FACTOR);
  }
  
  

  protected int findIndex(long hash) {
    long key;
    for (int i=(int) (hash & mask); (key = keys[i]) != EMPTY; i = (i+1) & mask) {
      if (key == hash) {
        return i;
      }
    }
    return -1;
  }
  
  /**
   * 
   * @param hash the key
   * @return {@code true} iff an association exists for the key in the map.
   */
  public boolean containsKey(long hash) {
    int index = findIndex(hash);
    return index >= 0;
  }
  
  protected static interface PutCallBack {
    boolean replace(int oldIndex, int newIndex);
  }
  
  public void put(long k, PutCallBack cb) {
    long key;
    int i;
    int oldIndex = -1;
    int newIndex = -1;
    for (i=(int) (k & mask); (key = keys[i]) != EMPTY; i = (i+1) & mask) {
      if (key == k) {
        if (newIndex >= 0) {
          // We've already set it closer to the source, so we delete this
          // old value
          keys[i] = DELETED;
        } else {
          newIndex = i;
        }
        if (cb.replace(i, newIndex)) {
          modCount++;
        }
        return;
      }
      if (key == DELETED && oldIndex <  0) {
        keys[i] = k;
        newIndex = i;
      }
    }
    assert key == EMPTY;
    modCount++;
    if (newIndex <= 0) {
      // If oldVal has a value, we put already put the value in a deleted slot.
      keys[i] = k;
      newIndex = i;
    }
    cb.replace(-1, newIndex);
    if (++size > threshold) {
      resizeTo(capacity*2);
    }
  }
  
  protected static interface RemoveCallBack {
    void removedFrom(int index);
  }
  
  protected void remove(long k, RemoveCallBack cb) {
    long key;
    int i;
    for (i=(int) (k & mask); (key = keys[i]) != EMPTY; i = (i+1) & mask) {
      if (key == k) {
        keys[i] = DELETED;
        cb.removedFrom(i);
        removeAtIndex(i);
        size--;
        modCount++;
        return;
      }
    }
    assert key == EMPTY;
  }
  
  protected abstract static interface Resizer {
    public void copyFrom(long key, int index);
  }
  
  protected abstract Resizer resizer();
  
  private void resizeTo(int newCap) {
    if (capacity == newCap) {
      return;
    }
    int oldCap = capacity;
    capacity = newCap;
    mask = capacity-1;
    threshold = (int)(capacity*LOAD_FACTOR);
    long[] oldKeys = keys;
    Resizer r = resizer();
    keys = new long[capacity];
    int oldSize = size;
    size = 0;
    resizeValues(capacity);
    for (int i=0; i<oldCap; i++) {
      long k = oldKeys[i];
      if (k != EMPTY && k != DELETED) {
        r.copyFrom(k, i);
      }
    }
    assert size == oldSize;
  }

  /**
   * Makes sure that the space required for the map is not
   * excessively large.  This can be useful if many objects
   * have been removed from the map.
   */
  public void compact() {
    resizeTo(roomFor(size));
  }
  private static int roomFor(int n) {
    n /= LOAD_FACTOR;
    int cap;
    for (cap=1; cap<n; cap<<=1) {}
    return cap;
  }
  
  /**
   * 
   * @return the number of associations in the map.
   */
  public int size() {
    return size;
  }
  

  /**
   * @return an object to use for iterating over the keys of the associations in the map.  
   * No guarantee is made as to the 
   * order keys are returned.  Associations may be deleted by using the {@link Iterator}'s {@code remove}() method.
   */
  public Iterable<Long> keys() {
    return new Iterable<Long>() {
      @Override
      public Iterator<Long> iterator() {
        return new KeyIterator();
      }};
  }
  
  public static interface Filter<T> {
	  boolean keep(long hash, T val);
  }
  
  /**
   * @return an array containing all of the keys in the array
   * in some order.  The array is newly created and may be modified.
   */
  public long[] keysArray() {
    long[] array = new long[size];
    int p=0;
    for (int i=0; i<capacity && p < size; i++) {
      long k = keys[i];
      if (k != EMPTY && k != DELETED) {
        array[p++] = k;
      }
    }
    return array;
  }
  
  /**
   * Apply the procedure to each key in the map in an undefined order.  If the procedure
   * returns {@code false}, the iteration terminates.  This is more efficient than iterating
   * over {@code keys()}, as no iterator need be created and the keys do not have to be converted
   * to {@link Long} objects (and probably back).
   * @param p the procedure to apply.
   */
  public void forEachKey(LongProcedure p) {
//    int left = size;
    for (int i=0; size > 0 && i<capacity; i++) {
      long k = keys[i];
      if (k != EMPTY && k != DELETED) {
        if (!p.apply(k)) {
          return;
        }
//        left--;
      }
    }
  }
  
}