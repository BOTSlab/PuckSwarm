package controllers;

import experiment.ExperimentManager;

public class ControllerUtils {
	public static Controller create(boolean presetCaches) {
		String controllerType = ExperimentManager.getCurrent().getProperty(
				"controllerType", "BucketBrigade", null);
		
		Controller controller = null;
		if (controllerType.equals("TestMovement"))
			controller = new TestMovementController();
		else if (controllerType.equals("TestAPS"))
			controller = new TestAPSController();
		else if (controllerType.equals("BHD"))
			controller = new BHDController();
		else if (controllerType.equals("TestKick"))
			controller = new TestKickController();
		else if (controllerType.equals("ProbSeek"))
			controller = new ProbSeekController();
		else if (controllerType.equals("CacheCons"))
			controller = new CacheConsController(presetCaches);
		else if (controllerType.equals("SimpleDistribution"))
			controller = new SimpleDistributionController();
		else if (controllerType.equals("BucketBrigade"))
			controller = new BucketBrigadeController();
		else if (controllerType.equals("ProjectileBucketBrigade"))
			controller = new ProjectileBucketBrigadeController();
		else {
			// Not one of the "pure controller" controllers, maybe its a BehaviourController?
			controller = createBehaviourController(controllerType);
		}
		
		if (controller == null) {
			System.err.println("ControllerUtils: Bad value for controllerType!");
			System.exit(-1);
		}

		return controller;
	}
	
	private static BehaviourController createBehaviourController(String type) {
		BehaviourController bc = new BehaviourController();
		
		if (type.equals("TestTurn")) {
			bc.addBehaviour(new TestTurnBehaviour());
			return bc;
		} else if (type.equals("VFHWander")) {
			bc.addBehaviour(new VFHWanderBehaviour());
			bc.addBehaviour(new StraightBehaviour());
			return bc;

		} else if (type.equals("OdoHome")) {
			bc.addBehaviour(new OdoHomeBehaviour());
			bc.addBehaviour(new StraightBehaviour());
			return bc;

		} else if (type.equals("APSHome")) {
			bc.addBehaviour(new APSHomeBehaviour());
			bc.addBehaviour(new StraightBehaviour());
			return bc;
		
		/*
		} else if (type.equals("bhd")) {
			bc.addBehaviour(new EscapeOthersBehaviour());
			bc.addBehaviour(new BackupAndTurnBehaviour());
			bc.addBehaviour(new SimpleAvoidBehaviour());
			bc.addBehaviour(new StraightBehaviour());
			return bc;
			
		} else if (type.equals("bhdPrime")) {
			bc.addBehaviour(new EscapeOthersBehaviour());
			bc.addBehaviour(new BackupAndTurnBehaviour());
			bc.addBehaviour(new SteerAwayBehaviour());
			bc.addBehaviour(new StraightBehaviour());
			return bc;

		} else if (type.equals("seek")) {
			bc.addBehaviour(new EscapeOthersBehaviour());
			bc.addBehaviour(new BackupAndTurnBehaviour());
			bc.addBehaviour(new SteerAwayBehaviour());
			bc.addBehaviour(new SeekBehaviour());
			bc.addBehaviour(new StraightBehaviour());
			return bc;

		} else if (type.equals("mel")) {
			bc.addBehaviour(new EscapeOthersBehaviour());
			bc.addBehaviour(new ColourBackupBehaviour());
			bc.addBehaviour(new SimpleAvoidBehaviour());
			bc.addBehaviour(new StraightBehaviour());
			return bc;

		} else if (type.equals("Proximal")) {
			bc.addBehaviour(new EscapeOthersBehaviour());
			bc.addBehaviour(new ColourBackupBehaviour());
			bc.addBehaviour(new SteerAwayBehaviour());
			bc.addBehaviour(new StraightBehaviour());
			return bc;

		} else if (type.equals("Seeker")) {
			bc.addBehaviour(new EscapeOthersBehaviour());
			bc.addBehaviour(new ColourBackupBehaviour());
			bc.addBehaviour(new SteerAwayBehaviour());
			bc.addBehaviour(new ColourSeekBehaviour());
			bc.addBehaviour(new StraightBehaviour());
			return bc;
		*/
		}

		// No match was found.
		return null;
	}	
}
