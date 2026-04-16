package tn.esprit.services;

import org.junit.jupiter.api.Test;
import tn.esprit.entities.RendezVous;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

class RendezVousServiceTest {

    @Test
    void ajouter_shouldBindAllFieldsAndExecuteUpdate() {
        Timestamp datetime = Timestamp.valueOf("2026-04-16 10:00:00");
        Timestamp createdAt = Timestamp.valueOf("2026-04-16 09:30:00");
        RendezVous rendezVous = new RendezVous(datetime, "CONFIRME", "ONLINE", "Controle", createdAt, 12, 34, "HIGH");

        JdbcTestSupport.MockPreparedStatementState state = new JdbcTestSupport.MockPreparedStatementState(
                "INSERT INTO rendez_vous(datetime, statut, mode, motif, created_at, idPatient, idStaff, urgency_level) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                null,
                null,
                1
        );
        PreparedStatement preparedStatement = JdbcTestSupport.preparedStatement(state);
        RendezVousService service = JdbcTestSupport.newServiceInstance(RendezVousService.class);
        JdbcTestSupport.injectConnection(service, JdbcTestSupport.connection(preparedStatement));

        service.ajouter(rendezVous);

        assertEquals(datetime, state.getParameters().get(1));
        assertEquals("CONFIRME", state.getParameters().get(2));
        assertEquals("ONLINE", state.getParameters().get(3));
        assertEquals("Controle", state.getParameters().get(4));
        assertEquals(createdAt, state.getParameters().get(5));
        assertEquals(12, state.getParameters().get(6));
        assertEquals(34, state.getParameters().get(7));
        assertEquals("HIGH", state.getParameters().get(8));
    }

    @Test
    void modifier_shouldBindIdAndAllFields() {
        Timestamp datetime = Timestamp.valueOf("2026-04-16 11:00:00");
        Timestamp createdAt = Timestamp.valueOf("2026-04-16 09:45:00");
        RendezVous rendezVous = new RendezVous(7, datetime, "ANNULE", "PHYSIQUE", "Suivi", createdAt, 20, 40, "LOW");

        JdbcTestSupport.MockPreparedStatementState state = new JdbcTestSupport.MockPreparedStatementState(
                "UPDATE rendez_vous SET datetime=?, statut=?, mode=?, motif=?, created_at=?, idPatient=?, idStaff=?, urgency_level=? WHERE id=?",
                null,
                null,
                1
        );
        PreparedStatement preparedStatement = JdbcTestSupport.preparedStatement(state);
        RendezVousService service = JdbcTestSupport.newServiceInstance(RendezVousService.class);
        JdbcTestSupport.injectConnection(service, JdbcTestSupport.connection(preparedStatement));

        service.modifier(rendezVous);

        assertEquals(datetime, state.getParameters().get(1));
        assertEquals("ANNULE", state.getParameters().get(2));
        assertEquals("PHYSIQUE", state.getParameters().get(3));
        assertEquals("Suivi", state.getParameters().get(4));
        assertEquals(createdAt, state.getParameters().get(5));
        assertEquals(20, state.getParameters().get(6));
        assertEquals(40, state.getParameters().get(7));
        assertEquals("LOW", state.getParameters().get(8));
        assertEquals(7, state.getParameters().get(9));
    }

    @Test
    void recuperer_shouldMapRows() {
        Timestamp firstDatetime = Timestamp.valueOf("2026-04-16 08:00:00");
        Timestamp secondDatetime = Timestamp.valueOf("2026-04-17 08:30:00");
        Timestamp createdAt = Timestamp.valueOf("2026-04-15 18:00:00");

        JdbcTestSupport.MockResultSet resultSet = JdbcTestSupport.resultSet(List.of(
                JdbcTestSupport.row("id", 1, "datetime", firstDatetime, "statut", "CONFIRME", "mode", "ONLINE", "motif", "Bilan", "created_at", createdAt, "idPatient", 11, "idStaff", 21, "urgency_level", "HIGH"),
                JdbcTestSupport.row("id", 2, "datetime", secondDatetime, "statut", "ANNULE", "mode", "PHYSIQUE", "motif", "Suivi", "created_at", createdAt, "idPatient", 12, "idStaff", 22, "urgency_level", "LOW")
        ));
        Statement statement = JdbcTestSupport.statement(resultSet);
        RendezVousService service = JdbcTestSupport.newServiceInstance(RendezVousService.class);
        JdbcTestSupport.injectConnection(service, JdbcTestSupport.connection(null, statement));

        List<RendezVous> rendezVousList = service.recuperer();

        assertEquals(2, rendezVousList.size());
        assertEquals(1, rendezVousList.get(0).getId());
        assertEquals(firstDatetime, rendezVousList.get(0).getDatetime());
        assertEquals("CONFIRME", rendezVousList.get(0).getStatut());
        assertEquals(2, rendezVousList.get(1).getId());
        assertEquals(secondDatetime, rendezVousList.get(1).getDatetime());
        assertEquals("ANNULE", rendezVousList.get(1).getStatut());
    }

    @Test
    void recupererParStaffId_shouldReturnOrderedRows() {
        Timestamp firstDatetime = Timestamp.valueOf("2026-04-17 12:00:00");
        Timestamp secondDatetime = Timestamp.valueOf("2026-04-16 12:00:00");
        Timestamp createdAt = Timestamp.valueOf("2026-04-15 18:00:00");

        JdbcTestSupport.MockPreparedStatementState state = new JdbcTestSupport.MockPreparedStatementState(
                "SELECT * FROM rendez_vous WHERE idStaff = ? ORDER BY datetime DESC",
                JdbcTestSupport.resultSet(List.of(
                        JdbcTestSupport.row("id", 4, "datetime", firstDatetime, "statut", "CONFIRME", "mode", "ONLINE", "motif", "Checkup", "created_at", createdAt, "idPatient", 31, "idStaff", 99, "urgency_level", "HIGH"),
                        JdbcTestSupport.row("id", 3, "datetime", secondDatetime, "statut", "CONFIRME", "mode", "ONLINE", "motif", "Consultation", "created_at", createdAt, "idPatient", 30, "idStaff", 99, "urgency_level", "MEDIUM")
                )),
                null,
                0
        );
        PreparedStatement preparedStatement = JdbcTestSupport.preparedStatement(state);
        RendezVousService service = JdbcTestSupport.newServiceInstance(RendezVousService.class);
        JdbcTestSupport.injectConnection(service, JdbcTestSupport.connection(preparedStatement));

        List<RendezVous> rendezVousList = service.recupererParStaffId(99);

        assertEquals(99, state.getParameters().get(1));
        assertEquals(2, rendezVousList.size());
        assertIterableEquals(List.of(4, 3), List.of(rendezVousList.get(0).getId(), rendezVousList.get(1).getId()));
    }
}