package me.bhop.bjdautilities;

import net.dv8tion.jda.client.entities.Group;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.requests.RestAction;
import net.dv8tion.jda.core.requests.restaction.AuditableRestAction;
import net.dv8tion.jda.core.requests.restaction.MessageAction;

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
        delegate = jda.getTextChannelById(delegate.getChannel().getIdLong()).getMessageById(delegate.getIdLong()).complete();
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
    public List<User> getMentionedUsers() {
        return delegate.getMentionedUsers();
    }

    @Override
    public List<TextChannel> getMentionedChannels() {
        return delegate.getMentionedChannels();
    }

    @Override
    public List<Role> getMentionedRoles() {
        return delegate.getMentionedRoles();
    }

    @Override
    public List<Member> getMentionedMembers(Guild guild) {
        return delegate.getMentionedMembers(guild);
    }

    @Override
    public List<Member> getMentionedMembers() {
        return delegate.getMentionedMembers();
    }

    @Override
    public List<IMentionable> getMentions(MentionType... types) {
        return delegate.getMentions(types);
    }

    @Override
    public boolean isMentioned(IMentionable mentionable, MentionType... types) {
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
    public OffsetDateTime getEditedTime() {
        return delegate.getEditedTime();
    }

    @Override
    public User getAuthor() {
        return delegate.getAuthor();
    }

    @Override
    public Member getMember() {
        return delegate.getMember();
    }

    @Override
    public String getJumpUrl() {
        return delegate.getJumpUrl();
    }

    @Override
    public String getContentDisplay() {
        return delegate.getContentDisplay();
    }

    @Override
    public String getContentRaw() {
        return delegate.getContentRaw();
    }

    @Override
    public String getContentStripped() {
        return delegate.getContentStripped();
    }

    @Override
    public List<String> getInvites() {
        return delegate.getInvites();
    }

    @Override
    public String getNonce() {
        return delegate.getNonce();
    }

    @Override
    public boolean isFromType(ChannelType type) {
        return delegate.isFromType(type);
    }

    @Override
    public ChannelType getChannelType() {
        return delegate.getChannelType();
    }

    @Override
    public boolean isWebhookMessage() {
        return delegate.isWebhookMessage();
    }

    @Override
    public MessageChannel getChannel() {
        return delegate.getChannel();
    }

    @Override
    public PrivateChannel getPrivateChannel() {
        return delegate.getPrivateChannel();
    }

    @Override
    public Group getGroup() {
        return delegate.getGroup();
    }

    @Override
    public TextChannel getTextChannel() {
        return delegate.getTextChannel();
    }

    @Override
    public Category getCategory() {
        return delegate.getCategory();
    }

    @Override
    public Guild getGuild() {
        return delegate.getGuild();
    }

    @Override
    public List<Attachment> getAttachments() {
        return delegate.getAttachments();
    }

    @Override
    public List<MessageEmbed> getEmbeds() {
        return delegate.getEmbeds();
    }

    @Override
    public List<Emote> getEmotes() {
        return delegate.getEmotes();
    }

    @Override
    public List<MessageReaction> getReactions() {
        return delegate.getReactions();
    }

    @Override
    public boolean isTTS() {
        return delegate.isTTS();
    }

    @Override
    public MessageAction editMessage(CharSequence newContent) {
        return delegate.editMessage(newContent);
    }

    @Override
    public MessageAction editMessage(MessageEmbed newContent) {
        return delegate.editMessage(newContent);
    }

    @Override
    public MessageAction editMessageFormat(String format, Object... args) {
        return delegate.editMessageFormat(format, args);
    }

    @Override
    public MessageAction editMessage(Message newContent) {
        return delegate.editMessage(newContent);
    }

    @Override
    public AuditableRestAction<Void> delete() {
        return delegate.delete();
    }

    @Override
    public JDA getJDA() {
        return delegate.getJDA();
    }

    @Override
    public boolean isPinned() {
        return delegate.isPinned();
    }

    @Override
    public RestAction<Void> pin() {
        return delegate.pin();
    }

    @Override
    public RestAction<Void> unpin() {
        return delegate.unpin();
    }

    @Override
    public RestAction<Void> addReaction(Emote emote) {
        return delegate.addReaction(emote);
    }

    @Override
    public RestAction<Void> addReaction(String unicode) {
        return delegate.addReaction(unicode);
    }

    @Override
    public RestAction<Void> clearReactions() {
        return delegate.clearReactions();
    }

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
