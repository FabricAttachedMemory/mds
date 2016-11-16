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

package com.hpl.erk.formatters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import com.hpl.erk.util.ObjUtils;
import com.hpl.erk.util.Strings;

/**
 * A general formatter for sequences, allowing specification of the separator (what, if anything, comes between items),
 * the delimiters (what, if anything come before and after the sequence), default and class-specific formatters (or 
 * format strings) to use for the items in the list, special forms for empty lists, and special separators to use before
 * the last element of a list (e.g., to insert "and") or when there are only two elements.
 * <p>
 *  Once a <code>SeqFormatter</code> has been constructed, there are two ways to use it.  First, you can iteratively call
 *  {@link #add(Object)}, {@link #addFormatted(String, Object...)}, {@link #addAll(Iterable)}, or {@link #addAll(T[])}.  These
 *  add elements to the list.  With <code>add()</code> and <code>addAll()</code>, the elements need to be of type <code>T</code> 
 *  (the parameter of the SeqFormatter, usually simply {@link Object}).  With <code>addFormatted(fmt, arg...)</code> the arguments
 *  are used to create a formatted string, which is added as the element.  Once all of the elements have been added, calling 
 *  {@link #toString()} will return the formatted list.  Note that objects may still be added afterwards.
 *  <p>
 *  The second way to use the formatter is to call {@link #format(Iterable, Option...)} or {@link #format(T[], Option...)}.
 *  These are essentially equivalent to calling <code>using(options).addAll(elements).toString()</code>, except that any 
 *  elements already added to the formatter are ignored for this operation and the elements added in this operation are removed
 *  afterward.  So the same formatter can be used for several calls to <code>format()</code>.  Similarly, options specified as part of the call
 *  are applied to this call only and do not affect subsequent calls to the formatter.
 * <p>
 * <code>SeqFormatter</code>s can be configured either by means of <em>modifier methods</em>, which return the formatter 
 * being configured and so can be chained together or by means of {@link Option}s, passed in as parameters.  Static methods
 * exist for several common forms, which can be parameterized with {@link Option}s or modified by chaining modifier methods.
 * For example, to get a formatter specifying angle brackets ("&lt;...&gt;") and semicolon separators, you could write
 *  <pre>
 *     SeqFormatter.angleBracketList().sep("; ")...
 *     SeqFormatter.angleBracketList(Options.sep("; "))...
 *     SeqFormatter.withSep("; ", Option.angleBrackets)...
 *     SeqFormatter.withSep("; ").using(Option.angleBrackets)...
 *     SeqFormatter.withSep("; ").delims("<", ">")...
 *     SeqFormatter.with(Option.sep("; "), Option.angleBrackets)...
 *     SeqFormatter.with(Option.sep("; "), Option.open("<"), Option.close(">"))...
 *     new SeqFormatter().sep("; ").delims("<", ">")...
 *  </pre>
 *  and probably lots of other possibilities.  The basic list formats are {@link #parenList(Option...)}, 
 *  {@link #bracketList(Option...)}, {@link #bracesList(Option...)}, and {@link #angleBracketList(Option...)}.  
 *  {@link #list(Option...)} is equivalent to <code>bracketList(...)</code>.  The default separator for all of these,
 *  which can be overridden by an option parameter of modified by a modifier method, is ", " (comma space).  Undelimited
 *  list formatters can be obtained by calling {@link #withSep(String, Option...)}, {@link #commaSep(Option...)}, 
 *  {@link #commaSpaceSep(Option...)}, {@link #spaceSep(Option...)},  {@link #tabSep(Option...)}, or {@link #lines(Option...)} (i.e., newline-separated).
 *  The most general form is obtained by {@link #with(Option...)} or, equivalently, {@link #cat(Option...)}. 
 *  <p>
 *  The modifiers and options are as follows:
 *  <ul>
 *  <li> Opening and closing delimiters are specified by means of {@link #open(String)}, {@link #close(String)}, {@link #delims(String, String)},
 *  {@link Option#open(String)}, {@link Option#close(String)}, {@link Option#delims(String, String)}, {@link Option#brackets},
 *  {@link Option#parens}, {@link Option#bracesList}, and {@link Option#angleBrackets}.
 *  <li> Separators are specified by means of {@link #sep(String)}, {@link Option#sep(String)}, {@link Option#commaSep}, {@link Option#commaSpaceSep},
 *  {@link Option#spaceSep}, {@link Option#tabSep}, and {@link Option#newlineSep}.
 *  <li> Both delimiters and comma-space separators can be specified with {@link Option#list}, {@link Option#parenList}, 
 *  {@link Option#bracesList}, or {@link Option#angleBracketList}.
 *  <li> A special value to use when the list is empty (e.g., "NIL" or "none") can be specified by {@link #empty(String)} or {@link Option#empty(String)}.
 *  <li> Special separators to use before the last element of the list or when there are only to elements can be specified by 
 *  {@link #last(String)}, {@link #last(String, String)}, {@link #pairSep}, {@link Option#lastSep(String)}, {@link Option#pairSep(String)},
 *  {@link Option#lastSep(String, String)}, {@link Option#finalAnd}, and {@link Option#oxfordComma}.  The two-argument forms specify
 *  first the form to use when there are more than two elements and, second, the form to use when there are only two elements.  
 *  <code>Option.finalAnd</code> specifies using commas, but <code>" and "</code> before the last item.  <code>Option.oxfordComma</code> specifies
 *  using commas, <code>" and "</code> for two-element lists, and <code>", and "</code> for lists of more than two members.
 *  <li>Special value formatters can be specified by {@link #fmt(Formatter)}, {@link #fmt(String)}, {@link #fmt(Class, Formatter)},
 *  {@link #fmt(Class, String)}, {@link Option#fmt(String)}, {@link Option#fmt(Class, Formatter)}, or {@link Option#fmt(Class, String)}.
 *  The methods that take strings use these as format paramers to <code>String.format(fmt, element, index)</code>, so both the value
 *  and its index are available.  Those taking {@link Formatter} objects allow passing in an object that knows how to format the
 *  value given the element and its index.  Methods and options taking class values only apply to values that are of the specified
 *  class.  Later-specified formatters override those specified earlier.  This includes non-class-formatters overriding earlier
 *  class-formatters.
 *  </ul>
 *  
 * 
 * @author Evan Kirshenbaum
 *
 * @param <T>
 */

