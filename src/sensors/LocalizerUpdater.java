package sensors;

import org.jbox2d.common.Transform;

import arena.Arena;

public interface LocalizerUpdater {

	public void init(Transform robotTransform, Arena arena);
	
	public void update();

	public void draw();

	public Transform getHomeTransform();
}
