import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.*;
import java.util.Base64;

/** Salted PBKDF2 password hashing. Format stored in DB: base64(salt):base64(hash) */
public class PasswordUtil {
    public static String hash(String password) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt) + ":" +
               Base64.getEncoder().encodeToString(pbkdf2(password, salt));
    }

    public static boolean verify(String password, String stored) {
        String[] p = stored.split(":");
        if (p.length != 2) return false;
        byte[] salt = Base64.getDecoder().decode(p[0]);
        byte[] expected = Base64.getDecoder().decode(p[1]);
        return MessageDigest.isEqual(expected, pbkdf2(password, salt));
    }

    private static byte[] pbkdf2(String password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 256);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
