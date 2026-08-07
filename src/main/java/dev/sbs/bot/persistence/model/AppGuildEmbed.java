package dev.sbs.bot.persistence.model;

import dev.simplified.persistence.JpaModel;
import dev.simplified.persistence.converter.ColorConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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

import java.awt.*;
import java.time.Instant;
import java.util.Objects;

@Getter
@Entity
@Table(
    name = "discord_guild_embeds",
    indexes = {
        @Index(
            columnList = "guild_id, key",
            unique = true
        )
    }
)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class AppGuildEmbed implements JpaModel {

    @Id
    @Setter
    @Column(name = "key")
    private String key;

    @Id
    @Setter
    @ManyToOne
    @JoinColumn(name = "guild_id", referencedColumnName = "guild_id")
    private AppGuild guild;

    @Setter
    @Column(name = "title", nullable = false)
    private String title;

    @Setter
    @Column(name = "color", nullable = false)
    @Convert(converter = ColorConverter.class)
    private Color color;

    @Setter
    @Column(name = "url")
    private String url;

    @Setter
    @Column(name = "description")
    private String description;

    @Setter
    @Column(name = "author_name")
    private String authorName;

    @Setter
    @Column(name = "author_url")
    private String authorUrl;

    @Setter
    @Column(name = "author_icon_url")
    private String authorIconUrl;

    @Setter
    @Column(name = "image_url")
    private String imageUrl;

    @Setter
    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Setter
    @Column(name = "video_url")
    private String videoUrl;

    @Setter
    @Column(name = "timestamp")
    private Instant timestamp;

    @Setter
    @Column(name = "footer_text")
    private String footerText;

    @Setter
    @Column(name = "footer_icon_url")
    private String footerIconUrl;

    @Setter
    @Column(name = "notes")
    private String notes;

    @Setter
    @Column(name = "submitter_discord_id")
    private Long submitterDiscordId;

    @Setter
    @Column(name = "editor_discord_id")
    private Long editorDiscordId;

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

        AppGuildEmbed that = (AppGuildEmbed) o;

        return Objects.equals(this.getGuild(), that.getGuild())
            && Objects.equals(this.getKey(), that.getKey())
            && Objects.equals(this.getTitle(), that.getTitle())
            && Objects.equals(this.getColor(), that.getColor())
            && Objects.equals(this.getUrl(), that.getUrl())
            && Objects.equals(this.getDescription(), that.getDescription())
            && Objects.equals(this.getAuthorName(), that.getAuthorName())
            && Objects.equals(this.getAuthorUrl(), that.getAuthorUrl())
            && Objects.equals(this.getAuthorIconUrl(), that.getAuthorIconUrl())
            && Objects.equals(this.getImageUrl(), that.getImageUrl())
            && Objects.equals(this.getThumbnailUrl(), that.getThumbnailUrl())
            && Objects.equals(this.getVideoUrl(), that.getVideoUrl())
            && Objects.equals(this.getTimestamp(), that.getTimestamp())
            && Objects.equals(this.getFooterText(), that.getFooterText())
            && Objects.equals(this.getFooterIconUrl(), that.getFooterIconUrl())
            && Objects.equals(this.getNotes(), that.getNotes())
            && Objects.equals(this.getSubmitterDiscordId(), that.getSubmitterDiscordId())
            && Objects.equals(this.getEditorDiscordId(), that.getEditorDiscordId())
            && Objects.equals(this.getUpdatedAt(), that.getUpdatedAt())
            && Objects.equals(this.getSubmittedAt(), that.getSubmittedAt());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getGuild(), this.getKey(), this.getTitle(), this.getColor(), this.getUrl(), this.getDescription(), this.getAuthorName(), this.getAuthorUrl(), this.getAuthorIconUrl(), this.getImageUrl(), this.getThumbnailUrl(), this.getVideoUrl(), this.getTimestamp(), this.getFooterText(), this.getFooterIconUrl(), this.getNotes(), this.getSubmitterDiscordId(), this.getEditorDiscordId(), this.getUpdatedAt(), this.getSubmittedAt());
    }

}