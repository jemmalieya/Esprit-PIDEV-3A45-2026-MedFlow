# Corrections - Système de Recherche Vocale (ProduitVoiceSearchService)

## Problèmes Identifiés
D'après les logs de diagnostic, le système avait les problèmes suivants:
- **Scores très bas** (0.0267, 0.0356, 0.0444) - bien au-dessous du seuil de 0.6
- **Timeout PowerShell** - "max_wait_exceeded" : la reconnaissance vocale n'avait pas assez de temps
- **Pas de reconnaissance phonétique** - les mots mal transcrits n'étaient pas matchés
- **Seuils trop strictes** pour une reconnaissance vocale imparfaite

## Corrections Appliquées

### 1. **Augmentation des Timeouts de Reconnaissance Vocale**
   - `InitialSilenceTimeout` : **3s → 5s** (laisse plus de temps avant de commencer)
   - `BabbleTimeout` : **8s → 12s** (plus de tolérance pour la parole)
   - `EndSilenceTimeout` : **1s → 2s** (attend plus de silence avant fin)
   - `EndSilenceTimeoutAmbiguous` : **1.5s → 2.5s** (moins ambiguë)
   - Timeout total Java : **3s min → 8s min**

### 2. **Réduction des Seuils de Validation**
   ```
   SCORE_MIN_VALIDATION    : 0.60 → 0.45  (plus flexible)
   SCORE_GAP_MIN           : 0.06 → 0.05  (moins strict)
   CONFIDENCE_MIN          : 0.30 → 0.20  (accepte plus)
   SCORE_MIN_FALLBACK      : NEW 0.40     (fallback mode)
   ```

### 3. **Ajout de la Reconnaissance Phonétique (Soundex)**
   - Nouvelle méthode `soundex()` pour encoder les mots phonétiquement
   - Bonus de 10% si le code Soundex correspond
   - Améliore le matching des mots mal transcrits (ex: "Doliprane" vs "Dolipran")

### 4. **Amélioration de la Logique de Scoring**
   - Calcul du score ajusté : `(similarité × 0.55) + (tokens × 0.20) + (confiance × 0.10) + bonus_préfixe + bonus_phonétique`
   - Fallback adaptatif : écart minimum réduit à 0.02 si confiance PowerShell < 0.10
   - Validation moins stricte si confiance très basse (mode fallback)

## Résultats Attendus
✅ Meilleure reconnaissance des produits même avec parole imparfaite
✅ Plus de temps pour que l'utilisateur parle
✅ Matching phonétique pour les erreurs de transcription
✅ Scores calculés plus intelligemment
✅ Moins de faux rejets

## Comment Tester
```java
// Dans ProduitController.java, au moment de l'appel vocal:
String resultat = produitVoiceSearchService.ecouterNomProduit(10, nomsProduits);
// Le timeout de 10s donne maintenant 10+ secondes réelles à l'utilisateur pour parler
```

## Notes Techniques
- L'algorithme Soundex divise les consonnes en 6 catégories phonétiques
- Les voyelles et h,w,y sont ignorés (non-phonétiques)
- Les codes doubles sont éliminés (B-F-P-V → tous "1")
- Encodage résultant : 4 caractères (lettre + 3 chiffres)

