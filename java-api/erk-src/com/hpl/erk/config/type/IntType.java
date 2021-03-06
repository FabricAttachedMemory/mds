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

package com.hpl.erk.config.type;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hpl.erk.IOUtils;
import com.hpl.erk.ReadableString;
import com.hpl.erk.TrueOnce;
import com.hpl.erk.config.PType;
import com.hpl.erk.config.ex.CantReadError;
import com.hpl.erk.config.ex.ReadError;
import com.hpl.erk.config.func.ArgSet;
import com.hpl.erk.config.func.Param;
import com.hpl.erk.util.NumUtils;

public class IntType extends SimpleType<Integer> {
  
  protected IntType() {
    super(Integer.class);
  }

  static final Pattern decimal = Pattern.compile("[+-]?[\\p{Digit}][\\p{Digit}_]*?(?<=[\\p{Digit}])"); 
  static final Pattern decimalWithComma = Pattern.compile("[+-]?[\\p{Digit}][\\p{Digit}_,]*(?<=[\\p{Digit}])"); 
  static final Pattern hex = Pattern.compile("[0[xX]\\p{XDigit}][\\p{XDigit}_]*(?<=[\\p{XDigit}])"); 
  @Override
  public Integer readVal(ReadableString input, String valTerminators) throws ReadError {
    int resetTo = input.getCursor();
    input.skipWS();
    final Pattern pat = valTerminators.contains(",") ? decimal : decimalWithComma;
    Integer val = readDigits(input, pat, 10);
    if (val != null) {
      return val;
    }
    val = readDigits(input, hex, 16);
    if (val != null) {
      return val;
    }
    throw new CantReadError(input, resetTo, this);
  }

  public Integer readDigits(ReadableString input, final Pattern re, final int radix) {
    Matcher m = input.consume(re);
    if (m != null) {
      String digitString = m.group();
      digitString = digitString.replace("_", "");
      digitString = digitString.replace(",", "");
      return Integer.parseInt(digitString, radix);
    }
    return null;
  }
  
  @Override
  public String toString() {
    return "int";
  }
  
  @Override
  public String format(Integer val) {
    if (val == null) {
      return "null";
    } else if (val == Integer.MAX_VALUE) {
      return "max";
    } else if (val == Integer.MIN_VALUE) {
      return "min";
    }
    return IOUtils.formatGrouped(val, "_");
  }
  
