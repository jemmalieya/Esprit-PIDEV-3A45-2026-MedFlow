# 🤖 Système d'Analyse IA pour Demandes Staff MedFlow

## 📋 Vue d'ensemble

Un système complet d'analyse automatisée des demandes d'accès staff utilisant **OpenAI API** pour examiner le CV, extraire les informations critiques et fournir un verdict d'approbation/rejet/examen approfondi.

---

## 🏗️ Architecture

### 1. **StaffRequestAIAnalysisService.java**
   - Service centralisé pour l'analyse IA
   - Intégration OpenAI Chat Completions API
   - Extraction du contenu du CV (PDF/DOCX/TXT)
   - Structuration du prompt d'analyse
   - Parsing sécurisé de la réponse JSON

### 2. **UserController.java** (Modifications)
   - `onViewStaffDocuments()` - Affiche CV + analyse IA résumée
   - `onApproveStaffRequest()` - Approuve la demande + email Brevo
   - `onRejectStaffRequest()` - Refuse la demande + raison + email Brevo
   - `onRefreshStaffRequests()` - Recharge la liste des demandes PENDING
   - `loadStaffRequests()` - Integration avec `userService.findPendingStaffRequests()`

### 3. **EmailService.java** (Nouvelles méthodes)
   - `sendStaffApprovalEmail()` - Email de confirmation
   - `sendStaffRejectionEmail()` - Email avec motif du refus

---

## 🚀 Fonctionnalités

### A. Analyse Automatisée du CV

**Données extraites automatiquement:**
- ✅ Qualifications clés
- ✅ Match expérience vs rôle demandé
- ✅ Vérification des accréditations
- ✅ Points forts identifiés
- ✅ Préoccupations/Red flags
- ✅ Questions pour l'admin

### B. Verdict Structuré

```json
{
  "verdict": "APPROVE | PENDING_REVIEW | REJECT",
  "confidence_score": 0-100,
  "critical_info": {
    "qualifications": "...",
    "experience_match": "...",
    "credentials": "...",
    "red_flags": "..."
  },
  "strengths": ["..."],
  "concerns": ["..."],
  "recommendation": "...",
  "questions_for_admin": ["..."]
}
```

### C. Workflow Admin

1. Admin sélectionne une demande staff dans le tableau
2. Clique "Ouvrir CV"
3. **L'IA analyse automatiquement:**
   - Le CV (extraction contenu)
   - Tous les détails de la demande (profil, expérience, établissement)
   - Les documents justificatifs
4. Affiche un résumé avec verdict IA
5. Admin prend décision finale (Approuver/Refuser)
6. Email automatique envoyé au candidat (Brevo)

---

## ⚙️ Configuration

### Variables d'environnement requises:

```bash
# OpenAI API
export OPENAI_API_KEY="sk-XXXXXXXXXXXXXXXXXXXXXXXX"

# Brevo Email (déjà configuré)
export BREVO_API_KEY="xkeysib-XXXXXXXXXXXXXXXXXXXXXXXX"
export BREVO_SENDER_EMAIL="noreply@medflow.tn"
export BREVO_SENDER_NAME="MedFlow Admin"
```

### Configuration de Run (IntelliJ IDEA):

1. Run → Edit Configurations
2. Ajouter les variables d'environnement:
   - `OPENAI_API_KEY=sk-...`
   - `BREVO_API_KEY=xkeysib-...`

---

## 📦 Dépendances ajoutées

```xml
<!-- JSON parsing (déjà présent) -->
<com.google.code.gson:gson> v2.10.1

<!-- Apache POI pour extraction DOCX -->
<org.apache.poi:poi-ooxml> v5.2.5
```

---

## 🔍 Flux Détaillé

### Étape 1: Sélection de la demande
```
Admin Dashboard → Table "Demandes staff en attente"
↓
Sélectionne une ligne (User avec staffRequestStatus = PENDING)
```

### Étape 2: Lancement analyse IA
```
Clique "Ouvrir CV"
↓
UserController.onViewStaffDocuments()
  ├─ Crée StaffRequestAIAnalysisService
  ├─ Appelle aiService.analyzeStaffRequest(user)
  │  └─ Extrait CV (PDF/DOCX/TXT)
  │  └─ Construit prompt d'analyse détaillé
  │  └─ Appelle OpenAI Chat Completions API
  │  └─ Parse réponse JSON
  │  └─ Retourne StaffRequestAnalysisResult
  └─ Affiche popup avec verdict
```

### Étape 3: Décision admin

**Option A: Approuver**
```
Admin clique "Approuver"
↓
UserController.onApproveStaffRequest()
  ├─ staffRequestStatus = "APPROVED"
  ├─ staffReviewedAt = NOW()
  ├─ staffReviewedBy = currentAdmin.id
  ├─ verified = true
  ├─ statutCompte = "ACTIF"
  ├─ userService.update(user)
  ├─ EmailService.sendStaffApprovalEmail()
  └─ loadStaffRequests() [rafraîchit tableau]
```

