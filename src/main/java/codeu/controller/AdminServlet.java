package codeu.controller;

import codeu.model.data.User;
import codeu.model.data.Conversation;
import codeu.model.data.Message;
import codeu.model.store.basic.UserStore;
import codeu.model.store.basic.MessageStore;
import codeu.model.store.basic.ConversationStore;
import java.io.IOException;
import java.time.Instant;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.google.common.annotations.VisibleForTesting;
import java.util.Comparator;

/** Servlet class responsible for the admin page. */
public class AdminServlet extends HttpServlet {

  private static ConversationStore conversationStore;
  private static UserStore userStore;
  private static MessageStore messageStore;

  /**
   * Set up state for handling admin-related requests. This method is only called when running in a
   * server, not when running in a test.
   */
  @Override
  public void init() throws ServletException {
    super.init();
    initializeDataStores(UserStore.getInstance(), MessageStore.getInstance(), ConversationStore.getInstance());
  }

  /**
   * Sets the UserStore used by this servlet. This function provides a common setup method for use
   * by the test framework or the servlet's init() function.
   * @VisibleForTesting
   */
  @VisibleForTesting void initializeDataStores(UserStore userStore, MessageStore messageStore, ConversationStore conversationStore) {
    this.userStore = userStore;
    this.messageStore = messageStore;
    this.conversationStore = conversationStore;
  }

  /**
   * This function fires when a user requests the /admin URL. It simply forwards the request to
   * admin.jsp.
   */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {

    List<Conversation> conversations = conversationStore.getAllConversations();
    List<User> users = userStore.getAllUsers();
    List<Message> messages = messageStore.getAllMessages();

    //Create a Map that maps the names of stats to the statistics in String form
    HashMap<String, String> stats = new HashMap<>();
    stats.put("number of conversations", Integer.toString(conversations.size()));
    stats.put("number of messages", Integer.toString(messages.size()));
    stats.put("number of users", Integer.toString(users.size()));

    HashMap<UUID, Integer> numMessages = getNumMessagesMap(messages);
    HashMap<UUID, Integer> numWords = getNumWordsMap(messages);

    User mostRecentUser = getMostRecentUser(users);
    User mostActiveUser = getMostActiveUser(numMessages);
    User wordiestUser = getWordiestUser(numMessages, numWords);

    stats.put("newest user", userToString(mostRecentUser));
    stats.put("most active user", userToString(mostActiveUser));
    stats.put("wordiest user", userToString(wordiestUser));

    //Add the map to the request
    request.setAttribute("statistics map", stats);

    request.getRequestDispatcher("/WEB-INF/view/admin.jsp").forward(request, response);
  }

  /**
   * This function should not be triggered by anything.
   */
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Illegal Post request - This address does not handle Post requests");
  }

  /*
  * @return a HashMap mapping UUID's of users to the number of messages that User has sent
  */
  private static HashMap<UUID, Integer> getNumMessagesMap(List<Message> messages) {
    HashMap<UUID, Integer> numMessages = new HashMap<>();
    for (Message m : messages) {
      UUID userID = m.getAuthorId();
      if (!numMessages.containsKey(userID)) {
        numMessages.put(userID, 0);
      }
      numMessages.put(userID, numMessages.get(userID) + 1);
    }
    return numMessages;
  }

  /*
  * @return a HashMap mapping UUID's of users to the number of words that User has sent
  */
  private static HashMap<UUID, Integer> getNumWordsMap(List<Message> messages) {
    HashMap<UUID, Integer> numWords = new HashMap<>();
    for (Message m : messages) {
      UUID userID = m.getAuthorId();
      if (!numWords.containsKey(userID)) {
        numWords.put(userID, 0);
      }
      numWords.put(userID, numWords.get(userID) + getNumWords(m.getContent()));
    }
    return numWords;
  }

  /*
  * @return the User who sends the most words in each message
  */
  private static User getMostActiveUser(HashMap<UUID, Integer> numMessages) {
    int mostMessages = 0;
    UUID mostMessagesID = null;
    for (Map.Entry<UUID, Integer> entry : numMessages.entrySet()) {
      if (entry.getValue() > mostMessages) {
        mostMessages = entry.getValue();
        mostMessagesID = entry.getKey();
      }
    }
    return userStore.getUser(mostMessagesID);
  }

  /*
  * @return the User who has sent the most messages
  */
  private static User getWordiestUser(HashMap<UUID, Integer> numMessages, HashMap<UUID, Integer> numWords) {
    double highestAverage = 0;
    UUID wordiestID = null;
    for (Map.Entry<UUID, Integer> entry : numMessages.entrySet()) {
      double avg = numWords.get(entry.getKey()) / (double) entry.getValue();
      if (avg > highestAverage) {
        highestAverage = avg;
        wordiestID = entry.getKey();
      }
    }
    return userStore.getUser(wordiestID);
  }

  private static int getNumWords(String str) {
    return str.split("\\s+").length;
  }

  /*
  * Returns teh User who most recently registered.
  *
  * @return the most recent User
  */
  private static User getMostRecentUser(List<User> users) {
    User recent = null;
    for (User user : users) {
      if (recent == null) {
        recent = user;
      } else if (user.getCreationTime().compareTo(recent.getCreationTime()) > 0) {
        recent = user;
      }
    }
    return users.stream().max(Comparator.comparing(user -> user.getCreationTime())).orElse(null);
  }

  private String userToString(User user) {
    if (user == null) {
      return "No user";
    } else {
      return user.getName();
    }
  }

}
