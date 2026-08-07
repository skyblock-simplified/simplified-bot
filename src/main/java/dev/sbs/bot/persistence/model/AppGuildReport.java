package dev.sbs.bot.persistence.model;

import dev.simplified.persistence.JpaModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Getter
@Entity
@Table(
    name = "discord_guild_reports",
    indexes = {
        @Index(
            columnList = "guild_id, report_type_key"
        )
    }
)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class AppGuildReport implements JpaModel {

    @Id
    @Setter
    @ManyToOne
    @JoinColumns({
        @JoinColumn(name = "guild_id", nullable = false, referencedColumnName = "guild_id"),
        @JoinColumn(name = "report_type_key", nullable = false, referencedColumnName = "key")
    })
    private AppGuildReportType type;

    @Setter
    @Column(name = "reported_discord_id")
    private Long reportedDiscordId;

    @Setter
    @Column(name = "reported_mojang_uuid")
    private String reportedMojangUniqueId;

    @Setter
    @Column(name = "submitter_discord_id", nullable = false)
    private Long submitterDiscordId;

    @Setter
    @Column(name = "assignee_discord_id")
    private Long assigneeDiscordId;

    @Setter
    @Column(name = "reason")
    private String reason;

    @Setter
    @Column(name = "proof")
    private String proof;

    @Setter
    @Column(name = "media_links", nullable = false)
    private List<String> mediaLinks;

    @Setter
    @Column(name = "notes")
    private String notes;

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

        AppGuildReport that = (AppGuildReport) o;

        return Objects.equals(this.getType(), that.getType())
            && Objects.equals(this.getReportedDiscordId(), that.getReportedDiscordId())
            && Objects.equals(this.getReportedMojangUniqueId(), that.getReportedMojangUniqueId())
            && Objects.equals(this.getSubmitterDiscordId(), that.getSubmitterDiscordId())
            && Objects.equals(this.getAssigneeDiscordId(), that.getAssigneeDiscordId())
            && Objects.equals(this.getReason(), that.getReason())
            && Objects.equals(this.getProof(), that.getProof())
            && Objects.equals(this.getMediaLinks(), that.getMediaLinks())
            && Objects.equals(this.getNotes(), that.getNotes())
            && Objects.equals(this.getUpdatedAt(), that.getUpdatedAt())
            && Objects.equals(this.getSubmittedAt(), that.getSubmittedAt());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getType(), this.getReportedDiscordId(), this.getReportedMojangUniqueId(), this.getSubmitterDiscordId(), this.getAssigneeDiscordId(), this.getReason(), this.getProof(), this.getMediaLinks(), this.getNotes(), this.getUpdatedAt(), this.getSubmittedAt());
    }

}