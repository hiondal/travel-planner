import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class GenToken2 {
    public static void main(String[] args) {
        String secret = "dev-jwt-secret-key-for-local-development-only";
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        Date now = new Date();
        Date expiry = new Date(now.getTime() + 86400000L);

        String token = Jwts.builder()
                .subject("usr_test001")
                .claim("email", "test@example.com")
                .claim("tier", "FREE")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();

        System.out.println(token);
    }
}
