package com.adib.fintrack.service;

import com.adib.fintrack.entity.ProfileEntity;
import com.adib.fintrack.repository.ProfileRepository;
import com.adib.fintrack.security.AppUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {
    private final ProfileRepository profileRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        ProfileEntity existingProfile = profileRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Profile not found with email: " + email));
        return new AppUserDetails(
                existingProfile.getEmail(),
                existingProfile.getPassword(),
                existingProfile.getPasswordChangedAt()
        );
    }
}