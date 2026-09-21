package iwish.common;

import java.io.Serializable;
import java.util.*;

public final class Models {
    private Models() {}
    public static class User implements Serializable { public int id; public String name,email,password; public User(){} public User(int id,String name,String email,String password){this.id=id;this.name=name;this.email=email;this.password=password;} public String toString(){return name;} }
    public static class WishItem implements Serializable { public int id,ownerId; public String title,category,description; public double price,contributed; public boolean purchased; public WishItem(){} public WishItem(int id,int ownerId,String title,String category,String description,double price){this.id=id;this.ownerId=ownerId;this.title=title;this.category=category;this.description=description;this.price=price;} public double remaining(){return Math.max(0,price-contributed);} public String toString(){return title+" — $"+String.format("%.2f",price);} }
    public static class Friendship implements Serializable { public int requesterId,receiverId; public String status; public Friendship(){} public Friendship(int a,int b,String s){requesterId=a;receiverId=b;status=s;} }
    public static class Contribution implements Serializable { public int id,itemId,buyerId; public double amount; public Contribution(){} public Contribution(int id,int itemId,int buyerId,double amount){this.id=id;this.itemId=itemId;this.buyerId=buyerId;this.amount=amount;} }
    public static class Notification implements Serializable { public int id,userId; public String text; public Date createdAt; public boolean read; public Notification(){} public Notification(int id,int userId,String text){this.id=id;this.userId=userId;this.text=text;createdAt=new Date();} }
    public static class Snapshot implements Serializable { public User user; public List<User> friends=new ArrayList<>(), incoming=new ArrayList<>(), discoverable=new ArrayList<>(); public List<WishItem> myItems=new ArrayList<>(), friendItems=new ArrayList<>(); public List<Notification> notifications=new ArrayList<>(); }
}
