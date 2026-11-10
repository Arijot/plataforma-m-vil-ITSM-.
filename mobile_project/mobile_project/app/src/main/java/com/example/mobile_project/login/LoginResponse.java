package com.example.mobile_project.login;
import com.example.mobile_project.user.User;
import com.google.gson.annotations.SerializedName;

public class LoginResponse {
    private String message;

    @SerializedName("access_token")
    private String accessToken;

    @SerializedName("error_code")
    private String errorCode;

    @SerializedName("user")
    private User user;

    public String getMessage() { return message; }
    public String getAccessToken() { return accessToken; }
    public String getErrorCode() { return errorCode; }
    public User getUser() { return user; }
}
