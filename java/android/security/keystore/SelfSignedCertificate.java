package android.security.keystore;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.security.auth.x500.X500Principal;

final class SelfSignedCertificate
{
    private static final int TAG_INTEGER = 0x02;
    private static final int TAG_BIT_STRING = 0x03;
    private static final int TAG_NULL = 0x05;
    private static final int TAG_OBJECT_IDENTIFIER = 0x06;
    private static final int TAG_UTC_TIME = 0x17;
    private static final int TAG_GENERALIZED_TIME = 0x18;
    private static final int TAG_SEQUENCE = 0x30;
    private static final int TAG_CONTEXT_ZERO = 0xa0;

    private static final byte[] OID_SHA256_WITH_RSA = {0x2a, (byte)0x86, 0x48, (byte)0x86, (byte)0xf7, 0x0d, 0x01, 0x01, 0x0b};
    private static final byte[] OID_ECDSA_WITH_SHA256 = {0x2a, (byte)0x86, 0x48, (byte)0xce, 0x3d, 0x04, 0x03, 0x02};

    private SelfSignedCertificate()
    {
    }

    static X509Certificate create(KeyPair keyPair, X500Principal subject, BigInteger serialNumber, Date notBefore, Date notAfter)
            throws GeneralSecurityException
    {
        boolean rsa = "RSA".equalsIgnoreCase(keyPair.getPrivate().getAlgorithm());
        byte[] algorithmIdentifier = rsa ? sequence(encode(TAG_OBJECT_IDENTIFIER, OID_SHA256_WITH_RSA), encode(TAG_NULL, new byte[0]))
                                         : sequence(encode(TAG_OBJECT_IDENTIFIER, OID_ECDSA_WITH_SHA256));
        byte[] name = subject.getEncoded();

        byte[] tbsCertificate = sequence(
                encode(TAG_CONTEXT_ZERO, encode(TAG_INTEGER, new byte[] {2})),
                encode(TAG_INTEGER, serialNumber.toByteArray()),
                algorithmIdentifier,
                name,
                sequence(time(notBefore), time(notAfter)),
                name,
                keyPair.getPublic().getEncoded());

        Signature signature = Signature.getInstance(rsa ? "SHA256withRSA" : "SHA256withECDSA");
        signature.initSign(keyPair.getPrivate());
        signature.update(tbsCertificate);
        byte[] signatureBytes = signature.sign();

        byte[] bitString = new byte[signatureBytes.length + 1];
        System.arraycopy(signatureBytes, 0, bitString, 1, signatureBytes.length);

        byte[] certificate = sequence(tbsCertificate, algorithmIdentifier, encode(TAG_BIT_STRING, bitString));
        return (X509Certificate)CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(certificate));
    }

    private static byte[] time(Date date)
    {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US);
        calendar.setTime(date);
        int year = calendar.get(Calendar.YEAR);
        boolean utc = year >= 1950 && year < 2050;
        SimpleDateFormat format = new SimpleDateFormat(utc ? "yyMMddHHmmss'Z'" : "yyyyMMddHHmmss'Z'", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return encode(utc ? TAG_UTC_TIME : TAG_GENERALIZED_TIME, format.format(date).getBytes(StandardCharsets.US_ASCII));
    }

    private static byte[] sequence(byte[]... elements)
    {
        ByteArrayOutputStream content = new ByteArrayOutputStream();
        for (byte[] element : elements) content.write(element, 0, element.length);
        return encode(TAG_SEQUENCE, content.toByteArray());
    }

    private static byte[] encode(int tag, byte[] content)
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream(content.length + 6);
        output.write(tag);
        int length = content.length;
        if (length < 0x80)
        {
            output.write(length);
        }
        else
        {
            int byteCount = 0;
            for (int remaining = length; remaining > 0; remaining >>>= 8) byteCount++;
            output.write(0x80 | byteCount);
            for (int shift = (byteCount - 1) * 8; shift >= 0; shift -= 8) output.write((length >>> shift) & 0xff);
        }
        output.write(content, 0, content.length);
        return output.toByteArray();
    }
}
