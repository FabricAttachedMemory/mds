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

package com.hpl.mds.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.function.LongFunction;

import com.hpl.mds.Field;
import com.hpl.mds.ManagedArray;
import com.hpl.mds.ManagedList;
import com.hpl.mds.ManagedMap;
import com.hpl.mds.ManagedObject;
import com.hpl.mds.ManagedRecord;
import com.hpl.mds.ManagedSet;
import com.hpl.mds.ManagedType;
import com.hpl.mds.NativeLibraryLoader;
import com.hpl.mds.RecordExtension;
import com.hpl.mds.RecordType;
import com.hpl.mds.exceptions.BoundToNamespaceException;
import com.hpl.mds.impl.ManagedRecordProxy.FromHandle;
import com.hpl.mds.naming.Namespace;
import com.hpl.mds.naming.Prior;
import com.hpl.mds.string.ManagedMapFromString;
import com.hpl.mds.string.ManagedString;

public class RecordTypeProxy <R extends ManagedRecord> extends Proxy implements ManagedTypeImpl<R>, RecordType<R> {

  private static final NativeLibraryLoader NATIVE_LIB_LOADER = NativeLibraryLoader.getInstance();
  
  public static class RecordCreationError extends RuntimeException {

    private RecordCreationError(RecordTypeProxy<? extends ManagedRecord> rtp, Throwable cause) {
      super(String.format("Couldn't create a record of type '%s'", rtp.name()), cause);
    }

  }

  private static final Proxy.Table<RecordTypeProxy<? extends ManagedRecord>> proxyTable = new Proxy.Table<>(RecordTypeProxy::release);



  private ManagedStringProxy name_;
  private final Constructor<? extends R> fromHandleCtor_;
  private final Class<? extends R> implClass_;
  private RecordTypeProxy<R> forward_;
  private RecordTypeProxy<? super R>[] supers_ = null;
  private RecordTypeProxy<? super R> super_ = null;
  private RecordArrayTypeProxy<R> arrayType_ = null;

  private static native void release(long h); 
  // declare RecordType by name in ManagedSpace
  private static native long declareType(long nameHandle);
  private static native long declareType(long nameHandle, long superHandle);
  private static native long lookupHandle(long h, long ctxtHandle, long namespaceHandle, long nameHandle);
  private static native boolean bindHandle(long h, long ctxtHandle, long namespaceHandle, long nameHandle, long valHandle);
  private static native boolean isSameAs(long aHandle, long bHandle);
  // ensureCreated() returns the handle, which may be to a pre-existing type.
  private static native long ensureCreated(long h);
  private static native long nameHandle(long h);
  private static native long superHandle(long h);

  private RecordTypeProxy(long handle, ManagedStringProxy name, Class<? extends R> implClass) {
    super(handle);
    if (implClass == null) {
      // If we don't know what class to create, we don't know how to create it.
      fromHandleCtor_ = null;
      forward_ = null;
    } else {
      try {
        fromHandleCtor_ = implClass.getConstructor(FromHandle.class, Long.TYPE, RecordType.class);
        fromHandleCtor_.setAccessible(true);
        forward_ = this;
      } catch (NoSuchMethodException e) {
        throw new UnsupportedOperationException(String.format("Class %s has no FromHandle constructor", implClass), e);
      }
    }
    this.name_ = name;
    this.implClass_ = implClass;
  }

  //    private RecordTypeProxy(long handle) {
  //      this(handle, null, null);
  //    }

  @Override
  void releaseHandleIndex(long index) {
    proxyTable.release(index);
  }
  
  RecordTypeProxy<R> forward() {
    return forward_;
  }
  
  /*
   * We're assuming identity of proxy handle index.  We should probably
   * look up and cache the underlying hash in the proxy.
  @Override
  public boolean equals(Object other) {
    if (this == other) { 
      return true; 
    }
    if (other == null) { 
      return false; 
    }
    if (!(other instanceof RecordTypeProxy)) { 
      return false; 
    }
    return isSameAs(handleIndex_, ((ManagedRecordProxy)other).handleIndex());
  }
*/

//   RecordTypeProxy constructor invokes native code to declare RecordType

//  private RecordTypeProxy(ManagedStringProxy name, Class<? extends R> implClass) {
//    this(declareType(name.handleIndex()), name, implClass);
//  }

