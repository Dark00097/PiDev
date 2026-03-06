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
(1, 'System', 'Admin', 'admin@nexora.com', '+21600000000', 'ROLE_ADMIN', 'ACTIVE', 'PBKDF2$210000$vilkFWvVrCUKaSHVKswdnA==$D6x8vFgqQ70O7fi+oBIRwcOKNu6nsnQUIrxElRpLkuc=');

--
-- Déchargement des données de la table `coffrevirtuel`
--

INSERT INTO `coffrevirtuel` (`idCoffre`, `nom`, `objectifMontant`, `montantActuel`, `dateCreation`, `dateObjectifs`, `status`, `estVerrouille`) VALUES
(6, 'Rahma', 1000.00, 120.00, '2026-02-05', '2026-02-04', 'Actif', 0);

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
  ADD KEY `fk_cashback_transaction` (`idTransaction`);

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
  ADD CONSTRAINT `fk_cashback_transaction` FOREIGN KEY (`idTransaction`) REFERENCES `transactions` (`idTransaction`) ON DELETE CASCADE;

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
