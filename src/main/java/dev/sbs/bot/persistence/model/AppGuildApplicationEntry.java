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
    name = "discord_guild_application_entries",
    indexes = {
        @Index(
            columnList = "guild_id, application_key, submitter_discord_id",
            unique = true
        ),
        @Index(
            columnList = "guild_id, application_key"
        )
    }
)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class AppGuildApplicationEntry implements JpaModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Setter
    @ManyToOne
    @JoinColumns({
        @JoinColumn(name = "guild_id", referencedColumnName = "guild_id", nullable = false),
        @JoinColumn(name = "application_key", referencedColumnName = "key", nullable = false)
    })
    private AppGuildApplication application;

    @Setter
    @Column(name = "submitter_discord_id")
    private Long submitterDiscordId;

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

        AppGuildApplicationEntry that = (AppGuildApplicationEntry) o;

        return Objects.equals(this.getId(), that.getId())
            && Objects.equals(this.getApplication(), that.getApplication())
            && Objects.equals(this.getSubmitterDiscordId(), that.getSubmitterDiscordId())
            && Objects.equals(this.getUpdatedAt(), that.getUpdatedAt())
            && Objects.equals(this.getSubmittedAt(), that.getSubmittedAt());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getId(), this.getApplication(), this.getSubmitterDiscordId(), this.getUpdatedAt(), this.getSubmittedAt());
    }

}