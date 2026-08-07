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
import java.util.Objects;

@Getter
@Entity
@Table(
    name = "discord_skyblock_events",
    indexes = {
        @Index(
            columnList = "emoji_key"
        )
    }
)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class SkyBlockEvent implements JpaModel {

    @Id
    @Setter
    @Column(name = "key")
    private String key;

    @Setter
    @Column(name = "name", nullable = false)
    private String name;

    @Setter
    @ManyToOne
    @JoinColumn(name = "emoji_key", referencedColumnName = "key")
    private AppEmoji botEmoji;

    @Setter
    @Column(name = "description", nullable = false)
    private String description;

    @Setter
    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Setter
    @Column(name = "status")
    private String status;

    @Setter
    @Column(name = "interval_expression")
    private String intervalExpression;

    @Setter
    @Column(name = "thirdparty_json_url")
    private String thirdPartyJsonUrl;

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

        SkyBlockEvent that = (SkyBlockEvent) o;

        return this.isEnabled() == that.isEnabled()
            && Objects.equals(this.getKey(), that.getKey())
            && Objects.equals(this.getName(), that.getName())
            && Objects.equals(this.getBotEmoji(), that.getBotEmoji())
            && Objects.equals(this.getDescription(), that.getDescription())
            && Objects.equals(this.getStatus(), that.getStatus())
            && Objects.equals(this.getIntervalExpression(), that.getIntervalExpression())
            && Objects.equals(this.getThirdPartyJsonUrl(), that.getThirdPartyJsonUrl())
            && Objects.equals(this.getUpdatedAt(), that.getUpdatedAt())
            && Objects.equals(this.getSubmittedAt(), that.getSubmittedAt());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getKey(), this.getName(), this.getBotEmoji(), this.getDescription(), this.isEnabled(), this.getStatus(), this.getIntervalExpression(), this.getThirdPartyJsonUrl(), this.getUpdatedAt(), this.getSubmittedAt());
    }

}