  //    private RecordTypeProxy(ManagedStringProxy name, Class<? extends R> implClass, RecordTypeProxy<? super R> superType) {
  //      this(declareType(name.handleIndex(), superType.handleIndex()), name, implClass);
  //    }

  @Override
  public <RT extends ManagedRecord> RecordFieldProxy<RT,R> fieldIn(RecordType<RT> recType, CharSequence name) {
    //		System.out.println("RecordTypeProxy.fieldIn recType " + name);
    return RecordFieldProxy.in(recType, name, this);
  }

  public static <R extends ManagedRecord> RecordTypeProxy<R> downcast(RecordType<R> recType) {
    return (RecordTypeProxy<R>)recType;
  }

  public static <R extends ManagedRecord> RecordTypeProxy<R> declare(CharSequence name,
                                                                     Class<? extends R> implClass,
                                                                     Collection<RecordType<? super R>> supers) {
    ManagedStringProxy msn = ManagedStringProxy.valueOf(name);
    long handle;
    int nSupers = supers.size();
    if (nSupers == 0) {
      handle = declareType(msn.handleIndex());
    } else if (nSupers == 1) {
      RecordType<? super R> onlySuper = supers.iterator().next();
      RecordTypeProxy<? super R> superProxy = RecordTypeProxy.downcast(onlySuper);
      handle = declareType(msn.handleIndex(), superProxy.handleIndex());
    } else {
      throw new IllegalStateException("Multiple inheritance of records not yet implemented");
    }
    return fromHandle(handle, msn, implClass);
  }


  @Override
  public ManagedList.Type<R> inList() {
    return Stub.notImplemented();
  }
  
  @Override
  public ManagedArray.Type<R> inArray() {
    if (arrayType_ == null) {
      cacheArrayType(RecordArrayTypeProxy.forType(this));
    }
    return arrayType_;
  }

  void cacheArrayType(RecordArrayTypeProxy<R> t) {
    arrayType_ = t;
  }
  
  @Override
  public ManagedSet.Type<R> inSet() {
    return Stub.notImplemented();
  }

  @Override
  public <K extends ManagedObject> ManagedMap.Type<K, R> inMapFrom(
                                                                   ManagedType<K> keyType) {
    return Stub.notImplemented();
  }

  @Override
  public ManagedMapFromString.Type<R> inMapFromString() {
    return Stub.notImplemented();
  }



  @Override
  public <RT extends ManagedRecord> Field<RT, R> findFieldIn(
                                                             RecordType<RT> recType, CharSequence name) {
    return Stub.notImplemented();
  }

  @Override
  public R construct(Object... ctorParams) {
    return Stub.notImplemented();
  }
  @Override
  public boolean isCreated() {
    return Stub.notImplemented();
  }

  @Override
  public ManagedString name() {
    return name_;
  }

  @Override
  public Field<? super R, ? extends ManagedObject>[] fields() {
    return Stub.notImplemented();
  }

  protected RecordTypeProxy<? super R> supertype() {
    if (super_ == null) {
      RecordTypeProxy<? super R>[] supers = (RecordTypeProxy<? super R>[])supertypes();
      if (supers.length > 0) {
        super_ = supers[0];
      }
    }
    return super_;
  }
  
  @Override
  public RecordType<? super R>[] supertypes() {
    if (supers_ == null) {
      RecordTypeProxy<? extends ManagedRecord> superType = fromHandle(superHandle(handleIndex_));
      @SuppressWarnings("unchecked")
      RecordTypeProxy<? super R>[] array = superType == null ? new RecordTypeProxy[0] : new RecordTypeProxy[] {superType};
      supers_ = array;
    }
    return supers_;
  }

  @Override
  public boolean ensureCreated() {
    long curr = ensureCreated(handleIndex());
    if (curr != handleIndex_) {
    	/*
    	 * There already was one.  We need to turn
    	 * it into a RecordTypeProxy<R>, but with the 
    	 * same name and constructor as we have and 
    	 * set our forward to it.
    	 */
    	forward_ = fromHandle(curr, name_, implClass_);
    	return false;
    }
    return true;
  }

  @Override
  public RecordExtension<R> extension() {
    return Stub.notImplemented();
  }

