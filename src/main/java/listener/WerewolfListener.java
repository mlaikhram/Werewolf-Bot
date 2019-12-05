package listener;

import model.WerewolfSession;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import util.MessageUtils;

import java.util.HashMap;
import java.util.Map;

public class WerewolfListener extends ListenerAdapter {

    private JDA jda;

    // <Moderator ID, Session> keeps track of sessions
    private Map<String, WerewolfSession> sessions;

    // <User ID, Moderator ID> keeps track of users who are awaiting a response
    private Map<String, String> openQueries;

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
                    for (int i = 3; i < messageTokens.length; ++i) {
                        if (MessageUtils.isUserMention(messageTokens[i])) {
                            System.out.println(MessageUtils.mentionToUserID(messageTokens[i]));
                            sourceChannel.getJDA().getUserById(MessageUtils.mentionToUserID(messageTokens[i])).openPrivateChannel().queue((channel) -> {
                                channel.sendMessage(author.getName() + " told me to message you").queue();
                            });
                        }
                    }
//                    event.getAuthor().openPrivateChannel().queue((channel) -> {
//                        channel.sendMessage("Did you call for me?").queue();
//                    });
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
