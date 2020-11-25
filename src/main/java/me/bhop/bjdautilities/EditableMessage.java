package me.bhop.bjdautilities;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.api.requests.restaction.pagination.ReactionPaginationAction;
import org.apache.commons.collections4.Bag;
import org.jetbrains.annotations.NotNull;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.Formatter;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class EditableMessage implements Message {
    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);

    public static EditableMessage wrap(Message message) {
        return new EditableMessage(message);
    }

    private Message delegate;
    private ScheduledFuture<?> task = null;

    private EditableMessage(Message message) {
        this.delegate = message;
    }

    /**
     * Update the textual content of the message.
     *
     * @param content the new content
     */
    public void setContent(String content) {
        delegate.editMessage(new MessageBuilder(content).build()).queue();
    }

    /**
     * Update the embed content of the message.
     *
     * @param content the new content
     */
    public void setContent(MessageEmbed content) {
        delegate.editMessage(content).queue();
    }

    /**
     * Enable an updater for the textual content of the message on a fixed timer.
     *
     * It is recommended to keep the interval at least 5 seconds due to Discord rate limiting.
     *
     * @param updater a supplier of the new content
     * @param interval the interval of the update, in seconds
     */
    public void setTextRepeat(Supplier<String> updater, int interval) {
        if (task == null)
            task = executor.scheduleAtFixedRate(() -> setContent(updater.get()), interval, interval, TimeUnit.SECONDS);
        else throw new UnsupportedOperationException("You may only run one updater at a time!");
    }

    /**
     * Enable an updater for the embed content of the message on a fixed timer.
     *
     * It is recommended to keep the interval at least 5 seconds due to Discord rate limiting.
     *
     * @param updater a supplier of the new content
     * @param interval the interval of the update, in seconds
     */
    public void setEmbedRepeat(Supplier<MessageEmbed> updater, int interval) {
        if (task == null)
            task = executor.scheduleAtFixedRate(() -> setContent(updater.get()), interval, interval, TimeUnit.SECONDS);
        else throw new UnsupportedOperationException("You may only run one updater at a time!");
    }

    /**
     * Refresh the delegate instance.
     *
     * This is typically useful for updating which reactions have been applied to the message.
     *
     * @param jda the {@link JDA} instance
     */
    public void refreshMessage(JDA jda) {
        delegate = jda.getTextChannelById(delegate.getChannel().getIdLong()).retrieveMessageById(delegate.getIdLong()).complete();
    }

    /**
     * Cancels the content updater, if it is running.
     */
    public void cancelUpdater() {
        if (task != null)
            task.cancel(true);
        task = null;
    }

    // Below is all delegate methods...


    @Override
    @Nonnull
    public List<User> getMentionedUsers() {
        return delegate.getMentionedUsers();
    }

    @Override
    @Nonnull
    public Bag<User> getMentionedUsersBag() {
        return delegate.getMentionedUsersBag();
    }

    @Override
    @Nonnull
    public List<TextChannel> getMentionedChannels() {
        return delegate.getMentionedChannels();
    }

    @Override
    @Nonnull
    public Bag<TextChannel> getMentionedChannelsBag() {
        return delegate.getMentionedChannelsBag();
    }

    @Override
    @Nonnull
    public List<Role> getMentionedRoles() {
        return delegate.getMentionedRoles();
    }

    @Override
    @Nonnull
    public Bag<Role> getMentionedRolesBag() {
        return delegate.getMentionedRolesBag();
    }

    @Override
    @Nonnull
    public List<Member> getMentionedMembers(@NotNull Guild guild) {
        return delegate.getMentionedMembers(guild);
    }

    @Override
    @Nonnull
    public List<Member> getMentionedMembers() {
        return delegate.getMentionedMembers();
    }

    @Override
    @Nonnull
    public List<IMentionable> getMentions(@NotNull MentionType... types) {
        return delegate.getMentions(types);
    }

    @Override
    public boolean isMentioned(@NotNull IMentionable mentionable, @NotNull MentionType... types) {
        return delegate.isMentioned(mentionable, types);
    }

    @Override
    public boolean mentionsEveryone() {
        return delegate.mentionsEveryone();
    }

    @Override
    public boolean isEdited() {
        return delegate.isEdited();
    }

    @Override
    @Nullable
    public OffsetDateTime getTimeEdited() {
        return delegate.getTimeEdited();
    }

    @Override
    @Nonnull
    public User getAuthor() {
        return delegate.getAuthor();
    }

    @Override
    @Nullable
    public Member getMember() {
        return delegate.getMember();
    }

    @Override
    @Nonnull
    public String getJumpUrl() {
        return delegate.getJumpUrl();
    }

    @Override
    @Nonnull
    public String getContentDisplay() {
        return delegate.getContentDisplay();
    }

    @Override
    @Nonnull
    public String getContentRaw() {
        return delegate.getContentRaw();
    }

    @Override
    @Nonnull
    public String getContentStripped() {
        return delegate.getContentStripped();
    }

    @Override
    @Nonnull
    public List<String> getInvites() {
        return delegate.getInvites();
    }

    @Override
    @Nullable
    public String getNonce() {
        return delegate.getNonce();
    }

    @Override
    public boolean isFromType(@NotNull ChannelType type) {
        return delegate.isFromType(type);
    }

    @Override
    public boolean isFromGuild() {
        return delegate.isFromGuild();
    }

    @Override
    @Nonnull
    public ChannelType getChannelType() {
        return delegate.getChannelType();
    }

    @Override
    public boolean isWebhookMessage() {
        return delegate.isWebhookMessage();
    }

    @Override
    @Nonnull
    public MessageChannel getChannel() {
        return delegate.getChannel();
    }

    @Override
    @Nonnull
    public PrivateChannel getPrivateChannel() {
        return delegate.getPrivateChannel();
    }

    @Override
    @Nonnull
    public TextChannel getTextChannel() {
        return delegate.getTextChannel();
    }

    @Override
    @Nullable
    public Category getCategory() {
        return delegate.getCategory();
    }

    @Override
    @Nonnull
    public Guild getGuild() {
        return delegate.getGuild();
    }

    @Override
    @Nonnull
    public List<Attachment> getAttachments() {
        return delegate.getAttachments();
    }

    @Override
    @Nonnull
    public List<MessageEmbed> getEmbeds() {
        return delegate.getEmbeds();
    }

    @Override
    @Nonnull
    public List<Emote> getEmotes() {
        return delegate.getEmotes();
    }

    @Override
    @Nonnull
    public Bag<Emote> getEmotesBag() {
        return delegate.getEmotesBag();
    }

    @Override
    @Nonnull
    public List<MessageReaction> getReactions() {
        return delegate.getReactions();
    }

    @Override
    public boolean isTTS() {
        return delegate.isTTS();
    }

    @Override
    @Nullable
    public MessageActivity getActivity() {
        return delegate.getActivity();
    }

    @Override
    @CheckReturnValue
    @Nonnull
    public MessageAction editMessage(@NotNull CharSequence newContent) {
        return delegate.editMessage(newContent);
    }

    @Override
    @CheckReturnValue
    @Nonnull
    public MessageAction editMessage(@NotNull MessageEmbed newContent) {
        return delegate.editMessage(newContent);
    }

    @Override
    @CheckReturnValue
    @Nonnull
    public MessageAction editMessageFormat(@NotNull String format, @NotNull Object... args) {
        return delegate.editMessageFormat(format, args);
    }

    @Override
    @CheckReturnValue
    @Nonnull
    public MessageAction editMessage(@NotNull Message newContent) {
        return delegate.editMessage(newContent);
    }

    @Override
    @CheckReturnValue
    @Nonnull
    public AuditableRestAction<Void> delete() {
        return delegate.delete();
    }

    @Override
    @Nonnull
    public JDA getJDA() {
        return delegate.getJDA();
    }

    @Override
    public boolean isPinned() {
        return delegate.isPinned();
    }

    @Override
    @CheckReturnValue
    @Nonnull
    public RestAction<Void> pin() {
        return delegate.pin();
    }

    @Override
    @CheckReturnValue
    @Nonnull
    public RestAction<Void> unpin() {
        return delegate.unpin();
    }

    @Override
    @CheckReturnValue
    @Nonnull
    public RestAction<Void> addReaction(@NotNull Emote emote) {
        return delegate.addReaction(emote);
    }

    @Override
    @CheckReturnValue
    @Nonnull
    public RestAction<Void> addReaction(@NotNull String unicode) {
        return delegate.addReaction(unicode);
    }

    @Override
    @CheckReturnValue
    @Nonnull
    public RestAction<Void> clearReactions() {
        return delegate.clearReactions();
    }

    @Override
    @CheckReturnValue
    @Nonnull
    public RestAction<Void> clearReactions(@NotNull String unicode) {
        return delegate.clearReactions(unicode);
    }

    @Override
    @CheckReturnValue
    @Nonnull
    public RestAction<Void> clearReactions(@NotNull Emote emote) {
        return delegate.clearReactions(emote);
    }

    @Override
    @CheckReturnValue
    @Nonnull
    public RestAction<Void> removeReaction(@NotNull Emote emote) {
        return delegate.removeReaction(emote);
    }

    @Override
    @CheckReturnValue
    @Nonnull
    public RestAction<Void> removeReaction(@NotNull Emote emote, @NotNull User user) {
        return delegate.removeReaction(emote, user);
    }

    @Override
    @CheckReturnValue
    @Nonnull
    public RestAction<Void> removeReaction(@NotNull String unicode) {
        return delegate.removeReaction(unicode);
    }

    @Override
    @CheckReturnValue
    @Nonnull
    public RestAction<Void> removeReaction(@NotNull String unicode, @NotNull User user) {
        return delegate.removeReaction(unicode, user);
    }

    @Override
    @CheckReturnValue
    @Nonnull
    public ReactionPaginationAction retrieveReactionUsers(@NotNull Emote emote) {
        return delegate.retrieveReactionUsers(emote);
    }

    @Override
    @CheckReturnValue
    @Nonnull
    public ReactionPaginationAction retrieveReactionUsers(@NotNull String unicode) {
        return delegate.retrieveReactionUsers(unicode);
    }

    @Override
    @CheckReturnValue
    @Nullable
    public MessageReaction.ReactionEmote getReactionByUnicode(@NotNull String unicode) {
        return delegate.getReactionByUnicode(unicode);
    }

    @Override
    @CheckReturnValue
    @Nullable
    public MessageReaction.ReactionEmote getReactionById(@NotNull String id) {
        return delegate.getReactionById(id);
    }

    @Override
    @CheckReturnValue
    @Nullable
    public MessageReaction.ReactionEmote getReactionById(long id) {
        return delegate.getReactionById(id);
    }

    @Override
    @CheckReturnValue
    @Nonnull
    public AuditableRestAction<Void> suppressEmbeds(boolean suppressed) {
        return delegate.suppressEmbeds(suppressed);
    }

    @Override
    @CheckReturnValue
    @Nonnull
    public RestAction<Message> crosspost() {
        return delegate.crosspost();
    }

    @Override
    public boolean isSuppressedEmbeds() {
        return delegate.isSuppressedEmbeds();
    }

    @Override
    @Nonnull
    public EnumSet<MessageFlag> getFlags() {
        return delegate.getFlags();
    }

    @Override
    @Nonnull
    public MessageType getType() {
        return delegate.getType();
    }

    @Override
    public Message getReferencedMessage() {
        return delegate.getReferencedMessage();
    }

    @Override
    @Nonnull
    public String getId() {
        return delegate.getId();
    }

    @Override
    public long getIdLong() {
        return delegate.getIdLong();
    }

    @Override
    @Nonnull
    public OffsetDateTime getTimeCreated() {
        return delegate.getTimeCreated();
    }

    @Override
    public void formatTo(Formatter formatter, int i, int i1, int i2) {
        delegate.formatTo(formatter, i, i1, i2);
    }
}
