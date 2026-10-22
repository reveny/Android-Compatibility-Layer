package android.view.textclassifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class EntityConfidence
{
    private final HashMap<String, Float> mScores = new HashMap<>();
    private final ArrayList<String> mSortedEntities = new ArrayList<>();

    EntityConfidence()
    {
    }

    EntityConfidence(Map<String, Float> scores)
    {
        if (scores != null) mScores.putAll(scores);

        mSortedEntities.addAll(mScores.keySet());
        mSortedEntities.sort((first, second) -> Float.compare(mScores.get(second), mScores.get(first)));
    }

    List<String> getEntities()
    {
        return new ArrayList<>(mSortedEntities);
    }

    int getEntityCount()
    {
        return mSortedEntities.size();
    }

    String getEntity(int index)
    {
        return mSortedEntities.get(index);
    }

    float getConfidenceScore(String entity)
    {
        Float score = mScores.get(entity);
        return score == null ? 0 : score.floatValue();
    }

    Map<String, Float> toMap()
    {
        return new HashMap<>(mScores);
    }

    @Override
    public String toString()
    {
        return mScores.toString();
    }
}