  @Override
  public RecordExtension<R> extension(CharSequence name) {
    return Stub.notImplemented();
  }

  @Override
  public RecordExtension<R> extension(RecordType<? super R> etype) {
    return Stub.notImplemented();
  }



  private static <R extends ManagedRecord> RecordTypeProxy<R> fromHandle(long recTypeHandle,
                                                                         ManagedStringProxy name,
                                                                         Class<? extends R> implClass) {
    final LongFunction<RecordTypeProxy<R>> creator = (i) -> {
      /*
       * If we get here, we don't have it.  
       */
      ManagedStringProxy n = name;
      if (n == null) {
        n = ManagedStringProxy.fromHandle(nameHandle(recTypeHandle));
      }
      Class<? extends R> ic = implClass;
      if (ic == null) {
        /*
         * If we don't know the implClass, we walk up the hierarchy until we find one that we do know
         * and use its impl class.  Since R is one we know, we should find it. (If R is ManagedRecord,
         * we won't.  In that case, we should just create a ManagedRecord.  The notion here is that  
         * if we're looking for an R and we get a managed record handle index we don't know, we look 
         * up its manifest type and use it to create the record.  The constructor will be the lowest one
         * we've actually declared.
         * 
         * TODO: Note!!!  This assumes that once we've decided that we don't know a particular record type,
         * we don't declare it later.  If we do that, we'll find the old stub type, which will have the
         * wrong ctor.  We can patch it, but anything already created from it will be wrong.
         */
        RecordTypeProxy<? extends ManagedRecord> superType = fromHandle(superHandle(recTypeHandle));
        if (superType != null) {
          @SuppressWarnings("unchecked")
          Class<? extends R> ic2 =  (Class<? extends R>)superType.implClass_;
          ic = ic2;
        }
      }
      return new RecordTypeProxy<>(recTypeHandle, n, ic);
    };
    RecordTypeProxy<? extends ManagedRecord> rtp = proxyTable.fromIndex(recTypeHandle, creator);
    if (implClass != null && rtp.implClass_ != implClass) {
      /*
       * What we got back must've already been in the table.  We'll replace it.
       */
      RecordTypeProxy<R> newRTP = creator.apply(recTypeHandle);
      proxyTable.replace(recTypeHandle, newRTP);
      rtp = newRTP;
    }
    @SuppressWarnings("unchecked")
    RecordTypeProxy<R> downcast = (RecordTypeProxy<R>)rtp;
    return downcast;
  }
  public static <RT extends ManagedRecord> RecordTypeProxy<RT> fromHandle(long recTypeHandle) {
    return fromHandle(recTypeHandle, null, null);
  }

  @Override
  public String toString() {
    return name().toString();
  }

  R createFromRecordHandle(long handle) {
    if (handle == 0) {
      return null;
    }
    if (fromHandleCtor_ == null) {
      throw new UnsupportedOperationException(String.format("Don't know how to create a record of type '%s'", name()));
    }
    try {
      return fromHandleCtor_.newInstance(FromHandle.FROM_HANDLE, handle, this);
    } catch (InstantiationException | IllegalAccessException
        | IllegalArgumentException | InvocationTargetException e) {
      throw new RecordCreationError(this, e);
    }
  }

  @Override
  public R lookupName(Namespace ns, CharSequence name) {
    NamespaceProxy nsp = (NamespaceProxy)ns;
    ManagedStringProxy msp = ManagedStringProxy.valueOf(name);
    IsoContextProxy ctxt = IsoContextProxy.current();
    long handle = lookupHandle(handleIndex_, ctxt.handleIndex(), nsp.handleIndex(), msp.handleIndex());
    return ManagedRecordProxy.fromHandle(handle, this);
  }

  @Override
  public R bindIn(Namespace ns, CharSequence name, R val, Prior prior) {
    NamespaceProxy nsp = (NamespaceProxy)ns;
    ManagedStringProxy msp = ManagedStringProxy.valueOf(name);
    IsoContextProxy ctxt = IsoContextProxy.current();
    if (!bindHandle(handleIndex_, ctxt.handleIndex(), nsp.handleIndex(), msp.handleIndex(), ManagedRecordProxy.handleOf(val))) {
      throw new BoundToNamespaceException(nsp, name);
    }
    return val;
  }
}