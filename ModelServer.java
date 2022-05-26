import java.util.*;

public final class ModelServer {

    private TreeMap<Integer, String> usersRegistered;
    private TreeMap<String, ChannelUser> channels;

    public ModelServer() {
        usersRegistered = new TreeMap<Integer, String>();
        channels = new TreeMap<String, ChannelUser>();
    }

   
    public int getUserId(String nickname) {
        if (usersRegistered.containsValue(nickname)) {
            for (Map.Entry<Integer, String> entry : usersRegistered.entrySet()) {
                if (entry.getValue().equals(nickname)) {
                    return entry.getKey();
                }
            }
        }
        return -1;
    }

 
    public String getNickname(int userId) {
        if (usersRegistered.containsKey(userId)) {
            return usersRegistered.get(userId);
        }
        return null;
    }

    public Collection<String> getRegisteredUsers() {
        Collection<String> collectionUsers = new LinkedList<String>();
        for (Map.Entry<Integer, String> entry : usersRegistered.entrySet()) {
            collectionUsers.add(entry.getValue());
        }
        return collectionUsers;
    }

   
    public Collection<String> getChannels() {
        Collection<String> c = new TreeSet<String>(channels.keySet());
        return c;
    }

    
    public Collection<String> getUsersInChannel(String channelName) {
        if (channels.get(channelName) == null) {
            return new TreeSet<>();
        }

        Collection<String> channelUsers = new TreeSet<>(channels.get(channelName).getUsers());
        return channelUsers;
    }

    
    public String getOwner(String channelName) {
        if (channels.containsKey(channelName)) {
            return channels.get(channelName).getOwner();
        }
        return null;
    }

  
    public Broadcast registerUser(int userId) {
        String nickname = generateUniqueNickname();
        usersRegistered.put(userId, nickname);
        
        return Broadcast.connected(nickname);
    }

