package sensors;

import java.awt.Color;

import org.jbox2d.common.Color3f;

public enum SensedType {
		
	RED_PUCK(Color3f.RED, Color.red),
	GREEN_PUCK(Color3f.GREEN, Color.green),
	MAGENTA_PUCK(new Color3f(1.0f, 0.0f, 1.0f), Color.magenta), 
	CYAN_PUCK(new Color3f(0.0f, 1.0f, 1.0f), Color.cyan), 
	ORANGE_PUCK(new Color3f(1.0f, 0.5f, 0.0f), Color.orange), 
	GRAY_PUCK(new Color3f(0.5f, 0.5f, 0.5f), Color.gray), 
	YELLOW_PUCK(new Color3f(1.0f, 1.0f, 0.0f), Color.yellow), 
	PINK_PUCK(new Color3f(1.0f, 192f/255f, 203f/255f), Color.pink), 
	// Make sure to fix the static methods below if adding a new puck colour!

	NOTHING(Color3f.WHITE, Color.white), 
	WALL(Color3f.BLACK, Color.black),
	ROBOT(Color3f.BLUE, Color.blue),
	// Should show up in displayed images but not affect behaviour.
	HIDDEN(new Color3f(0.4f, 0.4f, 0.4f), Color.lightGray); 		
	
	// JBox2D colour.
	public Color3f color3f;
	
	// Java AWT colour.
	public Color color;
	
	SensedType(Color3f color3f, Color color) {
		this.color3f = color3f;
		this.color = color;
	}
	
	//
	// We isolate the total number of puck colours and the definitions of those
	// colours to this class.  The following static variables methods allow 
	// other classes to enquire about the set of possible pucks.
	//
	
	public static final SensedType[] puckTypes = {RED_PUCK, GREEN_PUCK, MAGENTA_PUCK, CYAN_PUCK, ORANGE_PUCK, GRAY_PUCK, YELLOW_PUCK, PINK_PUCK};
	
	public static final int NPUCK_COLOURS = puckTypes.length;
	
	public static Color3f[] getPuckColours() {
		Color3f[] array = new Color3f[NPUCK_COLOURS];
		for (int i=0; i<NPUCK_COLOURS; i++)
			array[i] = puckTypes[i].color3f;
		return array;
	}

	public static boolean isPuckOfColor(SensedType sensedType, Color3f color) {
		for (SensedType type : puckTypes)
			if (sensedType == type)
				return sensedType.color3f.equals(color);
		return false;
	}

	public static SensedType getPuckType(int k) {
		if (k >=0 && k < NPUCK_COLOURS)
			return puckTypes[k];
		else {
			System.err.println("SensedType: Invalid puck index!");
			System.exit(-1);
			return null;
		}
	}

	public static int getPuckIndex(SensedType sensedType) {
		int i = sensedType.ordinal();
		if (i >= 0 && i < NPUCK_COLOURS)
			return i;
		else {
			System.err.println("SensedType: No index for this type!");
			(new Exception()).printStackTrace();
			System.exit(-1);
			return 0;
		}
	}

	public static String getPuckColorName(int k) {
		if (k >=0 && k < NPUCK_COLOURS) {
			String typeString = puckTypes[k].toString();
			int underscorePos = typeString.indexOf('_');
			return typeString.substring(0, underscorePos).toLowerCase();
		} else {
			System.err.println("SensedType: Invalid puck index!");
			System.exit(-1);
			return null;
		}
	}

	public static boolean isPuckType(SensedType givenType) {
		for (SensedType type : puckTypes)
			if (type == givenType)
				return true;
		return false;
	}
}
