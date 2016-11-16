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

package test;

import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import com.hpl.mds.impl.Stub;

public class AsyncCollector<T,A,R> implements Collector<T,A,CompletableFuture<R>>{

  @Override
  public Supplier<A> supplier() {
    // TODO Auto-generated method stub
    return Stub.notImplemented();
  }

  @Override
  public BiConsumer<A, T> accumulator() {
    // TODO Auto-generated method stub
    return Stub.notImplemented();
  }

  @Override
  public BinaryOperator<A> combiner() {
    // TODO Auto-generated method stub
    return Stub.notImplemented();
  }

  @Override
  public Function<A, CompletableFuture<R>> finisher() {
    // TODO Auto-generated method stub
    return Stub.notImplemented();
  }

  @Override
  public Set<java.util.stream.Collector.Characteristics> characteristics() {
    // TODO Auto-generated method stub
    return Stub.notImplemented();
  }

  public static void main(String[] args) {
    Random rnd = new Random();
    int nInts = 1_000_000;
    long count = rnd.ints(nInts).count();
    System.out.format("Done: %,d%n", count);
  }

}