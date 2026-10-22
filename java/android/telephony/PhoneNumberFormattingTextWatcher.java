package android.telephony;

import android.text.Editable;
import android.text.Selection;
import android.text.TextWatcher;

import java.util.Locale;

public class PhoneNumberFormattingTextWatcher implements TextWatcher
{
    private final String mCountryCode;
    private boolean mSelfChange;
    private boolean mStopFormatting;

    public PhoneNumberFormattingTextWatcher()
    {
        this(Locale.getDefault().getCountry());
    }

    public PhoneNumberFormattingTextWatcher(String countryCode)
    {
        if (countryCode == null) throw new IllegalArgumentException();
        mCountryCode = countryCode;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after)
    {
        if (mSelfChange || mStopFormatting) return;
        if (count > 0 && hasSeparator(s, start, count)) mStopFormatting = true;
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count)
    {
        if (mSelfChange || mStopFormatting) return;
        if (count > 0 && hasSeparator(s, start, count)) mStopFormatting = true;
    }

    @Override
    public synchronized void afterTextChanged(Editable s)
    {
        if (mStopFormatting)
        {
            mStopFormatting = s.length() != 0;
            return;
        }
        if (mSelfChange) return;
        String formatted = reformat(s);
        if (formatted == null || formatted.contentEquals(s)) return;
        mSelfChange = true;
        s.replace(0, s.length(), formatted, 0, formatted.length());
        if (formatted.equals(s.toString())) Selection.setSelection(s, s.length());
        mSelfChange = false;
    }

    private String reformat(CharSequence s)
    {
        StringBuilder dialable = new StringBuilder();
        for (int index = 0; index < s.length(); index++)
        {
            char c = s.charAt(index);
            if (PhoneNumberUtils.isNonSeparator(c))
            {
                dialable.append(c);
            }
            else if (!PhoneNumberUtils.isSeparator(c))
            {
                return null;
            }
        }
        if (!PhoneNumberUtils.isNanpCountry(mCountryCode)) return null;
        String digits = dialable.toString();
        String prefix = "";
        if (digits.startsWith("+1"))
        {
            prefix = "+1 ";
            digits = digits.substring(2);
        }
        else if (digits.startsWith("1"))
        {
            prefix = "1 ";
            digits = digits.substring(1);
        }
        else if (digits.startsWith("+"))
        {
            return null;
        }
        for (int index = 0; index < digits.length(); index++)
        {
            if (!PhoneNumberUtils.isISODigit(digits.charAt(index))) return null;
        }
        int length = digits.length();
        if (length == 0) return prefix.trim();
        if (length > 10) return dialable.toString();
        if (length <= 3) return prefix + digits;
        if (length <= 7 && prefix.isEmpty()) return digits.substring(0, 3) + "-" + digits.substring(3);
        if (length <= 6) return prefix + digits.substring(0, 3) + "-" + digits.substring(3);
        return prefix + digits.substring(0, 3) + "-" + digits.substring(3, 6) + "-" + digits.substring(6);
    }

    private static boolean hasSeparator(CharSequence s, int start, int count)
    {
        for (int index = start; index < start + count && index < s.length(); index++)
        {
            if (!PhoneNumberUtils.isNonSeparator(s.charAt(index))) return true;
        }
        return false;
    }
}
