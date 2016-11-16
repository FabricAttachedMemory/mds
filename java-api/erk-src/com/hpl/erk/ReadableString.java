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

package com.hpl.erk;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReadableString {
  private final String base;
  private final int len;
  private int cursor = 0;
  
  private static final Pattern ws = Pattern.compile("\\s+");
  private static final Pattern word = Pattern.compile("\\s*(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)");

  public ReadableString(String base) {
    this.base = base;
    this.len = base.length();
  }
  
  public int getCursor() {
    return cursor;
  }
  
  public void setCursor(int cursor) {
    this.cursor = cursor;
  }
  
  public String remaining() {
    return base.substring(cursor);
  }
  
  public Matcher consume(Pattern pattern) {
    Matcher m = pattern.matcher(remaining());
    if (m.lookingAt()) {
      cursor += m.group().length();
      return m;
    }
    return null;
  }
  
  public boolean skipWS() {
    Matcher m = consume(ws);
    return (m != null);
  }
  
  public boolean consume(String s) {
    if (remaining().startsWith(s)) {
      cursor += s.length();
      return true;
    }
    return false;
  }

  public char nextChar() {
    if (cursor == len) {
      return 0;
    }
    return base.charAt(cursor++);
  }
  
  public String nextWord() {
    Matcher m = consume(word);
    if (m == null) {
      return null;
    }
    return m.group(1);
  }

  private static final Pattern line = Pattern.compile("(.*?)\n");
  public String nextLine() {
    Matcher m = consume(line);
    if (m == null) {
      int length = base.length();
      if (cursor == length) {
        return null;
      }
      String s = remaining();
      cursor = length;
      return s;
    }
    return m.group(1);
    
  }


  public boolean consume(char c) {
    if (cursor < len && base.charAt(cursor) == c) {
      cursor++;
      return true;
    }
    return false;
  }
  
  @Override
  public String toString() {
    return String.format("\"%s||%s\"", base.substring(0, cursor), base.substring(cursor));
  }
  
  public static class ReadError extends Exception {

    private static final long serialVersionUID = -1151452046257039149L;

    public ReadError(ReadableString s, int resetTo, String fmt, Object...args) {
      super(mkMsg(s, resetTo, fmt, args));
    }
    
    public ReadError(ReadableString s, int resetTo) {
      super(mkMsg(s, resetTo));
    }

    public ReadError(ReadableString s, int resetTo, Throwable cause, String fmt, Object...args) {
      super(mkMsg(s, resetTo, fmt, args), cause);
    }
    public ReadError(ReadableString s, int resetTo, Throwable cause) {
      super(mkMsg(s, resetTo), cause);
    }

    

    protected static String mkMsg(ReadableString s, int resetTo, 
                                  String fmt, Object[] args) 
    {
      return String.format(fmt, args)+" "+mkMsg(s, resetTo);
    }
    protected static String mkMsg(ReadableString s, int resetTo) 
    {
      s.setCursor(resetTo);
      return fmtLoc(s);
    }

    public static String fmtLoc(ReadableString s) {
      return "@ "+s.toString();
    }

  }
  
  public static void main(String[] args) {
    String s = "This\nis a \n   test  \nhere";
    ReadableString rs = new ReadableString(s);
    String line;
    while ((line = rs.nextLine()) != null) {
      System.out.format("[%s]%n", line);
    }
  }

  public boolean atEnd() {
    return cursor == len;
  }
  
  public boolean atAny(String chars) {
    final int n = chars.length();
    if (cursor == len || n == 0) {
      return false;
    }
    char c = base.charAt(cursor);
    for (int i=0; i<n; i++) {
      if (c == chars.charAt(i)) {
        return true;
      }
    }
    return false;
  }

  public String substring(int start, int end) {
    return base.substring(start, end);
  }

}