public class SeqFormatter<T> implements Cloneable, com.hpl.erk.func.Consumer<T>, Stringifier<Iterable<? extends T>> {
  private String sep;
  private String lastSep;
  private String pairSep;
  private String empty;
  private String open;
  private String close;
  private Formatter<? super T> formatter;
  private Formatter<String> wrapFormatter;
  private Formatter<? super Integer> numFormatter;
  transient private List<String> strings = null;
  private boolean noSep = false;
  
  /**
   * Options for configuring a {@link SeqFormatter}. 
   *
   */
  public static abstract class Option {
    /**
     * Separate items by commas (without following spaces)
     */
    public static final Option commaSep = new Option() {
      @Override
      <T> SeqFormatter<T> modify(SeqFormatter<T> f) {
        return f.sep(",");
      }
    };
    /**
     * Separate items by single spaces.
     */
    public static final Option spaceSep = new Option() {
      @Override
      <T> SeqFormatter<T> modify(SeqFormatter<T> f) {
        return f.sep(" ");
      }
    };
    /**
     * Separate items by commas followed by spaces.
     */
    public static final Option commaSpaceSep = new Option() {
      @Override
      <T> SeqFormatter<T> modify(SeqFormatter<T> f) {
        return f.sep(", ");
      }
    };
    /**
     * Separate items by tabs.
     */
    public static final Option tabSep = new Option() {
      @Override
      <T> SeqFormatter<T> modify(SeqFormatter<T> f) {
        return f.sep("\t");
      }
    };
    /**
     * Separate items by newlines.
     */
    public static final Option newlineSep = new Option() {
      @Override
      <T> SeqFormatter<T> modify(SeqFormatter<T> f) {
        return f.sep("\n");
      }
    };
    /**
     * Place parentheses (<code>"(...)"</code>) around the list.
     */
    public static final Option parens = new Option() {
      @Override
      <T> SeqFormatter<T> modify(SeqFormatter<T> f) {
        return f.delims("(", ")");
      }
    };
    /**
     * Place curly braces (<code>"{...}"</code>) around the list.
     */
    public static final Option braces = new Option() {
      @Override
      <T> SeqFormatter<T> modify(SeqFormatter<T> f) {
        return f.delims("{", "}");
        
      }
    };
    /**
     * Place (square) brackets (<code>"[...]"</code>) around the list.
     */
    public static final Option brackets = new Option() {
      @Override
      <T> SeqFormatter<T> modify(SeqFormatter<T> f) {
        return f.delims("[", "]");
      }
    };
    /**
     * Place angle brackets (<code>"<...>"</code>) around the list.
     */
    public static final Option angleBrackets = new Option() {
      @Override
      <T> SeqFormatter<T> modify(SeqFormatter<T> f) {
        return f.delims("<", ">");
      }
    };
    /**
     * Use {@link #commaSpaceSep} and also use <code>" and "</code> between the last two elements, e.g., "a and b" or 
     * "a, b and c".  
     */
    public static final Option finalAnd = new Option() {
      @Override
      <T> SeqFormatter<T> modify(SeqFormatter<T> f) {
        return commaSpaceSep.modify(f).last(" and ", " and ");
      }
    };
    /**
     * Like {@link #finalAnd}, but include the comma before the <code>"and"</code> when there are more than two items,
     * e.g., "a, b, and c".
     */
    public static final Option oxfordComma = new Option() {
      @Override
      <T> SeqFormatter<T> modify(SeqFormatter<T> f) {
        return commaSpaceSep.modify(f).last(", and ", " and ");
      }
    };
    /**
     * Equivalent to both {@link #brackets} and {@link #commaSpaceSep}.
     */
    public static final Option list = new Option() {
      @Override
      <T> SeqFormatter<T> modify(SeqFormatter<T> f) {
        return f.using(Option.brackets, Option.commaSpaceSep);
      }
    };
    /**
     * Equivalent to both {@link #parens} and {@link #commaSpaceSep}.
     */
    public static final Option parenList = new Option() {
      @Override
      <T> SeqFormatter<T> modify(SeqFormatter<T> f) {
        return f.using(Option.parens, Option.commaSpaceSep);
      }
    };
    /**
     * Equivalent to both {@link #braces} and {@link #commaSpaceSep}.
     */
    public static final Option bracesList = new Option() {
      @Override
      <T> SeqFormatter<T> modify(SeqFormatter<T> f) {
        return f.using(Option.braces, Option.commaSpaceSep);
      }
    };
    /**
     * Equivalent to both {@link #angleBrackets} and {@link #commaSpaceSep}.
     */
    public static final Option angleBracketList = new Option() {
      @Override
      <T> SeqFormatter<T> modify(SeqFormatter<T> f) {
        return f.using(Option.angleBrackets, Option.commaSpaceSep);
      }
    };
    public static Option wrapCSV = new Option() {
      @Override
      <T> SeqFormatter<T> modify(SeqFormatter<T> f) {
        return f.wrapCSV();
      }
    };
    
