package ru.haritonenko.userservice.consent.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.haritonenko.userservice.consent.db.entity.UserConsentEntity;
import ru.haritonenko.userservice.consent.db.repository.UserConsentRepository;
import ru.haritonenko.userservice.domain.db.entity.UserEntity;

@Service
@RequiredArgsConstructor
public class UserConsentService {

    private final UserConsentRepository repository;

    @Value("${app.personal-data.policy-version}")
    private String policyVersion;

    @Value("${app.personal-data.privacy-policy-version}")
    private String privacyPolicyVersion;

    public void recordRegistrationConsents(UserEntity user) {
        repository.save(UserConsentEntity.builder()
                .user(user)
                .consentType("PERSONAL_DATA_PROCESSING")
                .version(policyVersion)
                .build());
        repository.save(UserConsentEntity.builder()
                .user(user)
                .consentType("PRIVACY_POLICY")
                .version(privacyPolicyVersion)
                .build());
    }
}
