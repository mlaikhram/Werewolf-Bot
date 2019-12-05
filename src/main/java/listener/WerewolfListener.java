package listener;

import model.WerewolfSession;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import util.MessageUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WerewolfListener extends ListenerAdapter {

    private JDA jda;

    // <Moderator ID, Session> keeps track of sessions
    private Map<String, WerewolfSession> sessions;

    // <User ID, Moderator ID> keeps track of users who need to respond
    private Map<String, String> DMListeners;

    public WerewolfListener() {
        this.sessions = new HashMap<>();
    }

    public void setJDA(JDA jda) {
        this.jda = jda;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }

        String myID = jda.getSelfUser().getId();
        User author = event.getAuthor();
        MessageChannel sourceChannel = event.getChannel();
        String rawMessage = event.getMessage().getContentRaw();

        if (event.isFromType(ChannelType.TEXT)) {
//            System.out.println("message received from " + author + "!");
//            System.out.println(rawMessage);
//            System.out.println("my id is " + myID);

            String[] messageTokens = rawMessage.split(" ");
            for (String token : messageTokens) {
                System.out.println(token);
            }
            if (messageTokens[0].equals(MessageUtils.userIDToMention(myID))) {
                if (messageTokens.length >= 4 && messageTokens[1].equals("play") && messageTokens[2].equals("with")) {
                    initializeGame(event);
                    List<User> invitedUsers = new ArrayList<>();
                    for (int i = 3; i < messageTokens.length; ++i) {
                        try {
                            if (MessageUtils.isUserMention(messageTokens[i])) {
                                User target = sourceChannel.getJDA().getUserById(MessageUtils.mentionToUserID(messageTokens[i]));
                                if (target.isBot()) {
                                    throw new Exception(target.getName() + " is a bot! Bots do not know how to play Werewolf");
                                }
                                else if (target.getId().equals(author.getId())) {
                                    throw new Exception("You are moderating this game! You cannot participate as well");
                                }
                                target.openPrivateChannel().queue((channel) -> {
                                    channel.sendMessage(author.getName() + " invited you to play Werewolf! Would you like to join? (Respond with 'yes' or 'no')").queue();
                                });
                                DMListeners.put(target.getId(), author.getId());
                                invitedUsers.add(target);
                            }
                            else {
                                throw new Exception(messageTokens[i] + " is not a valid user");
                            }
                        }
                        catch (Exception e) {
                            event.getChannel().sendMessage(e.getMessage()).queue();
                        }
                    }
                    if (invitedUsers.size() < 4) {
                        event.getChannel().sendMessage("You need at least 4 valid players to play Werewolf").queue();

                        for (User user : invitedUsers) {
                            user.openPrivateChannel().queue((channel) -> {
                                channel.sendMessage(author.getName() + " did not send enough invites. This game has been cancelled");
                            });
                            DMListeners.remove(user.getId());
                        }
                        sessions.remove(author.getId());
                    }
                    else {
                        event.getAuthor().openPrivateChannel().queue((channel) -> {
                            channel.sendMessage("Waiting for invites to be accepted. Type 'ready' to begin with all users who have accepted the invite.").queue();
                        });
                    }
                }
                else {
                    event.getChannel().sendMessage("I didn't quite get that").queue();
                }
            }
        }
        else if (event.isFromType(ChannelType.PRIVATE)) {
            System.out.println("private message received from " + event.getAuthor() + "!");
            System.out.println(event.getMessage().getContentDisplay());

            if (sessions.containsKey(author.getId())) {

            }

            if (event.getMessage().getContentRaw().equals("!ping")) {
                event.getChannel().sendMessage("Private Pong!").queue();
            }
        }
    }

    public void initializeGame(MessageReceivedEvent event) {
        WerewolfSession session = new WerewolfSession(event.getChannel(), event.getAuthor());
        sessions.put(event.getAuthor().getId(), session);
    }
}
