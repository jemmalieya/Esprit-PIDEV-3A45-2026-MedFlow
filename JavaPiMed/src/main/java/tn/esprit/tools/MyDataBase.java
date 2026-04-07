package tn.esprit.tools;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDataBase {
    String url="jdbc:mysql://localhost:3306/medflow";
    String user="root";
    String password="";
    private Connection cnx;
    static MyDataBase myDB;
    private MyDataBase()  {
        try{
            cnx= DriverManager.getConnection(url,user,password);
            System.out.println("Connected to database successfully");
        }
        catch(SQLException e){
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
