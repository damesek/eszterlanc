package edu.berkeley.nlp.PCFGLA;

//import ims.hypergraph.EvalBF;

import ims.productParser.ProductParser;
import ims.productParser.ProductParserTrainer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import edu.berkeley.nlp.PCFGLA.smoothing.Smoother;
import edu.berkeley.nlp.util.Numberer;

public class LatticeLexicon extends SophisticatedLexicon {
	// protected Map<String, Set<String>> morph_analysis = new HashMap<String,
	// Set<String>>();
	private Numberer tagNumberer = null;
	private static double WEIGHT = Double.NaN;
	private static int THRESHOLD = Integer.MAX_VALUE;
	private static String LEXICONFILE;
	//protected static Map<String, Map<String, Double>> wordsPerMainPos = new ConcurrentHashMap<String, Map<String, Double>>();
	protected static Map<String, Map<String, Double>> mainPosPerWords = new ConcurrentHashMap<String, Map<String, Double>>();
	private static boolean IS_TEST = false;
	private static boolean FIRST_INIT;

	public static void init() {
		try {
			if (!FIRST_INIT)
				return;
			FIRST_INIT = false;

			BufferedReader reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(LEXICONFILE), "UTF-8"));

			String line;
			Numberer numberer = Numberer.getGlobalNumberer("tags");

			while ((line = reader.readLine()) != null) {
				line = line.replaceFirst("^[\\s]*", "");
				String l[] = line.split("[\\t\\s]");
				double num = Double.parseDouble(l[0].replaceAll("\\s", ""));
				String word = l[1];
				String pos = l[2];

				if (IS_TEST && !numberer.objects().contains(pos))
					continue;

				double preNums;

				if (!mainPosPerWords.containsKey(word))
					mainPosPerWords.put(word, new ConcurrentHashMap<String, Double>());
				preNums = mainPosPerWords.get(word).containsKey(pos) ? mainPosPerWords
						.get(word).get(pos) : 0;
				mainPosPerWords.get(word).put(pos, preNums + num);
			}
			reader.close();

			/*for (Entry<String, Map<String, Double>> ent : wordsPerMainPos
					.entrySet()) {
				double sum = 0.0;
				for (Entry<String, Double> ent2 : ent.getValue().entrySet()) {
					sum += ent2.getValue();
				}
				for (Entry<String, Double> ent2 : ent.getValue().entrySet()) {
					ent.getValue().put(ent2.getKey(), ent2.getValue() / sum);
				}
			}*/

