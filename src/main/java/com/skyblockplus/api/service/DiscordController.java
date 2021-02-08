package com.skyblockplus.api.service;

import com.skyblockplus.api.models.discordoauth2.AccessTokenExchangeModel;
import com.skyblockplus.api.models.discordoauth2.DiscordUserModel;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

import static com.skyblockplus.utils.BotUtils.CLIENT_ID;
import static com.skyblockplus.utils.BotUtils.CLIENT_SECRET;

@RestController
public class DiscordController {

    private final RestTemplate restTemplate;
    private final String scope = "identify%20guilds";
    private final String redirectUri = "http://localhost:8080/api/discord/callback";

    public DiscordController(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    @GetMapping("/api/discord/redirect")
    public ResponseEntity<Void> redirectDiscordOauth() {
        String redirectUrl = "https://discordapp.com/api/oauth2/authorize?client_id=" + CLIENT_ID + "&scope=" + scope + "&response_type=code&redirect_uri=" + redirectUri;
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(redirectUrl))
                .build();
    }

    @GetMapping("/api/discord/callback")
    public ResponseEntity onDiscordCallback(@RequestParam(value = "code") String userCode) {
        try {
            String tokenExchangeUrl = "https://discord.com/api/oauth2/token";

            MultiValueMap<String, String> tokenExchangeData = new LinkedMultiValueMap<>();
            tokenExchangeData.add("client_id", CLIENT_ID);
            tokenExchangeData.add("client_secret", CLIENT_SECRET);
            tokenExchangeData.add("grant_type", "authorization_code");
            tokenExchangeData.add("code", userCode);
            tokenExchangeData.add("redirect_uri", redirectUri);
            tokenExchangeData.add("scope", scope);

            HttpHeaders tokenExchangeHeaders = new HttpHeaders();
            tokenExchangeHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            ResponseEntity<AccessTokenExchangeModel> tokenExchangeResponse = this.restTemplate.exchange(tokenExchangeUrl, HttpMethod.POST, new HttpEntity<>(tokenExchangeData, tokenExchangeHeaders), AccessTokenExchangeModel.class);

            if (tokenExchangeResponse.getStatusCode() == HttpStatus.OK) {
                String accessToken = tokenExchangeResponse.getBody().getAccessToken();
                String tokenType = tokenExchangeResponse.getBody().getTokenType();

                String userExchangeUrl = "https://discord.com/api/users/@me";
                HttpHeaders userExchangeHeader = new HttpHeaders();
                userExchangeHeader.set("authorization", tokenType + " " + accessToken);

                ResponseEntity<DiscordUserModel> userExchangeResponse = this.restTemplate.exchange(userExchangeUrl, HttpMethod.GET, new HttpEntity(userExchangeHeader), DiscordUserModel.class);
                if (userExchangeResponse.getStatusCode() == HttpStatus.OK) {
                    String userId = userExchangeResponse.getBody().getId();

                    String redirectUrl = "http://localhost:3000/dashboard/" + userId;
                    return ResponseEntity.status(HttpStatus.FOUND)
                            .location(URI.create(redirectUrl))
                            .build();
                }
            }
        } catch (Exception ignored) {
        }
        String frontendRedirectUrl = "http://localhost:3000";
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(frontendRedirectUrl))
                .build();
    }
}
