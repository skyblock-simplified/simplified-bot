package dev.sbs.bot.processor.resource;

import dev.sbs.minecraftapi.client.hypixel.response.resource.ResourceSkills;
import dev.sbs.bot.processor.Processor;

/**
 * Syncs Hypixel resource API skill data into the database.
 * <p>
 * Currently stubbed - SQL write models have been removed and need to be replaced
 * with the new JSON-backed model layer.
 */
@SuppressWarnings("all")
public class ResourceSkillsProcessor extends Processor<ResourceSkills> {

    public ResourceSkillsProcessor(ResourceSkills resourceResponse) {
        super(resourceResponse);
    }

    @Override
    public void process() {
        // TODO: Re-implement with new model layer
        this.getLog().warn("ResourceSkillsProcessor is not yet implemented with the new model layer");
    }

}
