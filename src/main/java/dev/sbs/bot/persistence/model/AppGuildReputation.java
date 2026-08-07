package dev.sbs.bot.persistence.model;

import dev.simplified.persistence.JpaModel;
import jakarta.persistence.*;
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
    name = "discord_guild_reputation",
    indexes = {
        @Index(
            columnList = "guild_id, reputation_type_key"
        )
    }
)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class AppGuildReputation implements JpaModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Setter
    @ManyToOne
    @JoinColumns({
        @JoinColumn(name = "guild_id", nullable = false, referencedColumnName = "guild_id"),
        @JoinColumn(name = "reputation_type_key", nullable = false, referencedColumnName = "key")
    })
    private AppGuildReputationType type;

    @Setter
    @Column(name = "receiver_discord_id", nullable = false)
    private Long receiverDiscordId;

    @Setter
    @Column(name = "submitter_discord_id", nullable = false)
    private Long submitterDiscordId;

    @Setter
    @Column(name = "assignee_discord_id")
    private Long assigneeDiscordId;

    @Setter
    @Column(name = "reason")
    private String reason;

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

        AppGuildReputation that = (AppGuildReputation) o;

        return Objects.equals(this.getId(), that.getId())
            && Objects.equals(this.getType(), that.getType())
            && Objects.equals(this.getReceiverDiscordId(), that.getReceiverDiscordId())
            && Objects.equals(this.getSubmitterDiscordId(), that.getSubmitterDiscordId())
            && Objects.equals(this.getAssigneeDiscordId(), that.getAssigneeDiscordId())
            && Objects.equals(this.getReason(), that.getReason())
            && Objects.equals(this.getUpdatedAt(), that.getUpdatedAt())
            && Objects.equals(this.getSubmittedAt(), that.getSubmittedAt());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getId(), this.getType(), this.getReceiverDiscordId(), this.getSubmitterDiscordId(), this.getAssigneeDiscordId(), this.getReason(), this.getUpdatedAt(), this.getSubmittedAt());
    }

}