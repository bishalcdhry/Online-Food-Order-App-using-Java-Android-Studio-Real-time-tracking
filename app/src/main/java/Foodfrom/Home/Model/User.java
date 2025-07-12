package Foodfrom.Home.Model;

public class User {
    private String name;
    private String Phone;
    private String Email;
    private String password;

    // Default constructor
    public User() {
    }

    // Constructor with parameters
    public User(String name, String phone, String email) {
        this.name = name;
        this.Phone = phone;
        this.Email = email;
    }

    // Getter for fName
    public String getName() {
        return name;
    }

    // Setter for fName
    public void setName(String fName) {
        this.name = fName;
    }

    // Getter for phone
    public String getPhone() {
        return Phone;
    }

    // Setter for phone
    public void setPhone(String phone) {
        this.Phone = phone;
    }

    // Getter for email
    public String getEmail() {
        return Email;
    }

    // Setter for email
    public void setEmail(String email) {
        this.Email = email;
    }

    // Getter for password
    public String getpassword() {
        return password;
    }

    // Setter for password
    public void setpassword(String password) {
        this.password = password;
    }
}
