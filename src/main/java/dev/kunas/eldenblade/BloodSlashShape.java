package dev.kunas.eldenblade;

/** Shared mathematical sweep definition, independent of the camera and rendering APIs. */
public final class BloodSlashShape {
    public static final int SEGMENTS = 32;
    public static final int RADIAL_BANDS = 3;
    public static final int FLECKS = 8;
    public static final float SWEEP_TICKS = 3.2f;
    private BloodSlashShape() {}

    public static float clamp(float value) { return Math.max(0, Math.min(1, value)); }
    public static float smooth(float value) {
        float t = clamp(value);
        return t * t * (3 - 2 * t);
    }
    public static float reveal(float age) { return smooth(age / SWEEP_TICKS); }
    public static float angle(float u, boolean second) {
        return (float)Math.toRadians((second ? 78 : 84) - u * (second ? 162 : 164));
    }
    public static float radius(float v, float age) {
        return 4.65f + .35f * smooth(age / SWEEP_TICKS) - 4.4f * v;
    }
    public static float roll(boolean second) { return second ? -38 : 42; }
    public static float opacity(float u, float v, float age) {
        // The oldest part of the arc fades first, so a sweep does not become a static emblem.
        float localAge = age - u * SWEEP_TICKS;
        float fade = 1 - smooth((localAge - 3) / 7.5f);
        float ends = (float)Math.pow(Math.max(0, Math.sin(Math.PI * u)), .2);
        return fade * ends * (float)Math.sqrt(1 - clamp(v));
    }
}
