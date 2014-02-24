package models;

import javax.persistence.*;

import controllers.Google;
import play.db.ebean.*;
import com.avaje.ebean.*;

import java.util.Date;

@Entity
public class User extends Model {

    @Id
    public String email;
    public String name;
    public String password;
    public String token;
    public String googleToken;
    public String googleRefreshToken;
    public long tokenTime;

    public User(String email, String name, String password) {
      this.email = email;
      this.name = name;
      this.password = password;
    }

    public static Finder<String,User> find = new Finder<String,User>(
        String.class, User.class
    );

    public static User authenticate(String email, String password) {
        System.out.println(email);
        return find.where().eq("email", email)
            .eq("password", password).findUnique();
    }

    public static User isNewUser(String email) {
        System.out.println(email);
        return find.where().eq("email", email).findUnique();
    }

    public static void addGoogleOauth(String email, String googleToken, String googleRefreshToken) {
        User user = isNewUser(email);
        if( user != null ) {
            //System.out.println("FOUND USER");
            user.setGoogleToken(googleToken);
            user.setGoogleRefreshToken(googleRefreshToken);
            user.setTokenTime(new Date().getTime());
            user.save();
        }
    }

    public String getGoogleToken() {
        return googleToken;
    }

    public String getGoogleRefreshToken() {
        return googleRefreshToken;
    }

    public void setGoogleToken(String googleToken) {
        this.googleToken = googleToken;
    }

    public void setGoogleRefreshToken(String googleRefreshToken) {
        this.googleRefreshToken = googleRefreshToken;
    }

    public long getTokenTime() {
        return tokenTime;
    }

    public void setTokenTime(long tokenTime) {
        this.tokenTime = tokenTime;
    }

    public void refreshToken() {
        setGoogleToken(Google.refreshToken(getGoogleRefreshToken()));
        setTokenTime(new Date().getTime());
        save();
    }
}
