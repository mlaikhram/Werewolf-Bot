package listener;

import model.SessionStatus;
import model.WerewolfSession;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import util.MessageUtils;
import util.RoleAssigner;

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
        System.out.println(event.getMessage().getContentRaw());
        if (event.getAuthor().isBot()) {
            return;
        }

        String myID = jda.getSelfUser().getId();
        System.out.println("my id: " + myID);
        User author = event.getAuthor();
        MessageChannel sourceChannel = event.getChannel();
        String rawMessage = event.getMessage().getContentRaw();
        String[] messageTokens = rawMessage.split(" ");

        if (event.isFromType(ChannelType.TEXT) && MessageUtils.isUserMention(messageTokens[0])) {
//            System.out.println("message received from " + author + "!");
//            System.out.println(rawMessage);
//            System.out.println("my id is " + myID);

            System.out.println("id: " + MessageUtils.mentionToUserID(messageTokens[0]));
            if (MessageUtils.mentionToUserID(messageTokens[0]).toString().equals(myID)) {
//            if ("651588878968422411".equals("651588878968422411")) {
                System.out.println("in the loop");

                if (messageTokens.length >= 3 && messageTokens[1].equals("reflect")) {
                    try {
                        RoleAssigner.testAssign(author, messageTokens[2]);
                    }
                    catch(Exception e) {
                        e.printStackTrace();
                    }
                }

                if (messageTokens.length >= 4 && messageTokens[1].equals("play") && messageTokens[2].equals("with")) {
//                    initializeGame(event);
                    List<User> invitedUsers = new ArrayList<>();
                    for (int i = 3; i < messageTokens.length; ++i) {
                        try {
                            System.out.println(messageTokens[i]);
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
                        try {
                            session.openInvites();
                        }
                        catch (Exception e) {
                            session.getChannel().sendMessage("Something went wrong with the session. Closing session").queue();
                            for (String id : session.getRoles().keySet()) {
                                DMListeners.remove(id);
                            }
                            DMListeners.remove(session.getModerator().getId());
                            sessions.remove(session.getModerator().getId());
                        }
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
        try {
            SessionStatus sessionStatus = session.checkStatus();
            System.out.println("current status: " + sessionStatus);
            if (sessionStatus == SessionStatus.EXPIRED) {
                for (String id : session.getRoles().keySet()) {
                    DMListeners.remove(id);
                }
                DMListeners.remove(session.getModerator().getId());
                sessions.remove(session.getModerator().getId());
            } else if (sessionStatus == SessionStatus.WEREWOLF_KILL) {
                DMListeners.put(session.getModerator().getId(), session.getModerator().getId());
                session.promptWerewolves();
            } else if (sessionStatus == SessionStatus.BEGIN_SPECIAL_ROLES) {
                DMListeners.put(session.getModerator().getId(), session.getModerator().getId());
                System.out.println("Players to ask for prompt");
                for (String id : session.getRoles().keySet()) {
                    System.out.println(id);
                    DMListeners.put(id, session.getModerator().getId());
                }
                session.checkUsersForSpecialRoles();
            } else if (sessionStatus == SessionStatus.PENDING_SPECIAL_ROLES) {
                session.checkUsersForSpecialRoles();
            } else if (sessionStatus == SessionStatus.PENDING_REPLAY) {
                for (String id : session.getRoles().keySet()) {
                    DMListeners.remove(id);
                }
                DMListeners.put(session.getModerator().getId(), session.getModerator().getId());
                session.askForReplay();
            } else if (sessionStatus == SessionStatus.REPLAY) {
                Collection<String> userIDs = session.promptPlayers(); // TODO: if response list size == 0, end session
                for (String userID : userIDs) {
                    DMListeners.put(userID, session.getModerator().getId());
                }
                DMListeners.put(session.getModerator().getId(), session.getModerator().getId());
                session.openInvites();
            }
        }
        catch (Exception e) {
            session.getChannel().sendMessage("Something went wrong with the session. Closing session").queue();
            for (String id : session.getRoles().keySet()) {
                DMListeners.remove(id);
            }
            DMListeners.remove(session.getModerator().getId());
            sessions.remove(session.getModerator().getId());
        }
    }
}
