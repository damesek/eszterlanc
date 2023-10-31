package ims.productParser;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import edu.berkeley.nlp.PCFGLA.CoarseToFineNBestParser;
import edu.berkeley.nlp.PCFGLA.ParserData;

public class ProductParserData implements Serializable {
  private static final long serialVersionUID = 3051970420880508000L;
	
  protected List<CoarseToFineNBestParser> parsers = new LinkedList<CoarseToFineNBestParser>();
	
  public void saveModel(String fn) {
		try {
			FileOutputStream fos = new FileOutputStream(fn);
	    GZIPOutputStream gz = new GZIPOutputStream(fos);
	    ObjectOutputStream oos = new ObjectOutputStream(gz);
	    oos.writeObject(this);
	    oos.flush();
	    oos.close();
	    fos.close();
    } catch (Exception e) {
	    e.printStackTrace();
    }
  }

	public static ProductParserData loadModel(String fn){
		System.out.println("Loading the product parser from "+fn);
		ProductParserData ppd = null;
    try {
    	FileInputStream fis = new FileInputStream(fn);
	    GZIPInputStream gs = new GZIPInputStream(fis);
	    ObjectInputStream ois = new ObjectInputStream(gs);
	    ppd = (ProductParserData)ois.readObject();
	    ois.close();
    } catch (Exception e) {
	    e.printStackTrace();
    }
    return ppd;
	}

	public List<CoarseToFineNBestParser> getParsers() {
		return parsers;
	}

	public void setParsers(List<CoarseToFineNBestParser> parsers) {
		this.parsers = parsers;
	}

	
}
