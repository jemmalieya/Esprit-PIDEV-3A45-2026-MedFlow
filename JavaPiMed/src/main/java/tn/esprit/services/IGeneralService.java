package tn.esprit.services;

import tn.esprit.entities.Commande;

import java.sql.SQLException;
import java.util.List;

public interface IGeneralService <T>{
    void ajouter(T t) throws SQLException;
    void supprimer(T t);
    void modifier(T t);
    List<T> recuperer() throws SQLException;
    public T recupererParId(int id);
}
