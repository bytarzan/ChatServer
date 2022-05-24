import java.util.*;


public final class Broadcast {

    private final Map<String, List<String>> responses;

   
    private Broadcast() {
        responses = new TreeMap<>();
    }

    
    private void addResponse(String nick, String response) {
        if (!responses.containsKey(nick)) {
            responses.put(nick, new LinkedList<>());
        }
        List<String> userResponses = responses.get(nick);
        if (!userResponses.contains(response)) {
            userResponses.add(response);
        }
    }

   
    public static Broadcast okay(Command command, Collection<String> recipients) {
        Broadcast broadcast = new Broadcast();
        for (String recipient : recipients) {
            broadcast.addResponse(recipient, command.toString());
        }

        // Need to send response to user ID associated with *new* nick
        if (command instanceof NicknameCommand nickCommand) {
            broadcast.responses.remove(command.getSender());
            broadcast.addResponse(nickCommand.getNewNickname(), command.toString());
        }
        return broadcast;
    }

  
    public static Broadcast error(Command command, ServerResponse error) {
        if (error == ServerResponse.OKAY) {
            throw new IllegalArgumentException("Invalid error type");
        }
        Broadcast broadcast = new Broadcast();
        String recipient = command.getSender();
        int errorCode = error.getCode();
        String response = String.format(":%s ERROR %d", recipient, errorCode);
        broadcast.addResponse(recipient, response);
        return broadcast;
    }

 
    public static Broadcast connected(String recipient) {
        Broadcast broadcast = new Broadcast();
        String response = String.format(":%s CONNECT", recipient);
        broadcast.addResponse(recipient, response);
        return broadcast;
    }

   
    public static Broadcast disconnected(String user, Collection<String> recipients) {
        if (recipients.contains(user)) {
            throw new IllegalArgumentException("Disconnected user in broadcast");
        }
        Broadcast broadcast = new Broadcast();
        String response = String.format(":%s QUIT", user);
        for (String recipient : recipients) {
            broadcast.addResponse(recipient, response);
        }
        return broadcast;
    }

    
    public static Broadcast names(Command command, Collection<String> recipients, String owner) {
        // Relay JOIN or INVITE normally
        Broadcast broadcast = Broadcast.okay(command, recipients);

        // Also relay NAMES to user who joins channel
        String channelName, userToAdd;
        if (command instanceof JoinCommand joinCommand) {
            channelName = joinCommand.getChannel();
            userToAdd = joinCommand.getSender();
        } else if (command instanceof InviteCommand inviteCommand) {
            channelName = inviteCommand.getChannel();
            userToAdd = inviteCommand.getUserToInvite();
        } else {
            throw new IllegalArgumentException("Invalid command type");
        }
        String namesPayload = createNamesPayload(owner, recipients);
        String namesResponse = String
                .format(":%s NAMES %s :%s", userToAdd, channelName, namesPayload);
        broadcast.addResponse(userToAdd, namesResponse);
        return broadcast;
    }

    
    public Map<Integer, List<String>> getResponses(ServerModel model) {
        Map<Integer, List<String>> userIdResponses = new TreeMap<>();
        for (Map.Entry<String, List<String>> entry : responses.entrySet()) {
            int userId = model.getUserId(entry.getKey());
            userIdResponses.put(userId, entry.getValue());
        }
        return userIdResponses;
    }

    
    private static String createNamesPayload(String owner, Collection<String> nicks) {
        if (owner == null || nicks == null || !nicks.contains(owner)) {
            throw new IllegalArgumentException();
        }

        List<String> nicksList = new LinkedList<>(nicks);
        Collections.sort(nicksList);
        StringBuilder payload = new StringBuilder();
        for (String nick : nicksList) {
            if (nick.equals(owner)) {
                payload.append('@');
            }
            payload.append(nick);
            payload.append(' ');
        }
        return payload.toString().trim();
    }

   
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || o.getClass() != Broadcast.class) {
            return false;
        }
        return this.responses.equals(((Broadcast) o).responses);
    }

    @Override
    public int hashCode() {
        return responses.hashCode();
    }

    @Override
    public String toString() {
        return responses.toString();
    }

}
