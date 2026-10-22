package android.hardware;

public class GeomagneticField
{
    private static final float EARTH_SEMI_MAJOR_AXIS_KM = 6378.137f;
    private static final float EARTH_SEMI_MINOR_AXIS_KM = 6356.7524f;
    private static final float EARTH_REFERENCE_RADIUS_KM = 6371.2f;

    private static final long BASE_TIME = 1577836800000L;

    private static final float[][] G_COEFF = new float[][] {
        {0.0f},
        {-29404.5f, -1450.7f},
        {-2500.0f, 2982.0f, 1676.8f},
        {1363.9f, -2381.0f, 1236.2f, 525.7f},
        {903.1f, 809.4f, 86.2f, -309.4f, 47.9f},
        {-234.4f, 363.1f, 187.8f, -140.7f, -151.2f, 13.7f},
        {65.9f, 65.6f, 73.0f, -121.5f, -36.2f, 13.5f, -64.7f},
        {80.6f, -76.8f, -8.3f, 56.5f, 15.8f, 6.4f, -7.2f, 9.8f},
        {23.6f, 9.8f, -17.5f, -0.4f, -21.1f, 15.3f, 13.7f, -16.5f, -0.3f},
        {5.0f, 8.2f, 2.9f, -1.4f, -1.1f, -13.3f, 1.1f, 8.9f, -9.3f, -11.9f},
        {-1.9f, -6.2f, -0.1f, 1.7f, -0.9f, 0.6f, -0.9f, 1.9f, 1.4f, -2.4f, -3.9f},
        {3.0f, -1.4f, -2.5f, 2.4f, -0.9f, 0.3f, -0.7f, -0.1f, 1.4f, -0.6f, 0.2f, 3.1f},
        {-2.0f, -0.1f, 0.5f, 1.3f, -1.2f, 0.7f, 0.3f, 0.5f, -0.2f, -0.5f, 0.1f, -1.1f, -0.3f}};

    private static final float[][] H_COEFF = new float[][] {
        {0.0f},
        {0.0f, 4652.9f},
        {0.0f, -2991.6f, -734.8f},
        {0.0f, -82.2f, 241.8f, -542.9f},
        {0.0f, 282.0f, -158.4f, 199.8f, -350.1f},
        {0.0f, 47.7f, 208.4f, -121.3f, 32.2f, 99.1f},
        {0.0f, -19.1f, 25.0f, 52.7f, -64.4f, 9.0f, 68.1f},
        {0.0f, -51.4f, -16.8f, 2.3f, 23.5f, -2.2f, -27.2f, -1.9f},
        {0.0f, 8.4f, -15.3f, 12.8f, -11.8f, 14.9f, 3.6f, -6.9f, 2.8f},
        {0.0f, -23.3f, 11.1f, 9.8f, -5.1f, -6.2f, 7.8f, 0.4f, -1.5f, 9.7f},
        {0.0f, 3.4f, -0.2f, 3.5f, 4.8f, -8.6f, -0.1f, -4.2f, -3.4f, -0.1f, -8.8f},
        {0.0f, 0.0f, 2.6f, -0.5f, -0.4f, 0.6f, -0.2f, -1.7f, -1.6f, -3.0f, -2.0f, -2.6f},
        {0.0f, -1.2f, 0.5f, 1.3f, -1.8f, 0.1f, 0.7f, -0.1f, 0.6f, 0.2f, -0.9f, 0.0f, 0.5f}};

    private static final float[][] DELTA_G = new float[][] {
        {0.0f},
        {6.7f, 7.7f},
        {-11.5f, -7.1f, -2.2f},
        {2.8f, -6.2f, 3.4f, -12.2f},
        {-1.1f, -1.6f, -6.0f, 5.4f, -5.5f},
        {-0.3f, 0.6f, -0.7f, 0.1f, 1.2f, 1.0f},
        {-0.6f, -0.4f, 0.5f, 1.4f, -1.4f, 0.0f, 0.8f},
        {-0.1f, -0.3f, -0.1f, 0.7f, 0.2f, -0.5f, -0.8f, 1.0f},
        {-0.1f, 0.1f, -0.1f, 0.5f, -0.1f, 0.4f, 0.5f, 0.0f, 0.4f},
        {-0.1f, -0.2f, 0.0f, 0.4f, -0.3f, 0.0f, 0.3f, 0.0f, 0.0f, -0.4f},
        {0.0f, 0.0f, 0.0f, 0.2f, -0.1f, -0.2f, 0.0f, -0.1f, -0.2f, -0.1f, 0.0f},
        {0.0f, -0.1f, 0.0f, 0.0f, 0.0f, -0.1f, 0.0f, 0.0f, -0.1f, -0.1f, -0.1f, -0.1f},
        {0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, -0.1f}};

