package android.animation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public final class AnimatorSet extends Animator
{
    private static final int RULE_WITH = 0;
    private static final int RULE_AFTER = 1;

    private ArrayList<Node> mNodes = new ArrayList<>();
    private HashMap<Animator, Node> mNodeMap = new HashMap<>();

    private long mDuration = -1;
    private long mStartDelay;
    private TimeInterpolator mInterpolator;

    private boolean mStarted;
    private boolean mTerminated;
    private boolean mReversing;
    private long mStartedAtNanos = -1;
    private long mSeekPlayTime = -1;
    private ValueAnimator mDelayAnimator;
    private ChildListener mChildListener = new ChildListener();

    public AnimatorSet()
    {
    }

    public void playTogether(Animator... items)
    {
        if (items == null || items.length == 0) return;

        Builder builder = play(items[0]);
        for (int index = 1; index < items.length; index++) builder.with(items[index]);
    }

    public void playTogether(Collection<Animator> items)
    {
        if (items == null || items.isEmpty()) return;

        Builder builder = null;

        for (Animator animator : items)
        {
            if (builder == null) builder = play(animator);
            else builder.with(animator);
        }
    }

    public void playSequentially(Animator... items)
    {
        if (items == null || items.length == 0) return;

        if (items.length == 1)
        {
            play(items[0]);
            return;
        }

        for (int index = 0; index < items.length - 1; index++) play(items[index]).before(items[index + 1]);
    }

    public void playSequentially(List<Animator> items)
    {
        if (items == null || items.isEmpty()) return;

        if (items.size() == 1)
        {
            play(items.get(0));
            return;
        }

        for (int index = 0; index < items.size() - 1; index++) play(items.get(index)).before(items.get(index + 1));
    }

    public ArrayList<Animator> getChildAnimations()
    {
        ArrayList<Animator> children = new ArrayList<>();

        for (int index = 0; index < mNodes.size(); index++)
        {
            Node node = mNodes.get(index);
            if (!node.mInternal) children.add(node.mAnimation);
        }

        return children;
    }

    @Override
    public void setTarget(Object target)
    {
        for (int index = 0; index < mNodes.size(); index++)
        {
            Animator animation = mNodes.get(index).mAnimation;

            if (animation instanceof AnimatorSet) animation.setTarget(target);
            else if (animation instanceof ObjectAnimator) animation.setTarget(target);
        }
    }

    @Override
    public int getChangingConfigurations()
    {
        int configurations = super.getChangingConfigurations();

        for (int index = 0; index < mNodes.size(); index++) configurations |= mNodes.get(index).mAnimation.getChangingConfigurations();

        return configurations;
    }

    @Override
    public void setInterpolator(TimeInterpolator interpolator)
    {
        mInterpolator = interpolator;
    }

    @Override
    public TimeInterpolator getInterpolator()
    {
        return mInterpolator;
    }

    public Builder play(Animator animation)
    {
        if (animation == null) return null;

        return new Builder(animation);
    }

    private Node getNodeForAnimation(Animator animation)
    {
        Node node = mNodeMap.get(animation);

        if (node == null)
        {
            node = new Node(animation);
            mNodeMap.put(animation, node);
            mNodes.add(node);
        }

        return node;
    }

    @Override
    public void cancel()
    {
        if (!isStarted()) return;

        mTerminated = true;

        if (mDelayAnimator != null) mDelayAnimator.cancel();

        ArrayList<Node> nodes = new ArrayList<>(mNodes);

        for (int index = 0; index < nodes.size(); index++)
        {
            Node node = nodes.get(index);
            if (node.mStarted && !node.mEnded) node.mAnimation.cancel();
        }

        notifyCancel();
        finish();
    }

    @Override
    public void end()
    {
        if (!isStarted()) return;

        mTerminated = true;

        if (mDelayAnimator != null) mDelayAnimator.cancel();

        ArrayList<Node> ordered = sortedNodes(mReversing);

        for (int index = 0; index < ordered.size(); index++)
        {
            Node node = ordered.get(index);
            if (node.mEnded) continue;

            if (!node.mStarted)
            {
                node.mStarted = true;
                prepare(node);
                node.mAnimation.start();
            }

            node.mAnimation.end();
            node.mEnded = true;
        }

        finish();
    }

    private void finish()
    {
        boolean reversing = mReversing;

        for (int index = 0; index < mNodes.size(); index++) mNodes.get(index).mAnimation.removeListener(mChildListener);

        mStarted = false;
        mStartedAtNanos = -1;
        mReversing = false;
        clearPaused();

        notifyEnd(reversing);
    }

    @Override
    public boolean isRunning()
    {
        return mStarted && mDelayAnimator == null;
    }

    @Override
    public boolean isStarted()
    {
        return mStarted;
    }

    @Override
    public long getStartDelay()
    {
        return mStartDelay;
    }

    @Override
    public void setStartDelay(long startDelay)
    {
        if (startDelay < 0) startDelay = 0;

        mStartDelay = startDelay;
    }

    @Override
    public long getDuration()
    {
        return mDuration;
    }

    @Override
    public AnimatorSet setDuration(long duration)
    {
        if (duration < 0) throw new IllegalArgumentException("duration must be a value of zero or greater");

        mDuration = duration;
        return this;
    }

    @Override
    public void setupStartValues()
    {
        for (int index = 0; index < mNodes.size(); index++) mNodes.get(index).mAnimation.setupStartValues();
    }

    @Override
    public void setupEndValues()
    {
        for (int index = 0; index < mNodes.size(); index++) mNodes.get(index).mAnimation.setupEndValues();
    }

    @Override
    public void pause()
    {
        boolean previouslyPaused = isPaused();
        super.pause();

        if (previouslyPaused || !isPaused()) return;

        if (mDelayAnimator != null) mDelayAnimator.pause();

        for (int index = 0; index < mNodes.size(); index++)
        {
            Node node = mNodes.get(index);
            if (node.mStarted && !node.mEnded) node.mAnimation.pause();
        }
    }

    @Override
    public void resume()
    {
        boolean previouslyPaused = isPaused();
        super.resume();

        if (!previouslyPaused || isPaused()) return;

        if (mDelayAnimator != null) mDelayAnimator.resume();

        for (int index = 0; index < mNodes.size(); index++)
        {
            Node node = mNodes.get(index);
            if (node.mStarted && !node.mEnded) node.mAnimation.resume();
        }
    }

    public void setCurrentPlayTime(long playTime)
    {
        if (mReversing && getTotalDuration() == DURATION_INFINITE)
        {
            throw new UnsupportedOperationException("Error: Cannot seek in reverse in an infinite AnimatorSet");
        }

        if ((getTotalDuration() != DURATION_INFINITE && playTime > getTotalDuration() - mStartDelay) || playTime < 0)
        {
            throw new UnsupportedOperationException("Error: Play time should always be in between 0 and duration.");
        }

        HashMap<Node, Long> startTimes = computeStartTimes();
        ArrayList<Node> ordered = sortedNodes(false);

        for (int index = 0; index < ordered.size(); index++)
        {
            Node node = ordered.get(index);
            Animator animation = node.mAnimation;
            prepare(node);

            Long start = startTimes.get(node);
            long local = start == null || start == DURATION_INFINITE ? -1 : playTime - start;

            long childDuration = animation.getTotalDuration() == DURATION_INFINITE ? Long.MAX_VALUE
                                                                                    : animation.getTotalDuration() - animation.getStartDelay();
            long clamped = Math.max(0, Math.min(local - animation.getStartDelay(), childDuration));

            if (animation instanceof ValueAnimator) ((ValueAnimator)animation).setCurrentPlayTime(clamped);
            else if (animation instanceof AnimatorSet && local >= 0) ((AnimatorSet)animation).setCurrentPlayTime(clamped);
        }

        mSeekPlayTime = playTime;
    }

    public long getCurrentPlayTime()
    {
        if (mStarted && mStartedAtNanos >= 0)
        {
            long elapsed = (System.nanoTime() - mStartedAtNanos) / 1000000L;
            float scale = ValueAnimator.getDurationScale();
            if (scale > 0) elapsed = (long)(elapsed / scale);

            return Math.max(0, elapsed - mStartDelay);
        }

        return mSeekPlayTime < 0 ? 0 : mSeekPlayTime;
    }

    @Override
    public void start()
    {
        start(false);
    }

    private void start(boolean inReverse)
    {
        if (mStarted) return;

        mStarted = true;
        mTerminated = false;
        mReversing = inReverse;
        mStartedAtNanos = System.nanoTime();
        mSeekPlayTime = -1;
        clearPaused();

        for (int index = 0; index < mNodes.size(); index++)
        {
            Node node = mNodes.get(index);
            node.mStarted = false;
            node.mEnded = false;
            prepare(node);
        }

        notifyStart(inReverse);

        if (mStartDelay > 0 && !inReverse)
        {
            mDelayAnimator = ValueAnimator.ofFloat(0.0f, 1.0f);
            mDelayAnimator.setDuration(mStartDelay);
            mDelayAnimator.setInterpolator(null);
            mDelayAnimator.addListener(new AnimatorListenerAdapter()
            {
                @Override
                public void onAnimationEnd(Animator animation)
                {
                    if (mDelayAnimator != animation) return;

                    mDelayAnimator = null;
                    if (!mTerminated && mStarted) startReadyNodes();
                }
            });
            mDelayAnimator.start();
            return;
        }

        startReadyNodes();
    }

    private void prepare(Node node)
    {
        Animator animation = node.mAnimation;

        if (!node.mInternal)
        {
            if (mDuration >= 0) animation.setDuration(mDuration);
            if (mInterpolator != null) animation.setInterpolator(mInterpolator);
        }

        animation.removeListener(mChildListener);
        animation.addListener(mChildListener);
    }

    private boolean isReady(Node node)
    {
        if (!mReversing)
        {
            for (int index = 0; index < node.mDependencies.size(); index++)
            {
                Dependency dependency = node.mDependencies.get(index);

                if (dependency.mRule == RULE_WITH && !dependency.mNode.mStarted) return false;
                if (dependency.mRule == RULE_AFTER && !dependency.mNode.mEnded) return false;
            }

            return true;
        }

        for (int index = 0; index < mNodes.size(); index++)
        {
            Node other = mNodes.get(index);

            for (int inner = 0; inner < other.mDependencies.size(); inner++)
            {
                Dependency dependency = other.mDependencies.get(inner);
                if (dependency.mNode != node) continue;

                if (dependency.mRule == RULE_WITH && !other.mStarted) return false;
                if (dependency.mRule == RULE_AFTER && !other.mEnded) return false;
            }
        }

        return true;
    }

    private void startReadyNodes()
    {
        boolean progressed = true;

        while (progressed && !mTerminated && mStarted)
        {
            progressed = false;

            for (int index = 0; index < mNodes.size(); index++)
            {
                Node node = mNodes.get(index);
                if (node.mStarted || !isReady(node)) continue;

                node.mStarted = true;
                progressed = true;

                if (mReversing && node.mAnimation.canReverse()) node.mAnimation.reverse();
                else node.mAnimation.start();

                if (mTerminated || !mStarted) return;
            }
        }

        checkFinished();
    }

    private void checkFinished()
    {
        if (mTerminated || !mStarted || mDelayAnimator != null) return;

        for (int index = 0; index < mNodes.size(); index++)
        {
            if (!mNodes.get(index).mEnded) return;
        }

        finish();
    }

    private void childEnded(Animator animation)
    {
        if (mTerminated || !mStarted) return;

        Node node = mNodeMap.get(animation);
        if (node == null || node.mEnded) return;

        node.mStarted = true;
        node.mEnded = true;

        startReadyNodes();
    }

    private HashMap<Node, Long> computeStartTimes()
    {
        HashMap<Node, Long> startTimes = new HashMap<>();
        ArrayList<Node> ordered = sortedNodes(false);

        for (int index = 0; index < ordered.size(); index++)
        {
            Node node = ordered.get(index);
            long start = 0;

            for (int inner = 0; inner < node.mDependencies.size(); inner++)
            {
                Dependency dependency = node.mDependencies.get(inner);
                Long dependencyStart = startTimes.get(dependency.mNode);

                if (dependencyStart == null || dependencyStart == DURATION_INFINITE)
                {
                    start = DURATION_INFINITE;
                    break;
                }

                long candidate = dependencyStart;

                if (dependency.mRule == RULE_AFTER)
                {
                    long total = durationOf(dependency.mNode.mAnimation);

                    if (total == DURATION_INFINITE)
                    {
                        start = DURATION_INFINITE;
                        break;
                    }

                    candidate += total;
                }

                start = Math.max(start, candidate);
            }

            startTimes.put(node, start);
        }

        return startTimes;
    }

    private long durationOf(Animator animation)
    {
        if (mDuration >= 0 && !(animation instanceof AnimatorSet))
        {
            if (animation instanceof ValueAnimator && ((ValueAnimator)animation).getRepeatCount() == ValueAnimator.INFINITE)
            {
                return DURATION_INFINITE;
            }

            int repeats = animation instanceof ValueAnimator ? ((ValueAnimator)animation).getRepeatCount() + 1 : 1;
            return animation.getStartDelay() + mDuration * repeats;
        }

        return animation.getTotalDuration();
    }

    private ArrayList<Node> sortedNodes(boolean reverse)
    {
        ArrayList<Node> sorted = new ArrayList<>();
        ArrayList<Node> remaining = new ArrayList<>(mNodes);

        while (!remaining.isEmpty())
        {
            boolean progressed = false;

            for (int index = 0; index < remaining.size(); index++)
            {
                Node node = remaining.get(index);
                boolean ready = true;

                for (int inner = 0; inner < node.mDependencies.size(); inner++)
                {
                    if (!sorted.contains(node.mDependencies.get(inner).mNode))
                    {
                        ready = false;
                        break;
                    }
                }

                if (!ready) continue;

                sorted.add(node);
                remaining.remove(index);
                index--;
                progressed = true;
            }

            if (!progressed)
            {
                sorted.addAll(remaining);
                break;
            }
        }

        if (reverse) java.util.Collections.reverse(sorted);

        return sorted;
    }

    @Override
    public long getTotalDuration()
    {
        HashMap<Node, Long> startTimes = computeStartTimes();
        long longest = 0;

        for (int index = 0; index < mNodes.size(); index++)
        {
            Node node = mNodes.get(index);
            Long start = startTimes.get(node);
            if (start == null || start == DURATION_INFINITE) return DURATION_INFINITE;

            long total = durationOf(node.mAnimation);
            if (total == DURATION_INFINITE) return DURATION_INFINITE;

            longest = Math.max(longest, start + total);
        }

        return mStartDelay + longest;
    }

    @Override
    public boolean canReverse()
    {
        return getTotalDuration() != DURATION_INFINITE;
    }

    @Override
    public void reverse()
    {
        if (isStarted())
        {
            boolean reversing = !mReversing;
            end();
            start(reversing);
            return;
        }

        start(true);
    }

    boolean isReversing()
    {
        return mReversing;
    }

    @Override
    public AnimatorSet clone()
    {
        AnimatorSet copy = (AnimatorSet)super.clone();
        int count = mNodes.size();

        copy.mStarted = false;
        copy.mTerminated = false;
        copy.mReversing = false;
        copy.mStartedAtNanos = -1;
        copy.mSeekPlayTime = -1;
        copy.mDelayAnimator = null;
        copy.mChildListener = copy.new ChildListener();
        copy.mNodes = new ArrayList<>(count);
        copy.mNodeMap = new HashMap<>();

        HashMap<Node, Node> clonesMap = new HashMap<>(count);

        for (int index = 0; index < count; index++)
        {
            Node node = mNodes.get(index);
            Animator animation = node.mAnimation.clone();
            animation.removeListener(mChildListener);

            Node clone = new Node(animation);
            clone.mInternal = node.mInternal;
            clonesMap.put(node, clone);
            copy.mNodes.add(clone);
            copy.mNodeMap.put(animation, clone);
        }

        for (int index = 0; index < count; index++)
        {
            Node node = mNodes.get(index);
            Node clone = clonesMap.get(node);

            for (int inner = 0; inner < node.mDependencies.size(); inner++)
            {
                Dependency dependency = node.mDependencies.get(inner);
                clone.mDependencies.add(new Dependency(clonesMap.get(dependency.mNode), dependency.mRule));
            }
        }

        return copy;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder("AnimatorSet@" + Integer.toHexString(hashCode()) + "{");

        for (int index = 0; index < mNodes.size(); index++) builder.append("\n    ").append(mNodes.get(index).mAnimation.toString());

        return builder.append("\n}").toString();
    }

    private final class ChildListener implements AnimatorListener
    {
        @Override
        public void onAnimationStart(Animator animation)
        {
        }

        @Override
        public void onAnimationEnd(Animator animation)
        {
            childEnded(animation);
        }

        @Override
        public void onAnimationCancel(Animator animation)
        {
        }

        @Override
        public void onAnimationRepeat(Animator animation)
        {
        }
    }

    private static final class Dependency
    {
        final Node mNode;
        final int mRule;

        Dependency(Node node, int rule)
        {
            mNode = node;
            mRule = rule;
        }
    }

    private static final class Node
    {
        final Animator mAnimation;
        final ArrayList<Dependency> mDependencies = new ArrayList<>();
        boolean mInternal;
        boolean mStarted;
        boolean mEnded;

        Node(Animator animation)
        {
            mAnimation = animation;
        }

        void addDependency(Node node, int rule)
        {
            if (node == this) return;

            for (int index = 0; index < mDependencies.size(); index++)
            {
                Dependency existing = mDependencies.get(index);
                if (existing.mNode == node && existing.mRule == rule) return;
            }

            mDependencies.add(new Dependency(node, rule));
        }
    }

    public class Builder
    {
        private final Node mCurrentNode;

        Builder(Animator anim)
        {
            mCurrentNode = getNodeForAnimation(anim);
        }

        public Builder with(Animator anim)
        {
            Node node = getNodeForAnimation(anim);
            node.addDependency(mCurrentNode, RULE_WITH);

            return this;
        }

        public Builder before(Animator anim)
        {
            Node node = getNodeForAnimation(anim);
            node.addDependency(mCurrentNode, RULE_AFTER);

            return this;
        }

        public Builder after(Animator anim)
        {
            Node node = getNodeForAnimation(anim);
            mCurrentNode.addDependency(node, RULE_AFTER);

            return this;
        }

        public Builder after(long delay)
        {
            ValueAnimator animator = ValueAnimator.ofFloat(0.0f, 1.0f);
            animator.setDuration(delay);

            Node node = getNodeForAnimation(animator);
            node.mInternal = true;
            mCurrentNode.addDependency(node, RULE_AFTER);

            return this;
        }
    }
}
