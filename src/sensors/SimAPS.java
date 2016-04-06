package sensors;

import org.jbox2d.dynamics.Body;

public class SimAPS implements APS {

	Body body;
	
	public SimAPS(Body body) {
		this.body = body;
	}
	
	@Override
	public Pose getPose(String robotName) {
		return new Pose(body.m_xf.position.x, 
									body.m_xf.position.y, 
									body.getAngle());
	}
}
