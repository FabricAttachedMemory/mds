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

import java.util.AbstractSet;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;

public class IdentityHashSet<T> extends AbstractSet<T>{
  private static final Object PRESENT = new Object();
  private final IdentityHashMap<T, Object> map;
  public IdentityHashSet(Collection<T> coll) {
    this(coll.size());
    addAll(coll);
  }
  public IdentityHashSet(int size) {
    map = new IdentityHashMap<T, Object>(size);
  }
  public IdentityHashSet() {
    map = new IdentityHashMap<T, Object>();
  }
  @Override
  public Iterator<T> iterator() {
    return map.keySet().iterator();
  }
  @Override
  public int size() {
    return map.size();
  }
  public boolean add(T e) {
    return map.put(e, PRESENT) == null;
  };
  public boolean remove(Object e) {
    return map.remove(e) != null;
  }
  @Override
  public void clear() {
    map.clear();
  }
}