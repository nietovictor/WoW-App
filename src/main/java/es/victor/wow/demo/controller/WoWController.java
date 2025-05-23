package es.victor.wow.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import java.util.Map;
import java.util.HashMap;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.util.Base64;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Paths;
import java.nio.file.Files;

@Controller
public class WoWController {

    @GetMapping("/")
    public String showForm(Model model) {
        model.addAttribute("params", new BlizzardParams());
        return "wowform";
    }

    @PostMapping("/consultar")
    public String consultar(@ModelAttribute BlizzardParams params, Model model) {
        // Calcular namespace en función de la región
        String namespace = "profile-" + params.getRegion();
        String url = String.format(
            "https://%s.api.blizzard.com/profile/wow/character/%s/%s/mythic-keystone-profile?namespace=%s&locale=%s",
            params.getRegion(),
            params.getRealmSlug().toLowerCase(),
            params.getCharacterName().toLowerCase(),
            namespace,
            params.getLocale()
        );

        String rating = null;
        String error = null;
        String rgbColor = null;
        try {
            String token = BlizzardTokenManager.getToken(); // Usa el token Bearer directamente
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            Map<String, Object> body = response.getBody();
            if (body != null && body.containsKey("current_mythic_rating")) {
                Map<String, Object> mythic = (Map<String, Object>) body.get("current_mythic_rating");
                double ratingValue = ((Number) mythic.get("rating")).doubleValue();
                rating = String.valueOf(Math.round(ratingValue));
                if (mythic.containsKey("color")) {
                    Map<String, Object> color = (Map<String, Object>) mythic.get("color");
                    int r = ((Number) color.get("r")).intValue();
                    int g = ((Number) color.get("g")).intValue();
                    int b = ((Number) color.get("b")).intValue();
                    rgbColor = String.format("rgb(%d,%d,%d)", r, g, b);
                }
            } else {
                error = "No se encontró el rating para este personaje.";
            }
        } catch (Exception e) {
            error = "Error al consultar la API: " + e.getMessage();
        }
        model.addAttribute("params", params);
        model.addAttribute("rating", rating);
        model.addAttribute("ratingColor", rgbColor);
        model.addAttribute("error", error);
        return "wowform";
    }

    

    public static class BlizzardParams {
        private String region = "";
        private String realmSlug = "";
        private String characterName = "";
        private String namespace = "";
        private String locale = "";

        public BlizzardParams() {
            try {
                // Cargar desde resources usando el classloader
                ClassLoader classLoader = BlizzardTokenManager.class.getClassLoader();
                try (InputStream is = classLoader.getResourceAsStream("default_character.json")) {
                    if (is == null) throw new FileNotFoundException("No se encontró blizzard_credentials.json en resources");
                    ObjectMapper mapper = new ObjectMapper();
                    Map<String, String> creds = mapper.readValue(is, Map.class);
                    region = creds.get("region");
                    realmSlug = creds.get("realmSlug");
                    characterName = creds.get("character_name");
                    namespace = creds.get("namespace");
                    locale = creds.get("locale");
                }
            } catch (Exception e) {
                throw new RuntimeException("No se pudieron cargar las credenciales de Blizzard", e);
            }
        }

        // Getters y setters
        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }
        public String getRealmSlug() { return realmSlug; }
        public void setRealmSlug(String realmSlug) { this.realmSlug = realmSlug; }
        public String getCharacterName() { return characterName; }
        public void setCharacterName(String characterName) { this.characterName = characterName; }
        public String getNamespace() { return namespace; }
        public void setNamespace(String namespace) { this.namespace = namespace; }
        public String getLocale() { return locale; }
        public void setLocale(String locale) { this.locale = locale; }
    }
}