    private static final float[][] DELTA_H = new float[][] {
        {0.0f},
        {0.0f, -25.1f},
        {0.0f, -30.2f, -23.9f},
        {0.0f, 5.7f, -1.0f, 1.1f},
        {0.0f, 0.2f, 6.9f, 3.7f, -5.6f},
        {0.0f, 0.1f, 2.5f, -0.9f, 3.0f, 0.5f},
        {0.0f, 0.1f, -1.8f, -1.4f, 0.9f, 0.1f, 1.0f},
        {0.0f, 0.5f, 0.6f, -0.7f, -0.2f, -1.2f, 0.2f, 0.3f},
        {0.0f, -0.3f, 0.7f, -0.2f, 0.5f, -0.3f, -0.5f, 0.4f, 0.1f},
        {0.0f, -0.3f, 0.2f, -0.4f, 0.4f, 0.1f, 0.0f, -0.2f, 0.5f, 0.2f},
        {0.0f, 0.0f, 0.1f, -0.3f, 0.1f, -0.2f, 0.1f, 0.0f, -0.1f, 0.2f, 0.0f},
        {0.0f, 0.0f, 0.1f, 0.0f, 0.2f, 0.0f, 0.0f, 0.1f, 0.0f, -0.1f, 0.0f, 0.0f},
        {0.0f, 0.0f, 0.0f, -0.1f, 0.1f, 0.0f, 0.0f, 0.0f, 0.1f, 0.0f, 0.0f, 0.0f, -0.1f}};

    private static final float[][] SCHMIDT_QUASI_NORM_FACTORS = computeSchmidtQuasiNormFactors(G_COEFF.length);

    private float mX;
    private float mY;
    private float mZ;

    private float mGcLatitudeRad;
    private float mGcLongitudeRad;
    private float mGcRadiusKm;

    public GeomagneticField(float gdLatitudeDeg, float gdLongitudeDeg, float altitudeMeters, long timeMillis)
    {
        final int maxN = G_COEFF.length;

        gdLatitudeDeg = Math.min(90.0f - 1e-5f, Math.max(-90.0f + 1e-5f, gdLatitudeDeg));
        computeGeocentricCoordinates(gdLatitudeDeg, gdLongitudeDeg, altitudeMeters);

        LegendreTable legendre = new LegendreTable(maxN - 1, (float)(Math.PI / 2.0 - mGcLatitudeRad));

        float[] relativeRadiusPower = new float[maxN + 2];
        relativeRadiusPower[0] = 1.0f;
        relativeRadiusPower[1] = EARTH_REFERENCE_RADIUS_KM / mGcRadiusKm;
        for (int index = 2; index < relativeRadiusPower.length; ++index)
        {
            relativeRadiusPower[index] = relativeRadiusPower[index - 1] * relativeRadiusPower[1];
        }

        float[] sinMLon = new float[maxN];
        float[] cosMLon = new float[maxN];
        sinMLon[0] = 0.0f;
        cosMLon[0] = 1.0f;
        sinMLon[1] = (float)Math.sin(mGcLongitudeRad);
        cosMLon[1] = (float)Math.cos(mGcLongitudeRad);

        for (int m = 2; m < maxN; ++m)
        {
            int x = m >> 1;
            sinMLon[m] = sinMLon[m - x] * cosMLon[x] + cosMLon[m - x] * sinMLon[x];
            cosMLon[m] = cosMLon[m - x] * cosMLon[x] - sinMLon[m - x] * sinMLon[x];
        }

        float inverseCosLatitude = 1.0f / (float)Math.cos(mGcLatitudeRad);
        float yearsSinceBase = (timeMillis - BASE_TIME) / (365.0f * 24.0f * 60.0f * 60.0f * 1000.0f);

        float gcX = 0.0f;
        float gcY = 0.0f;
        float gcZ = 0.0f;

        for (int n = 1; n < maxN; n++)
        {
            for (int m = 0; m <= n; m++)
            {
                float g = G_COEFF[n][m] + yearsSinceBase * DELTA_G[n][m];
                float h = H_COEFF[n][m] + yearsSinceBase * DELTA_H[n][m];

                gcX += relativeRadiusPower[n + 2] * (g * cosMLon[m] + h * sinMLon[m]) * legendre.mPDeriv[n][m]
                    * SCHMIDT_QUASI_NORM_FACTORS[n][m];

                gcY += relativeRadiusPower[n + 2] * m * (g * sinMLon[m] - h * cosMLon[m]) * legendre.mP[n][m]
                    * SCHMIDT_QUASI_NORM_FACTORS[n][m] * inverseCosLatitude;

                gcZ -= (n + 1) * relativeRadiusPower[n + 2] * (g * cosMLon[m] + h * sinMLon[m]) * legendre.mP[n][m]
                    * SCHMIDT_QUASI_NORM_FACTORS[n][m];
            }
        }

        double latDiffRad = Math.toRadians(gdLatitudeDeg) - mGcLatitudeRad;
        mX = (float)(gcX * Math.cos(latDiffRad) + gcZ * Math.sin(latDiffRad));
        mY = gcY;
        mZ = (float)(-gcX * Math.sin(latDiffRad) + gcZ * Math.cos(latDiffRad));
    }

