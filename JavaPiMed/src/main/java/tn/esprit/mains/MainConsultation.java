package tn.esprit.mains;
import tn.esprit.entities.FicheMedicale;
import tn.esprit.entities.Prescription;
import tn.esprit.entities.RendezVous;
import tn.esprit.services.FicheMedicaleService;
import tn.esprit.services.PrescriptionService;
import tn.esprit.services.RendezVousService;
import java.sql.SQLException;


import tn.esprit.tools.MyDataBase;

public class MainConsultation {



        public static void main(String[] args) throws Exception {
            String text = "Hello from Java";

            String command = "Add-Type -AssemblyName System.Speech; " +
                    "$speak = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                    "$speak.Speak('" + text + "');";

            ProcessBuilder pb = new ProcessBuilder(
                    "C:\\Windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe",
                    "-NoProfile",
                    "-ExecutionPolicy", "Bypass",
                    "-Command",
                    "Add-Type -AssemblyName System.Speech; " +
                            "$speak = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                            "$speak.Speak('Bonjour, ceci est un test');"
            );

            pb.inheritIO();

            Process process = pb.start();
            int exitCode = process.waitFor();

            System.out.println("Exit code = " + exitCode);
        }
    }












   /* public static void main(String[] args) {

       
        
        java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());

        ///////////////////////RendezVous CRUD Test////////////////////////////////////
        /* 
        RendezVousService rvService = new RendezVousService();
        
        RendezVous rv = new RendezVous(
                now, 
                "confirmé", 
                "présentiel", 
                "consultation générale", 
                now, 
                19, 
                18, 
                "élevée" 
        );
       rvService.ajouter(rv);

        // Retrieve
        System.out.println("Liste des rendez-vous :");
        for (RendezVous r : rvService.recuperer()) {
            System.out.println(r);
        }

        // Update (example: change statut)
        if (!rvService.recuperer().isEmpty()) {
            RendezVous RVamodif = rvService.recuperer().get(55);
            first.setStatut("modifié");
            rvService.modifier(RVamodif);
            System.out.println("Après modification : " + RVamodif);
        }

        // Delete (example: delete the first rendez-vous)
        if (!rvService.recuperer().isEmpty()) {
            RendezVous RVasupp = rvService.recuperer().get(4);
            rvService.supprimer(RVasupp);
            System.out.println("Après suppression, liste :");
            for (RendezVous r : rvService.recuperer()) {
                System.out.println(r);
            }
        }

        ///////////////////////FicheMedicale CRUD Test////////////////////////////////////
         FicheMedicaleService fmService = new FicheMedicaleService();
        FicheMedicale fm = new FicheMedicale(
                64, 
                "diagnostic test", 
                "observations test", 
                "résultats examens test", 
                now, 
                now, 
                30, 
                now, 
                "signature test"
        );
       fmService.ajouter(fm);
 
        // Retrieve
        System.out.println("Liste des fiches médicales :");
        for (FicheMedicale f : fmService.recuperer()) {
            System.out.println(f);
        }

        // Update (example: change diagnostic)
        if (!fmService.recuperer().isEmpty()) {
            FicheMedicale first = fmService.recuperer().get(1);
            first.setDiagnostic("diagnostic modifié");
            fmService.modifier(first);
            System.out.println("Après modification : " + first);
        }

        // Delete (example: delete the first fiche médicale)
        if (!fmService.recuperer().isEmpty()) {
            FicheMedicale first = fmService.recuperer().get(0);
            fmService.supprimer(first);
            System.out.println("Après suppression, liste :");
            for (FicheMedicale f : fmService.recuperer()) {
                System.out.println(f);
            }
        }

        ///////////////////////Prescription CRUD Test////////////////////////////////////
        /// 
        PrescriptionService pService = new PrescriptionService();
        Prescription p = new Prescription(
                63, 
                "médicament test", 
                "dose test", 
                "fréquence test", 
                7, 
                "instructions test", 
                now
        );
     /*  pService.ajouter(p);

        // Retrieve
        System.out.println("Liste des prescriptions :");
        for (Prescription pr : pService.recuperer()) {
            System.out.println(pr);
        }

        // Update (example: change nom_medicament)
        if (!pService.recuperer().isEmpty()) {
            Prescription first = pService.recuperer().get(0);
            first.setNom_medicament("médicament modifié");
            pService.modifier(first);
            System.out.println("Après modification : " + first);
        }
        // Delete (example: delete the first prescription)
        if (!pService.recuperer().isEmpty()) {
            Prescription first = pService.recuperer().get(0);
            pService.supprimer(first);
            System.out.println("Après suppression, liste :");
            for (Prescription pr : pService.recuperer()) {
                System.out.println(pr);
            }
        }

    }

}
*/