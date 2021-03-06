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

package com.hpl.mds.generator;
import java.util.ArrayList;
import java.util.List;


public class Types {
  public static final Types types = new Types();
  public static final List<ValueType> allTypes = new ArrayList<>();
  public static final List<MaskedType> maskedTypes = new ArrayList<>();
  public static final List<PrimType> primTypes = new ArrayList<>();
  
  public static final PrimType BYTE = new PrimType("ManagedByte", "Byte", "byte");
  public static final PrimType SHORT = new PrimType("ManagedShort", "Short", "short");
  public static final PrimType INT = new PrimType("ManagedInt", "Integer", "int") {
    public String getShortName() {
      return "Int";
    }
  };
  public static final PrimType LONG = new PrimType("ManagedLong", "Long", "long");
  public static final PrimType FLOAT = new PrimType("ManagedFloat", "Float", "float") {
    public boolean isIntegral() {
      return false;
    };
  };
  public static final PrimType DOUBLE = new PrimType("ManagedDouble", "Double", "double") {
    public boolean isIntegral() {
      return false;
    };
  };
  public static final PrimType BOOLEAN = new PrimType("ManagedBoolean", "Boolean", "boolean") {
    public String getDefault() { return "false"; }
    public boolean isNumeric() { return false; }
    public String getKind() { return "kind::BOOL"; }
    public String getCppName() { return "bool"; };
  };
  public static final MaskedType STRING = new MaskedType("ManagedString", "String") {
    public String getBoxedArg() { return "CharSequence"; }
    public String getImplName() { return managedName+"Proxy"; }
    public String getJniName() { return "jlong"; }
  };
  
  static void register(ValueType type) {
    allTypes.add(type);
  }
  
  static void registerMasked(MaskedType type) {
    maskedTypes.add(type);
  }
  static void registerPrim(PrimType type) {
    primTypes.add(type);
  }
  
//  public List<PrimType> getPrimTypes() {
//    return primTypes;
//  }
  
  static class ValueType {
    public final String managedName;

    protected ValueType(String managedName) {
      this.managedName = managedName;
      register(this);
    }
    
    @Override
    public String toString() {
      return managedName;
    }
    
    public boolean isBool() {
      return this == BOOLEAN;
    }
    
    public boolean isString() {
      return this == STRING;
    }
    
    public boolean isInt() {
      return this == INT;
    }
    
    public String getDefault() {
      return "null";
    }
    
    public String getJniDefault() {
      String d = getDefault();
      return d == "null" ? "nullptr" : d;
    }
    
    public boolean isPrim() {
      return false;
    }
    
    public String getImplName() {
      return managedName+"Impl";
    }
    
    public String getJniName() {
      return "jobject";
    }
  }
      
  static class MaskedType extends ValueType {
    public final String javaName;

    protected MaskedType(String managedName, String javaName) {
      super(managedName);
      this.javaName = javaName;
      registerMasked(this);
    }
    
    public String getShortName() {
      return javaName;
    }
    
    public String getShortUpper() {
      return getShortName().toUpperCase();
    }
    
    public String getKind() {
      return "kind::"+getShortUpper();
    }
    
    public String getCppName() {
    	return getShortLower();
    }
    
    public String getCoreType() {
      return "managed_"+getCppName()+"_type_handle()";
    }
    
    public String getShortLower() {
      return getShortName().toLowerCase();
    }
    
    public String getFieldName() {
      return getShortName()+"Field";
    }

    public String getArrayFieldName() {
      return getShortName()+"ArrayField";
    }

    public String getBoxedName() {
      return javaName;
    }
    
    public String getBoxedArg() {
      return getBoxedName();
    }
    
    @Override
    public String getDefault() {
      return "null";
    }
    
    
    public boolean isNumeric() {
      return false;
    }
  }
  
  static class PrimType extends MaskedType {
    public final String primName;

    private PrimType(String managedName, String javaName, String primName) {
      super(managedName, javaName);
      this.primName = primName;
      registerPrim(this);
    }

    public String getBoxedName() {
      return javaName;
    }
    
    @Override
    public String getJniName() {
      return "j"+primName;
    }
    
    @Override
    public String getDefault() {
      return "0";
    }
    
    @Override
    public boolean isPrim() {
      return true;
    }
    
    public boolean isNumeric() {
      return true;
    }
    
    public boolean isNeedFloatCast() {
      return !isIntegral() && this != FLOAT;
    }
    
    public boolean isNeedIntCast() {
      return isNeedLongCast() || this==LONG;
    }
    
    public boolean isIntegral() {
      return true;
    }
    
    public boolean isNeedLongCast() {
      return !isIntegral();
    }

    public boolean isNeedCastFromInt() {
      return this==BYTE || this == SHORT;
    }
    
  }
}
