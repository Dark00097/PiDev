-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Hôte : 127.0.0.1
-- Généré le : dim. 15 fév. 2026 à 10:52
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
  `idUser` int(11) DEFAULT NULL,
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
  `estVerrouille` tinyint(1) NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

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
  CONSTRAINT `chk_users_role` CHECK (`role` in ('ROLE_ADMIN','ROLE_USER')),
  CONSTRAINT `chk_users_status` CHECK (`status` in ('PENDING','ACTIVE','DECLINED','INACTIVE','BANNED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Donnees initiales pour la table `users`
--

INSERT INTO `users` (`idUser`, `nom`, `prenom`, `email`, `telephone`, `role`, `status`, `password`) VALUES
(1, 'System', 'Admin', 'admin@nexora.com', '+21600000000', 'ROLE_ADMIN', 'ACTIVE', 'PBKDF2$210000$vilkFWvVrCUKaSHVKswdnA==$D6x8vFgqQ70O7fi+oBIRwcOKNu6nsnQUIrxElRpLkuc='),
(2, 'Ben Ali', 'Mouhamed', 'mouhanedsamali@gmail.com', '+21612345678', 'ROLE_USER', 'ACTIVE', 'PBKDF2$210000$vilkFWvVrCUKaSHVKswdnA==$D6x8vFgqQ70O7fi+oBIRwcOKNu6nsnQUIrxElRpLkuc='),
(3, 'Mahdi', 'Sassi', 'sassi.mahdi@email.com', '+21623456789', 'ROLE_USER', 'ACTIVE', 'PBKDF2$210000$vilkFWvVrCUKaSHVKswdnA==$D6x8vFgqQ70O7fi+oBIRwcOKNu6nsnQUIrxElRpLkuc='),
(4, 'Khalfaoui', 'Youssef', 'youssef.khalfaoui@email.com', '+21634567890', 'ROLE_USER', 'ACTIVE', 'PBKDF2$210000$vilkFWvVrCUKaSHVKswdnA==$D6x8vFgqQ70O7fi+oBIRwcOKNu6nsnQUIrxElRpLkuc='),
(5, ' Trabelsi', 'Safa', 'safa.trabelsi@email.com', '+21645678901', 'ROLE_USER', 'PENDING', 'PBKDF2$210000$vilkFWvVrCUKaSHVKswdnA==$D6x8vFgqQ70O7fi+oBIRwcOKNu6nsnQUIrxElRpLkuc=');

--
-- Donnees pour la table `partenaire`
--

INSERT INTO `partenaire` (`idPartenaire`, `nom`, `categorie`, `description`, `tauxCashback`, `tauxCashbackMax`, `plafondMensuel`, `conditions`) VALUES
(1, 'Carrefour', 'Shopping', 'Grande surface alimentaire et produits divers', 2.0, 5.0, 500.0, 'Achats supermarket'),
(2, 'Monoprix', 'Shopping', 'Chaîne de Supermarchés', 1.5, 4.0, 300.0, 'Achats alimentaires'),
(3, 'Zara', 'Shopping', 'Vêtements et accessoires', 3.0, 8.0, 400.0, 'Achats vestimentaires'),
(4, 'Virgin Megastore', 'Shopping', 'Musique, livres et divertissements', 2.5, 6.0, 200.0, 'Achats culturels'),
(5, 'Tunisair', 'Voyage', 'Compagnie aérienne nationale', 2.0, 5.0, 1000.0, 'Billets avion'),
(6, 'Seven Stars', 'Voyage', 'Agence de voyage', 3.0, 7.0, 1500.0, 'Voyages organisés'),
(7, 'Hotel Africa', 'Voyage', 'Hôtel de luxe', 2.5, 6.0, 800.0, 'Réservations hotel'),
(8, 'Orange', 'Telecom', 'Opérateur téléphonique', 1.0, 3.0, 200.0, 'Recharges et forfaits'),
(9, 'Ooredoo', 'Telecom', 'Opérateur téléphonique', 1.0, 3.0, 200.0, 'Recharges et forfaits'),
(10, 'TTnet', 'Telecom', 'Fournisseur internet', 1.5, 4.0, 250.0, 'Abonnements internet');

--
-- Donnees pour la table `compte`
--

INSERT INTO `compte` (`idCompte`, `numeroCompte`, `solde`, `dateOuverture`, `statutCompte`, `plafondRetrait`, `plafondVirement`, `typeCompte`) VALUES
(1, 'CB-2024-001', 15000.00, '2024-01-15', 'Actif', 1000.00, 5000.00, 'Courant'),
(2, 'EP-2024-001', 8500.00, '2024-02-20', 'Actif', 500.00, 2000.00, 'Epargne'),
(3, 'PRO-2024-001', 25000.00, '2024-03-10', 'Actif', 2000.00, 10000.00, 'Professionnel'),
(4, 'CB-2024-002', 3200.50, '2024-04-05', 'Actif', 800.00, 3000.00, 'Courant'),
(5, 'EP-2024-002', 12000.00, '2024-05-18', 'Actif', 600.00, 2500.00, 'Epargne'),
(44, 'NT0512', 1400.00, '2026-02-11', 'Bloque', 12.00, 17.00, 'Professionnel'),
(45, 'CB-04', 7820.00, '2026-02-24', 'Bloque', 500.00, 100.00, 'Epargne'),
(46, 'CB-01', 1453.00, '2026-02-25', 'Bloque', 5.00, 10.00, 'Courant'),
(47, 'CB-02', 0.00, '2026-02-25', 'Actif', 12.00, 10.00, 'Professionnel'),
(48, 'CB-18', 1245.24, '2026-02-26', 'Bloque', 5.00, 15.00, 'Epargne'),
(49, 'bouth0', 1450.00, '2026-02-03', 'Bloque', 120.00, 120.00, 'Professionnel'),
(50, 'CB-06', 1450.00, '2026-02-12', 'Bloque', 15.00, 14.00, 'Courant');

--
-- Donnees pour la table `transactions`
--

INSERT INTO `transactions` (`idTransaction`, `idCompte`, `categorie`, `dateTransaction`, `montant`, `typeTransaction`, `statutTransaction`, `soldeApres`, `description`) VALUES
(1, 1, 'Achat', '2024-06-01', 150.00, 'Debit', 'Termine', 14850.00, 'Achat Carrefour'),
(2, 1, 'Achat', '2024-06-02', 80.50, 'Debit', 'Termine', 14769.50, 'Achat Monoprix'),
(3, 1, 'Virement', '2024-06-03', 1000.00, 'Credit', 'Termine', 15769.50, 'Virement recu'),
(4, 1, 'Achat', '2024-06-04', 250.00, 'Debit', 'Termine', 15519.50, 'Achat Zara'),
(5, 1, 'Retrait', '2024-06-05', 200.00, 'Debit', 'Termine', 15319.50, 'Retrait GAB'),
(6, 2, 'Depot', '2024-06-01', 500.00, 'Credit', 'Termine', 9000.00, 'Depot salaire'),
(7, 2, 'Achat', '2024-06-10', 300.00, 'Debit', 'Termine', 8700.00, 'Achat en ligne'),
(8, 3, 'Virement', '2024-06-15', 5000.00, 'Credit', 'Termine', 30000.00, 'Virement professionnel'),
(9, 3, 'Achat', '2024-06-20', 1200.00, 'Debit', 'Termine', 28800.00, 'Equipement bureau'),
(10, 4, 'Achat', '2024-07-01', 45.00, 'Debit', 'Termine', 3155.50, 'Achat essence'),
(11, 2, 'Achat', '2024-08-01', 180.00, 'Debit', 'Termine', 8520.00, 'Achat Zara');

--
-- Donnees pour la table `cashback`
--

INSERT INTO `cashback` (`idCashback`, `idPartenaire`, `idTransaction`, `idUser`, `montantAchat`, `tauxApplique`, `montantCashback`, `dateAchat`, `dateCredit`, `dateExpiration`, `statut`) VALUES
(1, 1, 1, 2, 150.00, 2.0, 3.00, '2024-06-01', '2024-06-03', '2025-06-03', 'Credite'),
(2, 2, 2, 2, 80.50, 1.5, 1.21, '2024-06-02', '2024-06-04', '2025-06-04', 'Credite'),
(3, 3, 4, 2, 250.00, 3.0, 7.50, '2024-06-04', '2024-06-06', '2025-06-06', 'Credite'),
(4, 5, 5, 3, 200.00, 2.0, 4.00, '2024-06-05', NULL, NULL, 'EnAttente'),
(5, 1, 6, 3, 300.00, 2.0, 6.00, '2024-07-01', '2024-07-03', '2025-07-03', 'Credite'),
(6, 8, 7, 4, 50.00, 1.0, 0.50, '2024-07-10', '2024-07-12', '2025-07-12', 'Credite'),
(7, 3, 8, 4, 450.00, 3.0, 13.50, '2024-07-15', '2024-07-17', '2025-07-17', 'Credite'),
(8, 6, 9, 2, 1200.00, 3.0, 36.00, '2024-07-20', NULL, NULL, 'EnAttente'),
(9, 2, 10, 2, 45.00, 1.5, 0.68, '2024-08-01', '2024-08-03', '2025-08-03', 'Credite'),
(10, 4, 11, 3, 180.00, 2.5, 4.50, '2024-08-05', '2024-08-07', '2025-08-07', 'Credite');

--
-- Donnees pour la table `credit`
--

INSERT INTO `credit` (`idCredit`, `idCompte`, `typeCredit`, `montantDemande`, `montantAccord`, `duree`, `tauxInteret`, `mensualite`, `montantRestant`, `dateDemande`, `statut`) VALUES
(1, 1, 'Credit Immobilier', 100000.00, 100000.00, 240, 8.5, 867.00, 95000.00, '2024-03-15', 'Actif'),
(2, 3, 'Credit Professionnel', 50000.00, 45000.00, 60, 10.0, 955.00, 38000.00, '2024-04-20', 'Actif'),
(3, 4, 'Credit Consommation', 10000.00, 8000.00, 36, 12.0, 266.00, 6500.00, '2024-05-10', 'Actif'),
(4, 2, 'Credit Etude', 15000.00, 15000.00, 48, 6.0, 352.00, 14000.00, '2024-06-01', 'Actif'),
(5, 5, 'Credit Voiture', 30000.00, NULL, 72, 9.0, 0.00, 30000.00, '2024-07-15', 'EnAttente');

--
-- Donnees pour la table `garantiecredit`
--

INSERT INTO `garantiecredit` (`idGarantie`, `idCredit`, `typeGarantie`, `description`, `adresseBien`, `valeurEstimee`, `valeurRetenue`, `documentJustificatif`, `dateEvaluation`, `nomGarant`, `statut`) VALUES
(1, 1, 'Hypothèque', 'Appartement85m2', 'Rue de la Palestine, Tunis', 150000.00, 120000.00, 'acte_propriete.pdf', '2024-03-10', 'Ben Ali Mohamed', 'Valide'),
(2, 2, 'Nantissement', 'Materiel bureautique', '1 Avenue Habib Bourguiba, Sfax', 25000.00, 20000.00, 'facture_equipement.pdf', '2024-04-15', 'Sassi Mahdi', 'Valide'),
(3, 3, 'Salaire', 'Attestation de salaire', 'N/A', 5000.00, 4000.00, 'attestation_salaire.pdf', '2024-05-05', 'Khalfaoui Youssef', 'Valide'),
(4, 4, 'Garantie Parentale', 'Engagement parental', 'N/A', 20000.00, 15000.00, 'engagement_parent.pdf', '2024-05-28', 'Trabellsia', 'EnAttente');

--
-- Donnees pour la table `reclamation`
--

INSERT INTO `reclamation` (`idReclamation`, `dateReclamation`, `typeReclamation`, `description`, `status`) VALUES
(1, '2024-06-10', 'Transaction', 'Transaction non refletee sur le compte', 'EnCours'),
(2, '2024-06-15', 'Service Client', 'Demande information credit', 'Traitee'),
(3, '2024-07-01', 'Compte', 'Probleme de blocage compte', 'EnCours'),
(4, '2024-07-10', 'Virement', 'Virement non recu', 'Traitee'),
(5, '2024-07-20', 'Carte', 'Demande nouvelle carte', 'EnCours');

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
  `typeCompte` varchar(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `compte`
--

INSERT INTO `compte` (`idCompte`, `numeroCompte`, `solde`, `dateOuverture`, `statutCompte`, `plafondRetrait`, `plafondVirement`, `typeCompte`) VALUES
(44, 'NT0512', 1400.00, '2026-02-11', 'Bloque', 12.00, 17.00, 'Professionnel'),
(45, 'CB-04', 7820.00, '2026-02-24', 'Bloque', 500.00, 100.00, 'Epargne'),
(46, 'CB-01', 1453.00, '2026-02-25', 'Bloque', 5.00, 10.00, 'Courant'),
(47, 'CB-02', 0.00, '2026-02-25', 'Actif', 12.00, 10.00, 'Professionnel'),
(48, 'CB-18', 1245.24, '2026-02-26', 'Bloque', 5.00, 15.00, 'Epargne'),
(49, 'bouth0', 1450.00, '2026-02-03', 'Bloque', 120.00, 120.00, 'Professionnel'),
(50, 'CB-06', 1450.00, '2026-02-12', 'Bloque', 15.00, 14.00, 'Courant');

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
  `statut` varchar(30) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

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
  `statut` varchar(30) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

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
  `conditions` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `reclamation`
--

CREATE TABLE `reclamation` (
  `idReclamation` int(11) NOT NULL,
  `dateReclamation` date NOT NULL,
  `typeReclamation` varchar(50) NOT NULL,
  `description` varchar(150) NOT NULL,
  `status` varchar(30) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `transactions`
--

CREATE TABLE `transactions` (
  `idTransaction` int(11) NOT NULL,
  `idCompte` int(11) NOT NULL,
  `categorie` varchar(50) NOT NULL,
  `dateTransaction` varchar(20) NOT NULL,
  `montant` decimal(12,2) NOT NULL,
  `typeTransaction` varchar(30) NOT NULL,
  `statutTransaction` varchar(30) NOT NULL,
  `soldeApres` decimal(12,2) NOT NULL,
  `description` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Index pour les tables déchargées
--

--
-- Index pour la table `cashback`
--
ALTER TABLE `cashback`
  ADD PRIMARY KEY (`idCashback`),
  ADD KEY `fk_cashback_partenaire` (`idPartenaire`),
  ADD KEY `fk_cashback_transaction` (`idTransaction`),
  ADD KEY `fk_cashback_user` (`idUser`);

--
-- Index pour la table `coffrevirtuel`
--
ALTER TABLE `coffrevirtuel`
  ADD PRIMARY KEY (`idCoffre`);

--
-- Index pour la table `compte`
--
ALTER TABLE `compte`
  ADD PRIMARY KEY (`idCompte`),
  ADD UNIQUE KEY `numeroCompte` (`numeroCompte`);

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
-- Index pour la table `partenaire`
--
ALTER TABLE `partenaire`
  ADD PRIMARY KEY (`idPartenaire`);

--
-- Index pour la table `reclamation`
--
ALTER TABLE `reclamation`
  ADD PRIMARY KEY (`idReclamation`);

--
-- Index pour la table `transactions`
--
ALTER TABLE `transactions`
  ADD PRIMARY KEY (`idTransaction`),
  ADD KEY `fk_transaction_compte` (`idCompte`);

--
-- Index pour la table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`idUser`),
  ADD UNIQUE KEY `uq_users_email` (`email`),
  ADD KEY `idx_users_role` (`role`);

--
-- AUTO_INCREMENT pour les tables déchargées
--

--
-- AUTO_INCREMENT pour la table `cashback`
--
ALTER TABLE `cashback`
  MODIFY `idCashback` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `coffrevirtuel`
--
ALTER TABLE `coffrevirtuel`
  MODIFY `idCoffre` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=7;

--
-- Donnees pour la table `coffrevirtuel`
--

INSERT INTO `coffrevirtuel` (`idCoffre`, `nom`, `objectifMontant`, `montantActuel`, `dateCreation`, `dateObjectifs`, `status`, `estVerrouille`) VALUES
(1, 'Voiture', 20000.00, 5000.00, '2024-01-15', '2025-01-15', 'Actif', 0),
(2, 'Voyage', 5000.00, 1200.00, '2024-02-20', '2024-12-31', 'Actif', 0),
(3, 'Maison', 100000.00, 15000.00, '2024-03-10', '2026-03-10', 'Actif', 1),
(4, 'Etudes Enfants', 30000.00, 8000.00, '2024-04-05', '2028-09-01', 'Actif', 0),
(5, 'Urgence', 10000.00, 3500.00, '2024-05-18', '2025-05-18', 'Actif', 0),
(6, 'Rahma', 1000.00, 120.00, '2026-02-05', '2026-02-04', 'Actif', 0);

--
-- AUTO_INCREMENT pour la table `compte`
--
ALTER TABLE `compte`
  MODIFY `idCompte` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=51;

--
-- AUTO_INCREMENT pour la table `credit`
--
ALTER TABLE `credit`
  MODIFY `idCredit` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `garantiecredit`
--
ALTER TABLE `garantiecredit`
  MODIFY `idGarantie` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `partenaire`
--
ALTER TABLE `partenaire`
  MODIFY `idPartenaire` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `reclamation`
--
ALTER TABLE `reclamation`
  MODIFY `idReclamation` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `transactions`
--
ALTER TABLE `transactions`
  MODIFY `idTransaction` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `users`
--
ALTER TABLE `users`
  MODIFY `idUser` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- Contraintes pour les tables déchargées
--

--
-- Contraintes pour la table `cashback`
--
ALTER TABLE `cashback`
  ADD CONSTRAINT `fk_cashback_partenaire` FOREIGN KEY (`idPartenaire`) REFERENCES `partenaire` (`idPartenaire`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_cashback_transaction` FOREIGN KEY (`idTransaction`) REFERENCES `transactions` (`idTransaction`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_cashback_user` FOREIGN KEY (`idUser`) REFERENCES `users` (`idUser`) ON DELETE SET NULL;

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
-- Contraintes pour la table `transactions`
--
ALTER TABLE `transactions`
  ADD CONSTRAINT `fk_transaction_compte` FOREIGN KEY (`idCompte`) REFERENCES `compte` (`idCompte`) ON DELETE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
