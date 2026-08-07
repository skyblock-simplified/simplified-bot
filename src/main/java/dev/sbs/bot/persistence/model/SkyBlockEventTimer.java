package dev.sbs.bot.persistence.model;

import dev.simplified.persistence.JpaModel;
import dev.sbs.minecraftapi.skyblock.date.Season;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
    name = "discord_skyblock_event_timers",
    indexes = {
        @Index(
            columnList = "event_key"
        )
    }
)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class SkyBlockEventTimer implements JpaModel {

    @Id
    @Setter
    @Column(name = "event_key")
    private SkyBlockEvent event;

    @Setter
    @ManyToOne
    @Column(name = "start_season_key", nullable = false)
    private Season start;

    @Setter
    @Column(name = "start_season_day", nullable = false)
    private Integer startDay;

    @Setter
    @ManyToOne
    @Column(name = "end_season_key", nullable = false)
    private Season end;

    @Setter
    @Column(name = "end_season_day", nullable = false)
    private Integer endDay;

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

        SkyBlockEventTimer that = (SkyBlockEventTimer) o;

        return Objects.equals(this.getEvent(), that.getEvent())
            && Objects.equals(this.getStart(), that.getStart())
            && Objects.equals(this.getStartDay(), that.getStartDay())
            && Objects.equals(this.getEnd(), that.getEnd())
            && Objects.equals(this.getEndDay(), that.getEndDay())
            && Objects.equals(this.getUpdatedAt(), that.getUpdatedAt())
            && Objects.equals(this.getSubmittedAt(), that.getSubmittedAt());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getEvent(), this.getStart(), this.getStartDay(), this.getEnd(), this.getEndDay(), this.getUpdatedAt(), this.getSubmittedAt());
    }

}