package android.util;

public class StateSet
{
    public static final int[] WILD_CARD = new int[0];
    public static final int[] NOTHING = new int[] {0};

    private StateSet()
    {
    }

    public static boolean isWildCard(int[] stateSetOrSpec)
    {
        return stateSetOrSpec.length == 0 || stateSetOrSpec[0] == 0;
    }

    public static boolean stateSetMatches(int[] stateSpec, int[] stateSet)
    {
        if (stateSet == null) return stateSpec == null || isWildCard(stateSpec);

        for (int specState : stateSpec)
        {
            if (specState == 0) return true;

            boolean mustMatch = specState > 0;
            int wanted = mustMatch ? specState : -specState;

            boolean present = false;
            for (int state : stateSet)
            {
                if (state == 0) break;
                if (state == wanted)
                {
                    present = true;
                    break;
                }
            }

            if (present != mustMatch) return false;
        }

        return true;
    }

    public static boolean stateSetMatches(int[] stateSpec, int state)
    {
        for (int specState : stateSpec)
        {
            if (specState == 0) return true;

            if (specState > 0)
            {
                if (specState != state) return false;
                continue;
            }

            if (-specState == state) return false;
        }

        return true;
    }

    public static int[] trimStateSet(int[] states, int newSize)
    {
        if (states.length == newSize) return states;

        int[] trimmed = new int[newSize];
        System.arraycopy(states, 0, trimmed, 0, newSize);
        return trimmed;
    }

    public static String dump(int[] states)
    {
        StringBuilder text = new StringBuilder();
        for (int state : states)
        {
            if (text.length() != 0) text.append(' ');
            text.append(state);
        }
        return text.toString();
    }
}
