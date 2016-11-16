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

package com.hpl.mds.test.valid;

import com.hpl.mds.ManagedArray;
import com.hpl.mds.annotations.RecordSchema;
import com.hpl.mds.annotations.Static;
import com.hpl.mds.prim.ManagedBoolean;
import com.hpl.mds.prim.ManagedInt;
import com.hpl.mds.prim.container.array.ManagedBooleanArray;
import com.hpl.mds.prim.container.array.ManagedIntArray;
import com.hpl.mds.test.RecordPlainSimple;

@RecordSchema
public interface MethodsArraySchema {

    // instance method
    static void booleanArrayMethod(MethodsArray.Private self, ManagedBooleanArray array) {
    }

    // instance method
    static void booleanArrayMethod2(MethodsArray.Private self, ManagedArray<ManagedBoolean> array) {
    }

    // instance method
    static void intArrayMethod(MethodsArray.Private self, ManagedIntArray array) {
    }

    // instance method
    static void intArrayMethod2(MethodsArray.Private self, ManagedArray<ManagedInt> array) {
    }

    // instance method
    static ManagedIntArray returnArrayMethod(MethodsArray.Private self) {
        return null;
    }

    // instance method
    static ManagedArray<ManagedInt> returnArrayMethod2(MethodsArray.Private self) {
        return null;
    }

    // instance method
    static ManagedArray<RecordPlainSimple> returnRecordArrayMethod(MethodsArray.Private self) {
        return null;
    }

    // constructor
    static void MethodsArray(MethodsArray.Private self, ManagedIntArray array) {
    }

    @Static
    static void staticMethod(ManagedIntArray array) {
    }

}