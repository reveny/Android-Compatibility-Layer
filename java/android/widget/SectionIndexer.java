package android.widget;

public interface SectionIndexer
{
    Object[] getSections();

    int getPositionForSection(int sectionIndex);

    int getSectionForPosition(int position);
}