    /**
     * Returns an option that specifies using the supplied string to separate items.
     * @param sep the string to use
     * @return the option
     */
    public static Option sep(final String sep) {
      return new Option() {
        @Override
        <T> SeqFormatter<T> modify(SeqFormatter<T> f) {
          return f.sep(sep);
        }
      };
    }
    /**
     * Returns an option that specifies using the supplied string when the list is
     * empty.
     * @param form the string to use
     * @return the option
     */
    public static Option empty(final String form) {
      return new Option() {
        @Override
        <T> SeqFormatter<T> modify(SeqFormatter<T> f) {
          return f.empty(form);
        }
      };
    }
    /**
     * Returns an option that specifies using the supplied string as the opening
     * delimiter.
     * @param open the string to use
     * @return the option
     */
    public static Option open(final String open) {
      return new Option() {
        @Override
        <T> SeqFormatter<T> modify(SeqFormatter<T> f) {
          return f.open(open);
        }
      };
    }
    /**
     * Returns an option that specifies using the supplied string as the closing
     * delimiter
     * @param close the string to use
     * @return the option
     */
    public static Option close(final String close) {
      return new Option() {
        @Override
        <T> SeqFormatter<T> modify(SeqFormatter<T> f) {
          return f.close(close);
        }
      };
    }
    /**
     * Returns an option that specifies using the supplied strings as the delimiters
     * @param open the opening delimiter
     * @param close the closing delimiter
     * @return the option
     */
    public static Option delims(final String open, final String close) {
      return new Option() {
        @Override
        <T> SeqFormatter<T> modify(SeqFormatter<T> f) {
          return f.delims(open, close);
        }
      };
    }
    /**
     * Returns an option that specifies using the supplied strings before the last
     * item in the list.
     * @param lastSep the separator to use when there are more than two items
     * @param pairSep the separator to use when there are two items
     * @return the option
     */
    public static Option lastSep(final String lastSep, final String pairSep) {
      return new Option() {
        @Override
        <T> SeqFormatter<T> modify(SeqFormatter<T> f) {
          return f.last(lastSep, pairSep);
        }
      };
    }
    /**
     * Returns an option that specifies using the supplied string before the
     * last item in the list.
     * @param lastSep the separator to use
     * @return the option
     */
    public static Option lastSep(final String lastSep) {
      return new Option() {
        @Override
        <T> SeqFormatter<T> modify(SeqFormatter<T> f) {
          return f.last(lastSep);
        }
      };
    }

