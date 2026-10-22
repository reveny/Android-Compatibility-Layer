package android.content;

public interface DialogInterface
{
    int BUTTON_POSITIVE = -1;
    int BUTTON_NEGATIVE = -2;
    int BUTTON_NEUTRAL = -3;
    @Deprecated
    int BUTTON1 = BUTTON_POSITIVE;
    @Deprecated
    int BUTTON2 = BUTTON_NEGATIVE;
    @Deprecated
    int BUTTON3 = BUTTON_NEUTRAL;

    void cancel();

    void dismiss();

    interface OnShowListener
    {
        void onShow(DialogInterface dialog);
    }

    interface OnMultiChoiceClickListener
    {
        void onClick(DialogInterface dialog, int which, boolean isChecked);
    }

    interface OnClickListener
    {
        void onClick(DialogInterface dialog, int which);
    }

    interface OnCancelListener
    {
        void onCancel(DialogInterface dialog);
    }

    interface OnDismissListener
    {
        void onDismiss(DialogInterface dialog);
    }

    interface OnKeyListener
    {
        boolean onKey(DialogInterface dialog, int keyCode, android.view.KeyEvent event);
    }
}
