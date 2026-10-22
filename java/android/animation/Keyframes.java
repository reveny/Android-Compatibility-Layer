package android.animation;

import java.util.List;

interface Keyframes extends Cloneable
{
    void setEvaluator(TypeEvaluator evaluator);

    Class getType();

    Object getValue(float fraction);

    List<Keyframe> getKeyframes();

    Keyframes clone();
}
