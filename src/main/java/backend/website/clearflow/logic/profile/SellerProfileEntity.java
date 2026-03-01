package backend.website.clearflow.logic.profile;

import backend.website.clearflow.logic.profile.verification.SellerVerificationStatus;
import backend.website.clearflow.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "seller_profiles")
@NoArgsConstructor
public class SellerProfileEntity extends BaseEntity {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "contact_phone", length = 30)
    private String contactPhone;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "inn", length = 12)
    private String inn;

    @Column(name = "bik", length = 9)
    private String bik;

    @Column(name = "settlement_account", length = 20)
    private String settlementAccount;

    @Column(name = "corporate_account", length = 20)
    private String corporateAccount;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "ozon_seller_link", length = 500)
    private String ozonSellerLink;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    private SellerVerificationStatus verificationStatus;

    @Column(name = "verification_comment", length = 2000)
    private String verificationComment;

    @Column(name = "verification_submitted_at")
    private Instant verificationSubmittedAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "verified_by")
    private UUID verifiedBy;

    public SellerProfileEntity(
            UUID userId,
            String fullName,
            String contactPhone,
            String companyName,
            String inn,
            String bankName,
            String bik,
            String settlementAccount,
            String corporateAccount,
            String address,
            String ozonSellerLink
    ) {
        this.userId = userId;
        this.fullName = normalizeNullableText(fullName);
        this.contactPhone = normalizeNullableText(contactPhone);
        this.companyName = normalizeNullableText(companyName);
        this.inn = normalizeNullableText(inn);
        this.bankName = normalizeNullableText(bankName);
        this.bik = normalizeNullableText(bik);
        this.settlementAccount = normalizeNullableText(settlementAccount);
        this.corporateAccount = normalizeNullableText(corporateAccount);
        this.address = normalizeNullableText(address);
        this.ozonSellerLink = normalizeNullableText(ozonSellerLink);
        this.verificationStatus = SellerVerificationStatus.PENDING;
        this.verificationComment = "Profile is waiting for admin review";
        this.verificationSubmittedAt = Instant.now();
    }
}
