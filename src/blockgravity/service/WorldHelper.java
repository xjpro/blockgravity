package blockgravity.service;

public class WorldHelper {

	public interface BlockModifier {
		boolean modify(int x, int z);
	}

	public static boolean inCircle(int centerX, int centerZ, int radius, BlockModifier modifier) {
		for (int x = centerX - radius; x <= centerX; x++) {
			for (int z = centerZ - radius; z <= centerZ; z++) {
				if ((x - centerX) * (x - centerX) + (z - centerZ) * (z - centerZ) <= radius * radius) {
					int otherX = centerX - (x - centerX);
					int otherZ = centerZ - (z - centerZ);
					// (x, z), (x, otherZ), (otherX , z), (otherX, otherZ) are in the circle
					if (modifier.modify(x, z)) return true;
					if (modifier.modify(x, otherZ)) return true;
					if (modifier.modify(otherX, z)) return true;
					if (modifier.modify(otherX, otherZ)) return true;
				}
			}
		}
		return false;
	}
}