    /**
     * Returns an option that specifies separating items by newlines followed
     * by the specified prefix.
     * @param prefix the prefix to use
     * @return the option
     */
    public static Option indentedLineSep(final String prefix) {
      return new Option() {
        @Override
        <T> SeqFormatter<T> modify(SeqFormatter<T> f) {
          return f.sep("\n"+prefix);
        }
      };
    }
    /**
     * Returns an option that specifies separating items by newlines followed
     * by an indentation of the specified number of spaces
     * @param prefix the indent
     * @return the option
     */
    public static Option indentedLineSep(int indent) {
      return indentedLineSep(Strings.spaces(indent));
    }
    /**
     * Returns an option that specifies using the supplied string between items
     * when there are exactly two items in the list.
     * @param pairSep the separator to use
     * @return the option
     */
    public static Option pairSep(final String pairSep) {
      return new Option() {
        @Override
        <T> SeqFormatter<T> modify(SeqFormatter<T> f) {
          return f.pairs(pairSep);
        }
      };
    }
    /**
     * Returns an option that specifies using the supplied string as the format parameter to
     * format the items.  The first argument to the format is the item, and the second argument
     * is the index of the item in the list.
     * @param format the format specifier
     * @return the option
     */
    public static Option fmt(final String format) {
      return new Option() {
        @Override
        <T> SeqFormatter<T> modify(SeqFormatter<T> f) {
          return f.fmt(format);
        }
      };
    }
    /**
     * Returns an option that specifies using the supplied string as the format parameter
     * to format items of the supplied class.  The first argument to the format is the item, and the second argument
     * is the index of the item in the list.
     * @param clss the class bound
     * @param format the format specifier
     * @return the option
     */
    public static <X> Option fmt(final Class<X> clss, final String format) {
      return new Option() {
        @Override
        <T> SeqFormatter<T> modify(SeqFormatter<T> f) {
          return f.fmt(clss, format);
        }
      };
    }
    /**
     * Returns an option that specifies using the supplied {@link Formatter} to format items of the supplied class.
     * @param clss the class bound
     * @param formatter the formatter to use
     * @return the option
     */
    public static <X> Option fmt(final Class<X> clss, final Formatter<? super X> formatter) {
      return new Option() {
        @Override
        <T> SeqFormatter<T> modify(SeqFormatter<T> f) {
          return f.fmt(clss, formatter);
        }
      };
    }
    
    public static Option wrapFmt(final Formatter<String> formatter) {
      return new Option() {
        @Override
        <T> SeqFormatter<T> modify(SeqFormatter<T> f) {
          return f.wrapFmt(formatter);
        }
      };
    }
    public static Option wrapFmt(final String format) {
      return new Option() {
        @Override
        <T> SeqFormatter<T> modify(SeqFormatter<T> f) {
          return f.wrapFmt(format);
        }
      };
    }
    
    public static <X> Option numbered(final Formatter<? super Integer> formatter) {
      return new Option() {
        @Override
        <T> SeqFormatter<T> modify(SeqFormatter<T> f) {
          return f.numbered(formatter);
        }
      };
    }
    
    public static <X> Option numbered(final String format) {
      return new Option() {
        @Override
        <T> SeqFormatter<T> modify(SeqFormatter<T> f) {
          return f.numbered(format);
        }
      };
    }
    
    public static <X> Option numbered(final int numWidth) {
      return new Option() {
        @Override
        <T> SeqFormatter<T> modify(SeqFormatter<T> f) {
          return f.numbered(numWidth);
        }
      };
    }
    
    public static <X> Option numbered() {
      return new Option() {
        @Override
        <T> SeqFormatter<T> modify(SeqFormatter<T> f) {
          return f.numbered();
        }
      };
    }
    
    abstract <T> SeqFormatter<T> modify(SeqFormatter<T> f);
  }
  
  
  /**
   * An object that formats a list item given the item and its index in the list. 
   * @param <T>
   */
  public static interface Formatter<T> {
    /**
     * 
     * @param obj an object
     * @param index the index of the object in the list
     * @return a formatted representation of the object.
     */
    public String format(T obj, int index);
  }
  
