package android.view;

import android.graphics.Matrix;
import android.os.Bundle;
import android.os.LocaleList;
import android.util.Pair;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillValue;

import java.util.List;

public abstract class ViewStructure
{
    public static final String EXTRA_VIRTUAL_STRUCTURE_TYPE = "android.view.extra.VIRTUAL_STRUCTURE_TYPE";
    public static final String EXTRA_VIRTUAL_STRUCTURE_VERSION_NUMBER = "android.view.extra.VIRTUAL_STRUCTURE_VERSION_NUMBER";

    public ViewStructure()
    {
    }

    public abstract void setId(int id, String packageName, String typeName, String entryName);

    public abstract void setDimens(int left, int top, int scrollX, int scrollY, int width, int height);

    public abstract void setTransformation(Matrix matrix);

    public abstract void setElevation(float elevation);

    public abstract void setAlpha(float alpha);

    public abstract void setVisibility(int visibility);

    public abstract void setEnabled(boolean state);

    public abstract void setClickable(boolean state);

    public abstract void setLongClickable(boolean state);

    public abstract void setContextClickable(boolean state);

    public abstract void setFocusable(boolean state);

    public abstract void setFocused(boolean state);

    public abstract void setAccessibilityFocused(boolean state);

    public abstract void setCheckable(boolean state);

    public abstract void setChecked(boolean state);

    public abstract void setSelected(boolean state);

    public abstract void setActivated(boolean state);

    public abstract void setOpaque(boolean opaque);

    public abstract void setClassName(String className);

    public abstract void setContentDescription(CharSequence contentDescription);

    public abstract void setText(CharSequence text);

    public abstract void setText(CharSequence text, int selectionStart, int selectionEnd);

    public abstract void setTextStyle(float size, int foregroundColor, int backgroundColor, int style);

    public abstract void setTextLines(int[] charOffsets, int[] baselines);

    public void setTextIdEntry(String entryName)
    {
    }

    public abstract void setHint(CharSequence hint);

    public void setHintIdEntry(String entryName)
    {
    }

    public abstract CharSequence getText();

    public abstract int getTextSelectionStart();

    public abstract int getTextSelectionEnd();

    public abstract CharSequence getHint();

    public abstract Bundle getExtras();

    public abstract boolean hasExtras();

    public abstract void setChildCount(int num);

    public abstract int addChildCount(int num);

    public abstract int getChildCount();

    public abstract ViewStructure newChild(int index);

    public abstract ViewStructure asyncNewChild(int index);

    public abstract AutofillId getAutofillId();

    public abstract void setAutofillId(AutofillId id);

    public abstract void setAutofillId(AutofillId parentId, int virtualId);

    public abstract void setAutofillType(int type);

    public abstract void setAutofillHints(String[] hint);

    public abstract void setAutofillValue(AutofillValue value);

    public abstract void setAutofillOptions(CharSequence[] options);

    public void setImportantForAutofill(int mode)
    {
    }

    public void setReceiveContentMimeTypes(String[] mimeTypes)
    {
    }

    public abstract void setInputType(int inputType);

    public abstract void setDataIsSensitive(boolean sensitive);

    public void setMinTextEms(int minEms)
    {
    }

    public void setMaxTextEms(int maxEms)
    {
    }

    public void setMaxTextLength(int maxLength)
    {
    }

    public abstract void asyncCommit();

    public abstract void setWebDomain(String domain);

    public abstract void setLocaleList(LocaleList localeList);

    public abstract HtmlInfo.Builder newHtmlInfoBuilder(String tagName);

    public abstract void setHtmlInfo(HtmlInfo htmlInfo);

    public abstract static class HtmlInfo
    {
        public HtmlInfo()
        {
        }

        public abstract String getTag();

        public abstract List<Pair<String, String>> getAttributes();

        public abstract static class Builder
        {
            public Builder()
            {
            }

            public abstract Builder addAttribute(String name, String value);

            public abstract HtmlInfo build();
        }
    }
}
