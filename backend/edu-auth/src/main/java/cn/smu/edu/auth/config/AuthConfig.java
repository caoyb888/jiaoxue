package cn.smu.edu.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

@Configuration
public class AuthConfig {

    @Value("${edu.jwt.private-key}")
    private String privateKeyPem;

    @Bean
    public PrivateKey jwtPrivateKey() throws Exception {
        String clean = privateKeyPem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] keyBytes = Base64.getDecoder().decode(clean);
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
