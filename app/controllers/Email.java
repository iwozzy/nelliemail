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

import views.html.*;

/**
 * Created by paulwo on 2/8/14.
 */
public class Email extends Controller {

    /*
        Returns the Gmail inbox folder for the currently logged in user
     */

    public static Folder getInbox() {
        OAuth2Authenticator.initialize();

        User currentUser = User.isNewUser(session("email"));

        try {
            IMAPStore imapSslStore = OAuth2Authenticator.connectToImap("imap.gmail.com", 993, currentUser.email, currentUser.googleToken, true);
            Folder inbox = imapSslStore.getFolder("inbox");
            inbox.open(Folder.READ_ONLY);

            System.out.println("---------------------" + inbox.getMessageCount() + "-----------------------");
            System.out.println("---------------------" + inbox.getMessage(1368).isSet(FLAGS.Flag.SEEN) + "-----------------------");

            return inbox;
        } catch (Exception e) {
            System.out.println(e);
        }

        return null;
    }


    /*
        TODO Need to return actual JSON vs ArrayLit
     */

    @BodyParser.Of(BodyParser.Json.class)
    public static Result getCover() {
        User currentUser = User.isNewUser(session("email"));
        OAuth2Authenticator.initialize();
        ObjectNode result = Json.newObject();

        ArrayList<Message> coverEmails = new ArrayList<Message>();

        Folder inbox = getInbox();

        try {
            inbox.open(Folder.READ_ONLY);
            int messageCount = inbox.getMessageCount();

            //BE CAREFUL WITH i>=1
            for(int i = messageCount; i >= 1 ; i--) {
                Message message = inbox.getMessage(i);
                if(!message.isSet(FLAGS.Flag.SEEN)) {
                    coverEmails.add(message);
                }

                if(coverEmails.size() == 5) {
                    i = 0;
                }
            }

            result.put("status","OK");

            for(Message m : coverEmails) {
                result.put("message"+m.getMessageNumber(),m.getSubject());
            }
        } catch (Exception e) {
            System.out.println(e);
        }

        return ok(cover.render(coverEmails));

    }

    public static Result getOutbox() {
        User currentUser = User.isNewUser(session("email"));
        OAuth2Authenticator.initialize();

        ArrayList<Message> sentMessagesPrimary = new ArrayList<Message>();
        ArrayList<Message> receivedMessagesInReplyTo = new ArrayList<Message>();
        ArrayList<Message> followUp = new ArrayList<Message>();

        try {
            IMAPStore imapSslStore = OAuth2Authenticator.connectToImap("imap.gmail.com",993,currentUser.email,currentUser.googleToken,true);
            Folder outbox = imapSslStore.getFolder("[Gmail]/Sent Mail");
            Folder inbox = imapSslStore.getFolder("inbox");

            outbox.open(Folder.READ_ONLY);
            inbox.open(Folder.READ_ONLY);

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
