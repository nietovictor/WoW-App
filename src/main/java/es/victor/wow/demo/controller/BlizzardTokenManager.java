package es.victor.wow.demo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public class BlizzardTokenManager {
    private static String token = null;
    private static long expiresAt = 0;
    private static final ReentrantLock lock = new ReentrantLock();

    private static String clientId;
    private static String clientSecret;

    static {
        try {
            // Cargar desde resources usando el classloader
            ClassLoader classLoader = BlizzardTokenManager.class.getClassLoader();
            try (InputStream is = classLoader.getResourceAsStream("blizzard_credentials.json")) {
                if (is == null) throw new FileNotFoundException("No se encontró blizzard_credentials.json en resources");
                ObjectMapper mapper = new ObjectMapper();
                Map<String, String> creds = mapper.readValue(is, Map.class);
                clientId = creds.get("client_id");
                clientSecret = creds.get("client_secret");
            }
        } catch (Exception e) {
            throw new RuntimeException("No se pudieron cargar las credenciales de Blizzard", e);
        }
    }

    public static String getToken() {
        lock.lock();
        try {
            long now = System.currentTimeMillis();
            if (token == null || now >= expiresAt) {
                fetchToken();
            }
            return token;
        } finally {
            lock.unlock();
        }
    }

    private static void fetchToken() {
        RestTemplate restTemplate = new RestTemplate();
        String url = "https://oauth.battle.net/oauth/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String auth = clientId + ":" + clientSecret;
        headers.set("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString(auth.getBytes()));

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
        Map<String, Object> respBody = response.getBody();
        token = (String) respBody.get("access_token");
        int expiresIn = (Integer) respBody.get("expires_in");
        expiresAt = System.currentTimeMillis() + (expiresIn - 60) * 1000L; // 60s margen de seguridad
    }
}