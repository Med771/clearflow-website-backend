package backend.website.clearflow.logic.profile;

import backend.website.clearflow.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "seller_profiles")
public class SellerProfileEntity extends BaseEntity {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "company_name")
    private String companyName;

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
}
