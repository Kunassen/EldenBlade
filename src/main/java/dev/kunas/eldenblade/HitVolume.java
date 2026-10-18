package dev.kunas.eldenblade;

/** Separating-axis test: axis-aligned target box versus a yaw-oriented 7-block cube. */
public final class HitVolume {
    private HitVolume() {}
    public static boolean intersects(double dx, double dy, double dz, double hx, double hy, double hz,
                                     double fx, double fz) {
        double rx = fz, rz = -fx;
        return Math.abs(dy) <= 3.5 + hy
            && Math.abs(dx) <= hx + 3.5 * (Math.abs(fx) + Math.abs(rx))
            && Math.abs(dz) <= hz + 3.5 * (Math.abs(fz) + Math.abs(rz))
            && Math.abs(dx * fx + dz * fz) <= 3.5 + hx * Math.abs(fx) + hz * Math.abs(fz)
            && Math.abs(dx * rx + dz * rz) <= 3.5 + hx * Math.abs(rx) + hz * Math.abs(rz);
    }
}
