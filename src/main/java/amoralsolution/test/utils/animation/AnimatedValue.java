package amoralsolution.test.utils.animation;

public final class AnimatedValue {
    private final float speed;
    private float current;
    private float target;
    private boolean initialized;
    private long lastNanos;

    public AnimatedValue(long durationMs, Animation.Easing easing) {
        long d = Math.max(10L, durationMs);
        this.speed = 4600.0f / (float) d;
    }

    public float toward(float target) {
        if (!this.initialized) {
            this.initialized = true;
            this.target = target;
            this.current = target;
            this.lastNanos = System.nanoTime();
            return target;
        }
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - this.lastNanos) / 1_000_000_000.0f);
        this.lastNanos = now;
        this.target = target;
        if (dt > 0.0f) {
            float factor = (float) (1.0 - Math.exp(-this.speed * dt));
            this.current += (this.target - this.current) * factor;
            if (Math.abs(this.target - this.current) < 0.0001f) {
                this.current = this.target;
            }
        }
        return this.current;
    }

    public float value() {
        return this.current;
    }

    public float getTarget() {
        return this.target;
    }

    public void setValue(float value) {
        this.initialized = true;
        this.target = value;
        this.current = value;
        this.lastNanos = System.nanoTime();
    }
}
