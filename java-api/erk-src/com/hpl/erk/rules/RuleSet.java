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

package com.hpl.erk.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class RuleSet<Arg, Val> implements Function<Arg, Val> {
  private List<Predicate<Arg>> clauses = new ArrayList<>();
  private Val val = null;
  @Override
  public Val apply(Arg t) {
    @SuppressWarnings("unused")
    boolean found = clauses.stream().filter(p -> p.test(t)).findFirst().isPresent();
    return val;
  }
  
  public 
  void add(Function<Arg, Val> fn) {
    clauses.add(arg -> { 
      val = fn.apply(arg);
      return val != null;
    });
  }
  
  public
  void add(Predicate<Arg> guard, Function<Arg, Val> fn) {
    clauses.add(arg -> {
      if (guard.test(arg)) {
        val = fn.apply(arg);
        return true;
      } 
      return false;
    });
  }
  
  public
  void add(Predicate<Arg> guard, Supplier<Val> v) {
    add(guard, arg->v.get());
  }
  public
  void add(Predicate<Arg> guard, Val v) {
    add(guard, arg->v);
  }
  
  public <Intermediate> 
  void add(Function<Arg, Intermediate> guard, Function<Intermediate, Val> action) {
    clauses.add(arg -> {
      Intermediate i = guard.apply(arg);
      if (i != null) {
        val = action.apply(i);
        return true;
      }
      return false;
    });
  }
  
  public 
  void add(Supplier<Val> defVal) {
    clauses.add(arg -> {
     val = defVal.get();
     return true;
    });
  }
  
  public 
  void add(Val defVal) {
    clauses.add(arg -> {
     val = defVal;
     return true;
    });
  }
  
}