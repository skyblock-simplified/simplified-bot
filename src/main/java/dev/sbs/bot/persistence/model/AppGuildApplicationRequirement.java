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
    name = "discord_guild_application_requirements",
    indexes = {
        @Index(
            columnList = "guild_id, application_key, requirement_key",
            unique = true
        ),
        @Index(
            columnList = "requirement_key"
        ),
        @Index(
            columnList = "setting_type_key"
        ),
        @Index(
            columnList = "application_key, guild_id"
        ),
    }
)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class AppGuildApplicationRequirement implements JpaModel {

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
    @ManyToOne
    @JoinColumn(name = "requirement_key", referencedColumnName = "key")
    private AppApplicationRequirement requirement;

    @Setter
    @ManyToOne
    @JoinColumn(name = "setting_type_key", referencedColumnName = "key")
    private AppSettingType type;

    @Setter
    @Column(name = "value", nullable = false)
    private String value;

    @Setter
    @Column(name = "description")
    private String description;

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

        AppGuildApplicationRequirement that = (AppGuildApplicationRequirement) o;

        return Objects.equals(this.getId(), that.getId())
            && Objects.equals(this.getApplication(), that.getApplication())
            && Objects.equals(this.getRequirement(), that.getRequirement())
            && Objects.equals(this.getType(), that.getType())
            && Objects.equals(this.getValue(), that.getValue())
            && Objects.equals(this.getDescription(), that.getDescription())
            && Objects.equals(this.getUpdatedAt(), that.getUpdatedAt())
            && Objects.equals(this.getSubmittedAt(), that.getSubmittedAt());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getId(), this.getApplication(), this.getRequirement(), this.getType(), this.getValue(), this.getDescription(), this.getUpdatedAt(), this.getSubmittedAt());
    }

}