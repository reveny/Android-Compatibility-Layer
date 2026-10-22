package android.net.http;

import android.os.Bundle;
import java.io.ByteArrayInputStream;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SslCertificate
{
    private static final String ISO_8601_DATE_FORMAT = "yyyy-MM-dd HH:mm:ssZ";
    private static final String ISSUED_TO = "issued-to";
    private static final String ISSUED_BY = "issued-by";
    private static final String VALID_NOT_BEFORE = "valid-not-before";
    private static final String VALID_NOT_AFTER = "valid-not-after";
    private static final String X509_CERTIFICATE = "x509-certificate";

    private final DName mIssuedTo;
    private final DName mIssuedBy;
    private final Date mValidNotBefore;
    private final Date mValidNotAfter;
    private final X509Certificate mX509Certificate;

    @Deprecated
    public SslCertificate(String issuedTo, String issuedBy, String validNotBefore, String validNotAfter)
    {
        this(issuedTo, issuedBy, parseDate(validNotBefore), parseDate(validNotAfter), null);
    }

    @Deprecated
    public SslCertificate(String issuedTo, String issuedBy, Date validNotBefore, Date validNotAfter)
    {
        this(issuedTo, issuedBy, validNotBefore, validNotAfter, null);
    }

    public SslCertificate(X509Certificate certificate)
    {
        this(certificate.getSubjectDN().getName(), certificate.getIssuerDN().getName(), certificate.getNotBefore(),
            certificate.getNotAfter(), certificate);
    }

    private SslCertificate(String issuedTo, String issuedBy, Date validNotBefore, Date validNotAfter, X509Certificate certificate)
    {
        mIssuedTo = new DName(issuedTo);
        mIssuedBy = new DName(issuedBy);
        mValidNotBefore = cloneDate(validNotBefore);
        mValidNotAfter = cloneDate(validNotAfter);
        mX509Certificate = certificate;
    }

    public static Bundle saveState(SslCertificate certificate)
    {
        if (certificate == null) return null;
        Bundle bundle = new Bundle();
        bundle.putString(ISSUED_TO, certificate.getIssuedTo().getDName());
        bundle.putString(ISSUED_BY, certificate.getIssuedBy().getDName());
        bundle.putString(VALID_NOT_BEFORE, certificate.getValidNotBefore());
        bundle.putString(VALID_NOT_AFTER, certificate.getValidNotAfter());
        X509Certificate x509Certificate = certificate.mX509Certificate;
        if (x509Certificate != null)
        {
            try
            {
                bundle.putByteArray(X509_CERTIFICATE, x509Certificate.getEncoded());
            }
            catch (CertificateEncodingException ignored)
            {
            }
        }
        return bundle;
    }

    public static SslCertificate restoreState(Bundle bundle)
    {
        if (bundle == null) return null;
        X509Certificate x509Certificate = null;
        byte[] bytes = bundle.getByteArray(X509_CERTIFICATE);
        if (bytes != null)
        {
            try
            {
                CertificateFactory factory = CertificateFactory.getInstance("X.509");
                x509Certificate = (X509Certificate)factory.generateCertificate(new ByteArrayInputStream(bytes));
            }
            catch (Exception ignored)
            {
            }
        }
        return new SslCertificate(bundle.getString(ISSUED_TO), bundle.getString(ISSUED_BY),
            parseDate(bundle.getString(VALID_NOT_BEFORE)), parseDate(bundle.getString(VALID_NOT_AFTER)), x509Certificate);
    }

    public Date getValidNotBeforeDate()
    {
        return cloneDate(mValidNotBefore);
    }

    @Deprecated
    public String getValidNotBefore()
    {
        return formatDate(mValidNotBefore);
    }

    public Date getValidNotAfterDate()
    {
        return cloneDate(mValidNotAfter);
    }

    @Deprecated
    public String getValidNotAfter()
    {
        return formatDate(mValidNotAfter);
    }

    public DName getIssuedTo()
    {
        return mIssuedTo;
    }

    public DName getIssuedBy()
    {
        return mIssuedBy;
    }

    public X509Certificate getX509Certificate()
    {
        return mX509Certificate;
    }

    @Override
    public String toString()
    {
        return "Issued to: " + mIssuedTo.getDName() + ";\n" + "Issued by: " + mIssuedBy.getDName() + ";\n";
    }

    private static Date parseDate(String string)
    {
        if (string == null) return null;
        try
        {
            return new SimpleDateFormat(ISO_8601_DATE_FORMAT, Locale.US).parse(string);
        }
        catch (ParseException exception)
        {
            return null;
        }
    }

    private static String formatDate(Date date)
    {
        if (date == null) return "";
        return new SimpleDateFormat(ISO_8601_DATE_FORMAT, Locale.US).format(date);
    }

    private static Date cloneDate(Date date)
    {
        if (date == null) return null;
        return (Date)date.clone();
    }

    public class DName
    {
        private String mDName;
        private String mCName;
        private String mOName;
        private String mUName;

        public DName(String dName)
        {
            if (dName == null) return;
            mDName = dName;
            for (String component : splitComponents(dName))
            {
                int equals = component.indexOf('=');
                if (equals <= 0) continue;
                String key = component.substring(0, equals).trim().toUpperCase(Locale.ROOT);
                String value = unescape(component.substring(equals + 1).trim());
                if (("CN".equals(key) || "2.5.4.3".equals(key)) && mCName == null) mCName = value;
                if (("O".equals(key) || "2.5.4.10".equals(key)) && mOName == null) mOName = value;
                if (("OU".equals(key) || "2.5.4.11".equals(key)) && mUName == null) mUName = value;
            }
        }

        public String getDName()
        {
            return mDName != null ? mDName : "";
        }

        public String getCName()
        {
            return mCName != null ? mCName : "";
        }

        public String getOName()
        {
            return mOName != null ? mOName : "";
        }

        public String getUName()
        {
            return mUName != null ? mUName : "";
        }

        private List<String> splitComponents(String name)
        {
            List<String> components = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            boolean quoted = false;
            for (int index = 0; index < name.length(); index++)
            {
                char character = name.charAt(index);
                if (character == '\\' && index + 1 < name.length())
                {
                    current.append(character).append(name.charAt(++index));
                    continue;
                }
                if (character == '"') quoted = !quoted;
                if (!quoted && (character == ',' || character == ';' || character == '+'))
                {
                    components.add(current.toString());
                    current.setLength(0);
                    continue;
                }
                current.append(character);
            }
            if (current.length() > 0) components.add(current.toString());
            return components;
        }

        private String unescape(String value)
        {
            if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\""))
            {
                value = value.substring(1, value.length() - 1);
            }
            StringBuilder builder = new StringBuilder(value.length());
            for (int index = 0; index < value.length(); index++)
            {
                char character = value.charAt(index);
                if (character == '\\' && index + 1 < value.length()) character = value.charAt(++index);
                builder.append(character);
            }
            return builder.toString();
        }
    }
}
