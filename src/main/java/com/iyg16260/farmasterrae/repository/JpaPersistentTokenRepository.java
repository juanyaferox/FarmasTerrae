package com.iyg16260.farmasterrae.repository;

import com.iyg16260.farmasterrae.model.PersistentLogin;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.util.Date;

@Repository
public class JpaPersistentTokenRepository implements PersistentTokenRepository {
    private final PersistentLoginRepository repository;
    public JpaPersistentTokenRepository(PersistentLoginRepository repository) { this.repository = repository; }

    @Override @Transactional
    public void createNewToken(PersistentRememberMeToken token) {
        PersistentLogin row = new PersistentLogin();
        row.setSeries(token.getSeries()); row.setUsername(token.getUsername());
        row.setTokenValue(token.getTokenValue()); row.setLastUsed(token.getDate().toInstant().atZone(ZoneOffset.UTC).toLocalDateTime());
        repository.save(row);
    }

    @Override @Transactional
    public void updateToken(String series, String tokenValue, Date lastUsed) {
        repository.findById(series).ifPresent(row -> {
            row.setTokenValue(tokenValue);
            row.setLastUsed(lastUsed.toInstant().atZone(ZoneOffset.UTC).toLocalDateTime());
            repository.save(row);
        });
    }

    @Override @Transactional(readOnly = true)
    public PersistentRememberMeToken getTokenForSeries(String series) {
        return repository.findById(series).map(row -> new PersistentRememberMeToken(row.getUsername(), row.getSeries(),
                row.getTokenValue(), Date.from(row.getLastUsed().toInstant(ZoneOffset.UTC)))).orElse(null);
    }

    @Override @Transactional
    public void removeUserTokens(String username) { repository.deleteByUsername(username); }
}
