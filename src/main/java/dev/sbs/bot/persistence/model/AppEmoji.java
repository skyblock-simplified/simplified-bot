package dev.sbs.bot.persistence.model;

import dev.simplified.persistence.JpaModel;
import dev.sbs.discordapi.context.message.ReactionContext;
import dev.sbs.discordapi.response.Emoji;
import discord4j.common.util.Snowflake;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Objects;
import java.util.function.Function;

@Getter
@Entity
@Table(
    name = "discord_emojis",
    indexes = {
        @Index(
            columnList = "guild_id"
        )
    }
)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class AppEmoji implements JpaModel {

    @Id
    @Setter
    @Column(name = "key")
    private String key;

    @Setter
    @Column(name = "name", nullable = false)
    private String name;

    @Setter
    @Column(name = "emoji_id", nullable = false, unique = true)
    private Long emojiId;

    @Setter
    @ManyToOne
    @JoinColumn(name = "guild_id", referencedColumnName = "guild_id", nullable = false)
    private AppGuild guild;

    @Setter
    @Column(name = "animated", nullable = false)
    private boolean animated;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @CreationTimestamp
    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    public @NotNull String getUrl() {
        return Emoji.getUrl(this.getEmojiId(), this.isAnimated());
    }

    public @NotNull Emoji getDiscordEmoji() {
        return this.getDiscordEmoji(null);
    }

    public @NotNull Emoji getDiscordEmoji(@Nullable Function<ReactionContext, Mono<Void>> interaction) {
        return Emoji.of(Snowflake.of(this.getEmojiId()), this.getKey(), this.isAnimated(), interaction);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AppEmoji that = (AppEmoji) o;

        return this.isAnimated() == that.isAnimated()
            && Objects.equals(this.getEmojiId(), that.getEmojiId())
            && Objects.equals(this.getGuild(), that.getGuild())
            && Objects.equals(this.getKey(), that.getKey())
            && Objects.equals(this.getName(), that.getName())
            && Objects.equals(this.getUpdatedAt(), that.getUpdatedAt())
            && Objects.equals(this.getSubmittedAt(), that.getSubmittedAt());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getEmojiId(), this.getGuild(), this.getKey(), this.getName(), this.isAnimated(), this.getUpdatedAt(), this.getSubmittedAt());
    }

}