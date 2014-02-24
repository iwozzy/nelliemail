package controllers;

import com.sun.mail.imap.protocol.FLAGS;
import models.User;
import play.mvc.*;
import oauth.OAuth2Authenticator;
import com.sun.mail.imap.*;

import play.mvc.BodyParser;
import play.libs.Json;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;


import javax.mail.FetchProfile;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.Date;

import views.html.*;

/**
 * Created by paulwo on 2/8/14.
 */
public class Email extends Controller {

    /*
        Returns the Gmail inbox folder for the currently logged in user
     */

    public static Folder getFolder(String name, boolean readWrite) {
        OAuth2Authenticator.initialize();

        User currentUser = User.isNewUser(session("email"));

        long timestamp = new Date().getTime();

        if((timestamp - currentUser.getTokenTime()) > 3000000) {
            System.out.println("-----Refreshing the token!-----");
            currentUser.refreshToken();
        }

        try {

            IMAPStore imapSslStore = OAuth2Authenticator.connectToImap("imap.gmail.com", 993, currentUser.email, currentUser.googleToken, true);
            Folder inbox = imapSslStore.getFolder(name);

            if (readWrite) {
                inbox.open(Folder.READ_WRITE);
            } else {
                inbox.open(Folder.READ_ONLY);
            }

            return inbox;

        } catch (Exception e) {

            System.out.println(e);

        }

        return null;
    }


    /*
        Gets the 5 unread emails from the inbox
        TODO Need to return actual JSON vs ArrayLit
     */

    @BodyParser.Of(BodyParser.Json.class)
    public static Result getCoverEmails() {

        ArrayList<Message> coverEmails = new ArrayList<Message>();

        Folder inbox = getFolder("inbox", false);

        try {
            int messageCount = inbox.getMessageCount();

            //BE CAREFUL WITH i>=1
            //I think the first message is the last i.e. message count
            for(int i = messageCount; i >= 1 ; i--) {
                Message message = inbox.getMessage(i);
                if(!message.isSet(FLAGS.Flag.SEEN)) {
                    coverEmails.add(message);
                }

                if(coverEmails.size() == 5) {
                    i = 0;
                }
            }

        } catch (Exception e) {
            System.out.println(e);
        }

        return ok(cover.render(coverEmails));

    }

    /*
        Gets all of the messages in the user's outbox.
        It then processes the messages to try to return the ones that haven't been replied to
     */

    public static Result getOutbox() {

        ArrayList<Message> sentMessagesPrimary = new ArrayList<Message>();
        ArrayList<Message> receivedMessagesInReplyTo = new ArrayList<Message>();
        ArrayList<Message> followUp = new ArrayList<Message>();

        Folder outbox = getFolder("[Gmail]/Sent Mail", false);
        Folder inbox = getFolder("inbox", false);

        try {

            int messageCount = outbox.getMessageCount();
            int inboxCount = inbox.getMessageCount();

            Message allInbox [] = inbox.getMessages();
            Message allOutbox [] = outbox.getMessages();

            FetchProfile fp = new FetchProfile();
            fp.add(FetchProfile.Item.ENVELOPE);
            fp.add("In-Reply-To");
            inbox.fetch(allInbox, fp);

            FetchProfile fpOut = new FetchProfile();
            fpOut.add(FetchProfile.Item.ENVELOPE);
            fpOut.add("In-Reply-To");
            fpOut.add("Message-Id");
            outbox.fetch(allOutbox, fpOut);


            for (int i = 0; i < outbox.getMessageCount(); i++) {
//                System.out.println(allOutbox[i].getFrom());
//                System.out.println(allOutbox[i].getHeader("In-Reply-To"));
                if(allOutbox[i].getHeader("In-Reply-To") == null ) {
//                    System.out.println(allOutbox[i].getSubject());
                    sentMessagesPrimary.add(allOutbox[i]);
                }

            }

            for (int i = 0; i < inbox.getMessageCount(); i++) {
//                System.out.println(allInbox[i].getFrom());
//                System.out.println(allInbox[i].getHeader("In-Reply-To"));
                if ( allInbox[i].getHeader("In-Reply-To") != null ) {
//                    System.out.println(allInbox[i].getSubject());
                    receivedMessagesInReplyTo.add(allInbox[i]);

                }
            }

            for (Message m : sentMessagesPrimary) {
                boolean found = false;
                for(Message mm : receivedMessagesInReplyTo) {
                    if(m.getHeader("Message-Id")[0].equals(mm.getHeader("In-Reply-To")[0])) {
                        found = true;
                    }
                }

                if(!found) {
                    if(m.getSubject() != null) {
                        followUp.add(m);
                        System.out.println("Need to follow up on: " + m.getSubject());
                    }
                }

            }



//            for (Message m : allInbox) {
//                System.out.println(m.getSubject());
//            }

//            System.out.println("!!!!!!!!!!!!" + messageCount + "!!!!!!!!!!!!");
//            System.out.println("!!!!!!!!!!!!" + outbox.getMessage(2722).getSubject() + "!!!!!!!!!!!!");
//
//            Enumeration allHeaders = inbox.getMessage(inboxCount).getAllHeaders();
//            String inReplyTo[] = inbox.getMessage(inboxCount).getHeader("In-Reply-To");
//
//            while(allHeaders.hasMoreElements()){
//                javax.mail.Header header = (javax.mail.Header)allHeaders.nextElement();
//                System.out.println("!!!" + header.getName() + "!!! = "+header.getValue());
//            }
//
//            System.out.println("Count: " + inReplyTo.length + " Content: " + inReplyTo[0] + " ... " + inReplyTo.toString());


        } catch (Exception e) {
            System.out.println(e);
        }

        Collections.reverse(followUp);

        return ok(cover.render(followUp));
    }

//    public static void deleteMessage(Message message) {
//        try {
//            message.setFlag(Flags.Flag.DELETED, true);
//        } catch (Exception e) {
//            System.out.println(e);
//        }
//    }

    /*
        In Gmail you have to move the messages to [Gmail]/Trash
        Called via AJAX with a parameter of message ID in the inbox

        TODO The message ID will change if messages are not deleted in the correct order....!!!!
     */

    public static Result deleteMessage () {

        String message = request().getQueryString("foo");
        System.out.println(message);
        int intMessage = Integer.parseInt(message);

        System.out.println("Getting ready to delete message number:" + intMessage);

        Folder inbox = getFolder("inbox", true);
        Folder trash = getFolder("[Gmail]/Trash", false);

        try {
            Message messageArray[] = new Message[1];
            Message messageToDelete = inbox.getMessage(intMessage);
            messageArray[0] = messageToDelete;

            inbox.copyMessages(messageArray,trash);
            //messageToDelete.setFlag(Flags.Flag.DELETED, true);
            inbox.close(true);
        } catch (Exception e) {
            System.out.println(e);
        }


        return ok();
    }
    /*
        Example of a nested json
     */

    @BodyParser.Of(BodyParser.Json.class)
    public static Result sayHello() {
        JsonNode json = request().body().asJson();

        ObjectNode result = Json.newObject();
        ObjectNode inside = Json.newObject();

        inside.put("inside","insideValue");

        result.put("status", "KO");
        result.put("message", "Missing parameter [name]");
        result.put("nested",inside);

        return ok(result);
    }

}