			for (Entry<String, Map<String, Double>> ent : mainPosPerWords
					.entrySet()) {
				double sum = 0.0;
				for (Entry<String, Double> ent2 : ent.getValue().entrySet()) {
					sum += ent2.getValue();
				}
				for (Entry<String, Double> ent2 : ent.getValue().entrySet()) {
					ent.getValue().put(ent2.getKey(), ent2.getValue() / sum);
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public LatticeLexicon(short[] numSubStates, int smoothingCutoff,
			double[] smoothParam, Smoother smoother, double threshold) {
		super(numSubStates, smoothingCutoff, smoothParam, smoother, threshold);
		//init();
	}

	public LatticeLexicon(short[] numSubStates, int smoothingCutoff,
			double[] smoothParam, Smoother smoother, double threshold,
			Numberer tnum) {
		super(numSubStates, smoothingCutoff, smoothParam, smoother, threshold);
		//init();
		tagNumberer = tnum;
	}

	public LatticeLexicon(int uwm, short[] numSubStates, int smoothingCutoff,
			double[] smoothParam, Smoother smoother, double threshold) {
		super(uwm, numSubStates, smoothingCutoff, smoothParam, smoother,
				threshold);
		//init();
	}

	public LatticeLexicon(int uwm, short[] numSubStates, int smoothingCutoff,
			double[] smoothParam, Smoother smoother, double threshold,
			Numberer tnum) {
		super(uwm, numSubStates, smoothingCutoff, smoothParam, smoother,
				threshold);
		//init();
		tagNumberer = tnum;
	}

	public SophisticatedLexicon splitAllStates(int[] counts,
			boolean moreSubstatesThanCounts, int mode) {
		SophisticatedLexicon l = super.splitAllStates(counts,
				moreSubstatesThanCounts, mode);
		((LatticeLexicon) l).tagNumberer = this.tagNumberer;
		return l;
	}

	public SophisticatedLexicon projectLexicon(double[] condProbs,
			int[][] mapping, int[][] toSubstateMapping) {
		SophisticatedLexicon l = super.projectLexicon(condProbs, mapping,
				toSubstateMapping);
		((LatticeLexicon) l).tagNumberer = this.tagNumberer;
		return l;
	}

	public SophisticatedLexicon copyLexicon() {
		SophisticatedLexicon l = super.copyLexicon();
		((LatticeLexicon) l).tagNumberer = this.tagNumberer;
		return l;
	}

	public double[] score(String word, short tag, int loc, boolean noSmoothing,
			boolean isSignature) {
		double[] resultArray = super.score(word, tag, loc, noSmoothing,
				isSignature);
//if (true) return resultArray;
		String pos = tagNumberer.object(tag).toString();
		if (isSignature || wordCounter.getCount(word) > THRESHOLD
				|| !mainPosPerWords.containsKey(word)) {
			return resultArray;
		}

		double pb_T_W = 0.0;
		if (mainPosPerWords.containsKey(word)
				&& mainPosPerWords.get(word).containsKey(pos)) {
			pb_T_W = mainPosPerWords.get(word).get(pos);
		}
		double c_W = wordCounter.getCount(word);
		c_W = c_W < 0.1 ? 1f : c_W;
		double p_W = c_W / totalTokens;
		for (int substate = 0; substate < resultArray.length; ++substate) {
			double c_Tseen = tagCounter[tag][substate];
			double p_T = (c_Tseen / totalTokens);
			double pb_W_T = pb_T_W * p_W / p_T;

			if (logarithmMode) {
				resultArray[substate] = Math.exp(resultArray[substate]);
			}

			// if (c_W == 0) {
			// resultArray[substate] = pb_W_T;
			// } else {
			resultArray[substate] = (resultArray[substate] * c_W + WEIGHT
					* pb_W_T)
					/ (c_W + WEIGHT);
			// }
		}

		smoother.smooth(tag, resultArray);
		if (logarithmMode) {
			for (int i = 0; i < resultArray.length; i++) {
				resultArray[i] = Math.log(resultArray[i]);
				if (Double.isNaN(resultArray[i]))
					resultArray[i] = Double.NEGATIVE_INFINITY;
			}
		}

		return resultArray;
	}

	public static String getMainPos(String tag) {
		if (tag.equals("PUNC"))
			return "PUNC";
		return tag.length() > 0 && !tag.equals(null) ? tag.substring(0, 1)
				: "ERROR";
	}

	public void setTagNumberer(Numberer numb) {
		tagNumberer = numb;
	}

	public static void main(String[] args) throws Exception {
		FIRST_INIT = true;
		if (args.length < 4) {
			System.out
					.println("Usage: java edu.berkeley.nlp.PCFGLA.LatticeLexicon train|test|prodtrain|prodtest weight threshold lexiconFile originalParamters");
			return;
		}

		try {
			LatticeLexicon.WEIGHT = Double.parseDouble(args[1]);
		} catch (Exception e) {
			System.out.println("The weight must be a double.");
			return;
		}

		try {
			LatticeLexicon.THRESHOLD = Integer.parseInt(args[2]);
		} catch (Exception e) {
			System.out.println("The threshold must be an integer.");
			return;
		}

		LEXICONFILE = args[3];
		init();
		
		if (args[0].equalsIgnoreCase("train")) {
			GrammarTrainer.main(Arrays.copyOfRange(args, 4, args.length));
		} else if (args[0].equalsIgnoreCase("test")) {
			IS_TEST = true;
			BerkeleyParser.main(Arrays.copyOfRange(args, 4, args.length));
		} else if(args[0].equalsIgnoreCase("prodtrain")) {
			ProductParserTrainer.main(Arrays.copyOfRange(args, 4, args.length));
		} else if(args[0].equalsIgnoreCase("prodtest")) {
			ProductParser.main(Arrays.copyOfRange(args, 4, args.length));
		} else {
			System.out
					.println("The first parameter must be \"train\",  \"test\", \"prodtrain\" or \"prodtest\".");
			return;
		}
	}
}
