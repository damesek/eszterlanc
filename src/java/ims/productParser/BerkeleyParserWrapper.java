package ims.productParser;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import edu.berkeley.nlp.PCFGLA.ArrayParser;
import edu.berkeley.nlp.PCFGLA.BerkeleyParser;
import edu.berkeley.nlp.PCFGLA.Binarization;
import edu.berkeley.nlp.PCFGLA.CoarseToFineMaxRuleParser;
import edu.berkeley.nlp.PCFGLA.CoarseToFineNBestParser;
import edu.berkeley.nlp.PCFGLA.Corpus;
import edu.berkeley.nlp.PCFGLA.CorpusStatistics;
import edu.berkeley.nlp.PCFGLA.FeaturizedLexicon;
import edu.berkeley.nlp.PCFGLA.Featurizer;
import edu.berkeley.nlp.PCFGLA.Grammar;
import edu.berkeley.nlp.PCFGLA.GrammarMerger;
import edu.berkeley.nlp.PCFGLA.GrammarTrainer;
import edu.berkeley.nlp.PCFGLA.LatticeLexicon;
import edu.berkeley.nlp.PCFGLA.Lexicon;
import edu.berkeley.nlp.PCFGLA.MultiThreadedParserWrapper;
import edu.berkeley.nlp.PCFGLA.OptionParser;
import edu.berkeley.nlp.PCFGLA.ParserData;
import edu.berkeley.nlp.PCFGLA.SimpleFeaturizer;
import edu.berkeley.nlp.PCFGLA.SimpleLexicon;
import edu.berkeley.nlp.PCFGLA.SophisticatedLexicon;
import edu.berkeley.nlp.PCFGLA.StateSetTreeList;
import edu.berkeley.nlp.PCFGLA.TreeAnnotations;
import edu.berkeley.nlp.PCFGLA.Corpus.TreeBankType;
import edu.berkeley.nlp.PCFGLA.GrammarTrainer.Options;
import edu.berkeley.nlp.PCFGLA.smoothing.NoSmoothing;
import edu.berkeley.nlp.PCFGLA.smoothing.SmoothAcrossParentBits;
import edu.berkeley.nlp.PCFGLA.smoothing.Smoother;
import edu.berkeley.nlp.io.PTBLineLexer;
import edu.berkeley.nlp.syntax.StateSet;
import edu.berkeley.nlp.syntax.Tree;
import edu.berkeley.nlp.util.Numberer;

public class BerkeleyParserWrapper {

	public static double calcConfidence(CoarseToFineMaxRuleParser parser,
			Tree<String> tree) {
		try {
			return (tree.getChildren().isEmpty()) ? Double.NEGATIVE_INFINITY
					: parser.getLogLikelihood(tree);
		} catch (Exception e) {
			return Double.NEGATIVE_INFINITY;
		}
	}

	public static ParserData train(List<Tree<String>> trainTrees) {
		return train(trainTrees, 8);
	}

	public static ParserData train(List<Tree<String>> trainTrees, int randSeed) {
		return train(trainTrees, randSeed, null);
	}