  private static final TrueOnce needInit = new TrueOnce();
  public static void ensureConfigParams() {
    if (!needInit.check()) {
      return;
    }
    
    PType<Integer> type = PType.of(Integer.class);

    type.new ConversionFrom<Long>(PType.of(Long.class)) {      @Override
      public Integer convert(Long val) {
        return val == null ? null : val.intValue();
      }
    }.register();
      
    type.constant("maxInt", Integer.MAX_VALUE).setHelp("Maximum int value");
    type.constant("minInt", Integer.MIN_VALUE).setHelp("Minimum int value");

    type.new CFunc("plus", Param.req("a", int.class), Param.req("b", int.class)) {
      @Override
      public Integer make(ArgSet args) {
        int a = args.get(int.class, "a");
        int b = args.get(int.class, "b");
        return a+b;
      }
    }
    .setHelp("Add two integers");

    type.new CFunc("times", Param.req("a", int.class), Param.req("b", int.class)) {
      @Override
      public Integer make(ArgSet args) {
        int a = args.get(int.class, "a");
        int b = args.get(int.class, "b");
        return a*b;
      }
    }
    .setHelp("Multiply two integers");

    type.new CFunc("minus",Param.req("a", int.class), Param.req("b", int.class)) {
      @Override
      public Integer make(ArgSet args) {
        int a = args.get(int.class, "a");
        int b = args.get(int.class, "b");
        return a-b;
      }
    }
    .setHelp("Subtract one integer from another");
    
    type.new CFunc("div", Param.req("a", int.class), Param.req("b", int.class)) {
      @Override
      public Integer make(ArgSet args) {
        int a = args.get(int.class, "a");
        int b = args.get(int.class, "b");
        return a/b;
      }
    }
    .setHelp("Divide one integer by another");
    
    type.new CFunc("mod", Param.req("a", int.class), Param.req("b", int.class)) {
      @Override
      public Integer make(ArgSet args) {
        int a = args.get(int.class, "a");
        int b = args.get(int.class, "b");
        return a%b;
      }
    }
    .setHelp("Remainder of one integer divided by another");
    
    type.new CFunc("sum", Param.req("first", int.class), Param.rest("rest", int.class)) {
      @Override
      public Integer make(ArgSet args) {
        Integer first = args.<Integer>get("first");
        List<Integer> rest = args.<List<Integer>>get("rest");
        int sum = NumUtils.whenNull(first, 0);
        for (Integer i : rest) {
          if (i != null) {
            sum += i;
          }
        }
        return sum;
      }
    }
    .setHelp("Add a sequence of integers");
    
    type.new CFunc("sum", Param.req("array", PType.arrayOf(int.class))) {
      @Override
      public Integer make(ArgSet args) {
        Integer[] array = args.<Integer[]>get("array");
        if (array == null) {
          return null;
        }
        int sum = 0;
        for (Integer i : array) {
          if (i != null) {
            sum += i;
          }
        }
        return sum;
      }
    }
    .setHelp("Add a sequence of integers");
    

    type.new CFunc("prod", Param.req("first", int.class), Param.rest("rest", int.class)) {
      @Override
      public Integer make(ArgSet args) {
        Integer first = args.<Integer>get("first");
        List<Integer> rest = args.<List<Integer>>get("rest");
        int prod = NumUtils.whenNull(first, 1);
        for (Integer i : rest) {
          if (i != null) {
            prod *= i;
          }
        }
        return prod;
      }}
    .setHelp("Multiply a sequence of integers");
    
    type.new CFunc("prod",
        Param.req("array", PType.arrayOf(int.class))) {
      @Override
      public Integer make(ArgSet args) {
        Integer[] array = args.<Integer[]>get("array");
        if (array == null) {
          return null;
        }
        int prod = 1;
        for (Integer i : array) {
          if (i != null) {
            prod *= i;
          }
        }
        return prod;
      }}
    .setHelp("Multiply a sequence of integers");
    

    type.new CFunc("clip",
                   Param.req("n", int.class),
                   Param.kwd("min", int.class, null),
                   Param.kwd("max", int.class, null)) {
      @Override
      public Integer make(ArgSet args) throws MFailed {
        Integer n = args.<Integer>get("n");
        if (n == null) {
          return null;
        }
        Integer min = args.<Integer>get("min");
        if (min != null && min > n) {
          return min;
        }
        Integer max = args.<Integer>get("max");
        if (max != null && max < n) {
          return max;
        }
        return n;
      }
    }
    .setHelp("Clip an integer to a range");
    
    type.new CFunc("max",
        Param.req("first", int.class),
        Param.rest("rest", int.class)) {
      @Override
      public Integer make(ArgSet args) {
        Integer max = args.<Integer>get("first");
        List<Integer> rest = args.<List<Integer>>get("rest");
        for (Integer i : rest) {
          if (i != null) {
            if (max == null || i > max) {
              max = i;
            }
          }
        }
        return max;
      }}
    .setHelp("The maximum of a sequence of integers");

    type.new CFunc("max",
        Param.req("array", PType.arrayOf(int.class))) {
      @Override
      public Integer make(ArgSet args) {
        Integer[] array = args.<Integer[]>get("array");
        if (array == null) {
          return null;
        }
        Integer max = null;
        for (Integer i : array) {
          if (i != null) {
            if (max == null || i > max) {
              max = i;
            }
          }
        }
        return max;
      }}
    .setHelp("The maximum of a sequence of integers");

    type.new CFunc("min",
        Param.req("first", int.class),
        Param.rest("rest", int.class)) {
      @Override
      public Integer make(ArgSet args) {
        Integer min = args.<Integer>get("first");
        List<Integer> rest = args.<List<Integer>>get("rest");
        for (Integer i : rest) {
          if (i != null) {
            if (min == null || i < min) {
              min = i;
            }
          }
        }
        return min;
      }}
    .setHelp("The minimum of a sequence of integers");
    
    type.new CFunc("min",
        Param.req("array", PType.arrayOf(int.class))) {
      @Override
      public Integer make(ArgSet args) {
        Integer[] array = args.<Integer[]>get("array");
        if (array == null) {
          return null;
        }
        Integer min = null;
        for (Integer i : array) {
          if (i != null) {
            if (min == null || i < min) {
              min = i;
            }
          }
        }
        return min;
      }}
    .setHelp("The minimum of a sequence of integers");
    
    type.new CFunc("and",
        Param.req("first", int.class),
        Param.rest("rest", int.class)) {
      @Override
      public Integer make(ArgSet args) {
        Integer val = args.<Integer>get("first");
        List<Integer> rest = args.<List<Integer>>get("rest");
        for (Integer i : rest) {
          if (i != null) {
            if (val == null) {
              val = i;
            } else {
              val &= i;
            }
          }
        }
        return val;
      }}
    .setHelp("The AND of a sequence of integers");
    
    type.new CFunc("and",
        Param.req("array", PType.arrayOf(int.class))) {
      @Override
      public Integer make(ArgSet args) {
        Integer[] array = args.<Integer[]>get("array");
        if (array == null) {
          return null;
        }
        Integer val = null;
        for (Integer i : array) {
          if (i != null) {
            if (val == null) {
              val = i;
            } else {
              val &= i;
            }
          }
        }
        return val;
      }}
    .setHelp("The AND of a sequence of integers");

    type.new CFunc("or",
        Param.req("first", int.class),
        Param.rest("rest", int.class)) {
      @Override
      public Integer make(ArgSet args) {
        Integer val = args.<Integer>get("first");
        List<Integer> rest = args.<List<Integer>>get("rest");
        for (Integer i : rest) {
          if (i != null) {
            if (val == null) {
              val = i;
            } else {
              val |= i;
            }
          }
        }
        return val;
      }}
    .setHelp("The OR of a sequence of integers");
    
    type.new CFunc("or",
        Param.req("array", PType.arrayOf(int.class))) {
      @Override
      public Integer make(ArgSet args) {
        Integer[] array = args.<Integer[]>get("array");
        if (array == null) {
          return null;
        }
        Integer val = null;
        for (Integer i : array) {
          if (i != null) {
            if (val == null) {
              val = i;
            } else {
              val |= i;
            }
          }
        }
        return val;
      }}
    .setHelp("The OR of a sequence of integers");
    
    type.new CFunc("abs", Param.req("n", int.class)) {
      @Override
      public Integer make(ArgSet args) {
        Integer n = args.<Integer>get("n");
        if (n == null) {
          return null;
        }
        return n < 0 ? -n : n;
      }}
    .setHelp("The absolute value of an integer");

    type.new CFunc("round", Param.req("n", double.class)) {
      @Override
      public Integer make(ArgSet args) {
        Double n = args.<Double>get("n");
        if (n == null) {
          return null;
        }
        return (int)Math.round(n);
      }}
    .setHelp("A real number rounded to the nearest integer");

    type.new CFunc("ceil", Param.req("n", double.class)) {
      @Override
      public Integer make(ArgSet args) {
        Double n = args.<Double>get("n");
        if (n == null) {
          return null;
        }
        return (int)Math.floor(n);
      }}
    .setHelp("The largest integer less than or equal to a real number");

    type.new CFunc("floor", Param.req("n", double.class)) {
      @Override
      public Integer make(ArgSet args) {
        Double n = args.<Double>get("n");
        if (n == null) {
          return null;
        }
        return (int)Math.ceil(n);
      }}
    .setHelp("The smallest integer greater than or equal to a real number");

    type.new CFunc("pow", 
        Param.req("base", int.class),
        Param.req("exp", int.class)) 
        {
      @Override
      public Integer make(ArgSet args) {
        Integer base = args.<Integer>get("base");
        Integer exp = args.<Integer>get("exp");
        if (base == null || exp == null) {
          return null;
        }
        return (int)Math.pow(base, exp);
      }}
    .setHelp("An integer base raised to an integer power");

    type.new CFunc("pow2", 
        Param.req("exp", int.class)) 
        {
      @Override
      public Integer make(ArgSet args) throws MFailed {
        Integer exp = args.<Integer>get("exp");
        if (exp == null) {
          return null;
        }
        if (exp < 0 || exp >= Integer.SIZE) {
          throw new MFailed(this, args, String.format("Exponent %s not in range [0, %d]", exp, Integer.SIZE));
        }
        return 1<<exp;
      }}
    .setHelp(String.format("2 raised to an integer power.  Exponent must be in [0, %d]", Integer.SIZE));
  }
  
  @Override
  public String describe() {
    return "An 32-bit integer value";
  }
  @Override
  public String syntax() {
    return "A sequence of digits, optionally preceded by a plus or minus sign.  " +
    		"Digits may be separated by underscores for readability.  If not in a sequence, commas may also be used. " +
    		"Hexadecimal numbers may be specified as \"0x\" followed by hexadecimal digits, with undesrcores as separators.";
        
  }


  public static void main(String[] args) {
    IntType t = new IntType();
    System.out.println(t.format(0));
    System.out.println(t.format(10));
    System.out.println(t.format(100));
    System.out.println(t.format(1000));
    System.out.println(t.format(10000));
    System.out.println(t.format(100000));
    System.out.println(t.format(1000000));
    System.out.println(t.format(10000000));
    System.out.println(t.format(-10));
    System.out.println(t.format(-100));
    System.out.println(t.format(-1000));
    System.out.println(t.format(-10000));
    System.out.println(t.format(-100000));
    System.out.println(t.format(-1000000));
    System.out.println(t.format(-10000000));
    System.out.println(t.format(Integer.MAX_VALUE));
  }

  

}
