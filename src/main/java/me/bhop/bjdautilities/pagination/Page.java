package me.bhop.bjdautilities.pagination;

import me.bhop.bjdautilities.ReactionMenu;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;

import java.awt.*;
import java.time.Instant;
import java.util.List;

public class Page {

    private final String title, titleUrl, footer, avatarUrl;
    private final CharSequence description;
    private final Color color;
    private final boolean includeTimestamp;
    private final List<Entry> contents;
    private final int entryLimit;

    /**
     * Cannot be accessed outside the package. Should be created using a {@link PageBuilder} instead.
     */
    Page(String title, String titleUrl, CharSequence description, Color color, String footer, String avatarUrl, boolean includeTimestamp, int entryLimit, List<Entry> contents) {
        this.title = title;
        this.titleUrl = titleUrl;
        this.description = description;
        this.color = color;
        this.footer = footer;
        this.avatarUrl = avatarUrl;
        this.includeTimestamp = includeTimestamp;
        this.contents = contents;
        this.entryLimit = entryLimit;
    }

    /**
     * Gets the title of the page.
     *
     * @return the title string
     */
    public String getTitle() {
        return title;
    }

    /**
     * Gets the title url of the page.
     *
     * @return the titles url string
     */
    public String getTitleUrl() {
        return titleUrl;
    }

    /**
     * Gets the color of the page.
     *
     * @return the color
     */
    public Color getColor() {
        return color;
    }

    /**
     * Gets the footer of the page.
     *
     * @return the footer string
     */
    public String getFooter() {
        return footer;
    }

    /**
     * Gets the avatarUrl of the page.
     *
     * @return the url used for the avatar
     */
    public String getAvatarUrl() {
        return avatarUrl;
    }

    /**
     * Gets whether the timestamp was included on the page.
     *
     * @return the boolean value
     */
    public boolean includeTimestamp() {
        return includeTimestamp;
    }

    /**
     * Gets the description of the page.
     *
     * @return the description
     */
    public CharSequence getDescription() {
        return description;
    }

    /**
     * Gets the limit of entries allowed on the page.
     *
     * @return the int value
     */
    public int getEntryLimit() {
        return entryLimit;
    }

    /**
     * Gets the list of entries of the page.
     *
     * @return the list of entries
     */
    public List<Entry> getContents() {
        return contents;
    }

    /**
     * Gets the page generated as an {@link MessageEmbed}.
     *
     * Cannot be accessed outside the package.
     *
     * {@link MessageEmbed}s are the foundation behind the {@link PaginationEmbed}
     *
     * @return the embed used as the page
     */
    MessageEmbed getGeneratedPage() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle(getTitle(), getTitleUrl());
        embedBuilder.setDescription(getDescription());
        embedBuilder.setColor(getColor());
        embedBuilder.setFooter(getFooter(), getAvatarUrl());
        if (includeTimestamp()) {
            embedBuilder.setTimestamp(Instant.now());
        }
        getContents().forEach(entry -> {
            StringBuilder builder = new StringBuilder();
            for (String str : entry.getLines()) {
                builder.append(str).append("\n");
            }
            embedBuilder.addField(entry.getTitle(), builder.toString(), entry.isInline());
        });
        return embedBuilder.build();
    }

    public static class Entry {

        protected final String title;
        protected final String[] lines;
        protected final boolean inline;

        /**
         * Used in pages. Recommended to use {@link PageBuilder}
         *
         * @param inline whether to have it on the same line as another entry. Only effective with multiple entries.
         * @param title the title of the entry
         * @param lines lines to add to the entry
         */
        public Entry(boolean inline, String title, String... lines ) {
            this.title = title;
            this.lines = lines;
            this.inline = inline;
        }

        /**
         * Gets the title of the entry
         *
         * @return the title string
         */
        public String getTitle() {
            return title;
        }

        /**
         * Gets the lines of the entry
         *
         * @return the strings set as lines
         */
        public String[] getLines() {
            return lines;
        }

        /**
         * Gets whether to make the entry inline or not
         *
         * @return the boolean value
         */
        public boolean isInline() {
            return inline;
        }
    }
}
