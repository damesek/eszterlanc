package ims.productParser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import edu.berkeley.nlp.PCFGLA.CoarseToFineNBestParser;
import edu.berkeley.nlp.PCFGLA.Option;
import edu.berkeley.nlp.PCFGLA.OptionParser;
import edu.berkeley.nlp.PCFGLA.ParserData;
import edu.berkeley.nlp.util.Numberer;

public class ProductParser implements Runnable {
	protected Thread thread;
	protected CoarseToFineNBestParser parser;
	protected static String sentence;
	public static int kbest = 50;
	public static boolean skipParsingErrors = false;
	protected static double EPS = -500f; 
	protected List<Parse> parses;
	protected static List<ProductParser> threads;
	
	public List<Parse> getParses() {
		return parses;
	}

	public ProductParser(CoarseToFineNBestParser p){
		parser = p;
	}

	public void start(){
		thread = new Thread(this);
		thread.start();
	}
	
	public Thread getThread(){
		return thread;
	}

	public void run() {
		parses = BerkeleyParserWrapper.parseNBest(parser, sentence);
	}

	public static class Options {

		@Option(name = "-gr", required = true, usage = "Grammarfile (Required)\n")
		public String grFileName;

		@Option(name = "-inputFile", usage = "Read input from this file instead of reading it from STDIN.")
		public String inputFile;

		@Option(name = "-inputDir", usage = "Read input from this directory.")
		public String inputDir = null;

		@Option(name = "-outputDir", usage = "Writes parses to this directory.")
		public String outputDir = null;

		@Option(name = "-kbest", usage = "Output the k best parse max-rule trees (Default: 1).")
		public int kbest = 1;

		@Option(name = "-productkbest", usage = "The k best parses uses for calculating the product (Default: 50).")
		public int productkbest = 50;

		@Option(name = "-outputFile", usage = "Store output in this file instead of printing it to STDOUT.")
		public String outputFile;

		@Option(name = "-skipParsingErrors", usage = "Do not write dummy sentences at parsing error (Default: false)")
		public boolean skipParsingErrors = false;
	}

	public static void init(ProductParserData ppd){
		System.out.println("Initializing the product parser...");
		threads = new LinkedList<ProductParser>();
		for(int id=0;id<ppd.parsers.size();++id){
			ProductParser p = new ProductParser(ppd.parsers.get(id));
			threads.add(p);
		}
	}
	
	public static Parse parse(String _sentence) throws Exception {
		return parseNBest(_sentence, 1).get(0);
	}
	
	public static List<Parse> parseNBest(String _sentence, int n) throws Exception {
		sentence = _sentence;
		for(ProductParser p : threads){
			p.run();
		}
		/*
		try {
			for(ProductParser p : threads){
				p.getThread().join();
			}
		} catch (InterruptedException e) {
			System.err.println("Main thread Interrupted");
		}*/

		List<Parse> parses = new LinkedList<Parse>();
		for(int i=0;i<threads.size();++i){
			List<Parse> newparses = threads.get(i).getParses();
			
			for(Parse cand : newparses){
				boolean found = false;
				for(Parse p : parses){
					if(p.tree.equals(cand.tree)){
						p.confidence += cand.confidence;
						found = true;
						break;
					}
				}
				if(!found){
					for(int j=0;j<i;++j){
						double c = BerkeleyParserWrapper.calcConfidence(threads.get(j).parser, cand.tree);
						cand.confidence += (c==Double.NEGATIVE_INFINITY ? EPS : c); 
					}
					parses.add(cand);
				}
			}
			for(Parse p : parses){
				boolean found = false;
				for(Parse cand : newparses){
					if(p.tree.equals(cand.tree)){
						found = true;
						break;
					}
				}
				if(!found){
					double c = BerkeleyParserWrapper.calcConfidence(threads.get(i).parser, p.tree);
					p.confidence += (c==Double.NEGATIVE_INFINITY ? EPS : c); 
				}
			}
		}
		
		List<Parse> candidates = new LinkedList<Parse>();
		if(parses.isEmpty()){
			throw new Exception("Cannot parse sentence!");
			//System.err.println("Cannot parse sentence: "+_sentence);
			//candidates.add(new Parse(Arrays.asList(sentence.split(" "))));
			//return candidates;
		}
		
		candidates.add(parses.get(0));
		for(int i=1;i<parses.size();++i){
			double conf = parses.get(i).confidence;
			int j=candidates.size()-1;
			while(j>=0 && candidates.get(j).confidence < conf)
				--j;
			candidates.add(j+1,parses.get(i));
			if(candidates.size()>n)
				candidates.remove(candidates.size()-1);
		}
		return candidates;
	}

	public static List<String> readFile(String file, boolean clean) {
		List<String> list = new LinkedList<String>();
		try{
			BufferedReader input  = new BufferedReader(new InputStreamReader(new FileInputStream(file),"UTF-8"));
			String line;
			while((line = input.readLine()) != null){
				if(clean){
					line = line.replace("@", "*AT*").replace("(", "*LRB*").replace(")", "*RRB*");
				}
				if(line.length()>0)
					list.add(line);
				if(list.size()>150000)
					return list;
			}
		}catch(IOException e){
			e.printStackTrace();
		}
		return list;
  }

	public static void parseFile(String in, String out, int kbest) throws Exception{
		List<String> corpus = readFile(in,true);
		BufferedWriter outtrees = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(out+"trees.gz")),"UTF-8"));
		BufferedWriter outprobs = null;
		if(kbest>1)	outprobs=new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(out+"probs.gz")),"UTF-8"));
		
		int n=0;
		for(String sentence : corpus){
			if(kbest == 1){
				String res = parse(sentence).tree.toString().replace("*AT*", "@");
				outtrees.write(res+"\n");
			} else {
				List<Parse> res = parseNBest(sentence, kbest);
				if(res.size()>2 || !skipParsingErrors){
					for(Parse p : res){
						outtrees.write(p.tree.toString().replace("*AT*", "@")+"\n");
						outprobs.write(p.confidence+"\n");
						outtrees.flush(); outprobs.flush();
					}
					outtrees.write("\n"); outprobs.write("\n");
				}
			}
			outtrees.flush();
			System.out.print(++n+"\r");
		}
		System.out.println();
		outtrees.close();
		if(kbest>1)	outprobs.close();
	}
	
	public static void main(String[] args) throws IOException {
		OptionParser optParser = new OptionParser(Options.class);
		Options opts = (Options) optParser.parse(args, true);

		ProductParserData ppd = ProductParserData.loadModel(opts.grFileName);
		init(ppd);
		ProductParser.kbest  = opts.productkbest;
		ProductParser.skipParsingErrors = opts.skipParsingErrors;
		
		Map<String, Numberer> nums = new HashMap<String, Numberer>();
		nums.put("tags", ppd.parsers.get(0).getGrammar().getTagNumberer());
		Numberer.setNumberers(nums);
		
		try {
			if(opts.inputDir == null)
				parseFile(opts.inputFile, opts.outputFile, opts.kbest);
			else {
				if(opts.outputDir != null)
					new File(opts.outputDir).mkdir();
				for(File f : new File(opts.inputDir).listFiles()){
					String outfn = opts.outputDir == null ? f.getAbsolutePath()+".parsed" : opts.outputDir+"/"+f.getName();
					System.out.println(outfn);
					parseFile(f.getAbsolutePath(), outfn, opts.kbest);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
