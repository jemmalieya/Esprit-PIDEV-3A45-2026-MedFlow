package tn.esprit.services;

import java.sql.SQLException;
import java.util.List;

public interface IGeneralService <T>{
    void ajouter(T t) ;
    void supprimer(T t);
    void modifier(T t);
    List<T> recuperer() ;
    T recupererParId(int id);

}
