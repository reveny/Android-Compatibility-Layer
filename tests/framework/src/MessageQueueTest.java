import android.os.Handler;
import android.os.Looper;
import android.os.Message;

public class MessageQueueTest
{
    private static int failures = 0;

    static void check(String name, boolean ok)
    {
        System.out.println((ok ? "PASS " : "FAIL ") + name);
        if (!ok) failures++;
    }

    public static void main(String[] args) throws Exception
    {
        Looper.prepare();
        Handler handler = new Handler();

        Message queued = Message.obtain(handler, 1);
        handler.sendMessage(queued);

        boolean threwOnDoubleEnqueue = false;
        try
        {
            handler.sendMessage(queued);
        }
        catch (IllegalStateException expected)
        {
            threwOnDoubleEnqueue = true;
        }
        check("double enqueue throws instead of self-linking the queue", threwOnDoubleEnqueue);

        boolean threwOnRecycleInUse = false;
        try
        {
            queued.recycle();
        }
        catch (IllegalStateException expected)
        {
            threwOnRecycleInUse = true;
        }
        check("recycle() of a queued message throws", threwOnRecycleInUse);

        Message free = Message.obtain();
        free.recycle();

        boolean threwOnDoubleRecycle = false;
        try
        {
            free.recycle();
        }
        catch (IllegalStateException expected)
        {
            threwOnDoubleRecycle = true;
        }
        check("double recycle throws instead of cycling the pool", threwOnDoubleRecycle);

        check("pool hands out distinct objects", Message.obtain() != Message.obtain());

        final boolean[] dueRan = {false};
        Thread due = new Thread(() ->
        {
            Looper.prepare();
            new Handler().post(() -> dueRan[0] = true);
            Looper.myLooper().quitSafely();
            Looper.loop();
        });
        due.start();
        due.join(5000);
        check("quitSafely delivers an already-due message", dueRan[0]);
        check("quitSafely terminates the loop", !due.isAlive());

        final boolean[] futureRan = {false};
        Thread future = new Thread(() ->
        {
            Looper.prepare();
            new Handler().postDelayed(() -> futureRan[0] = true, 60000);
            Looper.myLooper().quitSafely();
            Looper.loop();
        });
        future.start();
        future.join(5000);
        check("quitSafely drops a not-yet-due message and returns", !future.isAlive() && !futureRan[0]);

        final String[] seen = {null};
        Thread dispatch = new Thread(() ->
        {
            Looper.prepare();
            Handler inner = new Handler()
            {
                @Override
                public void handleMessage(Message message)
                {
                    seen[0] = message.getTarget() == null ? "null" : "present";
                    Looper.myLooper().quit();
                }
            };
            inner.sendEmptyMessage(7);
            Looper.loop();
        });
        dispatch.start();
        dispatch.join(5000);
        check("msg.getTarget() is non-null inside handleMessage", "present".equals(seen[0]));

        final int[] order = {0};
        final StringBuilder sequence = new StringBuilder();
        Thread ordering = new Thread(() ->
        {
            Looper.prepare();
            Handler inner = new Handler();
            inner.postDelayed(() -> sequence.append("b"), 40);
            inner.post(() -> sequence.append("a"));
            inner.postDelayed(() ->
            {
                sequence.append("c");
                Looper.myLooper().quit();
            }, 80);
            Looper.loop();
            order[0] = 1;
        });
        ordering.start();
        ordering.join(5000);
        check("messages dispatch in time order", "abc".contentEquals(sequence) && order[0] == 1);

        System.out.println(failures == 0 ? "ALL PASS" : failures + " FAILURES");
        System.exit(failures == 0 ? 0 : 1);
    }
}