  private static class CSVFormatter implements Formatter<String> {
    static Pattern needQuotePattern = Pattern.compile("[\"\\n,]");
    @Override
    public String format(String s, int index) {
      s = s.replace("\"", "\"\"");
      if (needQuotePattern.matcher(s).find()) {
        s = "\""+s+"\"";
      }
      return s;
    }
    
  }
  
  /**
   * Create a new {@link SeqFormatter}
   * @param options {@link Option}s to apply
   */
  public SeqFormatter(Option ... options) {
    using(options);
  }
  
  /**
   * Create a new {@link SeqFormatter} with specified delimiters and separator
   * @param open the opening delimiter
   * @param sep the separator
   * @param close the closing delimiter 
   * @param options further {@link Option}s to apply.
   */
  public SeqFormatter(String open, String sep, String close, Option ... options) {
    this(options);
    this.open = open;
    this.sep = sep;
    this.close = close;
  }
  
  /**
   * Create a new {@link SeqFormatter} with a specified separator.
   * @param sep the separator
   * @param options further {@link Option}s to apply
   */
  public SeqFormatter(String sep, Option ... options) {
    this(options);
    this.sep = sep;
  }

  /**
   * Modify this {@link SeqFormatter} to apply the given {@link Option}s.
   * @param options the options to apply
   * @return this object
   */
  public SeqFormatter<T> using(Option ...options) {
    for (Option opt : options ) {
      opt.modify(this);
    }
    return this;
  }
  
  /**
   * Modify this {@link SeqFormatter} to use the given separator
   * @param sep the separator
   * @return this object
   */
  public SeqFormatter<T> sep(String sep) {
    this.sep = sep;
    return this;
  }
  
  /**
   * Modify this {@link SeqFormatter} to use the given opening delimiter
   * @param open the opening delimiter
   * @return this object
   */
  public SeqFormatter<T> open(String open) {
    this.open = open;
    return this;
  }

  /**
   * Modify this {@link SeqFormatter} to use the given closing delimiter
   * @param close the closing delimiter
   * @return this object
   */
  public SeqFormatter<T> close(String close) {
    this.close= close;
    return this;
  }
  
  /**
   * Modify this {@link SeqFormatter} to use the given delimiters.  
   * @param open the opening delimiter
   * @param close the closing delimiter
   * @return this object
   */
  public SeqFormatter<T> delims(String open, String close) {
    return open(open).close(close);
  }
  
  /**
   * Modify this {@link SeqFormatter} to use the given separators
   * before the last item in the list
   * @param sep the separator to use when there are more than two items in the list
   * @param pairSep the separator to use when there are two items in the list
   * @return this object
   */
  public SeqFormatter<T> last(String sep, String pairSep) {
    return last(sep).pairs(pairSep);
  }
  
  /**
   * Modify this {@link SeqFormatter} to use the given separator before the last
   * item in the list.
   * @param sep the separator
   * @return this object
   */
  public SeqFormatter<T> last(String sep) {
    this.lastSep = sep;
    return this;
  }
  
  /**
   * Modify this {@link SeqFormatter} to use the given separator when there
   * are exactly two items in the list
   * @param sep the separator
   * @return this object
   */
  public SeqFormatter<T> pairs(String sep) {
    this.pairSep = sep;
    return this;
  }
  
  /**
   * Modify this {@link SeqFormatter} to use the given form when the list is empty
   * @param form the form to use
   * @return this object
   */
  public SeqFormatter<T> empty(String form) {
    this.empty = form;
    return this;
  }
  
  /**
   * Modify this {@link SeqFormatter} to use the given {@link Formatter} to format
   * items of the given class.
   * @param clss the class bound
   * @param formatter the formatter
   * @return this object
   */
  public <X> SeqFormatter<T> fmt(final Class<X> clss, final Formatter<? super X> formatter) {
    final Formatter<? super T> old = this.formatter;
    return fmt(new Formatter<T>() {
      @Override
      public String format(T obj, int index) {
        if (obj != null && clss.isAssignableFrom(obj.getClass())) {
          @SuppressWarnings("unchecked")
          X o = (X)obj;
          return formatter.format(o, index);
        } else if (old == null) {
          return String.format("%s", obj);
        } else {
          return old.format(obj, index);
        }
      }
    });
  }
  
