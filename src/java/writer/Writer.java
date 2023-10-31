package writer;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import settings.Settings;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Map;

public class Writer {
  private static final Logger LOGGER = Logger.getLogger(Writer.class.getName());

  static {
    PropertyConfigurator.configure(Settings.LOGGER_PROPS);
  }

  public static void writeListToFile(List<?> list, String file) {
    BufferedWriter writer = null;

    try {
      writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
              file), "utf-8"));

      for (Object l : list) {
        writer.write(l + "\n");
      }
    } catch (IOException e) {
      LOGGER.error(e);
    } finally {
      try {
        writer.close();
      } catch (IOException e) {
        LOGGER.error(e);
      }
    }
  }

  /**
   * @param map
   * @param file
   * @param encoding
   * @param separator
   */
  public static void writeMapToFile(Map<?, ?> map, String file, String encoding, String separator) {
    BufferedWriter writer = null;

    try {
      writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
              file), encoding));

      for (Map.Entry<?, ?> l : map.entrySet()) {
        writer.write(l.getKey() + separator + l.getValue() + "\n");
      }
    } catch (IOException e) {
      LOGGER.error(e);
    } finally {
      try {
        writer.close();
      } catch (IOException e) {
        LOGGER.error(e);
      }
    }
  }

  /**
   * Writes the given map to the specified file, with UTF-8 encoding, separated "\t" key/values.
   *
   * @param map
   * @param file
   */
  public static void writeMapToFile(Map<?, ?> map, String file) {
    writeMapToFile(map, file, "utf-8", "\t");
  }

  public static void writeStringToFile(String content, String file, String encoding) {

    BufferedWriter writer = null;

    try {
      writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
              file), encoding));
      writer.write(content);
    } catch (IOException e) {
      LOGGER.error(e);
    } finally {
      try {
        writer.close();
      } catch (IOException e) {
        LOGGER.error(e);
      }
    }
  }

  public static void writeStringToFile(String content, String file) {
    writeStringToFile(content, file, "utf-8");
  }
}