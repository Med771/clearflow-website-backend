package backend.website.clearflow.logic.user;

import backend.website.clearflow.model.BaseEntity;
import backend.website.clearflow.model.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "users")
@NoArgsConstructor
public class UserEntity extends BaseEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(nullable = false, length = 200)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(name = "is_block", nullable = false)
    private boolean isBlock;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "session_version", nullable = false)
    private long sessionVersion;

    @Column(name = "ozon_api_key_ciphertext", length = 2048)
    private String ozonApiKeyCiphertext;

    @Column(name = "ozon_api_key_key_version", length = 50)
    private String ozonApiKeyKeyVersion;

    @Column(name = "ozon_client_id", length = 100)
    private String ozonClientId;

    public UserEntity(
            String email, String password,
            UserRole role, boolean isActive, boolean isBlock,
            long sessionVersion, String ozonClientId
    ) {
        this.email = email;
        this.password = password;
        this.role = role;
        this.isActive = isActive;
        this.isBlock = isBlock;
        this.sessionVersion = sessionVersion;
        this.ozonClientId = this.normalizeNullableText(ozonClientId);
    }

    public UserEntity(
            String email, String password,
            UserRole role, boolean isActive, boolean isBlock,
            long sessionVersion,  UUID parentId
    ) {
        this.email = email;
        this.password = password;
        this.role = role;
        this.isActive = isActive;
        this.isBlock = isBlock;
        this.sessionVersion = sessionVersion;
        this.parentId = parentId;
    }
}
