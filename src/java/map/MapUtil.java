package map;

import java.util.Comparator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class MapUtil {
  public static <K, V> SortedMap<K, V> sortMapByValue(Map<K, V> map) {
    return sortMapByValue(map, true);
  }

  public static <K, V> SortedMap<K, V> sortMapByValue(Map<K, V> map,
      boolean descending) {
    Comparator<?> vc = null;
    if (descending) {
      vc = new DescValueComparer(map);
    } else {
      vc = new IncValueComparer(map);
    }
    SortedMap sm = new TreeMap(vc);
    sm.putAll(map);
    return sm;
  }
}