  /**
   * Modify this {@link SeqFormatter} to use the given format string (via {@link String#format(String, Object...)} to format
   * items of the given class. 
   * @param clss the class bound
   * @param format the format string
   * @return this object
   */
  public <X> SeqFormatter<T> fmt(Class<X> clss, String format) {
    return fmt(clss, useStringFormat(format));
  }
  
  public SeqFormatter<T> wrapFmt(Formatter<String> formatter) {
    this.wrapFormatter = formatter;
    return this;
  }
  
  public SeqFormatter<T> wrapFmt(final String format) {
    return wrapFmt(new Formatter<String>() {
      @Override
      public String format(String obj, int index) {
        return String.format(format, obj, index);
      }});
  }

  public SeqFormatter<T> wrapCSV() {
    return wrapFmt(new CSVFormatter());
  }

  public SeqFormatter<T> numbered(Formatter<? super Integer> formatter) {
    this.numFormatter = formatter;
    return this;
  }
  
  public SeqFormatter<T> numbered(final String fmt) {
    return numbered(new Formatter<Integer>() {
      @Override
      public String format(Integer num, int index) {
        return String.format(fmt, num);
      }
    });
  }
  
  public SeqFormatter<T> numbered(int numWidth) {
    return numbered("%"+numWidth+",d. ");
  }
  public SeqFormatter<T> numbered() {
    return numbered("%,d. ");
  }

  /**
   * Modify this {@link SeqFormatter} to use the given {@link Formatter} to format items.
   *
   * @param formatter the formatter
   * @return this object
   */
  public SeqFormatter<T> fmt(Formatter<? super T> formatter) {
    this.formatter = formatter;
    return this;
  }
  
 /**
  * Modify this {@link SeqFormatter} to use the given format string (via {@link String#format(String, Object...)} to format
  * items.  The first argument to the format is the item, and the second argument is the index
  * of the item in the list
  * @param format the format string
  * @return this object
  */
  public SeqFormatter<T> fmt(final String format) {
    return fmt(useStringFormat(format));
  }

  private static Formatter<Object> useStringFormat(final String format) {
    return new Formatter<Object>() {
      @Override
      public String format(Object obj, int index) {
        return String.format(format, obj, index);
      }};
  }
  
  /**
   * Add an element to the formatted list.
   * @param elt the element to add
   * @return this object
   */
  public SeqFormatter<T> add(T elt) {
    ensureList();
    final int index = strings.size();
    String s = formatter != null ? formatter.format(elt,  index) : String.format("%s", elt);
    if (wrapFormatter != null) {
      s = wrapFormatter.format(s, index);
    }
    if (numFormatter != null) {
      s = numFormatter.format(index, index)+s;
    }
    addToStrings(s);
    return this;
  }
  
  private void addToStrings(String s) {
    boolean wasNoSep = noSep;
    noSep = false;
    if (wasNoSep) {
      int lastIndex = strings.size()-1;
      if (lastIndex >= 0) {
        String old = strings.get(lastIndex);
        strings.set(lastIndex, old+s);
        return;
      }
    }
    strings.add(s);
  }

  private void ensureList() {
    if (strings == null) {
      strings = new ArrayList<>();
    }
  }
  
  /**
   * Add a string, returned by calling {@link String#format(String, Object...)}, as the representation of an
   * item in the list.
   * @param fmt the format string for {@link String#format(String, Object...)}.
   * @param args the arguments for {@link String#format(String, Object...)}.
   * @return this object.
   */
  public SeqFormatter<T> addFormatted(String fmt, Object ...args) {
    ensureList();
    String s = String.format(fmt, args);
    addToStrings(s);
    return this;
  }
  
  public SeqFormatter<T> join() {
    noSep = true;
    return this;
  }
  
  public SeqFormatter<T> prefix(String fmt, Object...args) {
    return addFormatted(fmt, args).join();
  }
  
  public SeqFormatter<T> suffix(String fmt, Object...args) {
    return join().addFormatted(fmt, args);
  }
  
  public SeqFormatter<T> infix(String fmt, Object...args) {
    return suffix(fmt, args).join();
  }
  
  /**
   * Add all of the elements in the array to the formatted list
   * @param array The elements to add
   * @return this object
   */
  public SeqFormatter<T> addAll(T[] array) {
    for (T elt : array) {
      add(elt);
    }
    return this;
  }
  
  /**
   * Add all of the elements to the formatted list
   * @param iterable The elements to add
   * @return this object
   */
  public SeqFormatter<T> addAll(Iterable<? extends T> iterable) {
    for (T elt : iterable) {
      add(elt);
    }
    return this;
  }
  