	public static ParserData train(List<Tree<String>> trainTrees,
			int randSeed, CoarseToFineNBestParser parser) {

		Lexicon lexicon = null, maxLexicon = null, previousLexicon = null;
		Grammar grammar = null, maxGrammar = null, previousGrammar = null;
		double maxLikelihood = Double.NEGATIVE_INFINITY;
		int startSplit = 0;
		//Numberer tagNumberer = new Numberer();
		Numberer tagNumberer = Numberer.getGlobalNumberer("tags");
		Numberer wordNumberer = new Numberer();
		GrammarTrainer.Options opts = new GrammarTrainer.Options(); 
//		 gives the default option values from GrammarTrainer
		
		double mergingPercentage = opts.mergingPercentage;
		boolean separateMergingThreshold = opts.separateMergingThreshold;
		List<Tree<String>> validationTrees = new LinkedList<Tree<String>>();
		double randomness = opts.randomization;
		GrammarTrainer.VERBOSE = opts.verbose;
		Random random = new Random(randSeed);
		boolean baseline = opts.baseline;
		int numSplitTimes = ProductParserTrainer.numSplits;
		int allowedDroppingIters = opts.di;
		int maxIterations = opts.splitMaxIterations;
		int minIterations = opts.splitMinIterations;
		double[] smoothParams = { opts.smoothingParameter1,
				opts.smoothingParameter2 };
		double filter = opts.filter;
		boolean allowMoreSubstatesThanCounts = false;
		boolean findClosedUnaryPaths = opts.findClosedUnaryPaths;
		opts.unknownLevel = ProductParserTrainer.unknownLevel;
		int nTrees = trainTrees.size();

		// EM: iterate until the validation likelihood drops for four
		// consecutive
		// iterations
		int iter = 0;
		int droppingIter = 0;

		short nSubstates = opts.nSubStates;
		short[] numSubStatesArray = GrammarTrainer.initializeSubStateArray(
				trainTrees, validationTrees, tagNumberer, nSubstates);
		StateSetTreeList trainStateSetTrees = new StateSetTreeList(trainTrees,
				numSubStatesArray, false, tagNumberer);
		StateSetTreeList validationStateSetTrees = new StateSetTreeList(
				validationTrees, numSubStatesArray, false, tagNumberer);// deletePC);
		validationTrees = null;

		// If we are splitting, we load the old grammar and start off by
		// splitting.
		if (parser != null) {
			startSplit = 2;
			//tagNumberer = parser.getTagNumberer();
			//wordNumberer = parser.getWordNumberer();
			
			maxGrammar = parser.getGrammar().copyGrammar(true);

			maxLexicon = parser.getLexicon().copyLexicon();
			numSubStatesArray = Arrays.copyOf(maxGrammar.numSubStates,
					maxGrammar.numSubStates.length);

			trainStateSetTrees = new StateSetTreeList(trainTrees,
					numSubStatesArray, false, tagNumberer);
			int n = 0;
			boolean secondHalf = false;
			for (Tree<StateSet> stateSetTree : trainStateSetTrees) {
				secondHalf = (n++ > nTrees / 2.0);
				maxLexicon.trainTree(stateSetTree, randomness, null,
						secondHalf, false,opts.rare, random);
				maxGrammar.tallyUninitializedStateSetTree(stateSetTree);
			}
			maxLexicon.optimize();
			maxGrammar.optimize(randomness, random);
			previousGrammar = grammar = maxGrammar;
			previousLexicon = lexicon = maxLexicon;
			System.out.println("Loading old grammar complete.");

			Map numbererMap = new HashMap();
			numbererMap.put("tags", tagNumberer);
			numbererMap.put("words", wordNumberer);
			return new ParserData(maxLexicon, maxGrammar, null, numbererMap,
					numSubStatesArray, opts.verticalMarkovization,
					opts.horizontalMarkovization, opts.binarization);
			
			/*System.out.println("Loading old grammar from " + parser);
			startSplit = 0; // we've already trained the grammar
			maxGrammar = parser.getGrammar().copyGrammar(true);
			numSubStatesArray = maxGrammar.numSubStates;
			previousGrammar = grammar = maxGrammar;
			previousLexicon = lexicon = maxLexicon;
			Numberer.setNumberers(pData.getNumbs());
			tagNumberer = Numberer.getGlobalNumberer("tags");
			System.out.println("Loading old grammar complete.");
			if (noSplit) {
				System.out.println("Will NOT split the loaded grammar.");
				startSplit = 1;
			}*/
		}

		if (mergingPercentage > 0) {
			System.out.println("Will merge " + (int) (mergingPercentage * 100)
					+ "% of the splits in each round.");
			System.out
					.println("The threshold for merging lexical and phrasal categories will be set separately: "
							+ separateMergingThreshold);
		}
		
		// get rid of the old trees
		trainTrees = null;
		validationTrees = null;
		System.gc();

		if (opts.simpleLexicon) {
			System.out
					.println("Replacing words which have been seen less than 5 times with their signature.");
			Corpus.replaceRareWords(trainStateSetTrees, new SimpleLexicon(
					numSubStatesArray, -1), opts.rare);
		}

		Featurizer feat = new SimpleFeaturizer(opts.rare, opts.reallyRare);
		// If we're training without loading a split grammar, then we run once
		// without splitting.
		if (parser == null) {
			grammar = new Grammar(numSubStatesArray, findClosedUnaryPaths,
					new NoSmoothing(), null, filter);
			// these two lines crash the compiler. dunno why.
			Lexicon tmp_lexicon = // (opts.featurizedLexicon) ?
			// new FeaturizedLexicon(numSubStatesArray,feat,trainStateSetTrees)
			// :
			(opts.simpleLexicon) ? new SimpleLexicon(numSubStatesArray, -1,
					smoothParams, new NoSmoothing(), filter, trainStateSetTrees)
					: /*
					 * new SophisticatedLexicon(numSubStatesArray,
					 * SophisticatedLexicon.DEFAULT_SMOOTHING_CUTOFF,
					 * smoothParams, new NoSmoothing(), filter)
					 */
					new LatticeLexicon(opts.unknownLevel, numSubStatesArray,
							SophisticatedLexicon.DEFAULT_SMOOTHING_CUTOFF,
							smoothParams, new NoSmoothing(), filter,
							tagNumberer);
			;
			if (opts.featurizedLexicon)
				tmp_lexicon = new FeaturizedLexicon(numSubStatesArray, feat,
						trainStateSetTrees);
			int n = 0;
			boolean secondHalf = false;
			for (Tree<StateSet> stateSetTree : trainStateSetTrees) {
				secondHalf = (n++ > nTrees / 2.0);
				tmp_lexicon.trainTree(stateSetTree, randomness, null,
						secondHalf, false, opts.rare, random);
			}
			lexicon = (opts.simpleLexicon) ? new SimpleLexicon(
					numSubStatesArray, -1, smoothParams, new NoSmoothing(),
					filter, trainStateSetTrees) : new LatticeLexicon(opts.unknownLevel, numSubStatesArray,
					SophisticatedLexicon.DEFAULT_SMOOTHING_CUTOFF,
					smoothParams, new NoSmoothing(), filter, tagNumberer);
			if (opts.featurizedLexicon)
				lexicon = new FeaturizedLexicon(numSubStatesArray, feat,
						trainStateSetTrees);
			for (Tree<StateSet> stateSetTree : trainStateSetTrees) {
				secondHalf = (n++ > nTrees / 2.0);
				lexicon.trainTree(stateSetTree, randomness, tmp_lexicon,
						secondHalf, false, opts.rare, random);
				grammar.tallyUninitializedStateSetTree(stateSetTree);
			}
			lexicon.tieRareWordStats(opts.rare);
			lexicon.optimize();
			grammar.optimize(randomness, random);
			// System.out.println(grammar);
			previousGrammar = maxGrammar = grammar; // needed for baseline -
													// when there is no EM loop
			previousLexicon = maxLexicon = lexicon;
		}

		// the main loop: split and train the grammar
		for (int splitIndex = startSplit; splitIndex < numSplitTimes * 3; splitIndex++) {

			// now do either a merge or a split and the end a smooth
			// on odd iterations merge, on even iterations split
			String opString = "";
			if (splitIndex % 3 == 2) {// (splitIndex==numSplitTimes*2){
				if (opts.smooth.equals("NoSmoothing"))
					continue;
				System.out.println("Setting smoother for grammar and lexicon.");
				Smoother grSmoother = new SmoothAcrossParentBits(0.01,
						maxGrammar.splitTrees);
				Smoother lexSmoother = new SmoothAcrossParentBits(0.1,
						maxGrammar.splitTrees);
				// Smoother grSmoother = new SmoothAcrossParentSubstate(0.01);
				// Smoother lexSmoother = new SmoothAcrossParentSubstate(0.1);
				maxGrammar.setSmoother(grSmoother);
				maxLexicon.setSmoother(lexSmoother);
				minIterations = maxIterations = opts.smoothMaxIterations;
				opString = "smoothing";
			} else if (splitIndex % 3 == 0) {
				// the case where we split
				if (opts.noSplit)
					continue;
				System.out.println("Before splitting, we have a total of "
						+ maxGrammar.totalSubStates() + " substates.");
				CorpusStatistics corpusStatistics = new CorpusStatistics(
						tagNumberer, trainStateSetTrees);
				int[] counts = corpusStatistics.getSymbolCounts();

				maxGrammar = maxGrammar.splitAllStates(randomness, counts,
						allowMoreSubstatesThanCounts, 0, random);
				maxLexicon = maxLexicon.splitAllStates(counts,
						allowMoreSubstatesThanCounts, 0);
				Smoother grSmoother = new NoSmoothing();
				Smoother lexSmoother = new NoSmoothing();
				maxGrammar.setSmoother(grSmoother);
				maxLexicon.setSmoother(lexSmoother);
				System.out.println("After splitting, we have a total of "
						+ maxGrammar.totalSubStates() + " substates.");
				System.out
						.println("Rule probabilities are NOT normalized in the split, therefore the training LL is not guaranteed to improve between iteration 0 and 1!");
				opString = "splitting";
				maxIterations = opts.splitMaxIterations;
				minIterations = opts.splitMinIterations;
			} else {
				if (mergingPercentage == 0)
					continue;
				// the case where we merge
				double[][] mergeWeights = GrammarMerger.computeMergeWeights(
						maxGrammar, maxLexicon, trainStateSetTrees);
				double[][][] deltas = GrammarMerger.computeDeltas(maxGrammar,
						maxLexicon, mergeWeights, trainStateSetTrees);
				boolean[][][] mergeThesePairs = GrammarMerger
						.determineMergePairs(deltas, separateMergingThreshold,
								mergingPercentage, maxGrammar);

				grammar = GrammarMerger.doTheMerges(maxGrammar, maxLexicon,
						mergeThesePairs, mergeWeights);
				short[] newNumSubStatesArray = grammar.numSubStates;
				trainStateSetTrees = new StateSetTreeList(trainStateSetTrees,
						newNumSubStatesArray, false);
				validationStateSetTrees = new StateSetTreeList(
						validationStateSetTrees, newNumSubStatesArray, false);

				// retrain lexicon to finish the lexicon merge (updates the
				// unknown words model)...
				if (opts.featurizedLexicon) {
					lexicon = new FeaturizedLexicon(newNumSubStatesArray, feat,
							trainStateSetTrees);
				} else
					lexicon = (opts.simpleLexicon) ? new SimpleLexicon(
							newNumSubStatesArray, -1, smoothParams,
							maxLexicon.getSmoother(), filter,
							trainStateSetTrees) : /*
												 * new SophisticatedLexicon(
												 * newNumSubStatesArray,
												 * SophisticatedLexicon
												 * .DEFAULT_SMOOTHING_CUTOFF,
												 * maxLexicon
												 * .getSmoothingParams(),
												 * maxLexicon.getSmoother(),
												 * maxLexicon
												 * .getPruningThreshold())
												 */
					new LatticeLexicon(opts.unknownLevel, newNumSubStatesArray,
							SophisticatedLexicon.DEFAULT_SMOOTHING_CUTOFF,
							maxLexicon.getSmoothingParams(),
							maxLexicon.getSmoother(),
							maxLexicon.getPruningThreshold(), tagNumberer);
				boolean updateOnlyLexicon = true;
				double trainingLikelihood = GrammarTrainer.doOneEStep(grammar,
						maxLexicon, null, lexicon, trainStateSetTrees,
						updateOnlyLexicon, opts.rare);
				// System.out.println("The training LL is "+trainingLikelihood);
				lexicon.optimize();// Grammar.RandomInitializationType.INITIALIZE_WITH_SMALL_RANDOMIZATION);
									// // M Step

				GrammarMerger.printMergingStatistics(maxGrammar, grammar);
				opString = "merging";
				maxGrammar = grammar;
				maxLexicon = lexicon;
				maxIterations = opts.mergeMaxIterations;
				minIterations = opts.mergeMinIterations;
			}
			// update the substate dependent objects
			previousGrammar = grammar = maxGrammar;
			previousLexicon = lexicon = maxLexicon;
			droppingIter = 0;
			numSubStatesArray = grammar.numSubStates;
			trainStateSetTrees = new StateSetTreeList(trainStateSetTrees,
					numSubStatesArray, false);
			validationStateSetTrees = new StateSetTreeList(
					validationStateSetTrees, numSubStatesArray, false);
			maxLikelihood = GrammarTrainer.calculateLogLikelihood(maxGrammar, maxLexicon,
					validationStateSetTrees);
			System.out.println("After " + opString + " in the "
					+ (splitIndex / 3 + 1)
					+ "th round, we get a validation likelihood of "
					+ maxLikelihood);
			iter = 0;

			// the inner loop: train the grammar via EM until validation
			// likelihood reliably drops
			do {
				iter += 1;
				System.out.println("Beginning iteration " + (iter - 1) + ":");

				// 1) Compute the validation likelihood of the previous
				// iteration
				System.out.print("Calculating validation likelihood...");
				double validationLikelihood = GrammarTrainer.calculateLogLikelihood(
						previousGrammar, previousLexicon,
						validationStateSetTrees); // The validation LL of
													// previousGrammar/previousLexicon
				System.out.println("done: " + validationLikelihood);

				// 2) Perform the E step while computing the training likelihood
				// of the previous iteration
				System.out.print("Calculating training likelihood...");
				grammar = new Grammar(grammar.numSubStates,
						grammar.findClosedPaths, grammar.smoother, grammar,
						grammar.threshold, tagNumberer);
				if (opts.featurizedLexicon)
					lexicon = lexicon.copyLexicon();
				// lexicon = new
				// FeaturizedLexicon(numSubStatesArray,feat,trainStateSetTrees);
				else
					lexicon = (opts.simpleLexicon) ? new SimpleLexicon(
							grammar.numSubStates, -1, smoothParams,
							lexicon.getSmoother(), filter, trainStateSetTrees)
							: new /*
								 * SophisticatedLexicon( grammar.numSubStates,
								 * SophisticatedLexicon
								 * .DEFAULT_SMOOTHING_CUTOFF,
								 * lexicon.getSmoothingParams(), lexicon
								 * .getSmoother(), lexicon
								 * .getPruningThreshold())
								 */
							LatticeLexicon(
									opts.unknownLevel,
									grammar.numSubStates,
									SophisticatedLexicon.DEFAULT_SMOOTHING_CUTOFF,
									lexicon.getSmoothingParams(), lexicon
											.getSmoother(), lexicon
											.getPruningThreshold(), tagNumberer);
				boolean updateOnlyLexicon = false;
				double trainingLikelihood = GrammarTrainer.doOneEStep(previousGrammar,
						previousLexicon, grammar, lexicon, trainStateSetTrees,
						updateOnlyLexicon, opts.rare); // The training LL of
														// previousGrammar/previousLexicon
				System.out.println("done: " + trainingLikelihood);

				// 3) Perform the M-Step
				lexicon.optimize(); // M Step
				grammar.optimize(0, random); // M Step

				// 4) Check whether previousGrammar/previousLexicon was in fact
				// better than the best
				if (iter < minIterations
						|| validationLikelihood >= maxLikelihood) {
					maxLikelihood = validationLikelihood;
					maxGrammar = previousGrammar;
					maxLexicon = previousLexicon;
					droppingIter = 0;
				} else {
					droppingIter++;
				}

				// 5) advance the 'pointers'
				previousGrammar = grammar;
				previousLexicon = lexicon;
			} while ((droppingIter < allowedDroppingIters) && (!baseline)
					&& (iter < maxIterations));

			// Dump a grammar file to disk from time to time
//			ParserData pData = new ParserData(maxLexicon, maxGrammar, null,
//					Numberer.getNumberers(), numSubStatesArray,
//					VERTICAL_MARKOVIZATION, HORIZONTAL_MARKOVIZATION,
//					binarization);
//			String outTmpName = outFileName + "_" + (splitIndex / 3 + 1) + "_"
//					+ opString + ".gr";
//			System.out.println("Saving grammar to " + outTmpName + ".");
//			if (pData.Save(outTmpName))
//				System.out.println("Saving successful.");
//			else
//				System.out.println("Saving failed!");
//			pData = null;

		}

		// The last grammar/lexicon has not yet been evaluated. Even though the
		// validation likelihood
		// has been dropping in the past few iteration, there is still a chance
		// that the last one was in
		// fact the best so just in case we evaluate it.
		
//		System.out.print("Calculating last validation likelihood...");
		double validationLikelihood = GrammarTrainer.calculateLogLikelihood(grammar, lexicon,
				validationStateSetTrees);
//		System.out.println("done.\n  Iteration " + iter
//				+ " (final) gives validation likelihood "
//				+ validationLikelihood);
		if (validationLikelihood > maxLikelihood) {
			maxLikelihood = validationLikelihood;
			maxGrammar = previousGrammar;
			maxLexicon = previousLexicon;
		}

		ParserData pData = new ParserData(maxLexicon, maxGrammar, null,
				Numberer.getNumberers(), numSubStatesArray,
				opts.verticalMarkovization, opts.horizontalMarkovization, opts.binarization);
		return pData;
//		System.out.println("Saving grammar to " + outFileName + ".");
//		System.out.println("It gives a validation data log likelihood of: "
//				+ maxLikelihood);
//		if (pData.Save(outFileName))
//			System.out.println("Saving successful.");
//		else
//			System.out.println("Saving failed!");
	}

