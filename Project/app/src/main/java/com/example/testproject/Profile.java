package com.example.testproject;

public class Profile
{
    private String username;
    private String firstname;
    private String lastname;
    private String bio;
    private float employerrating;
    private float workerrating;

    public Profile(String username, String firstname, String lastname, String bio, float employerrating, float workerrating) {
        this.username = username;
        this.firstname = firstname;
        this.lastname = lastname;
        this.bio = bio;
        this.employerrating = employerrating;
        this.workerrating = workerrating;
    }

    public Profile() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public float getEmployerrating() {
        return employerrating;
    }

    public void setEmployerrating(float employerrating) {
        this.employerrating = employerrating;
    }

    public float getWorkerrating() {
        return workerrating;
    }

    public void setWorkerrating(float workerrating) {
        this.workerrating = workerrating;
    }

    @Override
    public String toString() {
        return "Profile{" +
                "username='" + username + '\'' +
                ", firstname='" + firstname + '\'' +
                ", lastname='" + lastname + '\'' +
                ", bio='" + bio + '\'' +
                ", employerrating=" + employerrating +
                ", workerrating=" + workerrating +
                '}';
    }
}
