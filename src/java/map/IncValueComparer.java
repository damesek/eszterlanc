package map;

import java.util.Map;

@SuppressWarnings("rawtypes")
class IncValueComparer<K, V extends Comparable> extends DescValueComparer {
  public IncValueComparer(Map map) {
    super(map);
  }

  @Override
  public int compare(Object key1, Object key2) {
    return -super.compare(key1, key2);
  }
}