    private String generateUniqueNickname() {
        int suffix = 0;
        String nickname;
        Collection<String> existingUsers = getRegisteredUsers();
        do {
            nickname = "User" + suffix++;
        } while (existingUsers != null && existingUsers.contains(nickname));
        return nickname;
    }

   
    public Broadcast deregisterUser(int userId) {
        TreeSet<String> recipients = new TreeSet<String>();
        TreeSet<String> removedChans = new TreeSet<String>();
        String u = usersRegistered.get(userId);
        if (usersRegistered.containsKey(userId)) {
            for (Map.Entry<String, ChannelUser> entry : channels.entrySet()) {
                if (entry.getValue().containsUser(u)) {
                    recipients.addAll(entry.getValue().getUsers());
                    if (entry.getValue().getOwner().equals(u)) {
                        removedChans.add(entry.getKey());
                    } else {
                        entry.getValue().removeUser(u);
                    }
                }
            }
            for (String c : removedChans) {
                channels.remove(c);
            }
            usersRegistered.remove(userId);
            recipients.remove(u);
        }
        return Broadcast.disconnected(u, recipients);
    }

   
    public Broadcast changeNickname(NicknameCommand nickCommand) {
        String newName = nickCommand.getNewNickname();
        if (usersRegistered.values().contains(newName)) {
            return Broadcast.error(nickCommand, ServerResponse.NAME_ALREADY_IN_USE);
        }
        if (!isValidName(newName)) {
            return Broadcast.error(nickCommand, ServerResponse.INVALID_NAME);
        }

        TreeSet<String> users = new TreeSet<String>();
        String u = usersRegistered.get(nickCommand.getSenderId());

        for (Map.Entry<String, ChannelUser> entry : channels.entrySet()) {
            if (entry.getValue().containsUser(u)) {
                users.addAll(entry.getValue().getUsers());
                entry.getValue().removeUser(nickCommand.getSender());
                entry.getValue().addUser(newName);
            }
            if (entry.getValue().getOwner().equals(nickCommand.getSender())) {
                entry.getValue().setOwner(newName);
            }
        }

        usersRegistered.replace(nickCommand.getSenderId(), newName);
        return Broadcast.okay(nickCommand, users);
    }

 
    public static boolean isValidName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        for (char character : name.toCharArray()) {
            if (!Character.isLetterOrDigit(character)) {
                return false;
            }
        }
        return true;
    }

   
    public Broadcast createChannel(CreateCommand createCommand) {
        Collection<String> users = new LinkedList<String>();
        boolean privacy = createCommand.isInviteOnly();
        String channelName = createCommand.getChannel();


        if (channels.keySet().contains(channelName)) {
            return Broadcast.error(createCommand, ServerResponse.CHANNEL_ALREADY_EXISTS);
        }
        if (!(isValidName(channelName))) {
            return Broadcast.error(createCommand, ServerResponse.INVALID_NAME);
        }

        ChannelUser c = new ChannelUser(createCommand.getSender(), privacy, channelName);
        channels.put(channelName, c);

        users.add(createCommand.getSender());
        c.addUser(createCommand.getSender());

        return Broadcast.okay(createCommand, users);
    }

  
    public Broadcast joinChannel(JoinCommand joinCommand) {
        String nameChannel = joinCommand.getChannel();
        LinkedList<String> recipients = new LinkedList<String>();
        if (!channels.keySet().contains(nameChannel)) {
            return Broadcast.error(joinCommand, ServerResponse.NO_SUCH_CHANNEL);
        }
        ChannelUser c = channels.get(nameChannel);
        if (c.getPrivacy()) {
            return Broadcast.error(joinCommand, ServerResponse.JOIN_PRIVATE_CHANNEL);
        }
        c.addUser(joinCommand.getSender());
        recipients.addAll(c.getUsers());
        return Broadcast.names(joinCommand, recipients, c.getOwner());
    }

  
    public Broadcast sendMessage(MessageCommand messageCommand) {
        ChannelUser chans = channels.get(messageCommand.getChannel());
        LinkedList<String> recipients = new LinkedList<String>();
        if (!(channels.keySet().contains(messageCommand.getChannel()))) {
            return Broadcast.error(messageCommand, ServerResponse.NO_SUCH_CHANNEL);
        }
        if (!chans.getUsers().contains(messageCommand.getSender())) {
            return Broadcast.error(messageCommand, ServerResponse.USER_NOT_IN_CHANNEL);
        }
        recipients.addAll(chans.getUsers());
        return Broadcast.okay(messageCommand, recipients);
    }

  
        String userName = leaveCommand.getSender(); 
        TreeSet<String> recipients = new TreeSet<String>();
        ChannelUser chans = channels.get(leaveCommand.getChannel());
        if (!channels.keySet().contains(leaveCommand.getChannel())) {
            return Broadcast.error(leaveCommand, ServerResponse.NO_SUCH_CHANNEL);
        }
        if (!chans.getUsers().contains(userName)) {
            return Broadcast.error(leaveCommand, ServerResponse.USER_NOT_IN_CHANNEL);
        }
        recipients.addAll(chans.getUsers());
        if (chans.getOwner().equals(userName)) {
            channels.remove(leaveCommand.getChannel());
        } else {
            chans.removeUser(userName);
        }
        return Broadcast.okay(leaveCommand, recipients);
    }

    
    public Broadcast inviteUser(InviteCommand inviteCommand) {
        String channelName = inviteCommand.getChannel();
        if (!channels.keySet().contains(channelName)) {
            return Broadcast.error(inviteCommand, ServerResponse.NO_SUCH_CHANNEL);
        }

        if (!usersRegistered.values().contains(inviteCommand.getUserToInvite())) {
            return Broadcast.error(inviteCommand, ServerResponse.NO_SUCH_USER);
        }
        
        ChannelUser chans = channels.get(channelName);
        if (!chans.getOwner().equals(inviteCommand.getSender())) {
            return Broadcast.error(inviteCommand, ServerResponse.USER_NOT_OWNER);
        }

        if (!chans.getPrivacy()) {
            return Broadcast.error(inviteCommand, ServerResponse.INVITE_TO_PUBLIC_CHANNEL);
        }

        chans.addUser(inviteCommand.getUserToInvite());
        TreeSet<String> recipients = chans.getUsers();

        return Broadcast.names(inviteCommand, recipients, chans.getOwner());
    }

   
    public Broadcast kickUser(KickCommand kickCommand) {
        String nameChannel = kickCommand.getChannel();
        String userName = kickCommand.getSender();
        TreeSet<String> recipients = new TreeSet<String>();
        if (!channels.keySet().contains(nameChannel)) {
            return Broadcast.error(kickCommand, ServerResponse.NO_SUCH_CHANNEL);
        }
        ChannelUser chans = channels.get(nameChannel);
        System.out.println(chans.getUsers().size());
        if (!(usersRegistered.values().contains(kickCommand.getUserToKick()))) {
            return Broadcast.error(kickCommand, ServerResponse.NO_SUCH_USER);
        }
        if (!(chans.getUsers().contains(kickCommand.getUserToKick()))) {
            return Broadcast.error(kickCommand, ServerResponse.USER_NOT_IN_CHANNEL);
        }
        if (!(chans.getOwner().equals(userName))) {
            return Broadcast.error(kickCommand, ServerResponse.USER_NOT_OWNER);
        }
        if (chans.getOwner().equals(kickCommand.getUserToKick())) {
            channels.remove(nameChannel);
            recipients.add(kickCommand.getUserToKick());
        } else {
            chans.removeUser(kickCommand.getUserToKick());
        }

        recipients.addAll(chans.getUsers());
        recipients.add(kickCommand.getUserToKick());

        return Broadcast.okay(kickCommand, recipients);
    }

}