    public float getX()
    {
        return mX;
    }

    public float getY()
    {
        return mY;
    }

    public float getZ()
    {
        return mZ;
    }

    public float getDeclination()
    {
        return (float)Math.toDegrees(Math.atan2(mY, mX));
    }

    public float getInclination()
    {
        return (float)Math.toDegrees(Math.atan2(mZ, getHorizontalStrength()));
    }

    public float getHorizontalStrength()
    {
        return (float)Math.hypot(mX, mY);
    }

    public float getFieldStrength()
    {
        return (float)Math.sqrt(mX * mX + mY * mY + mZ * mZ);
    }

    private void computeGeocentricCoordinates(float gdLatitudeDeg, float gdLongitudeDeg, float altitudeMeters)
    {
        float altitudeKm = altitudeMeters / 1000.0f;
        float a2 = EARTH_SEMI_MAJOR_AXIS_KM * EARTH_SEMI_MAJOR_AXIS_KM;
        float b2 = EARTH_SEMI_MINOR_AXIS_KM * EARTH_SEMI_MINOR_AXIS_KM;
        double gdLatRad = Math.toRadians(gdLatitudeDeg);
        float clat = (float)Math.cos(gdLatRad);
        float slat = (float)Math.sin(gdLatRad);
        float tlat = slat / clat;
        float latRad = (float)Math.sqrt(a2 * clat * clat + b2 * slat * slat);

        mGcLatitudeRad = (float)Math.atan(tlat * (latRad * altitudeKm + b2) / (latRad * altitudeKm + a2));
        mGcLongitudeRad = (float)Math.toRadians(gdLongitudeDeg);

        float radSq = altitudeKm * altitudeKm + 2 * altitudeKm * (float)Math.sqrt(a2 * clat * clat + b2 * slat * slat)
            + (a2 * a2 * clat * clat + b2 * b2 * slat * slat) / (a2 * clat * clat + b2 * slat * slat);
        mGcRadiusKm = (float)Math.sqrt(radSq);
    }

    private static float[][] computeSchmidtQuasiNormFactors(int maxN)
    {
        float[][] schmidt = new float[maxN + 1][];
        schmidt[0] = new float[] {1.0f};

        for (int n = 1; n <= maxN; n++)
        {
            schmidt[n] = new float[n + 1];
            schmidt[n][0] = schmidt[n - 1][0] * (2 * n - 1) / (float)n;

            for (int m = 1; m <= n; m++)
            {
                schmidt[n][m] = schmidt[n][m - 1] * (float)Math.sqrt((n - m + 1) * (m == 1 ? 2 : 1) / (float)(n + m));
            }
        }

        return schmidt;
    }

    private static class LegendreTable
    {
        final float[][] mP;
        final float[][] mPDeriv;

        LegendreTable(int maxN, float thetaRad)
        {
            float cos = (float)Math.cos(thetaRad);
            float sin = (float)Math.sin(thetaRad);

            mP = new float[maxN + 1][];
            mPDeriv = new float[maxN + 1][];
            mP[0] = new float[] {1.0f};
            mPDeriv[0] = new float[] {0.0f};

            for (int n = 1; n <= maxN; n++)
            {
                mP[n] = new float[n + 1];
                mPDeriv[n] = new float[n + 1];

                for (int m = 0; m <= n; m++)
                {
                    if (n == m)
                    {
                        mP[n][m] = sin * mP[n - 1][m - 1];
                        mPDeriv[n][m] = cos * mP[n - 1][m - 1] + sin * mPDeriv[n - 1][m - 1];
                    }
                    else if (n == 1 || m == n - 1)
                    {
                        mP[n][m] = cos * mP[n - 1][m];
                        mPDeriv[n][m] = -sin * mP[n - 1][m] + cos * mPDeriv[n - 1][m];
                    }
                    else
                    {
                        float k = ((n - 1) * (n - 1) - m * m) / (float)((2 * n - 1) * (2 * n - 3));
                        mP[n][m] = cos * mP[n - 1][m] - k * mP[n - 2][m];
                        mPDeriv[n][m] = -sin * mP[n - 1][m] + cos * mPDeriv[n - 1][m] - k * mPDeriv[n - 2][m];
                    }
                }
            }
        }
    }
}
