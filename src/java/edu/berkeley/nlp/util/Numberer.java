package edu.berkeley.nlp.util;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gives unique integer serial numbers to a family of objects, identified by a
 * name space. A Numberer is like a collection of {@link Index}es, and for many
 * purposes it is more straightforward to use an Index, but Numberer can be
 * useful precisely because it maintains a global name space for numbered object
 * families, and provides facilities for mapping across numberings within that
 * space. At any rate, it's widely used in some existing packages.
 * 
 * @author Dan Klein
 */
public class Numberer implements Serializable {

	private static Map numbererMap = new ConcurrentHashMap();

	public synchronized static Map getNumberers() {
		//System.out.println("getNumberers CALLED");
		return numbererMap;
	}

	/**
	 * You need to call this after deserializing Numberer objects to restore the
	 * global namespace, since static objects aren't serialized.
	 */
	public synchronized static void setNumberers(Map numbs) {
		//System.out.println("setNumberers CALLED");
		numbererMap = numbs;
	}

	public synchronized static Numberer getGlobalNumberer(String type) {
		//System.out.println("getGlobalNumberer CALLED with " + type);
		Numberer n = (Numberer) numbererMap.get(type);
		if (n == null) {
			n = new Numberer();
			numbererMap.put(type, n);
		}
		return n;
	}

	/**
	 * Get a number for an object in namespace type. This looks up the Numberer
	 * for <code>type</code> in the global namespace map (creating it if none
	 * previously existed), and then returns the appropriate number for the key.
	 */
	public synchronized static int number(String type, Object o) {
		//System.out.println("number CALLED with " + type + " " + o);
		return getGlobalNumberer(type).number(o);
	}

	public synchronized static Object object(String type, int n) {
		//System.out.println("object CALLED with " + type + " " + n);
		return getGlobalNumberer(type).object(n);
	}

	/**
	 * For an Object <i>o</i> that occurs in Numberers of type <i>sourceType</i>
	 * and <i>targetType</i>, translates the serial number <i>n</i> of <i>o</i>
	 * in the <i>sourceType</i> Numberer to the serial number in the
	 * <i>targetType</i> Numberer.
	 */
	public synchronized static int translate(String sourceType, String targetType, int n) {
		return getGlobalNumberer(targetType).number(
				getGlobalNumberer(sourceType).object(n));
	}

	private int total;
	private Map intToObject;
	private Map objectToInt;
	private MutableInteger tempInt;
	private boolean locked = false;

	public synchronized int total() {
		return total;
	}

	public synchronized void lock() {
		locked = true;
	}

	public synchronized boolean hasSeen(Object o) {
		return objectToInt.keySet().contains(o);
	}

	public synchronized Set objects() {
		return objectToInt.keySet();
	}

	public synchronized int size() {
		return objectToInt.size();
	}

	public synchronized int number(Object o) {
		//System.out.println("number CALLED with " + o);
		MutableInteger i = (MutableInteger) objectToInt.get(o);
		if (i == null) {
			if (locked) {
				throw new NoSuchElementException("no object: " + o);
			}
			i = new MutableInteger(total);
			total++;
			objectToInt.put(o, i);
			intToObject.put(i, o);
		}
		return i.intValue();
	}

	public synchronized Object object(int n) {
		//System.out.println("object CALLED with " + n + " " + intToObject);
		tempInt.set(n);
		return intToObject.get(tempInt);
	}

	@Override
	public synchronized String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("[");
		for (int i = 0; i < total; i++) {
			sb.append(i);
			sb.append("->");
			sb.append(object(i));
			if (i < total - 1) {
				sb.append(", ");
			}
		}
		sb.append("]");
		return sb.toString();
	}

	public Numberer() {
		total = 0;
		tempInt = new MutableInteger();
		intToObject = new ConcurrentHashMap();
		objectToInt = new ConcurrentHashMap();
	}

	private final long serialVersionUID = 1L;

}
