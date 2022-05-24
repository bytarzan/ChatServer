public enum ServerResponse {

   
    OKAY(200),

 
    INVALID_NAME(401),

    
    NO_SUCH_CHANNEL(402),

    
    NO_SUCH_USER(403),

    
    USER_NOT_IN_CHANNEL(404),

    
    USER_NOT_OWNER(406),

    
    JOIN_PRIVATE_CHANNEL(407),

   
    INVITE_TO_PUBLIC_CHANNEL(408),

    
    NAME_ALREADY_IN_USE(500),

    
    CHANNEL_ALREADY_EXISTS(501);

    private final int value;

    ServerResponse(int value) {
        this.value = value;
    }

    public int getCode() {
        return value;
    }

}
