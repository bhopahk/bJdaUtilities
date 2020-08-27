package me.bhop.bjdautilities.pagination;

import me.bhop.bjdautilities.ReactionMenu;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.LinkedList;
import java.util.List;

public class PaginationEmbed {

    private final JDA jda;
    private ReactionMenu reactionMenu = null;
    private final String prevEmote, forwardEmote, removeEmote;
    private final List<Page> pages;
    private int currentPage = 0;

    /**
     * It is highly recommended to use a {@link Builder}.
     */
    public PaginationEmbed(JDA jda, String prevEmote, String forwardEmote, String removeEmote, List<Page> pages) {
        this.jda = jda;
        this.prevEmote = prevEmote;
        this.forwardEmote = forwardEmote;
        this.removeEmote = removeEmote;
        this.pages = pages;
        currentPage = 0;
        if (pages.isEmpty())
            throw new IllegalStateException("You cannot have an empty PaginationEmbed!");
        ReactionMenu.Builder reactionBuilder = new ReactionMenu.Builder(jda);
        reactionBuilder.setEmbed(pages.get(currentPage).getGeneratedPage());
        reactionBuilder.onClick(removeEmote, ReactionMenu::destroy);
        if (pages.size() > 1) {
            //Prev
            reactionBuilder.onClick(prevEmote, (prev, user) -> {
                if (currentPage == 0)
                    return;
                currentPage--;
                prev.getMessage().setContent(pages.get(currentPage).getGeneratedPage());
            });
            //Forward
            reactionBuilder.onClick(forwardEmote, (forward, user) -> {
                if (currentPage == pages.size())
                    return;
                currentPage++;
                forward.getMessage().setContent(pages.get(currentPage).getGeneratedPage());
            });
        }
        reactionMenu = reactionBuilder.build();
    }

    /**
     * Display this menu in a channel. It must not have been displayed yet.
     *
     * @param channel the channel to create the menu in
     * @throws IllegalStateException if the menu has already been displayed
     */

    public void display(MessageChannel channel) {
        if (reactionMenu.getMessage() != null)
            throw new IllegalStateException("This embed has already been displayed!");
        reactionMenu.display(channel);
    }
    /**
     * Deletes this menu immediately.
     */
    public void delete() {
        deleteIn(0);
    }

    /**
     * Deletes this menu after a given number of seconds.
     *
     * @param seconds time
     */
    public void deleteIn(int seconds) {
        if (reactionMenu.getMessage() == null)
            return;
        reactionMenu.destroyIn(seconds);
    }

    /**
     * Get the underlying {@link ReactionMenu} for this menu.
     *
     * @return the underlying reaction menu
     */
    public ReactionMenu getReactionMenu() {
        return reactionMenu;
    }

    /**
     * A convenient builder for creating {@link PaginationEmbed}s.
     */
    public static class Builder {

        private final JDA jda;
        private String prevEmote = "\u25C0", forwardEmote = "\u25B6", removeEmote = "\u274C";
        private final List<Page> pages = new LinkedList<>();

        /**
         * Create a new builder instance.
         *
         * @param jda the {@link JDA} instance
         */
        public Builder(JDA jda) {
            this.jda = jda;
        }

        /**
         * Ads a page to the list of pages available for the {@link PaginationEmbed} to show.
         *
         * @param page the page
         */

        public Builder addPage(Page page) {
            this.pages.add(page);
            return this;
        }

        /**
         * Sets the emote used to go back a page.
         *
         * This should be either the unicode of the emote (non escaped) or the string name (without :) for a server emote.
         *
         * @param prevEmote the emote name
         */
        public Builder setPrevEmote(String prevEmote) {
            this.prevEmote = prevEmote;
            return this;
        }

        /**
         * Sets the emote used to go forward a page.
         *
         * This should be either the unicode of the emote (non escaped) or the string name (without :) for a server emote.
         *
         * @param forwardEmote the emote name
         */
        public Builder setForwardEmote(String forwardEmote) {
            this.forwardEmote = forwardEmote;
            return this;
        }

        /**
         * Sets the emote used to remove the embed.
         *
         * This should be either the unicode of the emote (non escaped) or the string name (without :) for a server emote.
         *
         * @param removeEmote the emote name
         */
        public Builder setRemoveEmote(String removeEmote) {
            this.removeEmote = removeEmote;
            return this;
        }

        /**
         * Build the {@link PaginationEmbed} to be displayed at a later point.
         *
         * @return the compiled menu
         */
        public PaginationEmbed build() {
            return new PaginationEmbed(jda, prevEmote, forwardEmote, removeEmote, pages);
        }

        /**
         * Build the {@link PaginationEmbed} and display it immediately
         *
         * @param channel the channel to display in
         * @return the compiled menu
         */
        public PaginationEmbed buildAndDisplay(MessageChannel channel) {
            PaginationEmbed paginationEmbed = build();
            paginationEmbed.display(channel);
            return paginationEmbed;
        }
    }
}
