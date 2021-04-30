package chatserveur;



import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;


import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

import org.json.*;




@ServerEndpoint(value="/chatserver/{pseudo}/{roomName}", 
                configurator=ChatServer.EndpointConfigurator.class)
public class ChatServer {
    
    private static ChatServer singleton = new ChatServer();

    private ChatServer() {
    }

    /**
     * Acquisition de notre unique instance ChatServer
     */
    public static ChatServer getInstance() {
        return ChatServer.singleton;
    }

    /**
     * On maintient toutes les sessions utilisateurs dans une collection.
     */
    private static Map<String, Set> roomMap = new ConcurrentHashMap(8);
    private static Map<String, String> userMap = new ConcurrentHashMap(8);
    
    /**
     * Cette m�thode est d�clench�e � chaque connexion d'un utilisateur.
     * @throws IOException 
     */
    @OnOpen
    public void open(Session session,@PathParam("roomName") String roomName,@PathParam("pseudo") String pseudo ) throws IOException {
        //sendMessage( "Admin >>> Connection established for " + pseudo );
    	Set set = roomMap.get(roomName);
    	userMap.put(session.getId(), pseudo);
    	if (set == null) {
    		set = new CopyOnWriteArraySet();
            set.add(session);
            roomMap.put(roomName, set);
        } else {
            set.add(session);
        }
    	sendMessage(roomName, "Admin >>> Connection established for " + pseudo );
    	sendUserList(roomName);
    }

    /**
     * Cette m�thode est d�clench�e � chaque d�connexion d'un utilisateur.
     * @throws IOException 
     */
    @OnClose
    public void close(Session session,@PathParam("roomName") String roomName,@PathParam("pseudo") String pseudo) throws IOException {
    	userMap.remove(session.getId());
    	if(roomMap.containsKey(roomName)) {
    		roomMap.get(roomName).remove(session);
    	}
    	sendUserList(roomName);
        sendMessage(roomName, "Admin >>> Connection closed for " + pseudo );
    }

    /**
     * Cette m�thode est d�clench�e en cas d'erreur de communication.
     */
    @OnError
    public void onError(Throwable error) {
        System.out.println( "Error: " + error.getMessage() );
    }

    /**
     * Cette m�thode est d�clench�e � chaque r�ception d'un message utilisateur.
     */
    @OnMessage
   public void handleMessage(@PathParam("roomName") String roomName,String message, Session session) {
    	String pseudo = userMap.get(session.getId());
      //  String pseudo = (String) session.getUserProperties().get( "pseudo" );
        String fullMessage = pseudo + " >>> " + message; 
        sendMessage(roomName, fullMessage );
    }

    /**
     * Une m�thode priv�e, sp�cifique � notre exemple.
     * Elle permet l'envoie d'un message aux participants de la discussion.
     */
    private void sendMessage(String roomName, String fullMessage ) {
     	JSONObject json = new JSONObject();
     	json.put("messages", fullMessage);
       Set<Session> sessions = roomMap.get(roomName);
        for(Session s : sessions ) {
            try {
                s.getBasicRemote().sendText( json.toString() );
            } catch( Exception exception ) {
                System.out.println( "ERROR: cannot send message to " + s.getId() );
            }
        }     
    }
    
    private String CreateUserJsonData(String roomName) {
    	ArrayList<String> users = new ArrayList<String>();
     	Set<Session> sessions = roomMap.get(roomName);
     	JSONObject json = new JSONObject();
     	for(Session s : sessions ) {
     		users.add(userMap.get(s.getId()));
     	}
     	json.put("users", users);
     	return json.toString();
    }
    
    private void sendUserList(String roomName) {
    	Set<Session> sessions = roomMap.get(roomName);
    	for(Session s : sessions ) {
            try {
                s.getBasicRemote().sendText(CreateUserJsonData(roomName));
            } catch( Exception exception ) {
                System.out.println( "ERROR: cannot send message to " + s.getId() );
            	}
        	} 
		}

    
    /**
     * Permet de ne pas avoir une instance diff�rente par client.
     * ChatServer est donc g�rer en "singleton" et le configurateur utilise ce singleton. 
     */
    public static class EndpointConfigurator extends ServerEndpointConfig.Configurator {
        @Override 
        @SuppressWarnings("unchecked")
        public <T> T getEndpointInstance(Class<T> endpointClass) {
            return (T) ChatServer.getInstance();
        }
    }
}