  /**
   * Creates a copy of the object with respect to formatting rules but not with respect to content.
   */
  @Override
  public SeqFormatter<T> clone() {
    try {
      return ObjUtils.castClone(this, super.clone());
    } catch (CloneNotSupportedException e) {
      throw new IllegalStateException(e);
    }
  }
  
  /**
   * Returns a formatted representation of the supplied sequence using the formatting rules in this {@link SeqFormatter}
   * temporarily modified by any supplied {@link Option}s.  Current contents of this object are ignored (but restored afterwards)
   * and supplied content and options are undone following the operation.
   * @param iterable the elements to format
   * @param options extra options to apply
   * @return the formatted string.
   */
  public String format(Iterable<? extends T> iterable, Option...options) {
    if (options != null && options.length > 0) {
      return clone().using(options).addAll(iterable).toString();
    }
    final List<String> old = strings;
    strings = null;
    addAll(iterable);
    String s = toString();
    strings = old;
    return s;
  }
  
  /**
   * Returns a formatted representation of the supplied sequence using the formatting rules in this {@link SeqFormatter}
   * temporarily modified by any supplied {@link Option}s.  Current contents of this object are ignored (but restored afterwards)
   * and supplied content and options are undone following the operation.
   * @param array the elements to format
   * @param options extra options to apply
   * @return the formatted string.
   */
  public String format(T[] array, Option...options) {
    return format(Arrays.asList(array), options);
  }
  
  /**
   * Returns a formatted representation of the sequence specified by calls to {@link #add(Object)}, {@link #addFormatted(String, Object...)},
   * {@link #addAll(Iterable)}, and {@link #addAll(Object[])}.
   */
  @Override
  public String toString() {
    ensureList();
    int n = strings.size();
    if (n == 0 && empty != null) {
      return empty;
    }
    StringBuilder bldr = new StringBuilder();
    String s = Strings.notNull(sep);
    String ls = lastSep == null ? s : lastSep;
    if (n == 2 && pairSep != null) {
      ls = pairSep;
    }
    if (open != null) {
      bldr.append(open);
    }
    int i=0;
    for (String elt : strings) {
      if (i++ > 0) {
        bldr.append(i==n ? ls : s);
      }
      bldr.append(elt);
    }
    if (close != null) {
      bldr.append(close);
    }
    return bldr.toString();
  }

  /**
   * Creates a {@link SeqFormatter} configured by the provided {@link Option}s.  Equivalent to {@link SeqFormatter#with(Option...)}.
   * @param options options to apply
   * @return the SeqFormatter.
   */
  public static <T> SeqFormatter<T> cat(Option...options) {
    return with(options);
  }
  
  /**
   * Creates a {@link SeqFormatter} configured by the provided {@link Option}s.  Equivalent to {@link SeqFormatter#cat(Option...)}.
   * @param options options to apply
   * @return the SeqFormatter.
   */
  public static <T> SeqFormatter<T> with(Option... options) {
    return new SeqFormatter<T>(options);
  }

  /**
   * Creates a {@link SeqFormatter} using the supplied separator.
   * @param sep the separator
   * @param options extra options to apply
   * @return the SeqFormatter.
   */
  public static <T> SeqFormatter<T> withSep(final String sep, Option...options) {
    return SeqFormatter.<T>with(options).sep(sep);
  }

  /**
   * Creates a {@link SeqFormatter} using newlines as the separator
   * @param options extra options to apply
   * @return the SeqFormatter
   */
  public static <T> SeqFormatter<T> lines(Option...options) {
    return SeqFormatter.<T>with(options).using(Option.newlineSep);
  }
  /**
   * Creates a {@link SeqFormatter} using newlines followed by a line prefix as the separator
   * @param prefix the prefix
   * @param options extra options to apply
   * @return the SeqFormatter
   */
  public static <T> SeqFormatter<T> lines(String prefix, Option...options) {
    return SeqFormatter.<T>with(options).using(Option.indentedLineSep(prefix));
  }
  /**
   * Creates a {@link SeqFormatter} using newlines followed by an indentation as the separator
   * @param indent the number of spaces to indent
   * @param options extra options to apply
   * @return the SeqFormatter
   */
  public static <T> SeqFormatter<T> lines(int indent, Option...options) {
    return SeqFormatter.<T>with(options).using(Option.indentedLineSep(indent));
  }

