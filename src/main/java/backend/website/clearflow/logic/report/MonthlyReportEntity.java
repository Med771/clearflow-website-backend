package backend.website.clearflow.logic.report;

import backend.website.clearflow.model.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "monthly_reports")
@NoArgsConstructor
public class MonthlyReportEntity extends BaseEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "seller_id", nullable = false, updatable = false)
    private UUID sellerId;

    @Column(name = "report_month", nullable = false, updatable = false, length = 7)
    private String reportMonth;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Column(name = "pdf_content", nullable = false, columnDefinition = "bytea")
    private byte[] pdfContent;
}
