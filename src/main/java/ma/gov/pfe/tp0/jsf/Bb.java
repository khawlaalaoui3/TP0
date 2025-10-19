package ma.gov.pfe.tp0.jsf;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.model.SelectItem;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Backing bean pour la page JSF index.xhtml.
 * Portée view pour conserver l'état de la conversation qui dure pendant plusieurs requêtes HTTP.
 * La portée view nécessite l'implémentation de Serializable (le backing bean peut être mis en mémoire secondaire).
 */
@Named
@ViewScoped
public class Bb implements Serializable {

    private String roleSysteme;
    private boolean roleSystemeChangeable = true;
    private List<SelectItem> listeRolesSysteme;
    private String question;
    private String reponse;
    private StringBuilder conversation = new StringBuilder();

    @Inject
    private FacesContext facesContext;

    public Bb() {
    }

    public String getRoleSysteme() {
        return roleSysteme;
    }

    public void setRoleSysteme(String roleSysteme) {
        this.roleSysteme = roleSysteme;
    }

    public boolean isRoleSystemeChangeable() {
        return roleSystemeChangeable;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getReponse() {
        return reponse;
    }

    public void setReponse(String reponse) {
        this.reponse = reponse;
    }

    public String getConversation() {
        return conversation.toString();
    }

    public void setConversation(String conversation) {
        this.conversation = new StringBuilder(conversation);
    }

    /**
     * Envoie la question au serveur avec un correcteur orthographique.
     * Le correcteur utilise l'algorithme de Levenshtein pour détecter les fautes
     * et suggérer des corrections basées sur un dictionnaire prédéfini.
     *
     * @return null pour rester sur la même page.
     */
    public String envoyer() {
        if (question == null || question.isBlank()) {
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Texte question vide", "Il manque le texte de la question");
            facesContext.addMessage(null, message);
            return null;
        }

        // Dictionnaire de mots corrects
        String[] dictionnaire = {
                "bonjour", "bonsoir", "comment", "excusez", "merci",
                "please", "hello", "salut", "revoir", "comment allez vous",
                "pouvez", "pouvez vous", "merci beaucoup", "s'il vous plaît",
                "s'il te plaît", "oui", "non", "peut être", "cela", "ceci"
        };

        StringBuilder suggestionsBuilder = new StringBuilder();
        suggestionsBuilder.append("|| CORRECTEUR ORTHOGRAPHIQUE\n");

        String questionLower = question.toLowerCase(Locale.FRENCH);
        String[] mots = questionLower.split("\\s+");
        boolean erreurTrouvee = false;

        for (String mot : mots) {
            String meilleurCandidat = null;
            int distanceMin = 2;

            for (String motDictionnaire : dictionnaire) {
                int distance = levenshteinDistance(mot, motDictionnaire);
                if (distance < distanceMin) {
                    distanceMin = distance;
                    meilleurCandidat = motDictionnaire;
                    erreurTrouvee = true;
                }
            }

            if (meilleurCandidat != null && !mot.equals(meilleurCandidat)) {
                suggestionsBuilder.append("FAUTE_DE_TAPE \"").append(mot)
                        .append("\" → CORRECTION \"").append(meilleurCandidat).append("\"\n");
            }
        }

        if (!erreurTrouvee) {
            suggestionsBuilder.append("✓ Pas d'erreur détectée !\n");
        }

        suggestionsBuilder.append("Texte original : ").append(question).append("\n||");

        this.reponse = suggestionsBuilder.toString();

        if (this.conversation.isEmpty()) {
            this.roleSystemeChangeable = false;
        }

        afficherConversation();
        return null;
    }

    /**
     * Calcule la distance de Levenshtein entre deux chaînes de caractères.
     * Cette distance représente le nombre minimum de modifications (insertion, suppression, substitution)
     * nécessaires pour transformer une chaîne en une autre.
     *
     * @param s1 première chaîne
     * @param s2 deuxième chaîne
     * @return la distance de Levenshtein
     */
    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }

        return dp[s1.length()][s2.length()];
    }

    /**
     * Pour un nouveau chat. Termine la portée view en retournant "index".
     * @return "index"
     */
    public String nouveauChat() {
        return "index";
    }

    /**
     * Affiche la conversation dans le textarea de la page JSF.
     */
    private void afficherConversation() {
        this.conversation.append("== User:\n")
                .append(question)
                .append("\n== Serveur:\n")
                .append(reponse)
                .append("\n\n");
    }

    /**
     * Retourne la liste des rôles système prédéfinis.
     * @return liste des rôles
     */
    public List<SelectItem> getRolesSysteme() {
        if (this.listeRolesSysteme == null) {
            this.listeRolesSysteme = new ArrayList<>();

            String role = """
                You are a helpful assistant. You help the user to find the information they need.
                If the user type a question, you answer it.
                """;
            this.listeRolesSysteme.add(new SelectItem(role, "Assistant"));

            role = """
                You are an interpreter. You translate from English to French and from French to English.
                If the user type a French text, you translate it into English.
                If the user type an English text, you translate it into French.
                """;
            this.listeRolesSysteme.add(new SelectItem(role, "Traducteur Anglais-Français"));

            role = """
                You are a travel guide. If the user type the name of a country or of a town,
                you tell them what are the main places to visit.
                """;
            this.listeRolesSysteme.add(new SelectItem(role, "Guide touristique"));
        }

        return this.listeRolesSysteme;
    }
}