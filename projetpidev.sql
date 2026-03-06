-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Hôte : 127.0.0.1
-- Généré le : jeu. 05 mars 2026 à 04:11
-- Version du serveur : 10.4.32-MariaDB
-- Version de PHP : 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Base de données : `projetpidev`
--

-- --------------------------------------------------------

--
-- Structure de la table `cashback`
--

CREATE TABLE `cashback` (
  `idCashback` int(11) NOT NULL,
  `idPartenaire` int(11) NOT NULL,
  `idTransaction` int(11) NOT NULL,
  `montantAchat` double NOT NULL,
  `tauxApplique` double NOT NULL,
  `montantCashback` double NOT NULL,
  `dateAchat` varchar(20) NOT NULL,
  `dateCredit` varchar(20) DEFAULT NULL,
  `dateExpiration` varchar(20) DEFAULT NULL,
  `statut` varchar(30) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `cashback_entries`
--

CREATE TABLE `cashback_entries` (
  `id_cashback` int(11) NOT NULL,
  `id_user` int(11) NOT NULL,
  `id_partenaire` int(11) DEFAULT NULL,
  `partenaire_nom` varchar(120) NOT NULL,
  `montant_achat` double NOT NULL,
  `taux_applique` double NOT NULL,
  `montant_cashback` double NOT NULL,
  `date_achat` date NOT NULL,
  `date_credit` date DEFAULT NULL,
  `date_expiration` date DEFAULT NULL,
  `statut` varchar(30) NOT NULL,
  `transaction_ref` varchar(120) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `user_rating` double DEFAULT NULL,
  `user_rating_comment` varchar(255) DEFAULT NULL,
  `bonus_decision` varchar(20) NOT NULL DEFAULT 'Pending',
  `bonus_note` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `cashback_entries`
--

INSERT INTO `cashback_entries` (`id_cashback`, `id_user`, `id_partenaire`, `partenaire_nom`, `montant_achat`, `taux_applique`, `montant_cashback`, `date_achat`, `date_credit`, `date_expiration`, `statut`, `transaction_ref`, `created_at`, `user_rating`, `user_rating_comment`, `bonus_decision`, `bonus_note`) VALUES
(1, 3, NULL, 'Zara', 120, 3, 3.6, '2026-02-27', '2026-02-27', '2026-03-14', 'Expire', '', '2026-02-19 21:21:40', NULL, NULL, 'Pending', NULL),
(2, 2, 1, 'Geant', 10, 5, 0.5, '2026-02-20', NULL, '2026-08-20', 'En attente', '', '2026-02-19 21:30:12', NULL, NULL, 'Pending', NULL),
(3, 2, 1, 'Geant', 10, 5, 0.5, '2026-02-27', NULL, '2026-08-27', 'En attente', '', '2026-02-19 21:39:41', NULL, NULL, 'Pending', NULL),
(4, 3, 1, 'Geant', 1000, 3, 30, '2026-03-27', NULL, '2026-09-27', 'En attente', '', '2026-03-02 00:34:38', 2, 'bon', 'Rejected', 'Pas de bonus sur cette transaction'),
(5, 3, 1, 'Geant', 100, 2, 12, '2026-03-02', NULL, '2026-09-02', 'Valide', 'ADMIN_REWARD +5.00 (Bonus fidelite) | ADMIN_REWARD +5.00 (Bonus fidelite)', '2026-03-02 00:37:58', 5, '', 'Pending', 'Bonus fidelite'),
(6, 3, 1, 'Geant', 5, 1, 0.05, '2026-03-26', NULL, '2026-09-26', 'En attente', '', '2026-03-04 19:25:55', 5, 'd', 'Pending', NULL),
(7, 3, 1, 'Geant', 9, 1, 0.09, '2026-03-26', NULL, '2026-09-26', 'En attente', '', '2026-03-04 19:28:59', NULL, NULL, 'Pending', NULL),
(8, 3, 1, 'Geant', 2, 1, 0.02, '2026-03-19', NULL, '2026-09-19', 'En attente', '', '2026-03-04 20:21:08', NULL, NULL, 'Pending', NULL),
(9, 3, 2, 'carrefour', 130, 2, 2.6, '2026-03-26', NULL, '2026-09-26', 'En attente', '', '2026-03-04 20:28:11', NULL, NULL, 'Pending', NULL),
(10, 3, 2, 'carrefour', 5, 1, 0.05, '2026-03-20', NULL, '2026-09-20', 'En attente', '', '2026-03-04 20:38:56', NULL, NULL, 'Pending', NULL);

-- --------------------------------------------------------

--
-- Structure de la table `coffrevirtuel`
--

CREATE TABLE `coffrevirtuel` (
  `idCoffre` int(11) NOT NULL,
  `nom` varchar(50) NOT NULL,
  `objectifMontant` decimal(12,2) NOT NULL,
  `montantActuel` decimal(12,2) NOT NULL DEFAULT 0.00,
  `dateCreation` varchar(20) NOT NULL,
  `dateObjectifs` varchar(20) DEFAULT NULL,
  `status` varchar(20) NOT NULL,
  `estVerrouille` tinyint(1) NOT NULL DEFAULT 1,
  `idCompte` int(11) DEFAULT NULL,
  `idUser` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `coffrevirtuel`
--

INSERT INTO `coffrevirtuel` (`idCoffre`, `nom`, `objectifMontant`, `montantActuel`, `dateCreation`, `dateObjectifs`, `status`, `estVerrouille`, `idCompte`, `idUser`) VALUES
(6, 'Rahma', 1000.00, 120.00, '2026-02-05', '2026-02-04', 'Actif', 0, NULL, NULL);

-- --------------------------------------------------------

--
-- Structure de la table `compte`
--

CREATE TABLE `compte` (
  `idCompte` int(11) NOT NULL,
  `numeroCompte` varchar(30) NOT NULL,
  `solde` decimal(12,2) NOT NULL DEFAULT 0.00,
  `dateOuverture` varchar(10) NOT NULL,
  `statutCompte` varchar(20) NOT NULL,
  `plafondRetrait` decimal(12,2) NOT NULL,
  `plafondVirement` decimal(12,2) NOT NULL,
  `typeCompte` varchar(20) NOT NULL,
  `idUser` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `compte`
--

INSERT INTO `compte` (`idCompte`, `numeroCompte`, `solde`, `dateOuverture`, `statutCompte`, `plafondRetrait`, `plafondVirement`, `typeCompte`, `idUser`) VALUES
(44, 'NT0512', 1400.00, '2026-02-11', 'Bloque', 12.00, 17.00, 'Professionnel', NULL),
(45, 'CB-04', 7820.00, '2026-02-24', 'Bloque', 500.00, 100.00, 'Epargne', NULL),
(46, 'CB-01', 1453.00, '2026-02-25', 'Bloque', 5.00, 10.00, 'Courant', NULL),
(47, 'CB-02', 0.00, '2026-02-25', 'Actif', 12.00, 10.00, 'Professionnel', NULL),
(48, 'CB-18', 1245.24, '2026-02-26', 'Bloque', 5.00, 15.00, 'Epargne', NULL),
(49, 'bouth0', 1450.00, '2026-02-03', 'Bloque', 120.00, 120.00, 'Professionnel', NULL),
(50, 'CB-06', 1450.00, '2026-02-12', 'Bloque', 15.00, 14.00, 'Courant', NULL);

-- --------------------------------------------------------

--
-- Structure de la table `credit`
--

CREATE TABLE `credit` (
  `idCredit` int(11) NOT NULL,
  `idCompte` int(11) NOT NULL,
  `typeCredit` varchar(50) NOT NULL,
  `montantDemande` double NOT NULL,
  `montantAccord` double DEFAULT NULL,
  `duree` int(11) NOT NULL,
  `tauxInteret` double NOT NULL,
  `mensualite` double NOT NULL,
  `montantRestant` double NOT NULL,
  `dateDemande` varchar(20) NOT NULL,
  `statut` varchar(30) NOT NULL,
  `idUser` int(11) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `credit`
--

INSERT INTO `credit` (`idCredit`, `idCompte`, `typeCredit`, `montantDemande`, `montantAccord`, `duree`, `tauxInteret`, `mensualite`, `montantRestant`, `dateDemande`, `statut`, `idUser`) VALUES
(1, 45, 'Professionnel', 1222, 1, 36, 3.5, 0.03, 1, '2026-02-19', 'Refuse', 2);

-- --------------------------------------------------------

--
-- Structure de la table `garantiecredit`
--

CREATE TABLE `garantiecredit` (
  `idGarantie` int(11) NOT NULL,
  `idCredit` int(11) NOT NULL,
  `typeGarantie` varchar(50) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `adresseBien` varchar(255) DEFAULT NULL,
  `valeurEstimee` double NOT NULL,
  `valeurRetenue` double NOT NULL,
  `documentJustificatif` varchar(255) DEFAULT NULL,
  `dateEvaluation` varchar(20) NOT NULL,
  `nomGarant` varchar(100) DEFAULT NULL,
  `statut` varchar(30) NOT NULL,
  `idUser` int(11) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `garantiecredit`
--

INSERT INTO `garantiecredit` (`idGarantie`, `idCredit`, `typeGarantie`, `description`, `adresseBien`, `valeurEstimee`, `valeurRetenue`, `documentJustificatif`, `dateEvaluation`, `nomGarant`, `statut`, `idUser`) VALUES
(1, 1, 'Vehicle Title', 'fzevqgrebre', 'nzekjvfjkzr', 100000, 10, 'dzenfire', '2026-02-19', 'fbzuifvb', 'En attente', 2);

-- --------------------------------------------------------

--
-- Structure de la table `notifications`
--

CREATE TABLE `notifications` (
  `idNotification` int(11) NOT NULL,
  `recipient_user_id` int(11) DEFAULT NULL,
  `recipient_role` varchar(20) DEFAULT NULL,
  `related_user_id` int(11) DEFAULT NULL,
  `type` varchar(40) NOT NULL DEFAULT 'INFO',
  `title` varchar(160) NOT NULL,
  `message` text NOT NULL,
  `is_read` tinyint(1) NOT NULL DEFAULT 0,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `notifications`
--

INSERT INTO `notifications` (`idNotification`, `recipient_user_id`, `recipient_role`, `related_user_id`, `type`, `title`, `message`, `is_read`, `created_at`) VALUES
(1, NULL, 'ROLE_ADMIN', 6, 'USER_SIGNUP_PENDING', 'New user pending approval', 'User - mokhtar (dohinom812@kaoing.com) created an account and is waiting for admin review.', 1, '2026-03-01 19:42:01'),
(2, NULL, 'ROLE_ADMIN', 7, 'USER_SIGNUP_PENDING', 'New user pending approval', 'User - moncef (maciwox300@kaoing.com) created an account and is waiting for admin review.', 1, '2026-03-01 19:56:59'),
(3, 7, NULL, 7, 'ACCOUNT_STATUS', 'Account approved', 'Your account is now ACTIVE and ready to use.', 1, '2026-03-01 19:57:39'),
(4, NULL, 'ROLE_ADMIN', 8, 'USER_SIGNUP_PENDING', 'New user pending approval', 'User - azzedine (hodor18057@netoiu.com) created an account and is waiting for admin review.', 1, '2026-03-01 20:12:43'),
(5, 8, NULL, 8, 'ACCOUNT_STATUS', 'Account approved', 'Your account is now ACTIVE and ready to use.', 1, '2026-03-01 20:13:06'),
(6, 3, NULL, 3, 'ACCOUNT_STATUS', 'Account approved', 'Your account is now ACTIVE and ready to use.', 1, '2026-03-01 20:25:08'),
(7, NULL, 'ROLE_ADMIN', 3, 'CASHBACK_SUBMITTED', 'New cashback submitted', 'User #3 submitted a cashback request at partner \"Geant\" for purchase amount 9.00 DT.', 1, '2026-03-04 19:28:59'),
(8, NULL, 'ROLE_ADMIN', 3, 'CASHBACK_RATING', 'New cashback rating submitted', 'User #3 rated partner \"Geant\" with 5.0/5. Comment: d', 1, '2026-03-04 19:31:21'),
(9, 3, NULL, 3, 'CASHBACK_RATING', 'Rating sent', 'Your rating 5.0/5 for partner \"Geant\" was sent to admin. Your comment: d', 1, '2026-03-04 19:31:21'),
(10, 3, NULL, 3, 'CASHBACK_REWARD', 'Reward received', 'Admin granted you +5.00 DT for partner \"Geant\". Note: Bonus fidelite', 1, '2026-03-04 19:33:26'),
(11, NULL, 'ROLE_ADMIN', 3, 'CASHBACK_REWARD', 'Cashback reward granted', 'Admin granted +5.00 DT to user #3 for partner \"Geant\". Note: Bonus fidelite', 1, '2026-03-04 19:33:26'),
(12, NULL, 'ROLE_ADMIN', 3, 'CASHBACK_RATING', 'New cashback rating submitted', 'User #3 rated partner \"Geant\" with 5.0/5.', 1, '2026-03-04 20:04:55'),
(13, 3, NULL, 3, 'CASHBACK_RATING', 'Rating sent', 'Your rating 5.0/5 for partner \"Geant\" was sent to admin.', 1, '2026-03-04 20:04:55');

-- --------------------------------------------------------

--
-- Structure de la table `partenaire`
--

CREATE TABLE `partenaire` (
  `idPartenaire` int(11) NOT NULL,
  `nom` varchar(100) NOT NULL,
  `categorie` varchar(50) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `tauxCashback` double NOT NULL,
  `tauxCashbackMax` double NOT NULL,
  `plafondMensuel` double NOT NULL,
  `conditions` varchar(255) DEFAULT NULL,
  `status` varchar(30) NOT NULL DEFAULT 'Actif',
  `rating` double NOT NULL DEFAULT 4
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `partenaire`
--

INSERT INTO `partenaire` (`idPartenaire`, `nom`, `categorie`, `description`, `tauxCashback`, `tauxCashbackMax`, `plafondMensuel`, `conditions`, `status`, `rating`) VALUES
(1, 'Geant', 'Mode et Vetements', 'dezadzadza', 3, 12, 200, 'FZEFEZGQREQGVRE', 'Premium', 4),
(2, 'carrefour', 'Grande Distribution', 'sdfghjk', 5, 10, 500, 'sdfghj', 'Actif', 4);

-- --------------------------------------------------------

--
-- Structure de la table `reclamation`
--

CREATE TABLE `reclamation` (
  `idReclamation` int(11) NOT NULL,
  `dateReclamation` date NOT NULL,
  `typeReclamation` varchar(50) NOT NULL,
  `description` varchar(150) NOT NULL,
  `status` varchar(30) NOT NULL,
  `idUser` int(11) DEFAULT NULL,
  `idTransaction` int(11) DEFAULT NULL,
  `is_inappropriate` tinyint(1) NOT NULL DEFAULT 0,
  `is_blurred` tinyint(1) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `roue_fortune_points`
--

CREATE TABLE `roue_fortune_points` (
  `idUser` int(11) NOT NULL,
  `totalPoints` int(11) NOT NULL DEFAULT 0,
  `dernierTour` varchar(10) DEFAULT NULL,
  `dernierMois` varchar(7) DEFAULT NULL,
  `pointsGagnes` int(11) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `surplus_notifications`
--

CREATE TABLE `surplus_notifications` (
  `idUser` int(11) NOT NULL,
  `moisAffiche` varchar(7) NOT NULL,
  `dateCreation` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `transactions`
--

CREATE TABLE `transactions` (
  `idTransaction` int(11) NOT NULL,
  `idCompte` int(11) DEFAULT NULL,
  `idUser` int(11) DEFAULT NULL,
  `categorie` varchar(50) NOT NULL,
  `dateTransaction` varchar(20) NOT NULL,
  `montant` varchar(255) DEFAULT NULL,
  `typeTransaction` varchar(30) NOT NULL,
  `statutTransaction` varchar(30) NOT NULL,
  `soldeApres` decimal(12,2) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `montantPaye` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `transactions`
--

INSERT INTO `transactions` (`idTransaction`, `idCompte`, `idUser`, `categorie`, `dateTransaction`, `montant`, `typeTransaction`, `statutTransaction`, `soldeApres`, `description`, `montantPaye`) VALUES
(1, NULL, 3, 'Transport', '2026-03-05', 'eGkLFe8uJHI+X4cZ1d3/1Q==:3IVEDzLJBRGNVkD4fK3ePA==', 'Credit', 'Terminee', NULL, 'BUJB', 'm8mbFC6XNsZoO+NnFkeB5w==:8us0xC5E2CyEGKpKFoUQ6A==');

-- --------------------------------------------------------

--
-- Structure de la table `users`
--

CREATE TABLE `users` (
  `idUser` int(11) NOT NULL,
  `nom` varchar(80) NOT NULL,
  `prenom` varchar(80) NOT NULL,
  `email` varchar(190) NOT NULL,
  `telephone` varchar(30) NOT NULL,
  `role` varchar(20) NOT NULL DEFAULT 'ROLE_USER',
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE',
  `password` varchar(255) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `updated_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `account_opened_from` varchar(180) NOT NULL DEFAULT 'Unknown device',
  `last_online_at` timestamp NULL DEFAULT NULL,
  `last_online_from` varchar(180) DEFAULT NULL,
  `biometric_enabled` tinyint(1) NOT NULL DEFAULT 0,
  `profile_image_path` varchar(600) DEFAULT NULL,
  `account_opened_location` varchar(200) NOT NULL DEFAULT 'Unknown location',
  `account_opened_lat` decimal(10,7) DEFAULT NULL,
  `account_opened_lng` decimal(10,7) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `users`
--

INSERT INTO `users` (`idUser`, `nom`, `prenom`, `email`, `telephone`, `role`, `status`, `password`, `created_at`, `updated_at`, `account_opened_from`, `last_online_at`, `last_online_from`, `biometric_enabled`, `profile_image_path`, `account_opened_location`, `account_opened_lat`, `account_opened_lng`) VALUES
(1, 'System', 'Admin', 'admin@nexora.com', '+21600000000', 'ROLE_ADMIN', 'ACTIVE', 'PBKDF2$210000$vilkFWvVrCUKaSHVKswdnA==$D6x8vFgqQ70O7fi+oBIRwcOKNu6nsnQUIrxElRpLkuc=', '2026-02-17 00:39:41', '2026-03-05 03:10:18', 'Unknown device', '2026-03-05 03:10:18', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 0, NULL, 'Tunis, Tunis, Tunisia', 36.8064948, 10.1815316),
(2, 'karimmmmmm', 'naddari', 'nadderikarim@gmail.com', '92720527', 'ROLE_USER', 'ACTIVE', 'PBKDF2$210000$X1tm/Lm5CBCtk1cicfSSVQ==$R+KTHlgoAeZrPrEUSMBcX80G1z57rJxWJPH7ssfg9dE=', '2026-02-17 00:40:45', '2026-03-01 19:25:12', 'Unknown device', '2026-03-01 19:25:12', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 1, NULL, 'Unknown location', NULL, NULL),
(3, 'Karim', 'Naddari 2', 'nooobnaab@gmail.com', '92 720 527', 'ROLE_USER', 'ACTIVE', 'PBKDF2$210000$c9fv0oTzGgM7arOG00HJUQ==$bGzwOh1O3bez1Ck1WfOkEoDwmKVXj+aapnM3q9avgYA=', '2026-02-17 17:08:34', '2026-03-05 03:08:17', 'Unknown device', '2026-03-05 03:08:17', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 0, 'C:\\Users\\XPS\\.nexora-bank\\profile-images\\user_3_1772409358609.jpg', 'Tunis, Tunis, Tunisia', 36.8064948, 10.1815316),
(4, 'Saad', '-', 'hderirrkejr@gmail.com', '98765789', 'ROLE_USER', 'PENDING', 'PBKDF2$210000$mntxA4LH0aRzOp8a73yOGA==$q5O+SqFjbPgsQowrnzVkR7yz9oiQXCCcViqSXlMPcFM=', '2026-02-20 08:52:34', '2026-02-20 08:52:34', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', NULL, NULL, 0, NULL, 'Unknown location', NULL, NULL),
(5, 'Vlidation', '-', 'jxjeidkkfkrkf0@gmail.com', '876543456789', 'ROLE_USER', 'ACTIVE', 'PBKDF2$210000$7srxDJ6UnEot92OrBlOriQ==$YcoEl0GwKqD3Ca/zzjHUp1EMrcwbbo8i5LEmuW7UfA8=', '2026-02-20 08:55:55', '2026-02-20 09:02:48', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', '2026-02-20 08:59:50', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 1, NULL, 'Unknown location', NULL, NULL),
(6, 'mokhtar', '-', 'dohinom812@kaoing.com', '89485832', 'ROLE_USER', 'PENDING', 'PBKDF2$210000$Z32J6dlKUEXPFSz8rWjU+Q==$6f78meyaynINjvFzcjhZNHyTTd2lXglDAanzsaVQEz4=', '2026-03-01 19:42:01', '2026-03-01 19:42:01', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', NULL, NULL, 0, NULL, 'Unknown location', NULL, NULL),
(7, 'moncef', '-', 'maciwox300@kaoing.com', '87654567890', 'ROLE_USER', 'ACTIVE', 'PBKDF2$210000$tNox9ZMQ89HzK8ypklF1eQ==$TNxHX0WnyzAEVn39TO86D6Pjo0abMTzZ+BffaH+y5Vs=', '2026-03-01 19:56:59', '2026-03-01 19:58:23', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', '2026-03-01 19:58:23', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 0, NULL, 'Unknown location', NULL, NULL),
(8, 'azzedine', '-', 'hodor18057@netoiu.com', '98765456789', 'ROLE_USER', 'ACTIVE', 'PBKDF2$210000$0g5AYNXGTSa0mdkoJ32wug==$io50zJJLtusHQb1pZONPTrupCJYzV5ApfVtcPy1z5u4=', '2026-03-01 20:12:43', '2026-03-01 20:16:16', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', '2026-03-01 20:16:16', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 0, NULL, 'Unknown location', NULL, NULL);

-- --------------------------------------------------------

--
-- Structure de la table `user_activity_log`
--

CREATE TABLE `user_activity_log` (
  `idAction` int(11) NOT NULL,
  `idUser` int(11) NOT NULL,
  `action_type` varchar(50) NOT NULL,
  `action_source` varchar(180) DEFAULT NULL,
  `details` varchar(255) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `user_activity_log`
--

INSERT INTO `user_activity_log` (`idAction`, `idUser`, `action_type`, `action_source`, `details`, `created_at`) VALUES
(1, 3, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-01 20:46:55'),
(2, 3, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-01 20:54:34'),
(3, 3, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-01 21:45:13'),
(4, 3, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-01 21:53:09'),
(5, 1, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-01 21:56:13'),
(6, 1, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-01 21:57:27'),
(7, 3, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-01 22:55:19'),
(8, 3, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-01 23:04:13'),
(9, 3, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-01 23:08:55'),
(10, 3, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-01 23:14:02'),
(11, 3, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-01 23:33:31'),
(12, 3, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-01 23:36:19'),
(13, 3, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-01 23:38:48'),
(14, 3, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-01 23:41:15'),
(15, 3, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-01 23:54:32'),
(16, 3, 'PROFILE_IMAGE_UPDATE', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'Profile image updated.', '2026-03-01 23:55:58'),
(17, 3, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-02 00:06:52'),
(18, 3, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-02 00:10:36'),
(19, 3, 'BIOMETRIC_PREF', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'Biometric login enabled.', '2026-03-02 00:13:40'),
(20, 3, 'BIOMETRIC_PREF', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'Biometric login disabled.', '2026-03-02 00:13:44'),
(21, 3, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-02 00:15:26'),
(22, 3, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-02 00:18:30'),
(23, 3, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-02 00:23:59'),
(24, 3, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-02 00:33:36'),
(25, 3, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-02 00:52:23'),
(26, 3, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-02 00:57:35'),
(27, 3, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-02 01:00:28'),
(28, 3, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-02 01:02:58'),
(29, 3, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-02 01:08:43'),
(30, 1, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-02 01:09:45'),
(31, 3, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-02 01:10:51'),
(32, 3, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-02 01:17:22'),
(33, 1, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-02 01:18:03'),
(34, 1, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-02 01:21:39'),
(35, 3, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-02 01:22:29'),
(36, 3, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-02 01:33:03'),
(37, 3, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-03 10:08:38'),
(38, 3, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-03 10:19:40'),
(39, 1, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-03 19:09:07'),
(40, 1, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-03 19:18:42'),
(41, 1, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-03 20:40:23'),
(42, 1, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-03 21:08:49'),
(43, 1, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-03 21:24:06'),
(44, 1, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-03 21:29:43'),
(45, 1, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-03 21:35:58'),
(46, 1, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-03 21:45:14'),
(47, 1, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-03 21:51:04'),
(48, 1, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-03 21:53:15'),
(49, 1, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-03 21:55:39'),
(50, 1, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-03 21:57:04'),
(51, 1, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-03 21:59:41'),
(52, 1, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-03 22:00:39'),
(53, 1, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-03 22:01:18'),
(54, 3, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-04 01:43:02'),
(55, 3, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-04 02:05:29'),
(56, 3, 'AI_ACCOUNT_SECURED', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'AI risk level: HIGH; Password strengthened', '2026-03-04 02:06:03'),
(57, 3, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-04 02:07:09'),
(58, 3, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-04 02:16:03'),
(59, 3, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-04 19:12:53'),
(60, 1, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-04 19:14:08'),
(61, 3, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-04 19:21:13'),
(62, 3, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-04 19:25:34'),
(63, 1, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-04 19:26:25'),
(64, 3, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-04 19:28:40'),
(65, 1, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-04 19:29:38'),
(66, 3, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-04 19:31:01'),
(67, 1, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-04 19:31:50'),
(68, 1, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-04 19:32:52'),
(69, 3, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-04 19:33:45'),
(70, 3, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-04 19:42:52'),
(71, 3, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-04 19:59:44'),
(72, 3, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-04 20:01:55'),
(73, 3, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-04 20:04:06'),
(74, 3, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-04 20:06:34'),
(75, 3, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-04 20:13:07'),
(76, 3, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-04 20:20:50'),
(77, 1, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-04 20:23:23'),
(78, 3, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-04 20:27:19'),
(79, 3, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-04 20:34:43'),
(80, 3, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-04 20:38:24'),
(81, 3, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-04 20:44:52'),
(82, 3, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-05 02:01:48'),
(83, 3, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-05 02:07:52'),
(84, 3, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-05 02:19:00'),
(85, 1, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-05 02:21:28'),
(86, 1, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-05 02:30:01'),
(87, 3, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-05 02:54:38'),
(88, 3, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-05 02:58:54'),
(89, 1, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-05 03:00:10'),
(90, 1, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-05 03:07:46'),
(91, 3, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-05 03:08:17'),
(92, 1, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-05 03:09:16'),
(93, 1, 'LOGIN', 'XPS@DESKTOP-3JU5SI6 (Windows 11)', 'User login recorded.', '2026-03-05 03:10:18');

--
-- Index pour les tables déchargées
--

--
-- Index pour la table `cashback`
--
ALTER TABLE `cashback`
  ADD PRIMARY KEY (`idCashback`),
  ADD KEY `fk_cashback_partenaire` (`idPartenaire`),
  ADD KEY `fk_cashback_transaction` (`idTransaction`);

--
-- Index pour la table `cashback_entries`
--
ALTER TABLE `cashback_entries`
  ADD PRIMARY KEY (`id_cashback`),
  ADD KEY `fk_cashback_entries_user` (`id_user`),
  ADD KEY `fk_cashback_entries_partenaire` (`id_partenaire`);

--
-- Index pour la table `coffrevirtuel`
--
ALTER TABLE `coffrevirtuel`
  ADD PRIMARY KEY (`idCoffre`),
  ADD KEY `fk_coffre_compte` (`idCompte`),
  ADD KEY `fk_coffre_user` (`idUser`);

--
-- Index pour la table `compte`
--
ALTER TABLE `compte`
  ADD PRIMARY KEY (`idCompte`),
  ADD UNIQUE KEY `numeroCompte` (`numeroCompte`),
  ADD KEY `fk_compte_user` (`idUser`);

--
-- Index pour la table `credit`
--
ALTER TABLE `credit`
  ADD PRIMARY KEY (`idCredit`),
  ADD KEY `fk_credit_compte` (`idCompte`);

--
-- Index pour la table `garantiecredit`
--
ALTER TABLE `garantiecredit`
  ADD PRIMARY KEY (`idGarantie`),
  ADD KEY `fk_garantie_credit` (`idCredit`);

--
-- Index pour la table `notifications`
--
ALTER TABLE `notifications`
  ADD PRIMARY KEY (`idNotification`),
  ADD KEY `idx_notifications_recipient_user` (`recipient_user_id`),
  ADD KEY `idx_notifications_recipient_role` (`recipient_role`),
  ADD KEY `idx_notifications_created` (`created_at`),
  ADD KEY `idx_notifications_read` (`is_read`);

--
-- Index pour la table `partenaire`
--
ALTER TABLE `partenaire`
  ADD PRIMARY KEY (`idPartenaire`);

--
-- Index pour la table `reclamation`
--
ALTER TABLE `reclamation`
  ADD PRIMARY KEY (`idReclamation`),
  ADD KEY `fk_reclamation_user` (`idUser`),
  ADD KEY `fk_reclamation_transaction` (`idTransaction`);

--
-- Index pour la table `roue_fortune_points`
--
ALTER TABLE `roue_fortune_points`
  ADD PRIMARY KEY (`idUser`);

--
-- Index pour la table `surplus_notifications`
--
ALTER TABLE `surplus_notifications`
  ADD PRIMARY KEY (`idUser`,`moisAffiche`);

--
-- Index pour la table `transactions`
--
ALTER TABLE `transactions`
  ADD PRIMARY KEY (`idTransaction`),
  ADD KEY `fk_transaction_compte` (`idCompte`),
  ADD KEY `fk_transaction_user` (`idUser`);

--
-- Index pour la table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`idUser`),
  ADD UNIQUE KEY `uq_users_email` (`email`),
  ADD KEY `idx_users_role` (`role`);

--
-- Index pour la table `user_activity_log`
--
ALTER TABLE `user_activity_log`
  ADD PRIMARY KEY (`idAction`),
  ADD KEY `idx_user_activity_user` (`idUser`),
  ADD KEY `idx_user_activity_created` (`created_at`);

--
-- AUTO_INCREMENT pour les tables déchargées
--

--
-- AUTO_INCREMENT pour la table `cashback`
--
ALTER TABLE `cashback`
  MODIFY `idCashback` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `cashback_entries`
--
ALTER TABLE `cashback_entries`
  MODIFY `id_cashback` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- AUTO_INCREMENT pour la table `coffrevirtuel`
--
ALTER TABLE `coffrevirtuel`
  MODIFY `idCoffre` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=7;

--
-- AUTO_INCREMENT pour la table `compte`
--
ALTER TABLE `compte`
  MODIFY `idCompte` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=51;

--
-- AUTO_INCREMENT pour la table `credit`
--
ALTER TABLE `credit`
  MODIFY `idCredit` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT pour la table `garantiecredit`
--
ALTER TABLE `garantiecredit`
  MODIFY `idGarantie` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT pour la table `notifications`
--
ALTER TABLE `notifications`
  MODIFY `idNotification` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=14;

--
-- AUTO_INCREMENT pour la table `partenaire`
--
ALTER TABLE `partenaire`
  MODIFY `idPartenaire` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT pour la table `reclamation`
--
ALTER TABLE `reclamation`
  MODIFY `idReclamation` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `transactions`
--
ALTER TABLE `transactions`
  MODIFY `idTransaction` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT pour la table `users`
--
ALTER TABLE `users`
  MODIFY `idUser` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `user_activity_log`
--
ALTER TABLE `user_activity_log`
  MODIFY `idAction` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=94;

--
-- Contraintes pour les tables déchargées
--

--
-- Contraintes pour la table `cashback`
--
ALTER TABLE `cashback`
  ADD CONSTRAINT `fk_cashback_partenaire` FOREIGN KEY (`idPartenaire`) REFERENCES `partenaire` (`idPartenaire`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_cashback_transaction` FOREIGN KEY (`idTransaction`) REFERENCES `transactions` (`idTransaction`) ON DELETE CASCADE;

--
-- Contraintes pour la table `cashback_entries`
--
ALTER TABLE `cashback_entries`
  ADD CONSTRAINT `fk_cashback_entries_partenaire` FOREIGN KEY (`id_partenaire`) REFERENCES `partenaire` (`idPartenaire`) ON DELETE SET NULL,
  ADD CONSTRAINT `fk_cashback_entries_user` FOREIGN KEY (`id_user`) REFERENCES `users` (`idUser`) ON DELETE CASCADE;

--
-- Contraintes pour la table `coffrevirtuel`
--
ALTER TABLE `coffrevirtuel`
  ADD CONSTRAINT `fk_coffre_compte` FOREIGN KEY (`idCompte`) REFERENCES `compte` (`idCompte`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_coffre_user` FOREIGN KEY (`idUser`) REFERENCES `users` (`idUser`) ON DELETE SET NULL;

--
-- Contraintes pour la table `compte`
--
ALTER TABLE `compte`
  ADD CONSTRAINT `fk_compte_user` FOREIGN KEY (`idUser`) REFERENCES `users` (`idUser`) ON DELETE SET NULL ON UPDATE CASCADE;

--
-- Contraintes pour la table `credit`
--
ALTER TABLE `credit`
  ADD CONSTRAINT `fk_credit_compte` FOREIGN KEY (`idCompte`) REFERENCES `compte` (`idCompte`) ON DELETE CASCADE;

--
-- Contraintes pour la table `garantiecredit`
--
ALTER TABLE `garantiecredit`
  ADD CONSTRAINT `fk_garantie_credit` FOREIGN KEY (`idCredit`) REFERENCES `credit` (`idCredit`) ON DELETE CASCADE;

--
-- Contraintes pour la table `reclamation`
--
ALTER TABLE `reclamation`
  ADD CONSTRAINT `fk_reclamation_transaction` FOREIGN KEY (`idTransaction`) REFERENCES `transactions` (`idTransaction`) ON DELETE SET NULL ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_reclamation_user` FOREIGN KEY (`idUser`) REFERENCES `users` (`idUser`) ON DELETE SET NULL ON UPDATE CASCADE;

--
-- Contraintes pour la table `roue_fortune_points`
--
ALTER TABLE `roue_fortune_points`
  ADD CONSTRAINT `fk_roue_user` FOREIGN KEY (`idUser`) REFERENCES `users` (`idUser`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Contraintes pour la table `surplus_notifications`
--
ALTER TABLE `surplus_notifications`
  ADD CONSTRAINT `fk_surplus_user` FOREIGN KEY (`idUser`) REFERENCES `users` (`idUser`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Contraintes pour la table `transactions`
--
ALTER TABLE `transactions`
  ADD CONSTRAINT `fk_transaction_compte` FOREIGN KEY (`idCompte`) REFERENCES `compte` (`idCompte`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_transaction_user` FOREIGN KEY (`idUser`) REFERENCES `users` (`idUser`) ON DELETE SET NULL;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
