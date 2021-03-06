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

package com.hpl.erk.mash;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * This efficient String to T (or int) mapping class relies on strong, fast string hashes 
 * via MASH.  Thus, it works only for short strings (e.g. shorter than about 64 chars).
 * Future:  could offer .compact(bytes) call, which would convert to a sorted UniformLookupTable table = map.toLookupTable(nbytes);
 * Future:  could offer to de-duplicate the mapped values to a smaller range.
 * 
 * @author George.Forman@hp.com  (gforman)
 */
public class OpenStringMashMap<T> {
	
	public final ClosedLongHashMap map;// output from map gives index into values
	public final ArrayList<T> values = new ArrayList<T>();
	private final MASH_Algorithm64 hasher;
	
	/**
	 * Create a new map, which uses all characters and doesn't fold lowercase.
	 * @param expected - initial room for this many keys
	 * @param nValBytes - number of bytes that should be sufficient to map to the value indices
	 */
	public OpenStringMashMap(int expected, int nValBytes) {
	    this(expected,nValBytes,new CharMapGenerator64.AllChars());
	}

	public OpenStringMashMap(int expected, int nValBytes, CharMapGenerator64 charMapGenerator64) {
	    map = new ClosedLongHashMap(expected, nValBytes);
	    hasher = new MASH_Algorithm64(charMapGenerator64);
	}

	public long put(CharSequence key) {
		return put(key,0);
	}

	public long put(CharSequence key, T val) {
		int ival = values.size();
		values.add(val);
		return put(key, ival);
	}

	public long put(CharSequence key, int val) {
		long hash = hasher.mashSequence(key);
		put(hash, val);
		return hash;
	}

	public void put(long hashKey, int val) {
		map.put(hashKey, val);
	}

	
	public int getInt(CharSequence key) {
		long hash = hasher.mashSequence(key);
		return getInt(hash);
	}
	/** @return -1 if not found, else integer stored with this key */
	public int getInt(long hashKey) {
		return map.get(hashKey);
	}
	public T get(CharSequence key) {
		long hash = hasher.mashSequence(key);
		return get(hash);
	}
	/** @return null if not found, else the T stored with this key */
	public T get(long hashKey) {
		int i = getInt(hashKey);
		return i < 0 ? null : values.get(i);
	}
	
	
	
	
	
	
	public static void main(String[] args) {
		try {
			OpenStringMashMap<String> me = new OpenStringMashMap<String>(1<<21, 3);
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			for (String line; (line = in.readLine()) != null; ) {
				if (line.length() == 0) continue;
				if (me.getInt(line) >= 0) System.err.println("Duplicate: " + line);
				me.put(line, "hi");
				if (me.getInt(line) < 0) throw new IllegalArgumentException("Couldn't get what I put: " + line);
				
				if (!"hi".equals(me.get(line))) throw new IllegalArgumentException("Return result didn't match");
			}
			
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
	}
}