**Option B: Refuser**
```
Admin clique "Refuser"
↓
Popup: "Indiquez la raison du refus"
↓
UserController.onRejectStaffRequest()
  ├─ staffRequestStatus = "REJECTED"
  ├─ staffRequestReason = "[raison saisie]"
  ├─ staffReviewedAt = NOW()
  ├─ staffReviewedBy = currentAdmin.id
  ├─ statutCompte = "REJETÉ"
  ├─ userService.update(user)
  ├─ EmailService.sendStaffRejectionEmail(reason)
  └─ loadStaffRequests() [rafraîchit tableau]
```

---

## 💡 Exemples de Prompts IA

### Prompt envoyé à OpenAI:

```
Analyse détaillée d'une demande d'accès staff MedFlow.

📋 INFORMATIONS CANDIDAT:
- Nom: Dupont jean
- Email: jean.dupont@example.com
- Téléphone: 54430709
- CIN: 12345678

👔 TYPE DE POSTE:
- Rôle demandé: RESP_PATIENTS
- Spécialité: Cardiologie
- Années d'expérience: 8 ans
- Établissement actuel: Clinique La Nile
- Numéro d'autorisation: AUTH-2024-001

📑 CONTENU DU CV:
[extraction contenu CV...]

Fournis une analyse structurée en JSON avec:
- verdict: APPROVE/PENDING_REVIEW/REJECT
- confidence_score: 0-100
- critical_info: {qualifications, experience_match, credentials, red_flags}
- strengths: [...]
- concerns: [...]
- recommendation: "..."
- questions_for_admin: ["..."]

Sois objectif et basé sur critères réalistes d'embauche médical.
```

---

## 🎯 Cas d'usage

### ✅ Cas 1: Candidat qualifié
```
Entrée: CV complet, 10 ans expérience, diplôme vérifiable
AI Verdict: APPROVE (95% confiance)
Résultat: Approuvé automatiquement suggéré à l'admin
```

### ⏳ Cas 2: Candidat potentiel mais doutes
```
Entrée: CV OK mais expérience dans autre domaine
AI Verdict: PENDING_REVIEW (60% confiance)
Résultat: Admin doit examiner manuellement
Questions IA: "Transition de domaine médical claire?"
```

### ❌ Cas 3: Candidat non qualifié
```
Entrée: Pas de diplôme, pas de CV
AI Verdict: REJECT (90% confiance)
Résultat: Refusé, motif fourni
```

---

## 🔐 Sécurité & Considérations

### API Key Management:
- Stockée en variables d'environnement
- **Jamais** commitée en Git
- Acceptable en dev/staging
- Utiliser Vault/Secrets Manager en production

### Rate Limiting:
- OpenAI: ~3-5 req/sec (ajuster selon plan)
- Implémenter cache pour mêmes demandes
- Timeout: 30 secondes par défaut

### Data Privacy:
- CV = stocké localement sur filesystem
- Contenu CV envoyé à OpenAI (PII!)
- ⚠️ Vérifier conformité RGPD/données sensibles

---

## 🛠️ Maintenance & Extension

### Ajouter support pour autres formats CV:
Dans `extractCVContent()`:
```java
} else if (fileName.endsWith(".doc")) {
    return extractFromDOC(cvFile);  // Legacy Word format
} else if (fileName.endsWith(".odt")) {
    return extractFromODT(cvFile);  // OpenDocument
}
```

### Utiliser autre provider IA:
```java
// Remplacer OpenAI par Claude:
private static final String CLAUDE_API_URL = "https://api.anthropic.com/v1/messages";
```

### Cacher les analyses:
```java
// Stocker analysisResult en DB (nouveau champ dans User)
user.setStaffAIAnalysis(GSON.toJson(analysis));
```

---

## 📊 Statistiques & Monitoring

Données potentielles à logger:
- Nombre de demandes analysées/jour
- Temps moyen d'analyse (ms)
- Score de confiance moyen
- Taux approval/rejection/pending
- Coût API OpenAI/jour

---

## ✨ Prochaines étapes

1. **Tester le flow end-to-end:**
   - Soumettre demande staff (CV complet)
   - Admin révise et voit analyse IA
   - Approuver/Refuser
   - Vérifier email reçu

2. **Optimisations:**
   - Améliorer extraction PDF/DOCX
   - Ajouter cache des analyses
   - Analytics dashboard

3. **Production:**
   - Configurer secrets manager
   - Rate limiting robuste
   - Error handling & retry logic

---

## 📚 Fichiers modifiés

| Fichier | Type | Changements |
|---------|------|-----------|
| `StaffRequestAIAnalysisService.java` | ✨ CRÉÉ | Service IA complet |
| `UserController.java` | 📝 MODIFIÉ | Methods staff review + AI call |
| `EmailService.java` | 📝 MODIFIÉ | +sendStaffApprovalEmail, +sendStaffRejectionEmail |
| `pom.xml` | 📝 MODIFIÉ | +Apache POI |
| `AdminDashboard.fxml` | ✅ EXISTANT | "Ouvrir CV" button (déjà en place) |

---

## 🎓 Documentation OpenAI API

- [Chat Completions](https://platform.openai.com/docs/api-reference/chat/create)
- [Models](https://platform.openai.com/docs/models)
- [Authorization](https://platform.openai.com/docs/api-reference/authentication)

---

**Créé:** 2026-04-28  
**Statut:** MVP Ready  
**Auteur:** MedFlow Dev Team