  /**
   * Creates a {@link SeqFormatter} using commas as the separator
   * @param options extra options to apply
   * @return the SeqFormatter
   */
  public static <T> SeqFormatter<T> commaSep(Option...options) {
    return SeqFormatter.<T>with(options).using(Option.commaSep);
  }
  
  public static <T> SeqFormatter<T> asCSV(Option...options) {
    return SeqFormatter.<T>with(Option.commaSep, Option.wrapCSV).using(options);
  }
  /**
   * Creates a {@link SeqFormatter} using tabs as the separator
   * @param options extra options to apply
   * @return the SeqFormatter
   */
  public static <T> SeqFormatter<T> tabSep(Option...options) {
    return SeqFormatter.<T>with(options).using(Option.tabSep);
  }
  /**
   * Creates a {@link SeqFormatter} using spaces as the separator
   * @param options extra options to apply
   * @return the SeqFormatter
   */
  public static <T> SeqFormatter<T> spaceSep(Option...options) {
    return SeqFormatter.<T>with(options).using(Option.spaceSep);
  }
  /**
   * Creates a {@link SeqFormatter} using comma-space (<code>", "</code>) as the separator
   * @param options extra options to apply
   * @return the SeqFormatter
   */
  public static <T> SeqFormatter<T> commaSpaceSep(Option...options) {
    return SeqFormatter.<T>with(options).using(Option.commaSpaceSep);
  }
  /**
   * Creates a {@link SeqFormatter} using comma-space (<code>", "</code>) as the separator
   * and square brackets (<code>"[...]"</code>) as the delimiters.  Equivalent to 
   * {@link SeqFormatter#bracketList(Option...)}.
   * @param options extra options to apply
   * @return the SeqFormatter
   */
  public static <T> SeqFormatter<T> list(Option ...options) {
    return SeqFormatter.<T>with(Option.brackets, Option.commaSpaceSep).using(options);
  }
  /**
   * Creates a {@link SeqFormatter} using comma-space (<code>", "</code>) as the separator
   * and parents (<code>"(...)"</code>) as the delimiters.
   * @param options extra options to apply
   * @return the SeqFormatter
   */
  public static <T> SeqFormatter<T> parenList(Option ...options) {
    return SeqFormatter.<T>list(options).using(Option.parens);
  }
  /**
   * Creates a {@link SeqFormatter} using comma-space (<code>", "</code>) as the separator
   * and curly braces (<code>"{...}"</code>) as the delimiters.
   * @param options extra options to apply
   * @return the SeqFormatter
   */
  public static <T> SeqFormatter<T> bracesList(Option ...options) {
    return SeqFormatter.<T>list(options).using(Option.braces);
  }
  /**
   * Creates a {@link SeqFormatter} using comma-space (<code>", "</code>) as the separator
   * and square brackets (<code>"[...]"</code>) as the delimiters.  Equivalent to {@link SeqFormatter#list(Option...)}.
   * @param options extra options to apply
   * @return the SeqFormatter
   */
  public static <T> SeqFormatter<T> bracketList(Option ...options) {
    return SeqFormatter.<T>list(options);
  }
  /**
   * Creates a {@link SeqFormatter} using comma-space (<code>", "</code>) as the separator
   * and angle brackets (<code>"<...>"</code>) as the delimiters.
   * @param options extra options to apply
   * @return the SeqFormatter
   */
  public static <T> SeqFormatter<T> angleBracketList(Option ...options) {
    return SeqFormatter.<T>list(options).using(Option.angleBrackets);
  }
  
  
  
  public static void main(String[] args) {
    Integer[] a = { 1, 2, 3 };
    Integer[] b = { 4, 5, 6 };
    String s = SeqFormatter.<Integer>commaSep().format(a);
    String s2 = SeqFormatter.commaSep().format(Arrays.asList(a, b));
    System.out.format("%s%n%s%n", s, s2);
    String s3 = SeqFormatter.lines(5).format(a);
    System.out.format("%s%n", s3);
    String s4 = SeqFormatter.commaSpaceSep(Option.oxfordComma).add("a").add("b").infix("::").add("c").add("d").toString();
    System.out.format("%s%n", s4);
  }

  @Override
  public boolean see(T val) {
    add(val);
    return true;
  }
  
  public SeqFormatter<T> clear() {
    if (strings != null) {
      strings.clear();
    }
    return this;
  }

  @Override
  public String stringify(Iterable<? extends T> val) {
    return format(val);
  }

}