package android.database.sqlite;

import android.database.Cursor;
import android.os.CancellationSignal;

import java.util.Map;

public class SQLiteQueryBuilder
{
    private String mTables = "";
    private StringBuilder mWhereClause;
    private Map<String, String> mProjectionMap;
    private boolean mDistinct;
    private boolean mStrict;
    private SQLiteDatabase.CursorFactory mFactory;

    public void setTables(String tables)
    {
        mTables = tables == null ? "" : tables;
    }

    public String getTables()
    {
        return mTables;
    }

    public void setDistinct(boolean distinct)
    {
        mDistinct = distinct;
    }

    public boolean isDistinct()
    {
        return mDistinct;
    }

    public void setProjectionMap(Map<String, String> projectionMap)
    {
        mProjectionMap = projectionMap;
    }

    public Map<String, String> getProjectionMap()
    {
        return mProjectionMap;
    }

    public void setCursorFactory(SQLiteDatabase.CursorFactory factory)
    {
        mFactory = factory;
    }

    public SQLiteDatabase.CursorFactory getCursorFactory()
    {
        return mFactory;
    }

    public void setStrict(boolean strict)
    {
        mStrict = strict;
    }

    public boolean isStrict()
    {
        return mStrict;
    }

    public void appendWhere(CharSequence clause)
    {
        if (clause == null) return;

        if (mWhereClause == null) mWhereClause = new StringBuilder(clause.length() + 16);

        mWhereClause.append(clause);
    }

    public void appendWhereEscapeString(String clause)
    {
        if (clause == null) return;

        if (mWhereClause == null) mWhereClause = new StringBuilder(clause.length() + 16);

        android.database.DatabaseUtils.appendEscapedSQLString(mWhereClause, clause);
    }

    public void appendWhereStandalone(CharSequence clause)
    {
        if (clause == null) return;

        if (mWhereClause == null) mWhereClause = new StringBuilder(clause.length() + 16);

        if (mWhereClause.length() > 0) mWhereClause.append(" AND ");

        mWhereClause.append('(').append(clause).append(')');
    }

    public String getWhere()
    {
        return mWhereClause == null ? null : mWhereClause.toString();
    }

    public Cursor query(SQLiteDatabase database, String[] projection, String selection, String[] selectionArgs, String groupBy,
                        String having, String sortOrder)
    {
        return query(database, projection, selection, selectionArgs, groupBy, having, sortOrder, null, null);
    }

    public Cursor query(SQLiteDatabase database, String[] projection, String selection, String[] selectionArgs, String groupBy,
                        String having, String sortOrder, String limit)
    {
        return query(database, projection, selection, selectionArgs, groupBy, having, sortOrder, limit, null);
    }

    public Cursor query(SQLiteDatabase database, String[] projection, String selection, String[] selectionArgs, String groupBy,
                        String having, String sortOrder, String limit, CancellationSignal cancellationSignal)
    {
        if (database == null || mTables.isEmpty()) return null;

        String sql = buildQuery(projection, selection, groupBy, having, sortOrder, limit);

        return database.rawQuery(sql, selectionArgs);
    }

    public String buildQuery(String[] projection, String selection, String groupBy, String having, String sortOrder,
                             String limit)
    {
        StringBuilder query = new StringBuilder(120);

        query.append("SELECT ");
        if (mDistinct) query.append("DISTINCT ");

        appendColumns(query, mapColumns(projection));

        query.append(" FROM ").append(mTables);

        String where = combinedWhere(selection);
        if (where != null && !where.isEmpty()) query.append(" WHERE ").append(where);

        if (groupBy != null && !groupBy.isEmpty()) query.append(" GROUP BY ").append(groupBy);
        if (having != null && !having.isEmpty()) query.append(" HAVING ").append(having);
        if (sortOrder != null && !sortOrder.isEmpty()) query.append(" ORDER BY ").append(sortOrder);
        if (limit != null && !limit.isEmpty()) query.append(" LIMIT ").append(limit);

        return query.toString();
    }

    private String combinedWhere(String selection)
    {
        boolean hasBuilt = mWhereClause != null && mWhereClause.length() > 0;
        boolean hasSelection = selection != null && !selection.isEmpty();

        if (hasBuilt && hasSelection) return "(" + mWhereClause + ") AND (" + selection + ")";
        if (hasBuilt) return mWhereClause.toString();
        if (hasSelection) return selection;

        return null;
    }

    private String[] mapColumns(String[] projection)
    {
        if (projection == null || mProjectionMap == null) return projection;

        String[] mapped = new String[projection.length];

        for (int index = 0; index < projection.length; index++)
        {
            String replacement = mProjectionMap.get(projection[index]);
            mapped[index] = replacement != null ? replacement : projection[index];
        }

        return mapped;
    }

    private static void appendColumns(StringBuilder query, String[] columns)
    {
        if (columns == null || columns.length == 0)
        {
            query.append('*');
            return;
        }

        for (int index = 0; index < columns.length; index++)
        {
            if (index > 0) query.append(", ");

            query.append(columns[index]);
        }
    }

    public static String buildQueryString(boolean distinct, String tables, String[] columns, String where, String groupBy,
                                          String having, String orderBy, String limit)
    {
        StringBuilder query = new StringBuilder(120);

        query.append("SELECT ");
        if (distinct) query.append("DISTINCT ");

        appendColumns(query, columns);

        query.append(" FROM ").append(tables);

        if (where != null && !where.isEmpty()) query.append(" WHERE ").append(where);
        if (groupBy != null && !groupBy.isEmpty()) query.append(" GROUP BY ").append(groupBy);
        if (having != null && !having.isEmpty()) query.append(" HAVING ").append(having);
        if (orderBy != null && !orderBy.isEmpty()) query.append(" ORDER BY ").append(orderBy);
        if (limit != null && !limit.isEmpty()) query.append(" LIMIT ").append(limit);

        return query.toString();
    }
}
