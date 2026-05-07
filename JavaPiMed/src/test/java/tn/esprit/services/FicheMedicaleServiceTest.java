package tn.esprit.services;

import org.junit.jupiter.api.Test;
import tn.esprit.entities.FicheMedicale;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FicheMedicaleServiceTest {

    @Test
    void ajouter_shouldReturnGeneratedIdAndSetEntityId() {
        Timestamp startTime = Timestamp.valueOf("2026-04-16 09:00:00");
        Timestamp endTime = Timestamp.valueOf("2026-04-16 09:30:00");
        Timestamp createdAt = Timestamp.valueOf("2026-04-16 09:35:00");
        FicheMedicale ficheMedicale = new FicheMedicale(15, "Diagnostic", "Obs", "Examens", startTime, endTime, 30, createdAt, "Dr. Test");

        JdbcTestSupport.MockResultSet generatedKeys = JdbcTestSupport.resultSet(List.of(
                JdbcTestSupport.row("id", 55)
        ));
        JdbcTestSupport.MockPreparedStatementState state = new JdbcTestSupport.MockPreparedStatementState(
                "INSERT INTO fiche_medicale(rendez_vous_id, diagnostic, observations, resultats_examens, start_time, end_time, duree_minutes, created_at, signature) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                null,
                generatedKeys,
                1
        );
        PreparedStatement preparedStatement = JdbcTestSupport.preparedStatement(state);
        FicheMedicaleService service = JdbcTestSupport.newServiceInstance(FicheMedicaleService.class);
        JdbcTestSupport.injectConnection(service, JdbcTestSupport.connection(preparedStatement));

        int generatedId = service.ajouter(ficheMedicale, JdbcTestSupport.connection(preparedStatement));

        assertEquals(55, generatedId);
        assertEquals(55, ficheMedicale.getId());
        assertEquals(15, state.getParameters().get(1));
        assertEquals("Diagnostic", state.getParameters().get(2));
        assertEquals("Obs", state.getParameters().get(3));
        assertEquals("Examens", state.getParameters().get(4));
        assertEquals(startTime, state.getParameters().get(5));
        assertEquals(endTime, state.getParameters().get(6));
        assertEquals(30, state.getParameters().get(7));
        assertEquals(createdAt, state.getParameters().get(8));
        assertEquals("Dr. Test", state.getParameters().get(9));
    }

    @Test
    void modifier_shouldBindAllFields() {
        Timestamp startTime = Timestamp.valueOf("2026-04-16 10:00:00");
        Timestamp endTime = Timestamp.valueOf("2026-04-16 10:45:00");
        Timestamp createdAt = Timestamp.valueOf("2026-04-16 10:50:00");
        FicheMedicale ficheMedicale = new FicheMedicale(9, 18, "Diag", "Obs", "Exam", startTime, endTime, 45, createdAt, "Signature");

        JdbcTestSupport.MockPreparedStatementState state = new JdbcTestSupport.MockPreparedStatementState(
                "UPDATE fiche_medicale SET rendez_vous_id=?, diagnostic=?, observations=?, resultats_examens=?, start_time=?, end_time=?, duree_minutes=?, created_at=?, signature=? WHERE id=?",
                null,
                null,
                1
        );
        PreparedStatement preparedStatement = JdbcTestSupport.preparedStatement(state);
        FicheMedicaleService service = JdbcTestSupport.newServiceInstance(FicheMedicaleService.class);
        JdbcTestSupport.injectConnection(service, JdbcTestSupport.connection(preparedStatement));

        service.modifier(ficheMedicale);

        assertEquals(18, state.getParameters().get(1));
        assertEquals("Diag", state.getParameters().get(2));
        assertEquals("Obs", state.getParameters().get(3));
        assertEquals("Exam", state.getParameters().get(4));
        assertEquals(startTime, state.getParameters().get(5));
        assertEquals(endTime, state.getParameters().get(6));
        assertEquals(45, state.getParameters().get(7));
        assertEquals(createdAt, state.getParameters().get(8));
        assertEquals("Signature", state.getParameters().get(9));
        assertEquals(9, state.getParameters().get(10));
    }

    @Test
    void recuperer_shouldMapRows() {
        Timestamp startTime = Timestamp.valueOf("2026-04-16 08:00:00");
        Timestamp endTime = Timestamp.valueOf("2026-04-16 08:30:00");
        Timestamp createdAt = Timestamp.valueOf("2026-04-16 08:35:00");

        JdbcTestSupport.MockResultSet resultSet = JdbcTestSupport.resultSet(List.of(
                JdbcTestSupport.row("id", 4, "rendez_vous_id", 10, "diagnostic", "Diag1", "observations", "Obs1", "resultats_examens", "Exam1", "start_time", startTime, "end_time", endTime, "duree_minutes", 30, "created_at", createdAt, "signature", "Sig1"),
                JdbcTestSupport.row("id", 5, "rendez_vous_id", 11, "diagnostic", "Diag2", "observations", "Obs2", "resultats_examens", "Exam2", "start_time", startTime, "end_time", endTime, "duree_minutes", 35, "created_at", createdAt, "signature", "Sig2")
        ));
        Statement statement = JdbcTestSupport.statement(resultSet);
        FicheMedicaleService service = JdbcTestSupport.newServiceInstance(FicheMedicaleService.class);
        JdbcTestSupport.injectConnection(service, JdbcTestSupport.connection(null, statement));

        List<FicheMedicale> ficheMedicales = service.recuperer();

        assertEquals(2, ficheMedicales.size());
        assertEquals(4, ficheMedicales.get(0).getId());
        assertEquals(10, ficheMedicales.get(0).getRendez_vous_id());
        assertEquals("Diag1", ficheMedicales.get(0).getDiagnostic());
        assertEquals(5, ficheMedicales.get(1).getId());
        assertEquals(11, ficheMedicales.get(1).getRendez_vous_id());
        assertEquals("Diag2", ficheMedicales.get(1).getDiagnostic());
    }

    @Test
    void getByRendezVousId_shouldReturnFirstMatchOrNull() {
        Timestamp startTime = Timestamp.valueOf("2026-04-16 08:00:00");
        Timestamp endTime = Timestamp.valueOf("2026-04-16 08:30:00");
        Timestamp createdAt = Timestamp.valueOf("2026-04-16 08:35:00");

        JdbcTestSupport.MockPreparedStatementState state = new JdbcTestSupport.MockPreparedStatementState(
                "SELECT * FROM fiche_medicale WHERE rendez_vous_id = ? LIMIT 1",
                JdbcTestSupport.resultSet(List.of(
                        JdbcTestSupport.row("id", 8, "rendez_vous_id", 14, "diagnostic", "Diag", "observations", "Obs", "resultats_examens", "Exam", "start_time", startTime, "end_time", endTime, "duree_minutes", 30, "created_at", createdAt, "signature", "Sig")
                )),
                null,
                0
        );
        PreparedStatement preparedStatement = JdbcTestSupport.preparedStatement(state);
        FicheMedicaleService service = JdbcTestSupport.newServiceInstance(FicheMedicaleService.class);
        JdbcTestSupport.injectConnection(service, JdbcTestSupport.connection(preparedStatement));

        FicheMedicale ficheMedicale = service.getByRendezVousId(14);

        assertEquals(14, state.getParameters().get(1));
        assertEquals(8, ficheMedicale.getId());
        assertEquals("Diag", ficheMedicale.getDiagnostic());

        JdbcTestSupport.MockPreparedStatementState emptyState = new JdbcTestSupport.MockPreparedStatementState(
                "SELECT * FROM fiche_medicale WHERE rendez_vous_id = ? LIMIT 1",
                JdbcTestSupport.resultSet(List.of()),
                null,
                0
        );
        PreparedStatement emptyPreparedStatement = JdbcTestSupport.preparedStatement(emptyState);
        FicheMedicaleService emptyService = JdbcTestSupport.newServiceInstance(FicheMedicaleService.class);
        JdbcTestSupport.injectConnection(emptyService, JdbcTestSupport.connection(emptyPreparedStatement));

        assertNull(emptyService.getByRendezVousId(999));
    }
}