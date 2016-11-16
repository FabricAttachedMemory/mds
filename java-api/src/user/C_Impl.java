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

package user;

import com.hpl.mds.Field;
import com.hpl.mds.ManagedRecordBase;
import com.hpl.mds.RecordType;
import com.hpl.mds.prim.field.IntField;
import com.hpl.mds.string.ManagedString;
import com.hpl.mds.string.StringField;

public class C_Impl extends ManagedRecordBase implements C {
  protected static final 
    IntField<C> age = TYPE.intField("age");
  protected static final 
    StringField<C> name = TYPE.stringField("name");
  protected static final 
    Field<C, C> parent = TYPE.field("parent", C.TYPE);
  
  public RecordType<? extends C> type() {
    return TYPE;
  }

  protected C_Impl(RecordType<? extends C> type, CharSequence name, int age, C parent) {
    super(type);
    C_Impl.age.set(this, age);
    C_Impl.name.set(this, name);
    C_Impl.parent.set(this, parent);
  }
  
  
  public C_Impl(CharSequence name, int age, C parent) {
    this(TYPE, name, age, parent);
  }
  
  
  
  public int age() {
    return age.getInt(this);
  }
  
  public ManagedString name() {
    return name.get(this);
  }
  
  public C parent() {
    return parent.get(this);
  }
  
}