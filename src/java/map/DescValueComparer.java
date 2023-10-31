package map;

import java.util.Comparator;
import java.util.Map;

class DescValueComparer<K, V extends Comparable> implements Comparator {
  private final Map<K, V> map;

  @SuppressWarnings("unchecked")
  public DescValueComparer(Map map) {
    this.map = map;
  }

  @Override
  @SuppressWarnings("unchecked")
  public int compare(Object key1, Object key2) {
    V value1 = this.map.get(key1);
    V value2 = this.map.get(key2);
    int c = value1.compareTo(value2);
    if (c != 0) {
      return -c;
    }
    Integer hashCode1 = key1.hashCode();
    Integer hashCode2 = key2.hashCode();
    return -hashCode1.compareTo(hashCode2);
  }
}
