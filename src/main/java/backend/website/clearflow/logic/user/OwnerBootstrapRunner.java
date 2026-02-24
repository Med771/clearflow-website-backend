package backend.website.clearflow.logic.user;

import backend.website.clearflow.config.property.OwnerProperties;
import backend.website.clearflow.model.UserRole;
import lombok.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OwnerBootstrapRunner implements ApplicationRunner {

    private final UserRepository userRepository;
    private final OwnerProperties ownerProperties;
    private final PasswordEncoder passwordEncoder;

    public OwnerBootstrapRunner(
            UserRepository userRepository,
            OwnerProperties ownerProperties,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.ownerProperties = ownerProperties;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(@NonNull ApplicationArguments args) {
        UserEntity owner = userRepository.findByRole(UserRole.OWNER).orElseGet(UserEntity::new);
        owner.setRole(UserRole.OWNER);
        owner.setEmail(ownerProperties.email().trim().toLowerCase());
        owner.setPassword(passwordEncoder.encode(ownerProperties.password()));
        owner.setBlock(false);
        owner.setActive(true);
        owner.setParentId(null);
        userRepository.save(owner);
    }
}