	public static List<Tree<String>> readCorpus(String filename) {
		Corpus corpus = new Corpus(filename, TreeBankType.SINGLEFILE, 1.0,
				false);
		return Corpus.binarizeAndFilterTrees(corpus.getTrainTrees(), 1, 0,
				10000, Binarization.RIGHT, false, false);
	}

	public static CoarseToFineNBestParser constructNBestParser(ParserData pData) {
		Grammar grammar = pData.getGrammar();
		Lexicon lexicon = pData.getLexicon();
		BerkeleyParser.Options opts = new BerkeleyParser.Options();
		opts.kbest = ProductParser.kbest;
		CoarseToFineNBestParser parser = new CoarseToFineNBestParser(grammar, lexicon,
				opts.kbest, 1.0, -1, opts.viterbi,
				opts.substates, opts.scores, opts.accurate,
				opts.variational, false, true);
		parser.binarization = pData.getBinarization();
		return parser;
	}

	public static List<Parse> parseNBest(CoarseToFineNBestParser parser,
			String sent) {
		List<Parse> parses = new LinkedList<Parse>();
		try {
			List<String> sentence = Arrays.asList(sent.split(" "));
			List<Tree<String>> parsedTrees = parser.getKBestConstrainedParses(
					sentence, null, ProductParser.kbest);
			for (Tree<String> tree : parsedTrees)
				parses.add(new Parse(tree, parser));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return parses;
	}

	public static Parse parse(ParserData pData, String sent) {
		Grammar grammar = pData.getGrammar();
		Lexicon lexicon = pData.getLexicon();
		Numberer.setNumberers(pData.getNumbs());
		BerkeleyParser.Options opts = new BerkeleyParser.Options();
		double threshold = 1.0;

		CoarseToFineMaxRuleParser parser = new CoarseToFineMaxRuleParser(
				grammar, lexicon, threshold, -1, opts.viterbi, opts.substates,
				opts.scores, opts.accurate, false, true, true);

		parser.binarization = pData.getBinarization();

		Parse parse = new Parse();
		try {
			List<String> sentence = null;

			sentence = Arrays.asList(sent.split(" "));

			if (sentence.size() >= opts.maxLength) {
				parse.confidence = -1.0;
				return parse;
			}

			List<Tree<String>> parsedTrees = null;
			parsedTrees = new ArrayList<Tree<String>>();
			Tree<String> parsedTree = parser.getBestConstrainedParse(sentence,
					null, null);
			// if (opts.goldPOS && parsedTree.getChildren().isEmpty()){ // parse
			// error when using goldPOS, try without
			// parsedTree = parser.getBestConstrainedParse(sentence,null,null);
			// }
			parsedTrees.add(parsedTree);
			parse = new Parse(parsedTrees.get(0), parser, sentence);

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return parse;
	}

	public static List<Parse> parseCorpus(List<String> corpus,
			ParserData parserData, int nThreads) {
		BerkeleyParser.Options opts = new BerkeleyParser.Options();
		CoarseToFineMaxRuleParser parser = null;
		// if (opts.kbest==1)
		parser = new CoarseToFineMaxRuleParser(parserData.getGrammar(),
				parserData.getLexicon(), 1.0, -1, opts.viterbi, opts.substates,
				opts.scores, opts.accurate, false, true, true);
		// else parser = new CoarseToFineNBestParser(grammar, lexicon,
		// opts.kbest,threshold,-1,opts.viterbi,opts.substates,opts.scores,
		// opts.accurate, false, false, true);
		parser.binarization = parserData.getBinarization();
		MultiThreadedParserWrapper m_parser = new MultiThreadedParserWrapper(
				parser, nThreads);

		List<Parse> result = new LinkedList<Parse>();
		for (String sent : corpus) {
			List<String> sentence = Arrays.asList(sent.split(" "));
			m_parser.parseThisSentence(sentence);
			while (m_parser.hasNext()) {
				result.add(new Parse(m_parser.getNext().get(0), parser));
			}
		}
		while (!m_parser.isDone()) {
			while (m_parser.hasNext()) {
				result.add(new Parse(m_parser.getNext().get(0), parser));
			}
		}
		return result;
	}

}
