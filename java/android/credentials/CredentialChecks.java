package android.credentials;

final class CredentialChecks
{
    private CredentialChecks()
    {
    }

    static String requireType(String type)
    {
        if (type == null || type.isEmpty()) throw new IllegalArgumentException("type must not be empty");

        return type;
    }
}
