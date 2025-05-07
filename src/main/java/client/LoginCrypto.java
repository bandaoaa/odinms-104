package client;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.spec.RSAPrivateKeySpec;
import java.util.Random;
import javax.crypto.Cipher;
import org.apache.log4j.Logger;

import tools.HexTool;

public class LoginCrypto {

    private static final Logger log = Logger.getLogger(LoginCrypto.class);
    protected static final int extralength = 6;
    private static final String[] Alphabet = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"};
    private static final String[] Number = {"1", "2", "3", "4", "5", "6", "7", "8", "9"};
    private static final Random rand = new Random();
    private static KeyFactory RSAKeyFactory;

    public static String Generate_13DigitAsiasoftPassport() {
        StringBuilder sb = new StringBuilder();
        sb.append(Alphabet[rand.nextInt(Alphabet.length)]);
        for (int i = 0; i < 11; i++) {
            sb.append(Number[rand.nextInt(Number.length)]);
        }
        sb.append(Alphabet[rand.nextInt(Alphabet.length)]);
        return sb.toString();
    }

    private static String toSimpleHexString(byte[] bytes) {
        return HexTool.toString(bytes).replace(" ", "").toLowerCase();
    }

    private static String hashWithDigest(String in, String digest) {
        try {
            MessageDigest Digester = MessageDigest.getInstance(digest);
            Digester.update(in.getBytes("UTF-8"), 0, in.length());
            byte[] sha1Hash = Digester.digest();
            return toSimpleHexString(sha1Hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException("Hashing the password failed", ex);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Encoding the string failed", e);
        }
        
    }
    
    

    private static String hexSha1(String in) {
        return hashWithDigest(in, "SHA-1");
    }

    private static String hexSha512(String in) {
        return hashWithDigest(in, "SHA-512");
    }

    public static boolean checkSha1Hash(String hash, String password) {
        return hash.equals(hexSha1(password));
    }

    public static boolean checkSaltedSha512Hash(String hash, String password, String salt) {
        return hash.equals(makeSaltedSha512Hash(password, salt));
    }

    public static String makeSaltedSha512Hash(String password, String salt) {
        return hexSha512(new StringBuilder().append(password).append(salt).toString());
    }

    public static String makeSalt() {
        byte[] salt = new byte[16];
        rand.nextBytes(salt);
        return toSimpleHexString(salt);
    }

    public static String rand_s(String in) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(rand.nextBoolean() ? Alphabet[rand.nextInt(Alphabet.length)] : Number[rand.nextInt(Number.length)]);
        }
        return new StringBuilder().append(sb.toString()).append(in).toString();
    }

    public static String rand_r(String in) {
        return in.substring(6, 134);
    }
}