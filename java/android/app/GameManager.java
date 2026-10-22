package android.app;

import android.content.Context;

public final class GameManager
{
    public static final int GAME_MODE_UNSUPPORTED = 0;
    public static final int GAME_MODE_STANDARD = 1;
    public static final int GAME_MODE_PERFORMANCE = 2;
    public static final int GAME_MODE_BATTERY = 3;
    public static final int GAME_MODE_CUSTOM = 4;

    private final Context mContext;
    private GameState mGameState;

    public GameManager(Context context)
    {
        mContext = context;
    }

    public int getGameMode()
    {
        return GAME_MODE_STANDARD;
    }

    public void setGameState(GameState gameState)
    {
        mGameState = gameState;
    }
}
