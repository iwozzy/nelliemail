package controllers;

import java.util.*;

import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Http.Request;
import play.mvc.Http.RequestBody;
import play.libs.OAuth;
import play.libs.OAuth.ServiceInfo;
import play.libs.OAuth.ConsumerKey;
import play.libs.OAuth.RequestToken;

import views.html.*;

import com.evernote.auth.EvernoteAuth;
import com.evernote.auth.EvernoteService;

import com.evernote.edam.notestore.NoteMetadata;
import com.evernote.edam.notestore.NotesMetadataList;
import com.evernote.edam.notestore.NotesMetadataResultSpec;
import com.evernote.edam.notestore.NoteStore;
import com.evernote.edam.notestore.NoteFilter;

import com.evernote.edam.type.*;

import com.evernote.clients.NoteStoreClient;
import com.evernote.clients.ClientFactory;

import com.evernote.edam.error.EDAMUserException;
import com.evernote.edam.error.EDAMSystemException;
import com.evernote.edam.error.EDAMNotFoundException;
import com.evernote.thrift.TException;

import models.User;

public class Evernote extends Controller {
    
    private static final String CONSUMER_KEY = "";
    private static final String CONSUMER_SECRET = "";
    
    private static final String CALLBACK_URL = "http://localhost:9000/auth2";
    private static final String USER_STORE_URL = "https://sandbox.evernote.com/edam/user";
    private static final EvernoteService EVERNOTE_SERVICE = EvernoteService.SANDBOX;
    
    private static ConsumerKey key = new ConsumerKey(CONSUMER_KEY, CONSUMER_SECRET);
    
    private static final OAuth EVERNOTE = new OAuth (new ServiceInfo(
            "https://sandbox.evernote.com/oauth",
            "https://sandbox.evernote.com/oauth",
            "https://sandbox.evernote.com/OAuth.action",
            key
    ));
    
    
    public static Result auth() {
        
        RequestToken oauthResponse = EVERNOTE.retrieveRequestToken(CALLBACK_URL);
        System.out.println(oauthResponse.token);
        
        if(oauthResponse.token != null) {
            session("token", oauthResponse.token);
            session("secret", oauthResponse.secret);
            session("easter", "egg");
            return redirect(EVERNOTE.redirectUrl(oauthResponse.token));
        }
        
        return ok();//index.render());
    }
    
    public static Result auth2() {
        
        String notebooksList = "";
        RequestToken oauthResponse = EVERNOTE.retrieveAccessToken(
            new RequestToken(session("token"),session("secret")), 
            request().queryString().get("oauth_verifier")[0]);
            
        
        
        addTokenToUser(oauthResponse.token);
        
        return redirect(routes.Application.index());//index.render());//"You have the following in your Evernote: " + notebooksList));
    }
    
    public static void addTokenToUser(String token) {
        User currentUser = User.isNewUser(session("email"));
        currentUser.token = token;
        currentUser.save();
    }
    
    public static Result getNotebooks() {
        
        EvernoteAuth evernoteAuth = new EvernoteAuth(EVERNOTE_SERVICE, session("token"));
        List<Notebook> notebooks;
        List<String> notebookNames = new ArrayList<String>();
        
        try {
            NoteStoreClient noteStoreClient = new ClientFactory(evernoteAuth).createNoteStoreClient();
            notebooks = noteStoreClient.listNotebooks();
            
            for(Notebook notebook : notebooks) {
                notebookNames.add(notebook.getName());
            }
            
        } catch (EDAMUserException e) {
            System.out.println(e);
            
        } catch (EDAMSystemException e) {
            System.out.println(e);
            
        } catch (TException e) {
            System.out.println(e);
            
        }
        
        return ok(notebook.render(notebookNames));
    }
    
    public static Result getSharedNotebooks() {
        
        EvernoteAuth evernoteAuth = new EvernoteAuth(EVERNOTE_SERVICE, session("token"));
        List<LinkedNotebook> notebooks;
        List<String> notebookNames = new ArrayList<String>();
        
        try {
            NoteStoreClient noteStoreClient = new ClientFactory(evernoteAuth).createNoteStoreClient();
            notebooks = noteStoreClient.listLinkedNotebooks();
            
            System.out.println("Get all of the shared notebooks");
            
            for(LinkedNotebook notebook : notebooks) {
                System.out.println(notebook.getGuid() + "*** Shared notebook");
                notebookNames.add(notebook.getGuid());
            }
            
        } catch (EDAMUserException e) {
            System.out.println(e);
            
        } catch (EDAMSystemException e) {
            System.out.println(e);
            
        } catch (TException e) {
            System.out.println(e);
            
        } catch (EDAMNotFoundException e) {
            System.out.println(e);
        }
        
        return ok(notebook.render(notebookNames));
    }
    
    //http://dev.evernote.com/doc/reference/javadoc/com/evernote/edam/notestore/NoteStore.Client.html#findNotesMetadata(java.lang.String, com.evernote.edam.notestore.NoteFilter, int, int, com.evernote.edam.notestore.NotesMetadataResultSpec)
    
    public static Result getAllNotes() {
        
        EvernoteAuth evernoteAuth = new EvernoteAuth(EVERNOTE_SERVICE, session("token"));
        List<String> noteTitles = new ArrayList<String>();
        
        try {
            NoteStoreClient noteStoreClient = new ClientFactory(evernoteAuth).createNoteStoreClient();
            
            NoteFilter filter = new NoteFilter();
            
            NotesMetadataResultSpec resultSpec = new NotesMetadataResultSpec();
            resultSpec.setIncludeTitle(true);
            
            NotesMetadataList noteList = noteStoreClient.findNotesMetadata(filter,0,10, resultSpec);
            
            List<NoteMetadata> notes = noteList.getNotes();
            
            for(NoteMetadata note : notes) {
                noteTitles.add(note.getTitle());
            } 
        } catch (EDAMUserException e) {
            System.out.println(e);
        } catch (EDAMSystemException e) {
            System.out.println(e);
        } catch (TException e) {
            System.out.println(e);
        } catch (EDAMNotFoundException e) {
            System.out.println(e);
        }   
        
        return ok(note.render(noteTitles));
    }
}
