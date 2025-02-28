package com.sp.SwimmingPool.service;

import com.sp.SwimmingPool.model.entity.Member;
import com.sp.SwimmingPool.repos.MemberRepository;
import com.sp.SwimmingPool.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public OAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(oAuth2UserRequest);

        try {
            String registrationId = oAuth2UserRequest.getClientRegistration().getRegistrationId();
            Map<String, Object> attributes = oAuth2User.getAttributes();

            log.debug("Processing OAuth2 login for provider: {}", registrationId);
            log.debug("OAuth2 attributes: {}", attributes);

            String email = null;
            String name = null;
            String pictureUrl = null;

            // Extract provider-specific data
            if ("github".equals(registrationId)) {
                // For GitHub, email might not be in the attributes, need to get it from the API
                try {
                    email = getGitHubEmail(oAuth2UserRequest.getAccessToken().getTokenValue());
                    log.debug("Retrieved GitHub email: {}", email);
                } catch (Exception e) {
                    log.error("Failed to get GitHub email", e);
                    // Set a default email if can't get from GitHub API
                    // This is to prevent null email issues - will be updated during registration
                    email = attributes.get("login") + "@github.placeholder.com";
                    log.debug("Using placeholder email: {}", email);
                }

                name = (String) attributes.get("name");
                if (name == null || name.trim().isEmpty()) {
                    name = (String) attributes.get("login");
                }
                pictureUrl = (String) attributes.get("avatar_url");
            } else if ("google".equals(registrationId)) {
                email = (String) attributes.get("email");
                name = (String) attributes.get("name");
                pictureUrl = (String) attributes.get("picture");
            } else {
                throw new OAuth2AuthenticationException("Unsupported OAuth2 provider: " + registrationId);
            }

            // Add missing details to attributes for use in the OAuth2SuccessHandler
            if (email != null) {
                // Create a mutable copy of the attributes and add email if it doesn't exist
                Map<String, Object> mutableAttributes = new HashMap<>(attributes);
                if (!mutableAttributes.containsKey("email") || mutableAttributes.get("email") == null) {
                    mutableAttributes.put("email", email);
                }
                attributes = mutableAttributes;
            }

            log.info("OAuth2 login processed. Provider: {}, Email: {}, Name: {}",
                    registrationId, email, name);

            // Check if the user already exists
            Optional<Member> memberOptional = email != null ? memberRepository.findByEmail(email) : Optional.empty();
            if (memberOptional.isPresent()) {
                Member member = memberOptional.get();
                log.debug("Existing member found with email: {}", email);
                return UserPrincipal.create(member, attributes);
            }

            // Return a temporary principal with available data for the registration process
            return UserPrincipal.builder()
                    .email(email)
                    .name(name)
                    .userType("MEMBER")
                    .role("MEMBER")
                    .attributes(attributes)
                    .authorities(oAuth2User.getAuthorities())
                    .build();

        } catch (Exception ex) {
            log.error("Error in OAuth2 authentication", ex);
            throw new OAuth2AuthenticationException(ex.getMessage());
        }
    }
    private String getGitHubEmail(String accessToken) {
        String emailsUrl = "https://api.github.com/user/emails";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<?> entity = new HttpEntity<>(headers);

        try {
            var response = restTemplate.exchange(
                    emailsUrl,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            if (response.getBody() != null && !response.getBody().isEmpty()) {
                // Find primary email first
                Optional<Map<String, Object>> primaryEmail = response.getBody().stream()
                        .filter(email -> Boolean.TRUE.equals(email.get("primary")) &&
                                Boolean.TRUE.equals(email.get("verified")))
                        .findFirst();

                if (primaryEmail.isPresent()) {
                    log.debug("Found primary GitHub email: {}", primaryEmail.get().get("email"));
                    return (String) primaryEmail.get().get("email");
                }

                // If no primary email, get any verified email
                Optional<Map<String, Object>> verifiedEmail = response.getBody().stream()
                        .filter(email -> Boolean.TRUE.equals(email.get("verified")))
                        .findFirst();

                if (verifiedEmail.isPresent()) {
                    log.debug("Found verified GitHub email: {}", verifiedEmail.get().get("email"));
                    return (String) verifiedEmail.get().get("email");
                }

                // Last resort: get any email
                if (!response.getBody().isEmpty()) {
                    String email = (String) response.getBody().get(0).get("email");
                    log.warn("Using unverified GitHub email: {}", email);
                    return email;
                }
            }

            log.error("No email found in GitHub response: {}", response.getBody());
            throw new OAuth2AuthenticationException("No email found for GitHub user");
        } catch (Exception ex) {
            log.error("Error retrieving GitHub emails", ex);
            throw new OAuth2AuthenticationException("Failed to retrieve GitHub user's email: " + ex.getMessage());
        }
    }
}