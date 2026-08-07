package dev.sbs.bot.processor.resource;

import dev.sbs.minecraftapi.client.hypixel.response.resource.ResourceCollections;
import dev.sbs.bot.processor.Processor;

/**
 * Syncs Hypixel resource API collection data into the database.
 * <p>
 * Currently stubbed - SQL write models have been removed and need to be replaced
 * with the new JSON-backed model layer.
 */
@SuppressWarnings("all")
public class ResourceCollectionsProcessor extends Processor<ResourceCollections> {

    public ResourceCollectionsProcessor(ResourceCollections resourceResponse) {
        super(resourceResponse);
    }

    @Override
    public void process() {
        // TODO: Re-implement with new model layer
        this.getLog().warn("ResourceCollectionsProcessor is not yet implemented with the new model layer");
    }

}
