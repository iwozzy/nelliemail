package controllers;

import play.*;
import play.mvc.*;
import play.data.*;
import static play.data.Form.*;

import models.*;
import views.html.*;
import models.User;

public class Application extends Controller {
        
    public static Result index() {
        if(session("email") == null) {
            return ok(index.render(form(Login.class)));
            //return login();
        } else {
            return ok(app.render());
        }
    }
    
    public static Result login() {
        return ok();//login.render(form(Login.class)));
    }
    
    public static Result logOut() {
        session().clear();
        return ok(index.render(form(Login.class)));
    }
    
    
    

    public static Result authenticate() {
        System.out.println("here");
        Form<Login> loginForm = form(Login.class).bindFromRequest();

        if(loginForm.hasErrors()) {
            return badRequest(index.render(loginForm));
        } else {
            User currentUser = User.isNewUser(loginForm.get().email);
            
            System.out.println("********"+currentUser.email+"*******");
            System.out.println("********"+currentUser.token+"*******");
            session().clear();
            session("email", currentUser.email);
            if ( currentUser.googleRefreshToken != null ) {
                //System.out.println("CURRENT USER RT: " + currentUser.googleRefreshToken);
                session("refresh_token", currentUser.googleRefreshToken);
            }
            if(currentUser.token != null) {
                session("token", currentUser.token);
            }
            return redirect(routes.Application.index());
        }
    }

    /*
        This hands sometimes: CLEAN and COMPILE in the playframework console
     */
    public static class Login {
        public String email;
        public String password;


        public String validate() {

            System.out.println("First: " + email);
            System.out.println("Password: " + password);

            if (User.authenticate(email, password) == null) {
                return "Invalid user or password";
            }

            return null;
        }
    }

}


