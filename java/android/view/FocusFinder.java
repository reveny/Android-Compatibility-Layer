package android.view;

import android.graphics.Rect;
import java.util.ArrayList;

public class FocusFinder
{
    private static final ThreadLocal<FocusFinder> sFocusFinder = new ThreadLocal<FocusFinder>()
    {
        @Override
        protected FocusFinder initialValue()
        {
            return new FocusFinder();
        }
    };

    private final Rect mFocusedRect = new Rect();
    private final Rect mOtherRect = new Rect();
    private final Rect mBestCandidateRect = new Rect();

    private FocusFinder()
    {
    }

    public static FocusFinder getInstance()
    {
        return sFocusFinder.get();
    }

    public final View findNextFocus(ViewGroup root, View focused, int direction)
    {
        return findNextFocus(root, focused, null, direction);
    }

    public View findNextFocusFromRect(ViewGroup root, Rect focusedRect, int direction)
    {
        mFocusedRect.set(focusedRect);
        return findNextFocus(root, null, mFocusedRect, direction);
    }

    public View findNextKeyboardNavigationCluster(View root, View currentCluster, int direction)
    {
        ArrayList<View> clusters = new ArrayList<>();
        root.addKeyboardNavigationClusters(clusters, direction);
        if (clusters.isEmpty()) return null;

        int index = currentCluster == null ? -1 : clusters.indexOf(currentCluster);
        if (direction == View.FOCUS_BACKWARD)
        {
            if (index <= 0) return clusters.get(clusters.size() - 1);
            return clusters.get(index - 1);
        }

        if (index < 0 || index + 1 >= clusters.size()) return clusters.get(0);
        return clusters.get(index + 1);
    }

    public View findNearestTouchable(ViewGroup root, int x, int y, int direction, int[] deltas)
    {
        ArrayList<View> touchables = root.getTouchables();
        int minDistance = Integer.MAX_VALUE;
        View closest = null;
        int edgeSlop = ViewConfiguration.get(root.getContext()).getScaledEdgeSlop();
        Rect closestBounds = new Rect();
        Rect touchableBounds = mOtherRect;

        for (int index = 0; index < touchables.size(); index++)
        {
            View touchable = touchables.get(index);

            touchable.getDrawingRect(touchableBounds);
            root.offsetRectBetweenParentAndChild(touchable, touchableBounds, true, true);

            if (!isTouchCandidate(x, y, touchableBounds, direction)) continue;

            int distance = Integer.MAX_VALUE;

            switch (direction)
            {
                case View.FOCUS_LEFT:
                    distance = x - touchableBounds.right + 1;
                    break;
                case View.FOCUS_RIGHT:
                    distance = touchableBounds.left;
                    break;
                case View.FOCUS_UP:
                    distance = y - touchableBounds.bottom + 1;
                    break;
                case View.FOCUS_DOWN:
                    distance = touchableBounds.top;
                    break;
                default:
                    break;
            }

            if (distance < edgeSlop && (closest == null || closestBounds.contains(touchableBounds)
                                        || (!touchableBounds.contains(closestBounds) && distance < minDistance)))
            {
                minDistance = distance;
                closest = touchable;
                closestBounds.set(touchableBounds);

                switch (direction)
                {
                    case View.FOCUS_LEFT:
                        deltas[0] = -distance;
                        break;
                    case View.FOCUS_RIGHT:
                        deltas[0] = distance;
                        break;
                    case View.FOCUS_UP:
                        deltas[1] = -distance;
                        break;
                    case View.FOCUS_DOWN:
                        deltas[1] = distance;
                        break;
                    default:
                        break;
                }
            }
        }

        return closest;
    }

    private boolean isTouchCandidate(int x, int y, Rect destination, int direction)
    {
        switch (direction)
        {
            case View.FOCUS_LEFT:
                return destination.left <= x && destination.top <= y && y <= destination.bottom;
            case View.FOCUS_RIGHT:
                return destination.left >= x && destination.top <= y && y <= destination.bottom;
            case View.FOCUS_UP:
                return destination.top <= y && destination.left <= x && x <= destination.right;
            case View.FOCUS_DOWN:
                return destination.top >= y && destination.left <= x && x <= destination.right;
            default:
                throw new IllegalArgumentException("direction must be one of {FOCUS_UP, FOCUS_DOWN, FOCUS_LEFT, FOCUS_RIGHT}.");
        }
    }

    private View findNextFocus(ViewGroup root, View focused, Rect focusedRect, int direction)
    {
        View next = null;

        if (focused != null) next = findNextUserSpecifiedFocus(root, focused, direction);
        if (next != null) return next;

        ArrayList<View> focusables = new ArrayList<>();
        root.addFocusables(focusables, direction);
        if (focusables.isEmpty()) return null;

        return findNextFocus(root, focused, focusedRect, direction, focusables);
    }

