package dev.sbs.bot.persistence.model;

import dev.simplified.persistence.JpaModel;
import dev.sbs.minecraftapi.persistence.model.Item;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
import java.util.Map;
import java.util.Objects;

@Getter
@Entity
@Table(
    name = "discord_optimizer_support_items"
)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class OptimizerSupportItem implements JpaModel {

    @Id
    @Setter
    @ManyToOne
    @JoinColumn(name = "item_id", referencedColumnName = "id")
    private Item item;

    @Setter
    @Column(name = "effects", nullable = false)
    private Map<String, Double> effects;

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

        OptimizerSupportItem that = (OptimizerSupportItem) o;

        return Objects.equals(this.getItem(), that.getItem())
            && Objects.equals(this.getEffects(), that.getEffects())
            && Objects.equals(this.getUpdatedAt(), that.getUpdatedAt())
            && Objects.equals(this.getSubmittedAt(), that.getSubmittedAt());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getItem(), this.getEffects(), this.getUpdatedAt(), this.getSubmittedAt());
    }

}