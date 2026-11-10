package com.example.mobile_project.login;

public class LoginRequest {
    private String email;
    private String password;
    private String dispositivo;

    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
        this.dispositivo = "Android Mobile";
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getDispositivo() { return dispositivo; }
    public void setDispositivo(String dispositivo) { this.dispositivo = dispositivo; }
}
