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

package com.hpl.erk.text;



/** Normalize a database or search string for how it might be presented vs. typed/indexed.
 * 
 * @author George.Forman@hp.com  (gforman)
 */
public class Normalizer {

	
	/** Normalization map: lowercased, unaccented, etc.  Punctuation & all whitespace --> ' '. */
	public static char[] map;
	static {
		final int nChars = 1<<16;
		map = new char[nChars];
		for (int i = 0; i < nChars; i++) {
			char ch = (char) i;
			boolean keep = Character.isLetterOrDigit(ch) || Character.isIdeographic(ch);
			ch = !keep ? ' ' : Character.toLowerCase(Normalizer.mapAccentsAway(ch));//MAYDO: specify locale for toLowerCase
			map[i] = ch; 
		}
	}

	
	/** Normalize the string as a data field might be stored or presented to the user.  
	 * Preserves accents (after normalizing their composition), but trims spaces. 
	 */
	public static String normalizeField(String s) {
		s = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFKC);
		s = s.trim();
		return s;
	}
	
    private enum State { INITIAL, AFTER_SPACE, AFTER_NONSPACE };

    /** Normalize the string for how it might be typed.  Lowercase, map out accents, wide-chars, etc. 
	 * <pre>
	 * Removes/replaces:
	 * - leading/trailing whitespace/punctuation
	 * - map away accents
	 * - map German s-zet --> s
	 * - multiple whitespace/punctuation reduced to a single space.
	 * - lowercase	
	 * 
	 * PRE: previously s = normalizeField(s)
	 */
	/**
	 * @param s
	 * @return
	 */
	public static String normalizeTyped(String s) {
	  

		int len = s.length();
		StringBuilder buf = new StringBuilder(len);
		
		State state = State.INITIAL;

		for (int i = 0; i < len; i++) {
          char c = map[s.charAt(i)];// mapped character
		  if (c == ' ') {
		    if (state != State.INITIAL) {
		      state = State.AFTER_SPACE;
		    }
		  } else {
		    if (state == State.AFTER_SPACE) {
		      buf.append(' ');
		    }
		    buf.append(c);
		    state = State.AFTER_NONSPACE;
		  }
		}

		return buf.toString();
	}

  /** Map accented characters to unaccented versions. */
  public static char mapAccentsAway(char c) {
    String s = new String(new char[] {c});
    String n = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFKD);
    if (n.length() > 1) {
      char replacement = n.charAt(0);
      boolean replace = true;
      for (int i=1; replace && i<n.length(); i++) {
        final char c2 = n.charAt(i);
        final int type = Character.getType(c2);
        replace = type == Character.NON_SPACING_MARK;
      }
      if (replace) {
        c = replacement;
      }
    }
    return c;
  }

}



















