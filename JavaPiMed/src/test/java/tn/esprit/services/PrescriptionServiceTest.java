package tn.esprit.services;

import org.junit.jupiter.api.Test;
import tn.esprit.entities.Prescription;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PrescriptionServiceTest {

    @Test
    void ajouter_shouldBindAllFields() {
        Timestamp createdAt = Timestamp.valueOf("2026-04-16 09:15:00");
        Prescription prescription = new Prescription(22, "Paracetamol", "500mg", "2 fois par jour", 7, "Après repas", createdAt);

        JdbcTestSupport.MockPreparedStatementState state = new JdbcTestSupport.MockPreparedStatementState(
                "INSERT INTO prescription(fiche_medicale_id, nom_medicament, dose, frequence, duree, instructions, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                null,
                null,
                1
        );
        PreparedStatement preparedStatement = JdbcTestSupport.preparedStatement(state);
        PrescriptionService service = JdbcTestSupport.newServiceInstance(PrescriptionService.class);
        JdbcTestSupport.injectConnection(service, JdbcTestSupport.connection(preparedStatement));

        service.ajouter(prescription, JdbcTestSupport.connection(preparedStatement));

        assertEquals(22, state.getParameters().get(1));
        assertEquals("Paracetamol", state.getParameters().get(2));
        assertEquals("500mg", state.getParameters().get(3));
        assertEquals("2 fois par jour", state.getParameters().get(4));
        assertEquals(7, state.getParameters().get(5));
        assertEquals("Après repas", state.getParameters().get(6));
        assertEquals(createdAt, state.getParameters().get(7));
    }

    @Test
    void modifier_shouldBindAllFieldsIncludingId() {
        Timestamp createdAt = Timestamp.valueOf("2026-04-16 09:15:00");
        Prescription prescription = new Prescription(3, 22, "Paracetamol", "500mg", "2 fois par jour", 7, "Après repas", createdAt);

        JdbcTestSupport.MockPreparedStatementState state = new JdbcTestSupport.MockPreparedStatementState(
                "UPDATE prescription SET fiche_medicale_id=?, nom_medicament=?, dose=?, frequence=?, duree=?, instructions=?, created_at=? WHERE id=?",
                null,
                null,
                1
        );
        PreparedStatement preparedStatement = JdbcTestSupport.preparedStatement(state);
        PrescriptionService service = JdbcTestSupport.newServiceInstance(PrescriptionService.class);
        JdbcTestSupport.injectConnection(service, JdbcTestSupport.connection(preparedStatement));

        service.modifier(prescription);

        assertEquals(22, state.getParameters().get(1));
        assertEquals("Paracetamol", state.getParameters().get(2));
        assertEquals("500mg", state.getParameters().get(3));
        assertEquals("2 fois par jour", state.getParameters().get(4));
        assertEquals(7, state.getParameters().get(5));
        assertEquals("Après repas", state.getParameters().get(6));
        assertEquals(createdAt, state.getParameters().get(7));
        assertEquals(3, state.getParameters().get(8));
    }

    @Test
    void recuperer_shouldMapRows() {
        Timestamp createdAt = Timestamp.valueOf("2026-04-16 09:15:00");

        JdbcTestSupport.MockResultSet resultSet = JdbcTestSupport.resultSet(List.of(
                JdbcTestSupport.row("id", 1, "fiche_medicale_id", 22, "nom_medicament", "Paracetamol", "dose", "500mg", "frequence", "2 fois par jour", "duree", 7, "instructions", "Après repas", "created_at", createdAt),
                JdbcTestSupport.row("id", 2, "fiche_medicale_id", 22, "nom_medicament", "Ibuprofen", "dose", "200mg", "frequence", "1 fois par jour", "duree", 5, "instructions", "Après repas", "created_at", createdAt)
        ));
        Statement statement = JdbcTestSupport.statement(resultSet);
        PrescriptionService service = JdbcTestSupport.newServiceInstance(PrescriptionService.class);
        JdbcTestSupport.injectConnection(service, JdbcTestSupport.connection(null, statement));

        List<Prescription> prescriptions = service.recuperer();

        assertEquals(2, prescriptions.size());
        assertEquals(1, prescriptions.get(0).getId());
        assertEquals("Paracetamol", prescriptions.get(0).getNom_medicament());
        assertEquals(2, prescriptions.get(1).getId());
        assertEquals("Ibuprofen", prescriptions.get(1).getNom_medicament());
    }

    @Test
    void getByFicheMedicaleId_shouldReturnAllMatches() {
        Timestamp createdAt = Timestamp.valueOf("2026-04-16 09:15:00");

        JdbcTestSupport.MockPreparedStatementState state = new JdbcTestSupport.MockPreparedStatementState(
                "SELECT * FROM prescription WHERE fiche_medicale_id = ?",
                JdbcTestSupport.resultSet(List.of(
                        JdbcTestSupport.row("id", 1, "fiche_medicale_id", 22, "nom_medicament", "Paracetamol", "dose", "500mg", "frequence", "2 fois par jour", "duree", 7, "instructions", "Après repas", "created_at", createdAt),
                        JdbcTestSupport.row("id", 2, "fiche_medicale_id", 22, "nom_medicament", "Ibuprofen", "dose", "200mg", "frequence", "1 fois par jour", "duree", 5, "instructions", "Après repas", "created_at", createdAt)
                )),
                null,
                0
        );
        PreparedStatement preparedStatement = JdbcTestSupport.preparedStatement(state);
        PrescriptionService service = JdbcTestSupport.newServiceInstance(PrescriptionService.class);
        JdbcTestSupport.injectConnection(service, JdbcTestSupport.connection(preparedStatement));

        List<Prescription> prescriptions = service.getByFicheMedicaleId(22);

        assertEquals(22, state.getParameters().get(1));
        assertEquals(2, prescriptions.size());
        assertEquals("Paracetamol", prescriptions.get(0).getNom_medicament());
        assertEquals("Ibuprofen", prescriptions.get(1).getNom_medicament());
    }
}