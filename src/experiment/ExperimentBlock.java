package experiment;


/**
 * Represents a block of experiments.  Such a block consist of one repeated
 * Experiment.
 */
public class ExperimentBlock {
	private int size, index;
	private Experiment[] experiments;
	
	/**
	 * Constructor
	 * @param size							Number of repetitions
	 * @param startIndex					Indicates that we should start at repetition
	 * 										'startIndex'.  This allows a halted ExperimentBlock
	 * 										(which terminated due to a crash or other) to
	 * 										be restarted.  Otherwise this should be 0.
	 * @param outputDir					Base directory for output.
	 * @param propertiesFilename		Name of the file storing the Experiment's properties.
	 */
	public ExperimentBlock(int size, int startIndex, String outputDir, String propertiesFilename) {
		
		this.size = size;
		index = startIndex;
		experiments = new Experiment[size];
		for (int i=startIndex; i<size; i++)
			experiments[i] = new Experiment(i, outputDir, propertiesFilename);
	}

	public Experiment getCurrent() {
		return experiments[index];
	}

	public boolean hasNext() {
		return index < size - 1;
	}

	public void next() {
		index++;
	}

	// The following methods are used only by Plotter.
	/*
	int getSize() {
		return size;
	}
	
	Experiment get(int i) {
		return experiments[i];
	}
	*/
}