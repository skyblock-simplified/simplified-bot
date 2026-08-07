package dev.sbs.bot.persistence.model;

import dev.simplified.persistence.JpaModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.Objects;

@Getter
@Entity
@Table(
    name = "discord_sbs_legacy_donors"
)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class SbsLegacyDonor implements JpaModel {

    @Id
    @Setter
    @Column(name = "discord_id", nullable = false, unique = true)
    private Long discordId;

    @Setter
    @Column(name = "amount", nullable = false)
    private Double amount;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @CreationTimestamp
    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SbsLegacyDonor that = (SbsLegacyDonor) o;

        return Objects.equals(this.getDiscordId(), that.getDiscordId())
            && Objects.equals(this.getAmount(), that.getAmount())
            && Objects.equals(this.getUpdatedAt(), that.getUpdatedAt())
            && Objects.equals(this.getSubmittedAt(), that.getSubmittedAt());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getDiscordId(), this.getAmount(), this.getUpdatedAt(), this.getSubmittedAt());
    }

}