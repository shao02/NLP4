package nlp.assignments;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import nlp.classify.*;
import nlp.util.CommandLineUtils;
import nlp.util.Counter;

/**
 * This is the main harness for assignment 2. To run this harness, use
 * <p/>
 * java nlp.assignments.ProperNameTester -path ASSIGNMENT_DATA_PATH -model
 * MODEL_DESCRIPTOR_STRING
 * <p/>
 * First verify that the data can be read on your system using the baseline
 * model. Second, find the point in the main method (near the bottom) where a
 * MostFrequentLabelClassifier is constructed. You will be writing new
 * implementations of the ProbabilisticClassifer interface and constructing them
 * there.
 */
public class ProperNameTester {

	//public static double UNI_weight = 1;
	public static double BI_weight =1;
	public static double TOKEN_weight=3;
	public static double TRI_weight=1; 

	//best setting is  TOKEN_weight=3;,TRI_weight=1; alpha =1.3
	public static class ProperNameFeatureExtractor implements FeatureExtractor<String, String> 
	{

		/**
		 * This method takes the list of characters representing the proper name
		 * description, and produces a list of features which represent that
		 * description. The basic implementation is that only character-unigram
		 * features are extracted. An easy extension would be to also throw
		 * character bigrams into the feature list, but better features are also
		 * possible.
		 */
		public Counter<String> extractFeatures(String name) {
			char[] characters = name.toCharArray();
			// dont use to LowerCase, this will reduce the performance about 3%

			Counter<String> features = new Counter<String>();
			//double TRI_weight = 1.0- UNI_weight -TOKEN_weight;
			// add character unigram features
			//BEST 0.8804 :UNI 0.5 , TOKEN 3.5 ,TRI-1.0

			//double UNI_weight = 0.1;
			//double TOKEN_weight = 0.7;
			//double TRI_weight = 0.2;

			/*for (int i = 0; i < characters.length; i++) {
				char character = characters[i];
				features.incrementCount("UNI-" + character, UNI_weight);
			}*/

			// TODO : extract bigram charecter-level features!
			
			features.incrementCount("BI-" +"<SOF>"+ characters[0], BI_weight);
			for (int i = 0; i < characters.length-1; i++) {
				features.incrementCount("BI-" + characters[i] + characters[i+1], BI_weight);
			}
			features.incrementCount("BI-" + characters[characters.length-1] + "<EOF>", BI_weight);
					
			String[] parts = name.split(" ");
			for(int i =0;i<parts.length;i++)
				features.incrementCount("TOKEN-" + parts[i], TOKEN_weight);

			/*for(int i =0;i<parts.length-1;i++)
				features.incrementCount("BITOKEN-" + parts[i] + parts[i+1], 1.3);
			features.incrementCount("BITOKEN-" + parts[parts.length-1] + "<EOF>", 1.3);*/
			if(characters.length>1)
				features.incrementCount("TRI-" +"<SOF>"+ characters[0]+characters[1], TRI_weight);
			for (int i = 0; i < characters.length-2; i++) {
				char character = characters[i];
				features.incrementCount("TRI-" + characters[i] + characters[i+1]+ characters[i+2], TRI_weight);
			}
			//last word
			
			if(characters.length >=3 )
			{
				features.incrementCount("TRI-" + characters[characters.length-2]+characters[characters.length-1] + "<EOF>", 1.0);
				features.incrementCount("TRI-" + characters[characters.length-1] + "<EOF><EOF>", TRI_weight);
			}
			else if(characters.length == 2)
			{
                features.incrementCount("TRI-" + characters[characters.length-2]+characters[characters.length-1] + "<EOF>", 1.0);
				features.incrementCount("TRI-" + characters[characters.length-1] + "<EOF><EOF>", TRI_weight);
			}
			else if(characters.length == 1)
			{
				features.incrementCount("TRI-" + characters[characters.length-1] + "<EOF><EOF>", TRI_weight);
			}

	

			
			return features;
		}
	}