    private View findNextUserSpecifiedFocus(ViewGroup root, View focused, int direction)
    {
        int id = focused.getNextFocusId(direction);
        if (id == View.NO_ID) return null;

        View userSetNextFocus = root.findViewById(id);
        if (userSetNextFocus == null || userSetNextFocus == focused) return null;
        if (userSetNextFocus.isFocusable() && userSetNextFocus.getVisibility() == View.VISIBLE) return userSetNextFocus;

        return null;
    }

    private View findNextFocus(ViewGroup root, View focused, Rect focusedRect, int direction, ArrayList<View> focusables)
    {
        if (focused != null)
        {
            if (focusedRect == null) focusedRect = mFocusedRect;
            focused.getFocusedRect(focusedRect);
            root.offsetDescendantRectToMyCoords(focused, focusedRect);
        }
        else if (focusedRect == null)
        {
            focusedRect = mFocusedRect;

            switch (direction)
            {
                case View.FOCUS_RIGHT:
                case View.FOCUS_DOWN:
                case View.FOCUS_FORWARD:
                    setFocusTopLeft(root, focusedRect);
                    break;
                case View.FOCUS_LEFT:
                case View.FOCUS_UP:
                case View.FOCUS_BACKWARD:
                    setFocusBottomRight(root, focusedRect);
                    break;
                default:
                    break;
            }
        }

        switch (direction)
        {
            case View.FOCUS_FORWARD:
            case View.FOCUS_BACKWARD:
                return findNextFocusInRelativeDirection(focusables, focused, direction);
            case View.FOCUS_UP:
            case View.FOCUS_DOWN:
            case View.FOCUS_LEFT:
            case View.FOCUS_RIGHT:
                return findNextFocusInAbsoluteDirection(focusables, root, focused, focusedRect, direction);
            default:
                throw new IllegalArgumentException("Unknown direction: " + direction);
        }
    }

    private View findNextFocusInRelativeDirection(ArrayList<View> focusables, View focused, int direction)
    {
        int count = focusables.size();
        if (count == 0) return null;

        if (direction == View.FOCUS_FORWARD)
        {
            if (focused != null)
            {
                int position = focusables.lastIndexOf(focused);
                if (position >= 0 && position + 1 < count) return focusables.get(position + 1);
            }
            return focusables.get(0);
        }

        if (focused != null)
        {
            int position = focusables.indexOf(focused);
            if (position > 0) return focusables.get(position - 1);
        }
        return focusables.get(count - 1);
    }

    private void setFocusBottomRight(ViewGroup root, Rect focusedRect)
    {
        int rootBottom = root.getScrollY() + root.getHeight();
        int rootRight = root.getScrollX() + root.getWidth();
        focusedRect.set(rootRight, rootBottom, rootRight, rootBottom);
    }

    private void setFocusTopLeft(ViewGroup root, Rect focusedRect)
    {
        int rootTop = root.getScrollY();
        int rootLeft = root.getScrollX();
        focusedRect.set(rootLeft, rootTop, rootLeft, rootTop);
    }

    View findNextFocusInAbsoluteDirection(ArrayList<View> focusables, ViewGroup root, View focused, Rect focusedRect, int direction)
    {
        mBestCandidateRect.set(focusedRect);

        switch (direction)
        {
            case View.FOCUS_LEFT:
                mBestCandidateRect.offset(focusedRect.width() + 1, 0);
                break;
            case View.FOCUS_RIGHT:
                mBestCandidateRect.offset(-(focusedRect.width() + 1), 0);
                break;
            case View.FOCUS_UP:
                mBestCandidateRect.offset(0, focusedRect.height() + 1);
                break;
            case View.FOCUS_DOWN:
                mBestCandidateRect.offset(0, -(focusedRect.height() + 1));
                break;
            default:
                break;
        }

        View closest = null;

        for (int index = 0; index < focusables.size(); index++)
        {
            View focusable = focusables.get(index);
            if (focusable == focused || focusable == root) continue;

            focusable.getFocusedRect(mOtherRect);
            root.offsetDescendantRectToMyCoords(focusable, mOtherRect);

            if (isBetterCandidate(direction, focusedRect, mOtherRect, mBestCandidateRect))
            {
                mBestCandidateRect.set(mOtherRect);
                closest = focusable;
            }
        }

        return closest;
    }

    boolean isBetterCandidate(int direction, Rect source, Rect candidate, Rect best)
    {
        if (!isCandidate(source, candidate, direction)) return false;
        if (!isCandidate(source, best, direction)) return true;
        if (beamBeats(direction, source, candidate, best)) return true;
        if (beamBeats(direction, source, best, candidate)) return false;

        return getWeightedDistanceFor(majorAxisDistance(direction, source, candidate), minorAxisDistance(direction, source, candidate))
               < getWeightedDistanceFor(majorAxisDistance(direction, source, best), minorAxisDistance(direction, source, best));
    }

