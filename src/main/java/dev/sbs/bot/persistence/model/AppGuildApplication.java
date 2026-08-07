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
import java.util.Objects;

@Getter
@Entity
@Table(
    name = "discord_guild_applications",
    indexes = {
        @Index(
            columnList = "guild_id, key",
            unique = true
        ),
        @Index(
            columnList = "guild_id, type_key"
        ),
        @Index(
            columnList = "guild_id, embed_key"
        )
    }
)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class AppGuildApplication implements JpaModel {

    @Id
    @Setter
    @Column(name = "key")
    private String key;

    @Setter
    @Column(name = "name", nullable = false)
    private String name;

    @Id
    @Setter
    @ManyToOne
    @JoinColumn(name = "guild_id", referencedColumnName = "guild_id", nullable = false)
    private AppGuild guild;

    @Setter
    @ManyToOne
    @JoinColumns({
        @JoinColumn(name = "guild_id", referencedColumnName = "guild_id", nullable = false, updatable = false, insertable = false),
        @JoinColumn(name = "type_key", referencedColumnName = "key", nullable = false, updatable = false, insertable = false)
    })
    private AppGuildApplicationType type;

    @Setter
    @ManyToOne
    @JoinColumns({
        @JoinColumn(name = "guild_id", referencedColumnName = "guild_id", nullable = false, updatable = false, insertable = false),
        @JoinColumn(name = "embed_key", referencedColumnName = "key", nullable = false, updatable = false, insertable = false)
    })
    private AppGuildEmbed embed;

    @Setter
    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Setter
    @Column(name = "notes")
    private String notes;

    @Setter
    @Column(name = "live_at")
    private Instant liveAt;

    @Setter
    @Column(name = "close_at")
    private Instant closeAt;

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

        AppGuildApplication that = (AppGuildApplication) o;

        return this.isEnabled() == that.isEnabled()
            && Objects.equals(this.getKey(), that.getKey())
            && Objects.equals(this.getName(), that.getName())
            && Objects.equals(this.getGuild(), that.getGuild())
            && Objects.equals(this.getType(), that.getType())
            && Objects.equals(this.getEmbed(), that.getEmbed())
            && Objects.equals(this.getNotes(), that.getNotes())
            && Objects.equals(this.getLiveAt(), that.getLiveAt())
            && Objects.equals(this.getCloseAt(), that.getCloseAt())
            && Objects.equals(this.getUpdatedAt(), that.getUpdatedAt())
            && Objects.equals(this.getSubmittedAt(), that.getSubmittedAt());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getKey(), this.getName(), this.getGuild(), this.getType(), this.getEmbed(), this.isEnabled(), this.getNotes(), this.getLiveAt(), this.getCloseAt(), this.getUpdatedAt(), this.getSubmittedAt());
    }

}