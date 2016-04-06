package utils;

public class AngleUtils {
	
	public static float PIf = (float) Math.PI;
	public static float TWO_PIf = (float) (2.0 * Math.PI);
	public static float PI_OVER_TWOf = (float) (Math.PI / 2.0);
	public static float TO_RADf = (float) (Math.PI / 180);
	public static float PI_SQDf = (float) (Math.PI * Math.PI);
	public static float PI_OVER_4f = (float) (Math.PI/4.0);

	public static double PI = Math.PI;
	public static double TWO_PI = (2.0 * Math.PI);
	public static double PI_OVER_TWO = (Math.PI / 2.0);
	public static double TO_RAD = (Math.PI / 180);
	public static double PI_SQD = (Math.PI * Math.PI);
	public static double PI_OVER_4 = (Math.PI/4.0);

    /**
     * Given an input angle in radians, return an equivalent angle constrained
     * to lie in the range (-pi, pi].
     */
    public static double constrainAngle(double angle) {
        while ( angle > Math.PI )
            angle -= 2 * Math.PI;
        while ( angle <= -Math.PI )
            angle += 2 * Math.PI;
        return angle;
    }    
    
    public static double getAngularDifference(double angleA, double angleB) {
        angleA = AngleUtils.constrainAngle(angleA);
        angleB = AngleUtils.constrainAngle(angleB);
        double error = Math.abs(angleA - angleB);
        if ( error > Math.PI ) {
            error -= Math.PI * 2;
            error = Math.abs(error);
        }
        return error;
    }
}