    boolean beamBeats(int direction, Rect source, Rect first, Rect second)
    {
        boolean firstInBeam = beamsOverlap(direction, source, first);
        boolean secondInBeam = beamsOverlap(direction, source, second);

        if (secondInBeam || !firstInBeam) return false;
        if (!isToDirectionOf(direction, source, second)) return true;
        if (direction == View.FOCUS_LEFT || direction == View.FOCUS_RIGHT) return true;

        return majorAxisDistance(direction, source, first) < majorAxisDistanceToFarEdge(direction, source, second);
    }

    int getWeightedDistanceFor(long majorAxisDistance, long minorAxisDistance)
    {
        long distance = 13 * majorAxisDistance * majorAxisDistance + minorAxisDistance * minorAxisDistance;
        return distance > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int)distance;
    }

    boolean isCandidate(Rect source, Rect destination, int direction)
    {
        switch (direction)
        {
            case View.FOCUS_LEFT:
                return (source.right > destination.right || source.left >= destination.right) && source.left > destination.left;
            case View.FOCUS_RIGHT:
                return (source.left < destination.left || source.right <= destination.left) && source.right < destination.right;
            case View.FOCUS_UP:
                return (source.bottom > destination.bottom || source.top >= destination.bottom) && source.top > destination.top;
            case View.FOCUS_DOWN:
                return (source.top < destination.top || source.bottom <= destination.top) && source.bottom < destination.bottom;
            default:
                throw new IllegalArgumentException("direction must be one of {FOCUS_UP, FOCUS_DOWN, FOCUS_LEFT, FOCUS_RIGHT}.");
        }
    }

    boolean beamsOverlap(int direction, Rect first, Rect second)
    {
        switch (direction)
        {
            case View.FOCUS_LEFT:
            case View.FOCUS_RIGHT:
                return second.bottom > first.top && second.top < first.bottom;
            case View.FOCUS_UP:
            case View.FOCUS_DOWN:
                return second.right > first.left && second.left < first.right;
            default:
                throw new IllegalArgumentException("direction must be one of {FOCUS_UP, FOCUS_DOWN, FOCUS_LEFT, FOCUS_RIGHT}.");
        }
    }

    boolean isToDirectionOf(int direction, Rect source, Rect destination)
    {
        switch (direction)
        {
            case View.FOCUS_LEFT:
                return source.left >= destination.right;
            case View.FOCUS_RIGHT:
                return source.right <= destination.left;
            case View.FOCUS_UP:
                return source.top >= destination.bottom;
            case View.FOCUS_DOWN:
                return source.bottom <= destination.top;
            default:
                throw new IllegalArgumentException("direction must be one of {FOCUS_UP, FOCUS_DOWN, FOCUS_LEFT, FOCUS_RIGHT}.");
        }
    }

    static int majorAxisDistance(int direction, Rect source, Rect destination)
    {
        return Math.max(0, majorAxisDistanceRaw(direction, source, destination));
    }

    static int majorAxisDistanceRaw(int direction, Rect source, Rect destination)
    {
        switch (direction)
        {
            case View.FOCUS_LEFT:
                return source.left - destination.right;
            case View.FOCUS_RIGHT:
                return destination.left - source.right;
            case View.FOCUS_UP:
                return source.top - destination.bottom;
            case View.FOCUS_DOWN:
                return destination.top - source.bottom;
            default:
                throw new IllegalArgumentException("direction must be one of {FOCUS_UP, FOCUS_DOWN, FOCUS_LEFT, FOCUS_RIGHT}.");
        }
    }

    static int majorAxisDistanceToFarEdge(int direction, Rect source, Rect destination)
    {
        return Math.max(1, majorAxisDistanceToFarEdgeRaw(direction, source, destination));
    }

    static int majorAxisDistanceToFarEdgeRaw(int direction, Rect source, Rect destination)
    {
        switch (direction)
        {
            case View.FOCUS_LEFT:
                return source.left - destination.left;
            case View.FOCUS_RIGHT:
                return destination.right - source.right;
            case View.FOCUS_UP:
                return source.top - destination.top;
            case View.FOCUS_DOWN:
                return destination.bottom - source.bottom;
            default:
                throw new IllegalArgumentException("direction must be one of {FOCUS_UP, FOCUS_DOWN, FOCUS_LEFT, FOCUS_RIGHT}.");
        }
    }

    static int minorAxisDistance(int direction, Rect source, Rect destination)
    {
        switch (direction)
        {
            case View.FOCUS_LEFT:
            case View.FOCUS_RIGHT:
                return Math.abs((source.top + source.height() / 2) - (destination.top + destination.height() / 2));
            case View.FOCUS_UP:
            case View.FOCUS_DOWN:
                return Math.abs((source.left + source.width() / 2) - (destination.left + destination.width() / 2));
            default:
                throw new IllegalArgumentException("direction must be one of {FOCUS_UP, FOCUS_DOWN, FOCUS_LEFT, FOCUS_RIGHT}.");
        }
    }
}
