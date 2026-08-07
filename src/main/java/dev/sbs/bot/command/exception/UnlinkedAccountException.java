package dev.sbs.bot.command.exception;

import dev.sbs.discordapi.command.exception.CommandException;

/**
 * {@link UnlinkedAccountException UnlinkedAccountExceptions} are thrown when the user has not linked their Hypixel Account.
 */
public class UnlinkedAccountException extends CommandException {

    public UnlinkedAccountException() {
        super("You must be verified to run this command.");
    }

}
