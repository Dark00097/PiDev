package com.nexora.bank.Utils;

import com.nexora.bank.AuthSession;
import com.nexora.bank.Models.User;

/**
 * ══════════════════════════════════════════════════════════════════
 * SessionManager — Pont entre AuthSession et le FrontOffice
 * ══════════════════════════════════════════════════════════════════
 *
 * PROBLÈME RÉSOLU :
 *   Le LoginController utilise AuthSession.setCurrentUser(user) pour
 *   stocker l'utilisateur connecté. Mais UserDashboardAccountsSectionController
 *   appelait SessionManager.getInstance().getCurrentUserId() qui retournait
 *   toujours 0 (session jamais initialisée) → fallback sur getAll() →
 *   TOUS les comptes s'affichaient.
 *
 * SOLUTION :
 *   SessionManager délègue maintenant directement à AuthSession.
 *   ★ Aucune modification du LoginController n'est nécessaire.
 *   ★ Aucune double session — une seule source de vérité : AuthSession.
 *
 * UTILISATION (inchangée dans UserDashboardAccountsSectionController) :
 *   int idUser = SessionManager.getInstance().getCurrentUserId();
 *   → retourne l'idUser du client connecté via AuthSession.
 * ══════════════════════════════════════════════════════════════════
 */
public class SessionManager {

    // ── Singleton ─────────────────────────────────────────────────────────────
    private static SessionManager instance;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    // ── Délégation totale à AuthSession ──────────────────────────────────────

    /**
     * Retourne l'utilisateur connecté depuis AuthSession.
     * AuthSession est la source de vérité (alimenté par LoginController).
     *
     * @return L'objet User connecté, ou null si aucune session active.
     */
    public User getCurrentUser() {
        // ★ DÉLÉGATION : lit depuis AuthSession (pas d'état interne)
        return AuthSession.getCurrentUser();
    }

    /**
     * Retourne l'ID de l'utilisateur connecté.
     * Retourne 0 si aucune session active → getByUser(0) = liste vide (sécurité).
     *
     * @return idUser du client connecté, ou 0 si non connecté.
     */
    public int getCurrentUserId() {
        // ★ LIT DEPUIS AuthSession — c'est lui qui est rempli par LoginController
        User user = AuthSession.getCurrentUser();
        return (user != null) ? user.getIdUser() : 0;
    }

    /**
     * Vérifie si un utilisateur est actuellement connecté.
     *
     * @return true si AuthSession contient un utilisateur, false sinon.
     */
    public boolean isLoggedIn() {
        return AuthSession.getCurrentUser() != null;
    }

    /**
     * Déconnecte l'utilisateur en vidant AuthSession.
     */
    public void logout() {
        AuthSession.setCurrentUser(null);
    }

    /**
     * Méthode conservée pour compatibilité.
     * En pratique, c'est LoginController qui alimente AuthSession directement.
     */
    public void setCurrentUser(User user) {
        AuthSession.setCurrentUser(user);
    }
}