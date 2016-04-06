package experiment;



/**
 * Returns only the default values of experiment parameters.  Useful for
 * testing...  For interactive exploration of parameters use EditableExperiment.
 * @author av
 *
 */
public class LiveExperiment extends Experiment {

	String code;
	
	LiveExperiment(int seed, String outputDir, String code) {
		super(code, seed, outputDir);
	}
	
	public String getStringCode() {
		return code + "__" + seed;
	}

	public String getStringCodeWithoutSeed() {
		return code;
	}
}
