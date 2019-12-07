package listener;

import model.SessionStatus;
import model.WerewolfSession;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import util.MessageUtils;

import java.util.*;

public class WerewolfListener extends ListenerAdapter {

    private JDA jda;

    // <Moderator ID, Session> keeps track of sessions
    private Map<String, WerewolfSession> sessions;

    // <User ID, Moderator ID> keeps track of users who need to respond
    private Map<String, String> DMListeners;

    public WerewolfListener() {
        this.sessions = new HashMap<>();
        this.DMListeners = new HashMap<>();
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
            if (messageTokens[0].equals(MessageUtils.userIDToMention(myID))) {
                if (messageTokens.length >= 4 && messageTokens[1].equals("play") && messageTokens[2].equals("with")) {
//                    initializeGame(event);
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
                                // DMListeners.put(target.getId(), author.getId());
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
                    // TODO: remove check and add it to session loop
                    if (invitedUsers.size() < WerewolfSession.MIN_PLAYER_COUNT) {
                        event.getChannel().sendMessage("You need at least " + WerewolfSession.MIN_PLAYER_COUNT + " valid players to play Werewolf").queue();

                        for (User user : invitedUsers) {
                            user.openPrivateChannel().queue((channel) -> {
                                channel.sendMessage(author.getName() + " did not send enough invites. This game has been cancelled");
                            });
                            DMListeners.remove(user.getId());
                        }
                        sessions.remove(author.getId());
                    }

                    else { // TODO: check for players/moderator already in a session or moderating a session
                        WerewolfSession session = new WerewolfSession(sourceChannel, author, invitedUsers);
                        sessions.put(author.getId(), session);
                        Collection<String> userIDs = session.promptPlayers(); // TODO: if response list size == 0, end session
                        for (String userID : userIDs) {
                            DMListeners.put(userID, session.getModerator().getId());
                        }
                        DMListeners.put(author.getId(), author.getId());
                        author.openPrivateChannel().queue((channel) -> {
                            channel.sendMessage("Waiting for invites to be accepted. Type 'ready' to begin with all users who have accepted the invite.").queue();
                        });
                        checkSessionStatus(session);
                    }
                }
                else {
                    event.getChannel().sendMessage("I didn't quite get that").queue();
                }
            }
        }
        else if (event.isFromType(ChannelType.PRIVATE)) {
            System.out.println("private message received from " + author + "!");
            System.out.println(rawMessage);

            if (DMListeners.containsKey(author.getId())) {
                WerewolfSession session = sessions.get(DMListeners.get(author.getId()));
                Collection<String> idsToRemove = session.sendResponse(author, rawMessage);
                for (String id: idsToRemove) {
                    DMListeners.remove(id);
                }
                checkSessionStatus(session);
            }
            else {
                author.openPrivateChannel().queue((channel) -> {
                    channel.sendMessage("I was not expecting a response from you. If you think I was, please wait a few seconds and send the message again").queue();
                });
            }
        }
    }

    private void checkSessionStatus(WerewolfSession session) {
        SessionStatus sessionStatus = session.checkStatus();
        System.out.println("current status: " + sessionStatus);
        if (sessionStatus == SessionStatus.EXPIRED) {
            for (String id : session.getRoles().keySet()) {
                DMListeners.remove(id);
            }
            DMListeners.remove(session.getModerator().getId());
            sessions.remove(session.getModerator().getId());
        }
    }
}
