package tn.esprit.tools;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDataBase {
   // String url="jdbc:mysql://localhost:3306/medflow";
    //String user="root";
   // String password="";
   String url = "jdbc:mysql://192.168.1.174:3306/medflow?useSSL=false&serverTimezone=UTC";
    String user = "medflow_user";
    String password = "medflow123";
    private Connection cnx;
    static MyDataBase myDB;
    private MyDataBase()  {
        try{
            // Ensure the MySQL JDBC driver is loaded
            Class.forName("com.mysql.cj.jdbc.Driver");
            cnx= DriverManager.getConnection(url,user,password);
            System.out.println("Connected to database successfully");
        }
        catch(SQLException | ClassNotFoundException e){
            System.out.println(e.getMessage());}
    }
    public static MyDataBase getInstance()
    {
        if(myDB==null) {
            myDB = new MyDataBase();

        }
        return myDB;
    }

    public Connection getCnx() {
        return cnx;
    }

    public void setCnx(Connection cnx) {
        this.cnx = cnx;
    }
}
