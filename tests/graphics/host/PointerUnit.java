package runtime;

import android.view.MotionEvent;

import java.util.ArrayList;

public class PointerUnit
{
    private static int checks;
    private static int failures;

    private static void Expect(boolean condition, String description)
    {
        checks++;

        if (condition)
        {
            System.out.println("  ok    " + description);
            return;
        }

        System.out.println("  FAIL  " + description);
        failures++;
    }

    private static final class Recorder implements PointerTracker.Sink
    {
        private final ArrayList<String> mSeen = new ArrayList<>();

        @Override
        public void onMotion(MotionEvent event)
        {
            StringBuilder line = new StringBuilder();
            line.append(name(event.getActionMasked()));

            if (event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN
                || event.getActionMasked() == MotionEvent.ACTION_POINTER_UP)
            {
                line.append("#").append(event.getActionIndex());
            }

            line.append("(");
            for (int index = 0; index < event.getPointerCount(); index++)
            {
                if (index > 0) line.append(" ");
                line.append(event.getPointerId(index)).append(":").append((int) event.getX(index)).append(",")
                    .append((int) event.getY(index));
            }
            line.append(")");

            mSeen.add(line.toString());
        }

        String log()
        {
            return String.join(" ", mSeen);
        }
    }

    private static String name(int action)
    {
        switch (action)
        {
            case MotionEvent.ACTION_DOWN: return "DOWN";
            case MotionEvent.ACTION_UP: return "UP";
            case MotionEvent.ACTION_MOVE: return "MOVE";
            case MotionEvent.ACTION_CANCEL: return "CANCEL";
            case MotionEvent.ACTION_POINTER_DOWN: return "PDOWN";
            case MotionEvent.ACTION_POINTER_UP: return "PUP";
            case MotionEvent.ACTION_HOVER_MOVE: return "HOVER";
            case MotionEvent.ACTION_HOVER_ENTER: return "ENTER";
            case MotionEvent.ACTION_HOVER_EXIT: return "EXIT";
            case MotionEvent.ACTION_SCROLL: return "SCROLL";
            default: return "?" + action;
        }
    }

    private static void CheckOneFinger()
    {
        PointerTracker tracker = new PointerTracker();
        Recorder recorder = new Recorder();

        tracker.track(MotionEvent.ACTION_DOWN, 0, 10, 20, 1, 0, recorder);
        tracker.track(MotionEvent.ACTION_MOVE, 0, 11, 21, 2, 0, recorder);
        tracker.track(MotionEvent.ACTION_UP, 0, 12, 22, 3, 0, recorder);

        Expect("DOWN(0:10,20) MOVE(0:11,21) UP(0:12,22)".equals(recorder.log()),
               "one finger goes down, moves and comes up: " + recorder.log());
        Expect(tracker.getPointerCount() == 0, "the finger is forgotten once it lifts");
    }

    private static void CheckSecondFinger()
    {
        PointerTracker tracker = new PointerTracker();
        Recorder recorder = new Recorder();

        tracker.track(MotionEvent.ACTION_DOWN, 7, 10, 10, 1, 0, recorder);
        tracker.track(MotionEvent.ACTION_POINTER_DOWN, 9, 50, 50, 2, 0, recorder);

        Expect(recorder.log().contains("PDOWN#1(7:10,10 9:50,50)"),
               "a second finger arrives as a pointer down at index one: " + recorder.log());
        Expect(tracker.getPointerCount() == 2, "both fingers are held");
    }

    private static void CheckFirstFingerLiftsFirst()
    {
        PointerTracker tracker = new PointerTracker();
        Recorder recorder = new Recorder();

        tracker.track(MotionEvent.ACTION_DOWN, 7, 10, 10, 1, 0, recorder);
        tracker.track(MotionEvent.ACTION_POINTER_DOWN, 9, 50, 50, 2, 0, recorder);
        tracker.track(MotionEvent.ACTION_POINTER_UP, 7, 10, 10, 3, 0, recorder);
        tracker.track(MotionEvent.ACTION_MOVE, 9, 60, 60, 4, 0, recorder);
        tracker.track(MotionEvent.ACTION_UP, 9, 60, 60, 5, 0, recorder);

        String log = recorder.log();

        Expect(log.contains("PUP#0(7:10,10 9:50,50)"), "the first finger lifts as index zero: " + log);
        Expect(log.contains("MOVE(9:60,60)"), "the remaining finger keeps its own id and slides down to index zero: " + log);
        Expect(log.endsWith("UP(9:60,60)"), "the last finger up is a plain up, not a pointer up: " + log);
        Expect(tracker.getPointerCount() == 0, "nothing is held at the end");
    }

    private static void CheckCancel()
    {
        PointerTracker tracker = new PointerTracker();
        Recorder recorder = new Recorder();

        tracker.track(MotionEvent.ACTION_DOWN, 1, 5, 5, 1, 0, recorder);
        tracker.track(MotionEvent.ACTION_POINTER_DOWN, 2, 6, 6, 2, 0, recorder);
        tracker.track(MotionEvent.ACTION_CANCEL, 1, 5, 5, 3, 0, recorder);

        Expect(recorder.log().contains("CANCEL(1:5,5 2:6,6)"), "a cancel reports every pointer still down: " + recorder.log());
        Expect(tracker.getPointerCount() == 0, "a cancel drops every pointer");
    }

    private static void CheckHover()
    {
        PointerTracker tracker = new PointerTracker();
        Recorder recorder = new Recorder();

        tracker.track(MotionEvent.ACTION_HOVER_ENTER, 0, 3, 4, 1, 0, recorder);
        tracker.track(MotionEvent.ACTION_HOVER_MOVE, 0, 5, 6, 2, 0, recorder);
        tracker.track(MotionEvent.ACTION_HOVER_EXIT, 0, 7, 8, 3, 0, recorder);

        Expect("ENTER(0:3,4) HOVER(0:5,6) EXIT(0:7,8)".equals(recorder.log()),
               "hover passes through without becoming a press: " + recorder.log());
        Expect(tracker.getPointerCount() == 0, "hovering never counts as a finger down");
    }

    private static void CheckStrayEvents()
    {
        PointerTracker tracker = new PointerTracker();
        Recorder recorder = new Recorder();

        tracker.track(MotionEvent.ACTION_MOVE, 4, 1, 1, 1, 0, recorder);
        tracker.track(MotionEvent.ACTION_UP, 4, 1, 1, 2, 0, recorder);

        Expect(recorder.log().isEmpty(), "a move or up for a finger that never went down is ignored: " + recorder.log());

        tracker.track(MotionEvent.ACTION_DOWN, 4, 1, 1, 3, 0, recorder);
        tracker.track(MotionEvent.ACTION_DOWN, 4, 2, 2, 4, 0, recorder);

        Expect(tracker.getPointerCount() == 1, "the same finger going down twice is still one finger");
    }

    public static void main(String[] args)
    {
        System.out.println("pointer tracking:");

        CheckOneFinger();
        CheckSecondFinger();
        CheckFirstFingerLiftsFirst();
        CheckCancel();
        CheckHover();
        CheckStrayEvents();

        if (failures != 0)
        {
            System.out.println("pointer FAILED: " + failures + " of " + checks + " checks failed");
            System.exit(1);
        }

        System.out.println("pointer PASSED: " + checks + " checks");
    }
}
