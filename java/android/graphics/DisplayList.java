package android.graphics;

import java.util.ArrayList;

class DisplayList
{
    private final ArrayList<Command> mCommands = new ArrayList<>();

    void clear()
    {
        mCommands.clear();
    }

    boolean isEmpty()
    {
        return mCommands.isEmpty();
    }

    int size()
    {
        return mCommands.size();
    }

    void record(Command command)
    {
        mCommands.add(command);
    }

    void replay(Canvas target)
    {
        for (int index = 0; index < mCommands.size(); index++)
        {
            mCommands.get(index).apply(target);
        }
    }

    interface Command
    {
        void apply(Canvas target);
    }
}
