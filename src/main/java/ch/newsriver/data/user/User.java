package ch.newsriver.data.user;

import ch.newsriver.data.user.river.RiverBase;

import java.util.HashMap;

/**
 * Created by eliapalme on 17/07/16.
 */


public class User {

    private long id;
    private Role role;
    private Usage usage;
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

    public Usage getUsage() {
        return usage;
    }

    public void setUsage(Usage usage) {
        this.usage = usage;
    }

    public Subscription getSubscription() {
        return subscription;
    }

    public void setSubscription(Subscription subscription) {
        this.subscription = subscription;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public enum Role {USER, ADMIN}

    public enum Usage {OK, WARNING, EXCEEDED}

    public enum Subscription {FREE, BUSINESS}
}
