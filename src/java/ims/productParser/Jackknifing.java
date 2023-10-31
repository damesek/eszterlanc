package ims.productParser;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import edu.berkeley.nlp.PCFGLA.Option;
import edu.berkeley.nlp.PCFGLA.OptionParser;
import edu.berkeley.nlp.syntax.Tree;

public class Jackknifing {
	public static class Options {
		@Option(name = "-out", required = true, usage = "Output Prefix for Parses (Required)")
		public String outFileName;

		@Option(name = "-path", required = true, usage = "Path to Corpus (Required)")
		public String path = null;

		@Option(name = "-nParser", usage = "The number of parsers (Default: 4)")
		public int nParser = 4;

		@Option(name = "-SMcycles", usage = "The number of split&merge iterations (Default: 3)")
		public int numSplits = 3;

		@Option(name = "-folds", usage = "The number of folds for knifing")
		public int foldnum = 10;
		
		@Option(name = "-gr", required = true, usage = "Grammarfile output (Required)\n")
		public String grFileName;

		@Option(name = "-kbest", usage = "Output the k best parse max-rule trees (Default: 50).")
		public int kbest = 50;

		@Option(name = "-uwm", usage = "Lexicon's unknownLevel")
		public int unknownLevel = 5;
	}

	public static void main(String[] args) throws Exception {
		OptionParser optParser = new OptionParser(Options.class);
		Options opts = (Options) optParser.parse(args, true);
		ProductParserTrainer.unknownLevel = opts.unknownLevel;
		
		List<Tree<String>> corpus = BerkeleyParserWrapper.readCorpus(opts.path);

		// Train a product parser with full split/merge 
		ProductParserTrainer.numSplits = opts.numSplits;
//		ProductParserData ppd  = ProductParserTrainer.train(corpus, opts.nParser, null);
//		ppd.saveModel(opts.grFileName);
		ProductParserData ppd = ProductParserData.loadModel(opts.grFileName);
		
		BufferedWriter outtrees = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(opts.outFileName+".trees.gz")),"UTF-8"));
		BufferedWriter outprobs = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(opts.outFileName+".probs.gz")),"UTF-8"));;
		int foldsize = (int)Math.ceil((double)corpus.size() / opts.foldnum);
		for(int fold=0; fold<opts.foldnum; ++fold){
			System.gc();

			int fromIndex = fold*foldsize;
			int toIndex   = Math.min((fold+1)*foldsize , corpus.size());
//			System.out.println(fromIndex+"-"+toIndex);
			List<Tree<String>> train = new LinkedList<Tree<String>>(corpus.subList(0, fromIndex));
			train.addAll(corpus.subList(toIndex, corpus.size()));
			
			System.out.println("Training parsers on fold "+fold+" ... on "+train.size()+" sentences");
			ProductParserData foldppd  = ProductParserTrainer.train(train, opts.nParser, ppd);
			System.out.println("Parsing on fold "+fold+" ...");
			ProductParser.init(foldppd);

			for(int i=fromIndex; i<toIndex; ++i){
				System.out.print(i-fromIndex+"\r");
				String sentence = "";
				for(String token : corpus.get(i).getYield())
					sentence += " " + token;
				List<Parse> parses = ProductParser.parseNBest(sentence.substring(1), opts.kbest);
//				if(parses.size()>2){
					for(Parse p : parses){
						outtrees.write(p.tree.toString().replace("*AT*", "@")); outtrees.write("\n");
						outprobs.write(Double.toString(p.confidence.isNaN() ? -500 : p.confidence)); outprobs.write("\n");
					}
					outtrees.write("\n"); outprobs.write("\n");
					outtrees.flush(); outprobs.flush();
//				}
			}
		}
		outtrees.close();
		outprobs.close();
	}
}
