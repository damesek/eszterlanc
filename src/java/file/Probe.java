package file;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import reader.Reader;

public class Probe {

  public static void main(String[] args) {
    List<String> lines = Reader.readLines("d:/n.x");

    Set<String> mails = new TreeSet<String>();

    for (String line : lines) {
      System.err.println(line);
      String mail = line.split("\t")[1];
      mails.add(mail);
    }
    
    System.err.println(mails.size());
  }
}
