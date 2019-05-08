package me.bhop.bjdautilities.pagination;

import java.awt.*;
import java.util.LinkedList;
import java.util.List;

public class PageBuilder {

    private String title, titleUrl, footer, avatarUrl;
    private CharSequence description;
    private Color color;
    private boolean includeTimestamp;
    private int entryLimit = 6;
    private final List<Page.Entry> contents = new LinkedList<>();

    /**
     * Creates an PageBuilder to be used to creates a page to add to a {@link PaginationEmbed}
     */
    public PageBuilder() {}

    /**
     * Sets the title of the page.
     *
     * @param title the string used for the title
     */
    public PageBuilder setTitle(String title) {
        return setTitle(title, null);
    }

    /**
     * Sets the title of the page and makes it a clickable link.
     *
     * @param title the string used for the title
     * @param titleUrl a url to be sent to when clicked
     */
    public PageBuilder setTitle(String title, String titleUrl) {
        this.title = title;
        this.titleUrl = titleUrl;
        return this;
    }

    /**
     * Sets the description of the page.
     *
     * @param description the string used for the description
     */
    public PageBuilder setDescription(CharSequence description) {
        this.description = description;
        return this;
    }

    /**
     * Sets the color of the page.
     *
     * @param color the color used for that page
     */
    public PageBuilder setColor(Color color) {
        this.color = color;
        return this;
    }

    /**
     * Sets the footer of the page.
     *
     * @param footer the string used for the footer
     */
    public PageBuilder setFooter(String footer) {
        return setFooter(footer, null);
    }

    /**
     * Sets the footer of the page and appends a small image next to it.
     *
     * @param footer the string used for the footer
     * @param avatarUrl a url to an image to be used for the avatar in the footer
     */
    public PageBuilder setFooter(String footer, String avatarUrl) {
        this.footer = footer;
        this.avatarUrl = avatarUrl;
        return this;
    }

    /**
     * Sets whether or not to include a timestamp on the page.
     *
     * @param includeTimestamp the true of false that says to include or exclude
     */
    public PageBuilder includeTimestamp(boolean includeTimestamp) {
        this.includeTimestamp = includeTimestamp;
        return this;
    }

    /**
     * Sets how many entries are allowed per page.
     *
     * @param entryLimit the int of how many entries
     */
    public PageBuilder setEntryLimit(int entryLimit) {
        this.entryLimit = entryLimit;
        return this;
    }

    /**
     * Adds an {@link me.bhop.bjdautilities.pagination.Page.Entry} to the page.
     *
     * Useful if you already have an entry you would like to add again.
     *
     * @param content the entry to add to the page
     */
    public PageBuilder addContent(Page.Entry content) {
        return addContent(content.isInline(), content.getTitle(), content.getLines());
    }

    /**
     * Adds an {@link me.bhop.bjdautilities.pagination.Page.Entry} to the page.
     *
     * @param inline whether to have it on the same line as another entry. Only effective with multiple entries.
     * @param title the title of the entry
     * @param lines lines to add to the entry
     */
    public PageBuilder addContent(boolean inline, String title, String... lines) {
        if (this.contents.size() > entryLimit) {
            throw new IllegalStateException("Cannot add more then the entry limits allow!");
        } else {
            this.contents.add(new Page.Entry(inline, title, lines));
        }
        return this;
    }

    /**
     * Builds the {@link Page}
     *
     * @return the compiled page
     */
    public Page build() {
       return new Page(title, titleUrl, description, color, footer, avatarUrl, includeTimestamp, entryLimit, contents);
    }

}
