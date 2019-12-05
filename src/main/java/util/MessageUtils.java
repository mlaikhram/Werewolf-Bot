package util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageUtils {

    public static String ID_FORMAT = "<@%s>";

    public static int mentionToUserID(String mention) {
        return Integer.parseInt(mention.replaceAll("[<@>]", ""));
    }

    public static String userIDToMention(String id) {
        return String.format(ID_FORMAT, id);
    }

    public static Collection<Integer> getMentionsFromText(String text) {
        Collection<Integer> matches = new ArrayList<>();
        Matcher m = Pattern.compile("<@[0-9]+>").matcher(text);
        while (m.find()) {
            matches.add(mentionToUserID(m.group()));
        }
        return matches;
    }
}
