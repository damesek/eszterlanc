package reader;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import settings.Settings;

import java.io.*;
import java.util.*;

public class Reader {

  private static final Logger LOGGER = Logger.getLogger(Reader.class.getName());

  // class loader of this class
  private static final ClassLoader CLASS_LOADER = Reader.class.getClassLoader();

  static {
    PropertyConfigurator.configure(Settings.LOGGER_PROPS);
  }

  /**
   * Reads the untouched lines from the given file with the specified encoding.
   *
   * @param file     raw text file
   * @param encoding file encoding
   * @return List of String lines
   */
  public static List<String> readLines(String file, String encoding) {
    try {
      return readLines(new InputStreamReader(new FileInputStream(file),
              encoding));
    } catch (UnsupportedEncodingException e) {
      LOGGER.error(e);
    } catch (FileNotFoundException e) {
      LOGGER.error(e);
    }

    return null;
  }

  /**
   * Reads the untouched lines from the given file with UTF-8 encoding.
   *
   * @param file raw text file
   * @return List of String lines
   */
  public static List<String> readLines(String file) {
    return readLines(file, Settings.ENCODING);
  }

  public static Properties readProperties(String propsFile) {

    Properties properties = new Properties();
    BufferedReader reader = null;

    try {
      reader = new BufferedReader(new InputStreamReader(new FileInputStream(
              propsFile), Settings.ENCODING));
      properties.load(reader);
    } catch (IOException e) {
      LOGGER.error(e);
    } finally {
      try {
        reader.close();
      } catch (IOException e) {
        LOGGER.error(e);
      }
    }

    return properties;
  }

  /**
   * Reads the untouched resource file (ex. from a jar file from the classpath)
   *
   * @return lines of resource
   */
  public static List<String> readLinesFromResource(String resourceFile,
                                                   String encoding) {

    try {
      return readLines(new InputStreamReader(
              CLASS_LOADER.getResourceAsStream(resourceFile), encoding));
    } catch (UnsupportedEncodingException e) {
      LOGGER.error(e);
    }

    return null;
  }

  /**
   * Reads untouched lines from the given InputStreamReader.
   *
   * @param inputStreamReader
   * @return
   */
  private static List<String> readLines(InputStreamReader inputStreamReader) {
    BufferedReader reader = null;
    List<String> lines = new ArrayList<>();
    String line;

    try {
      reader = new BufferedReader(inputStreamReader);
      while ((line = reader.readLine()) != null) {
        lines.add(line);
      }
    } catch (IOException e) {
      LOGGER.error(e);
    } finally {
      try {
        reader.close();
      } catch (IOException e) {
        LOGGER.error(e);
      }
    }

    return lines;
  }

  public static Object deserializeFromResource(String file) {
    ObjectInputStream objectInputStream = null;

    try {
      objectInputStream = new ObjectInputStream(CLASS_LOADER.getResourceAsStream(file));
      return objectInputStream.readObject();
    } catch (NullPointerException e) {
      LOGGER.error(e);
    } catch (IOException e) {
      LOGGER.error(e);
    } catch (ClassNotFoundException e) {
      LOGGER.error(e);
    } finally {
      try {
        objectInputStream.close();
      } catch (IOException e) {
        LOGGER.error(e);
      }
    }

    return null;
  }

  public static Object deserialize(String file) {
    ObjectInputStream objectInputStream = null;

    try {
      objectInputStream = new ObjectInputStream(new FileInputStream(file));
      return objectInputStream.readObject();
    } catch (Exception e) {
      LOGGER.error(e);
    } finally {
      try {
        objectInputStream.close();
      } catch (IOException e) {
        LOGGER.error(e);
      }
    }

    return null;
  }


  public static Map<String, Double> readMap(String file, String encoding, String separator) {

    List<String> lines = readLines(file, encoding);

    Map<String, Double> map = new HashMap<>();
    String[] split;
    for (String line : lines) {
      split = line.split(separator);
      map.put(split[0], Double.parseDouble(split[1]));
    }
    return map;
  }

  public static Map<String, Double> readMap(String file) {
    return readMap(file, "utf-8", "\t");
  }



}
