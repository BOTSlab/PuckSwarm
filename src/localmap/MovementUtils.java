package localmap;

import arena.Robot;
import utils.AngleUtils;

public class MovementUtils {
	/**
	 * Given a desired turn angle, compute the corresponding Movement.  This relationship 
	 * is given by equation (15) from "Smooth Nearness-Diagram Navigation" by Durham 
	 * and Bullo, IROS 2008 with the exception that we do not allow a forward speed of 0
	 * (negative forward speeds are also not generated).
	 */
	public static float getSpeedScale(float desiredTurn) {
		float speedScale = (AngleUtils.PI_OVER_4f - Math.abs(desiredTurn))/ AngleUtils.PI_OVER_4f;
		
		// Unlike Durham and Bullo we do not allow a forward speed of zero
		// as this can yield oscillatory behaviour on behalf of VFH+.
		return sat(speedScale, 0.25f, 1);
	}
	
	/**
	 * For the given desired turn angle, return a turning rate in the range [-1, 1].  This 
	 * relationship is given by equation (15) from "Smooth 
	 * Nearness-Diagram Navigation" by Durham and Bullo, IROS 2008.
	 */
	public static float getTorqueScale(float desiredTurn) {
		float K = 2.0f;
		return sat((float) (K * desiredTurn / (0.5 * Math.PI)), -1, 1);
	}	

	/** 
	 * Saturation function from equation (2) of "Smooth Nearness-Diagram Navigation"
	 * by Durham and Bullo, IROS 2008.
	 */
	private static float sat(float x, float a, float b) {
		if (x <= a)
			return a;
		else if (x >= b)
			return b;
		else
			return x;
	}
	
	public static MovementCommand applyVFH(VFHPlus vfh, LocalMap localMap, float desiredTurn) {
		return applyVFH(vfh, localMap, desiredTurn, false);
	}

	public static MovementCommand applyVFH(VFHPlus vfh, LocalMap localMap, float desiredTurn, boolean ignorePucks) {
		Float result = vfh.computeTurnAngle(localMap, desiredTurn, ignorePucks);
		vfh.updateGUI();
		if (result == null) {
			// There remains no valid turn angle.
			return new MovementCommand(0.5f, (float) (0.5f * Math.PI * localMap.getFreeerSide()));
			//return new MovementCommand(0, 0);
		} else {
			return new MovementCommand(1, (float) (0.5f * Math.PI * result.floatValue()));
		}
	}

}
