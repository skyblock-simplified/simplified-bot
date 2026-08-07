package dev.sbs.bot.persistence;

import dev.simplified.collection.ConcurrentList;
import dev.simplified.persistence.JpaModel;
import dev.simplified.persistence.RepositoryFactory;
import dev.sbs.bot.persistence.model.AppGuild;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * Repository factory for Discord SQL-backed models scoped to the
 * {@link AppGuild} package.
 */
@Getter
public class DiscordFactory implements RepositoryFactory {

    private final @NotNull ConcurrentList<Class<JpaModel>> models = RepositoryFactory.resolveModels(AppGuild.class);

}