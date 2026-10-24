package amoralsolution.test.utils.animation;

import java.util.concurrent.TimeUnit;

public final class Animation {
    private long startNanos;
    private long durationMillis;
    private float fromValue;
    private float toValue;
    private boolean reverse;
    private Easing easing;

    public Animation(long duration, Easing easing) {
        setDuration(duration);
        setEasing(easing);
        reset();
    }

    public void reset() {
        this.startNanos = System.nanoTime();
    }

    public void animate(float from, float to, long duration, Easing easing) {
        this.fromValue = from;
        this.toValue = to;
        setDuration(duration);
        setEasing(easing);
        this.reverse = false;
        reset();
    }

    public float getValue() {
        float progress = progress();
        if (this.reverse) {
            progress = 1.0F - progress;
        }
        return this.fromValue + (this.toValue - this.fromValue) * this.easing.ease(progress);
    }

    public float getToValue() {
        return this.toValue;
    }

    public float getFromValue() {
        return this.fromValue;
    }

    public boolean isFinished() {
        return elapsedMillis() >= this.durationMillis;
    }

    public void setReverse(boolean reverse) {
        if (this.reverse == reverse) {
            return;
        }
        long elapsed = Math.min(elapsedMillis(), this.durationMillis);
        this.reverse = reverse;
        this.startNanos = System.nanoTime() - TimeUnit.MILLISECONDS.toNanos(this.durationMillis - elapsed);
    }

    public void setDuration(long duration) {
        if (duration < 0L) {
            throw new IllegalArgumentException("Animation duration cannot be negative");
        }
        this.durationMillis = duration;
    }

    public void setEasing(Easing easing) {
        if (easing == null) {
            throw new NullPointerException("easing");
        }
        this.easing = easing;
    }

    private float progress() {
        if (this.durationMillis == 0L) {
            return 1.0F;
        }
        float p = elapsedMillis() / (float) this.durationMillis;
        return Math.max(0.0F, Math.min(p, 1.0F));
    }

    private long elapsedMillis() {
        return TimeUnit.NANOSECONDS.toMillis(Math.max(0L, System.nanoTime() - this.startNanos));
    }

    public enum Easing {
        LINEAR {
            @Override
            public float ease(float x) {
                return x;
            }
        },
        EASE_IN_QUAD {
            @Override
            public float ease(float x) {
                return x * x;
            }
        },
        EASE_OUT_QUAD {
            @Override
            public float ease(float x) {
                return 1.0F - (1.0F - x) * (1.0F - x);
            }
        },
        EASE_OUT_CUBIC {
            @Override
            public float ease(float x) {
                float inverse = 1.0F - x;
                return 1.0F - inverse * inverse * inverse;
            }
        },
        EASE_OUT_EXPO {
            @Override
            public float ease(float x) {
                return x >= 1.0F ? 1.0F : 1.0F - (float) Math.pow(2.0D, -10.0D * x);
            }
        },
        EASE_IN_OUT_QUAD {
            @Override
            public float ease(float x) {
                return x < 0.5F ? 2.0F * x * x : 1.0F - (float) Math.pow(-2.0F * x + 2.0F, 2.0F) / 2.0F;
            }
        };

        public abstract float ease(float x);
    }
}
