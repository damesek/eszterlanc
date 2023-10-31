package file;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Basic utility class for files. Collect file(name)s recursively from the given
 * directory.
 */
public class FileUtil {

  // to collect files
  private static List<File> files = null;
  // to collect the names of the files
  private static List<String> fileNames = null;

  /**
   * Add all files with the specified extension to a static list.
   * 
   * @param folder
   *          root folder
   * @param extension
   *          specifies the extension (e.g. 'txt')
   */
  private static void addFiles(File folder, String extension) {
    for (File file : folder.listFiles()) {
      if (file.isFile() && file.getName().endsWith(extension)) {
        files.add(file);
      } else if (file.isDirectory()) {
        addFiles(file, extension);
      }
    }
  }

  /**
   * Add all file names(!) with the specified extension to a static list.
   * 
   * @param folder
   *          root folder
   * @param extension
   *          specifies the extension (e.g. 'txt')
   */
  private static void addFileNames(File folder, String extension) {
    for (File file : folder.listFiles()) {
      if (file.isFile() && file.getName().endsWith(extension)) {
        fileNames.add(file.getAbsolutePath());
      } else if (file.isDirectory()) {
        addFileNames(file, extension);
      }
    }
  }

  /**
   * Collects the files (recursively) from the given (root) directory, with the
   * specified extension.
   * 
   * @param root
   *          directory
   * @param extension
   *          specifies the extension (e.g. 'txt')
   * @return List of the files
   */
  public static List<File> getFiles(String root, String extension) {
    files = new ArrayList<File>();
    addFiles(new File(root), extension);
    return files;
  }

  /**
   * Collects the file names(!) (recursively) from the given (root) directory,
   * with the specified extension.
   * 
   * @param root
   *          directory
   * @param extension
   *          specifies the extension (e.g. 'txt')
   * @return list of the filenames
   */
  public static List<String> getFileNames(String root, String extension) {
    fileNames = new ArrayList<String>();
    Map<String, String> s = new TreeMap<>();
 
    addFileNames(new File(root), extension);
    return fileNames;
  }
}
