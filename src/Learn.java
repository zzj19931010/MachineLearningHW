package cs362;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Learn {
	static public LinkedList<Option> options = new LinkedList<Option>();
	
	public static void main(String[] args) throws IOException {
		// Parse the command line.
		String[] manditory_args = { "mode"};
		createCommandLineOptions();
		CommandLineUtilities.initCommandLineParameters(args, Learn.options, manditory_args);
	
		String mode = CommandLineUtilities.getOptionValue("mode");
		String data = CommandLineUtilities.getOptionValue("data");
		String predictions_file = CommandLineUtilities.getOptionValue("predictions_file");
		String algorithm = CommandLineUtilities.getOptionValue("algorithm");
		String model_file = CommandLineUtilities.getOptionValue("model_file");
		String task = CommandLineUtilities.getOptionValue("task"); // classification vs. regression
		
		double lambda = 1.0;
		if (CommandLineUtilities.hasArg("lambda"))
		lambda = CommandLineUtilities.getOptionValueAsFloat("lambda");
		
		double online_learning_rate = 1.0;
		if (CommandLineUtilities.hasArg("online_learning_rate"))
		online_learning_rate = CommandLineUtilities.getOptionValueAsFloat("online_learning_rate");
		
		int online_training_iterations = 1;
		if (CommandLineUtilities.hasArg("online_training_iterations"))
		online_training_iterations = CommandLineUtilities.getOptionValueAsInt("online_training_iterations");
		
		double polynomial_kernel_exponent = 2;
		if (CommandLineUtilities.hasArg("polynomial_kernel_exponent"))
		polynomial_kernel_exponent = CommandLineUtilities.getOptionValueAsFloat("polynomial_kernel_exponent");
		
		double gaussian_kernel_sigma = 1;
		if (CommandLineUtilities.hasArg("gaussian_kernel_sigma"))
		gaussian_kernel_sigma = CommandLineUtilities.getOptionValueAsFloat("gaussian_kernel_sigma");
		
		String kernel = "linear_kernel";
		if (CommandLineUtilities.hasArg("kernel"))
		kernel = CommandLineUtilities.getOptionValue("kernel");
		
		boolean classify = true;
		
		if (task != null && task.equals("regression")) {
		    classify = false;
		}
		
		if (mode.equalsIgnoreCase("train")) {
			if (data == null || algorithm == null || model_file == null) {
				System.out.println("Train requires the following arguments: data, algorithm, model_file");
				System.exit(0);
			}
			// Load the training data.
			DataReader data_reader = new DataReader(data, classify);
			List<Instance> instances = data_reader.readData();
			data_reader.close();
			
			// Train the model.
			Predictor predictor = null;
			if(algorithm.equalsIgnoreCase("perceptron"))
				predictor = train(instances, algorithm, online_learning_rate, online_training_iterations);
			else if(algorithm.equalsIgnoreCase("naive_bayes"))
				predictor = train(instances, algorithm, lambda);
			else if(algorithm.equals("kernel_logistic_regression")){
				if(kernel.equals("linear_kernel"))
					predictor = train(instances, kernel);
				else if(kernel.equals("polynomial_kernel"))
					predictor = train(instances, kernel, polynomial_kernel_exponent);
				else if (kernel.equals("gaussian_kernel"))
					predictor = train(instances, kernel, gaussian_kernel_sigma);
			}
			else
				predictor = train(instances, algorithm);
			saveObject(predictor, model_file);		
			
		} else if (mode.equalsIgnoreCase("test")) {
			if (data == null || predictions_file == null || model_file == null) {
				System.out.println("Train requires the following arguments: data, predictions_file, model_file");
				System.exit(0);
			}
			
			// Load the test data.
			DataReader data_reader = new DataReader(data, classify);
			List<Instance> instances = data_reader.readData();
			data_reader.close();
			
			// Load the model.
			Predictor predictor = (Predictor)loadObject(model_file);
			evaluateAndSavePredictions(predictor, instances, predictions_file);
		} else {
			System.out.println("Requires mode argument.");
		}
	}
	

	private static Predictor train(List<Instance> instances, String algorithm) {
	    // TODO Train the model using "algorithm" on "data"
	    // TODO Evaluate the model
		Predictor predictor = null;
		// instantiate the predictor with different inherited class by looking at the algorithm
		if(algorithm.equalsIgnoreCase("majority"))
			predictor = new MajorityClassifier();
		else if(algorithm.equalsIgnoreCase("even_odd"))
			predictor = new EvenOddClassifier();
		else if(algorithm.equals("linear_regression"))
			predictor = new LinearRegression();
		else if(algorithm.equals("linear_kernel"))
			predictor = new LinearKernelLogisticRegression();
		predictor.train(instances);
	    return predictor;
	}
	private static Predictor train(List<Instance> instances, String algorithm, 
			double lambda) {
	    // TODO Train the model using "algorithm" on "data"
		Predictor predictor = null;
		if(algorithm.equals("naive_bayes"))
			predictor = new NaiveBayesClassifier(lambda);
		else if(algorithm.equals("polynomial_kernel"))
			predictor = new PolynomialKernelLogisticRegression(lambda);
		else if(algorithm.equals("gaussian_kernel"))
			predictor = new GaussianKernelLogisticRegression(lambda);
		predictor.train(instances);
		return predictor;
	}
	private static Predictor train(List<Instance> instances, String algorithm, 
			double online_learning_rate, int online_training_iterations) {
	    // TODO Train the model using "algorithm" on "data"
		Predictor predictor = null;
		if(algorithm.equals("perceptron"))
			predictor = new PerceptronClassifier(online_learning_rate, online_training_iterations);
		predictor.train(instances);
		return predictor;
	}
	
	private static void evaluateAndSavePredictions(Predictor predictor,
			List<Instance> instances, String predictions_file) throws IOException {
		PredictionsWriter writer = new PredictionsWriter(predictions_file);
		// TODO Evaluate the model if labels are available. 
		int num = 0;
		double accuracy = 0;
		// check whether the labels are available or not
        for (Instance instance : instances )
            if (instance.getLabel()!=null)
                num++;
        if(num>0)
        		accuracy = new AccuracyEvaluator().evaluate(instances, predictor);
        System.out.println("Accuracy is : " +accuracy);
		for (Instance instance : instances) {
			Label label = predictor.predict(instance);
			writer.writePrediction(label);
		}
		
		writer.close();
		
	}

	public static void saveObject(Object object, String file_name) {
		try {
			ObjectOutputStream oos =
				new ObjectOutputStream(new BufferedOutputStream(
						new FileOutputStream(new File(file_name))));
			oos.writeObject(object);
			oos.close();
		}
		catch (IOException e) {
			System.err.println("Exception writing file " + file_name + ": " + e);
		}
	}

	/**
	 * Load a single object from a filename. 
	 * @param file_name
	 * @return
	 */
	public static Object loadObject(String file_name) {
		ObjectInputStream ois;
		try {
			ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(new File(file_name))));
			Object object = ois.readObject();
			ois.close();
			return object;
		} catch (IOException e) {
			System.err.println("Error loading: " + file_name);
		} catch (ClassNotFoundException e) {
			System.err.println("Error loading: " + file_name);
		}
		return null;
	}
	
	public static void registerOption(String option_name, String arg_name, boolean has_arg, String description) {
		OptionBuilder.withArgName(arg_name);
		OptionBuilder.hasArg(has_arg);
		OptionBuilder.withDescription(description);
		Option option = OptionBuilder.create(option_name);
		
		Learn.options.add(option);		
	}
	
	private static void createCommandLineOptions() {
		registerOption("data", "String", true, "The data to use.");
		registerOption("mode", "String", true, "Operating mode: train or test.");
		registerOption("predictions_file", "String", true, "The predictions file to create.");
		registerOption("algorithm", "String", true, "The name of the algorithm for training.");
		registerOption("model_file", "String", true, "The name of the model file to create/load.");
		registerOption("task", "String", true, "The name of the task (classification or regression).");
		registerOption("online_learning_rate", "double", true, "The LTU learning rate.");
		registerOption("online_training_iterations", "int", true, "The number of training iterations for LTU.");
		registerOption("lambda", "double", true, "The level of smoothing for Naive Bayes.");
		registerOption("polynomial_kernel_exponent", "double", true, "The exponent of the polynomial kernel.");
		registerOption("gaussian_kernel_sigma", "double", true, "The sigma of the Gaussian kernel.");
		registerOption("gradient_ascent_training_iterations", "int", true, "The number of training iterations.");
		registerOption("gradient_ascent_learning_rate", "double", true, "The learning rate for logistic regression.");
		registerOption("kernel", "String", true, "The kernel for Kernel Logistic regression [linear_kernel, polynomial_kernel, gaussian_kernel].");
		// Other options will be added here.
	}
}
