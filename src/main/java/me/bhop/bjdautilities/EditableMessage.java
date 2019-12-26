package me.bhop.bjdautilities;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.api.requests.restaction.pagination.ReactionPaginationAction;
import org.apache.commons.collections4.Bag;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.OffsetDateTime;
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

    // Delegate Message

    @Nonnull
    @Override
    public List<User> getMentionedUsers() {
        return delegate.getMentionedUsers();
    }

    @Nonnull
    @Override
    public Bag<User> getMentionedUsersBag() {
        return delegate.getMentionedUsersBag();
    }

    @Nonnull
    @Override
    public List<TextChannel> getMentionedChannels() {
        return delegate.getMentionedChannels();
    }

    @Nonnull
    @Override
    public Bag<TextChannel> getMentionedChannelsBag() {
        return delegate.getMentionedChannelsBag();
    }

    @Nonnull
    @Override
    public List<Role> getMentionedRoles() {
        return delegate.getMentionedRoles();
    }

    @Nonnull
    @Override
    public Bag<Role> getMentionedRolesBag() {
        return delegate.getMentionedRolesBag();
    }

    @Nonnull
    @Override
    public List<Member> getMentionedMembers(@Nonnull Guild guild) {
        return delegate.getMentionedMembers(guild);
    }

    @Nonnull
    @Override
    public List<Member> getMentionedMembers() {
        return delegate.getMentionedMembers();
    }

    @Nonnull
    @Override
    public List<IMentionable> getMentions(@Nonnull MentionType... types) {
        return delegate.getMentions(types);
    }

    @Override
    public boolean isMentioned(@Nonnull IMentionable mentionable, @Nonnull MentionType... types) {
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

    @Nullable
    @Override
    public OffsetDateTime getTimeEdited() {
        return delegate.getTimeEdited();
    }

    @Nonnull
    @Override
    public User getAuthor() {
        return delegate.getAuthor();
    }

    @Nullable
    @Override
    public Member getMember() {
        return delegate.getMember();
    }

    @Nonnull
    @Override
    public String getJumpUrl() {
        return delegate.getJumpUrl();
    }

    @Nonnull
    @Override
    public String getContentDisplay() {
        return delegate.getContentDisplay();
    }

    @Nonnull
    @Override
    public String getContentRaw() {
        return delegate.getContentRaw();
    }

    @Nonnull
    @Override
    public String getContentStripped() {
        return delegate.getContentStripped();
    }

    @Nonnull
    @Override
    public List<String> getInvites() {
        return delegate.getInvites();
    }

    @Nullable
    @Override
    public String getNonce() {
        return delegate.getNonce();
    }

    @Override
    public boolean isFromType(@Nonnull ChannelType type) {
        return delegate.isFromType(type);
    }

    @Nonnull
    @Override
    public ChannelType getChannelType() {
        return delegate.getChannelType();
    }

    @Override
    public boolean isWebhookMessage() {
        return delegate.isWebhookMessage();
    }

    @Nonnull
    @Override
    public MessageChannel getChannel() {
        return delegate.getChannel();
    }

    @Nonnull
    @Override
    public PrivateChannel getPrivateChannel() {
        return delegate.getPrivateChannel();
    }

    @Nonnull
    @Override
    public TextChannel getTextChannel() {
        return delegate.getTextChannel();
    }

    @Nullable
    @Override
    public Category getCategory() {
        return delegate.getCategory();
    }

    @Nonnull
    @Override
    public Guild getGuild() {
        return delegate.getGuild();
    }

    @Nonnull
    @Override
    public List<Attachment> getAttachments() {
        return delegate.getAttachments();
    }

    @Nonnull
    @Override
    public List<MessageEmbed> getEmbeds() {
        return delegate.getEmbeds();
    }

    @Nonnull
    @Override
    public List<Emote> getEmotes() {
        return delegate.getEmotes();
    }

    @Nonnull
    @Override
    public Bag<Emote> getEmotesBag() {
        return delegate.getEmotesBag();
    }

    @Nonnull
    @Override
    public List<MessageReaction> getReactions() {
        return delegate.getReactions();
    }

    @Override
    public boolean isTTS() {
        return delegate.isTTS();
    }

    @Nullable
    @Override
    public MessageActivity getActivity() {
        return delegate.getActivity();
    }

    @Nonnull
    @Override
    public MessageAction editMessage(@Nonnull CharSequence newContent) {
        return delegate.editMessage(newContent);
    }

    @Nonnull
    @Override
    public MessageAction editMessage(@Nonnull MessageEmbed newContent) {
        return delegate.editMessage(newContent);
    }

    @Nonnull
    @Override
    public MessageAction editMessageFormat(@Nonnull String format, @Nonnull Object... args) {
        return delegate.editMessageFormat(format, args);
    }

    @Nonnull
    @Override
    public MessageAction editMessage(@Nonnull Message newContent) {
        return delegate.editMessage(newContent);
    }

    @Nonnull
    @Override
    public AuditableRestAction<Void> delete() {
        return delegate.delete();
    }

    @Nonnull
    @Override
    public JDA getJDA() {
        return delegate.getJDA();
    }

    @Override
    public boolean isPinned() {
        return delegate.isPinned();
    }

    @Nonnull
    @Override
    public RestAction<Void> pin() {
        return delegate.pin();
    }

    @Nonnull
    @Override
    public RestAction<Void> unpin() {
        return delegate.unpin();
    }

    @Nonnull
    @Override
    public RestAction<Void> addReaction(@Nonnull Emote emote) {
        return delegate.addReaction(emote);
    }

    @Nonnull
    @Override
    public RestAction<Void> addReaction(@Nonnull String unicode) {
        return delegate.addReaction(unicode);
    }

    @Nonnull
    @Override
    public RestAction<Void> clearReactions() {
        return delegate.clearReactions();
    }

    @Nonnull
    @Override
    public RestAction<Void> removeReaction(@Nonnull Emote emote) {
        return delegate.removeReaction(emote);
    }

    @Nonnull
    @Override
    public RestAction<Void> removeReaction(@Nonnull Emote emote, @Nonnull User user) {
        return delegate.removeReaction(emote, user);
    }

    @Nonnull
    @Override
    public RestAction<Void> removeReaction(@Nonnull String unicode) {
        return delegate.removeReaction(unicode);
    }

    @Nonnull
    @Override
    public RestAction<Void> removeReaction(@Nonnull String unicode, @Nonnull User user) {
        return delegate.removeReaction(unicode, user);
    }

    @Nonnull
    @Override
    public ReactionPaginationAction retrieveReactionUsers(@Nonnull Emote emote) {
        return delegate.retrieveReactionUsers(emote);
    }

    @Nonnull
    @Override
    public ReactionPaginationAction retrieveReactionUsers(@Nonnull String unicode) {
        return delegate.retrieveReactionUsers(unicode);
    }

    @Nullable
    @Override
    public MessageReaction.ReactionEmote getReactionByUnicode(@Nonnull String unicode) {
        return delegate.getReactionByUnicode(unicode);
    }

    @Nullable
    @Override
    public MessageReaction.ReactionEmote getReactionById(@Nonnull String id) {
        return delegate.getReactionById(id);
    }

    @Nullable
    @Override
    public MessageReaction.ReactionEmote getReactionById(long id) {
        return delegate.getReactionById(id);
    }

    @Nonnull
    @Override
    public MessageType getType() {
        return delegate.getType();
    }

    @Override
    public void formatTo(Formatter formatter, int flags, int width, int precision) {
        delegate.formatTo(formatter, flags, width, precision);
    }

    @Override
    public long getIdLong() {
        return delegate.getIdLong();
    }
}
