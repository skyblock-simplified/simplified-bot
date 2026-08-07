package dev.sbs.bot.persistence.model;

import dev.simplified.persistence.JpaModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
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
    name = "discord_guild_skyblock_events",
    indexes = {
        @Index(
            columnList = "guild_id, event_key",
            unique = true
        ),
        @Index(
            columnList = "guild_id"
        ),
        @Index(
            columnList = "event_key"
        )
    }
)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class AppGuildSkyBlockEvent implements JpaModel {

    @Id
    @Setter
    @ManyToOne
    @JoinColumn(name = "guild_id", referencedColumnName = "guild_id")
    private AppGuild guild;

    @Id
    @Setter
    @ManyToOne
    @JoinColumn(name = "event_key", referencedColumnName = "key")
    private SkyBlockEvent event;

    @Setter
    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Setter
    @Column(name = "mention_roles", nullable = false)
    private List<Long> mentionRoles;

    @Setter
    @Column(name = "webhook_url")
    private String webhookUrl;

    @Setter
    @Column(name = "channel_id")
    private Long channelId;

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

        AppGuildSkyBlockEvent that = (AppGuildSkyBlockEvent) o;

        return this.isEnabled() == that.isEnabled()
            && Objects.equals(this.getGuild(), that.getGuild())
            && Objects.equals(this.getEvent(), that.getEvent())
            && Objects.equals(this.getMentionRoles(), that.getMentionRoles())
            && Objects.equals(this.getWebhookUrl(), that.getWebhookUrl())
            && Objects.equals(this.getChannelId(), that.getChannelId())
            && Objects.equals(this.getUpdatedAt(), that.getUpdatedAt())
            && Objects.equals(this.getSubmittedAt(), that.getSubmittedAt());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getGuild(), this.getEvent(), this.isEnabled(), this.getMentionRoles(), this.getWebhookUrl(), this.getChannelId(), this.getUpdatedAt(), this.getSubmittedAt());
    }

}