	private static List<LabeledInstance<String, String>> loadData(
			String fileName) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(fileName));
		List<LabeledInstance<String, String>> labeledInstances = new ArrayList<LabeledInstance<String, String>>();
		while (reader.ready()) {
			String line = reader.readLine();
			String[] parts = line.split("\t");
			String label = parts[0];
			String name = parts[1];
			LabeledInstance<String, String> labeledInstance = new LabeledInstance<String, String>(
					label, name);
			labeledInstances.add(labeledInstance);
		}
		reader.close();
		return labeledInstances;
	}

	private static double testClassifier(
			ProbabilisticClassifier<String, String> classifier,
			List<LabeledInstance<String, String>> testData, boolean verbose) {
		double numCorrect = 0.0;
		double numTotal = 0.0;
		for (LabeledInstance<String, String> testDatum : testData) {
			String name = testDatum.getInput();
			String label = classifier.getLabel(name);
			double confidence = classifier.getProbabilities(name).getCount(
					label);
			if (label.equals(testDatum.getLabel())) {
				numCorrect += 1.0;
			} else {
				if (verbose) {
					// display an error
					System.err.println("Error: " + name + " guess=" + label
							+ " gold=" + testDatum.getLabel() + " confidence="
							+ confidence);
				}
			}
			numTotal += 1.0;
		}
		double accuracy = numCorrect / numTotal;
		//	System.out.println("Accuracy: " + accuracy);
		return accuracy;
	}

	public static void main(String[] args) throws IOException {
		// Parse command line flags and arguments
		Map<String, String> argMap = CommandLineUtils
				.simpleCommandLineParser(args);

		// Set up default parameters and settings
		String basePath = ".";
		String model = "baseline";
		boolean verbose = false;
		boolean useValidation = true;

		// Update defaults using command line specifications

		// The path to the assignment data
		if (argMap.containsKey("-path")) {
			basePath = argMap.get("-path");
		}
		System.out.println("Using base path: " + basePath);

		// A string descriptor of the model to use
		if (argMap.containsKey("-model")) {
			model = argMap.get("-model");
		}
		System.out.println("Using model: " + model);

		// A string descriptor of the model to use
		if (argMap.containsKey("-test")) {
			String testString = argMap.get("-test");
			if (testString.equalsIgnoreCase("test"))
				useValidation = false;
		}
		System.out.println("Testing on: "
				+ (useValidation ? "validation" : "test"));

		// Whether or not to print the individual speech errors.
		if (argMap.containsKey("-verbose")) {
			verbose = true;
		}

		// Load training, validation, and test data
		List<LabeledInstance<String, String>> trainingData = loadData(basePath
				+ "/pnp-train.txt");
		List<LabeledInstance<String, String>> validationData = loadData(basePath
				+ "/pnp-validate.txt");
		List<LabeledInstance<String, String>> testData = loadData(basePath
				+ "/pnp-test.txt");

		// Learn a classifier
		ProbabilisticClassifier<String, String> classifier = null;
		if (model.equalsIgnoreCase("baseline")) {
			classifier = new MostFrequentLabelClassifier.Factory<String, String>()
					.trainClassifier(trainingData);
		} else if (model.equalsIgnoreCase("sing")) {
			// TODO: construct your n-gram model here
			//TRI_weight = 1.0 - UNI_weight - TOKEN_weight;
					
			ProbabilisticClassifierFactory<String, String> factory = new MaximumEntropyClassifier.Factory<String, String, String>(
				1.3, 300, new ProperNameFeatureExtractor());
			classifier = factory.trainClassifier(trainingData);
			double acc = testClassifier(classifier, (useValidation ? validationData : testData),verbose);
			System.out.println(BI_weight+","+TOKEN_weight+","+TRI_weight+","+acc);
		} else if (model.equalsIgnoreCase("maxent")) {
			// TODO: construct your maxent model here
			
			BI_weight =0.05;
			TOKEN_weight= 0.8000000000000002;
			TRI_weight = 0.1499999999999998;
			
				ProbabilisticClassifierFactory<String, String> factory = new MaximumEntropyClassifier.Factory<String, String, String>(
					5.0, 150, new ProperNameFeatureExtractor());

				classifier = factory.trainClassifier(trainingData);
				double acc = testClassifier(classifier, (useValidation ? validationData : testData),verbose);
				//;System.out.println(alpha+","+BI_weight+","+TOKEN_weight+","+TRI_weight+","+acc)
				System.out.println(acc);

			//test
			/*
			for(double alpha = 5.00 ;alpha >= 2.5; alpha-=0.25)
			{
				for(BI_weight =0.05; BI_weight<= 0.9 ; BI_weight+=0.05)
					for(TOKEN_weight=0.05 ;TOKEN_weight <= 1.0 - BI_weight -0.05  ;TOKEN_weight+=0.05)
					{
						TRI_weight = 1.0 - BI_weight - TOKEN_weight;
						
						ProbabilisticClassifierFactory<String, String> factory = new MaximumEntropyClassifier.Factory<String, String, String>(
							alpha, 150, new ProperNameFeatureExtractor());
						classifier = factory.trainClassifier(trainingData);
						double acc = testClassifier(classifier, (useValidation ? validationData : testData),verbose);
						System.out.println(alpha+","+BI_weight+","+TOKEN_weight+","+TRI_weight+","+acc);
					}
			}*/
		} 
		else {
			throw new RuntimeException("Unknown model descriptor: " + model);
		}

		// Test classifier
		//testClassifier(classifier, (useValidation ? validationData : testData),verbose);
	}
}
