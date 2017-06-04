package ch.newsriver.data.user;

import ch.newsriver.data.user.river.RiverBase;

import java.util.HashMap;

/**
 * Created by eliapalme on 17/07/16.
 */


public class User {

    private Role role;
    private Limit limit;
    private Subscription subscription;
    private String name;
    private String email;
    private HashMap<Long, RiverBase> rivers = new HashMap<Long, RiverBase>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public HashMap<Long, RiverBase> getRivers() {
        return rivers;
    }

    public void setRivers(HashMap<Long, RiverBase> rivers) {
        this.rivers = rivers;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Limit getLimit() {
        return limit;
    }

    public void setLimit(Limit limit) {
        this.limit = limit;
    }

    public Subscription getSubscription() {
        return subscription;
    }

    public void setSubscription(Subscription subscription) {
        this.subscription = subscription;
    }

    public enum Role {USER, ADMIN}

    public enum Limit {OK, WARNING, EXCEEDED}

    public enum Subscription {FREE, BUSINESS}
}
