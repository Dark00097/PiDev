<?php

namespace App\Controller;

use App\Service\AuthService;
use App\Service\BankingService;
use App\Service\ExportService;
use App\Service\GeminiService;
use App\Service\NotificationService;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\File\UploadedFile;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

final class AdminController extends AbstractController
{
    #[Route('/admin', name: 'admin_dashboard', methods: ['GET', 'POST'])]
    public function index(
        Request $request,
        AuthService $authService,
        BankingService $bankingService,
        GeminiService $geminiService,
        NotificationService $notificationService,
    ): Response {
        $session = $request->getSession();
        $user = $authService->getAuthenticatedUser($session);
        if ($user === null) {
            return $this->redirectToRoute('login');
        }

        $blockedReason = $authService->getLoginBlockReason($user);
        if ($blockedReason !== null) {
            $authService->logoutUser($session);
            $this->addFlash('error', $blockedReason);
            return $this->redirectToRoute('login');
        }

        if (strtoupper((string) ($user['role'] ?? '')) !== 'ROLE_ADMIN') {
            return $this->redirectToRoute('login');
        }

        $tab = (string) $request->query->get('tab', 'dashboard');
        $panel = trim((string) $request->query->get('panel', ''));
        $editUserId = $this->positiveQueryInt($request, 'edit');
        [$tab, $panel] = $this->normalizeAdminTabAndPanel($tab, $panel);

        if ($request->isMethod('POST')) {
            $tab = (string) $request->request->get('tab', $tab);
            $panel = trim((string) $request->request->get('panel', $panel));
            [$tab, $panel] = $this->normalizeAdminTabAndPanel($tab, $panel);
            $this->handleAdminAction($request, $authService, $bankingService, $geminiService, $notificationService, $user);

            $routeParams = ['tab' => $tab];
            if ($panel !== '') {
                $routeParams['panel'] = $panel;
            }

            return $this->redirectToRoute('admin_dashboard', $routeParams);
        }

        $data = $this->buildAdminTabData($tab, $panel, $bankingService, $notificationService, $user, $editUserId);
        if ($tab === 'users') {
            $data['support']['ai_assistant'] = $request->getSession()->get('nexora.users_ai_assistant');
            $data['support']['gemini_enabled'] = $geminiService->isConfigured();
        }

        $tabTemplate = $this->resolveAdminTabTemplate($tab);
        $tabStylesheets = $this->resolveAdminTabStylesheets($tab);

        return $this->render('interfaces/admin/MainView.html.twig', array_merge($data, [
            'mode' => 'admin',
            'route_name' => 'admin_dashboard',
            'tab_template' => $tabTemplate,
            'tab_stylesheets' => $tabStylesheets,
            'current_user' => $user,
            'feature_links' => [
                ['label' => 'Security Center', 'href' => $this->generateUrl('admin_features', ['section' => 'security'])],
                ['label' => 'Statistics', 'href' => $this->generateUrl('admin_features', ['section' => 'statistics'])],
            ],
            'tabs' => [
                ['key' => 'dashboard', 'label' => 'Tableau de Bord'],
                ['key' => 'users', 'label' => 'Gestion Utilisateurs'],
                ['key' => 'admin_account', 'label' => 'Compte admin'],
                ['key' => 'accounts', 'label' => 'Comptes Bancaires'],
                ['key' => 'transactions', 'label' => 'Transactions'],
                ['key' => 'credits', 'label' => 'Gestion Crédit'],
                ['key' => 'cashback', 'label' => 'Gestion Cashback'],
                ['key' => 'notifications', 'label' => 'Notifications'],
            ],
            'panel' => $panel,
        ]));
    }

    #[Route('/admin/credits/export/pdf/{kind}', name: 'admin_credits_export_pdf', requirements: ['kind' => 'credits|garanties'], methods: ['GET'])]
    public function exportCreditsAndGarantiesPdf(
        string $kind,
        Request $request,
        AuthService $authService,
        BankingService $bankingService,
        ExportService $exportService,
    ): Response {
        $session = $request->getSession();
        $user = $authService->getAuthenticatedUser($session);
        if ($user === null) {
            return $this->redirectToRoute('login');
        }

        $blockedReason = $authService->getLoginBlockReason($user);
        if ($blockedReason !== null) {
            $authService->logoutUser($session);
            $this->addFlash('error', $blockedReason);

            return $this->redirectToRoute('login');
        }

        if (strtoupper((string) ($user['role'] ?? '')) !== 'ROLE_ADMIN') {
            return $this->redirectToRoute('login');
        }

        $selectedUserId = $this->positiveQueryInt($request, 'userId');
        $users = $bankingService->listUsers();
        $userNamesById = [];
        foreach ($users as $userRow) {
            $id = (int) ($userRow['idUser'] ?? 0);
            if ($id <= 0) {
                continue;
            }

            $userNamesById[$id] = trim(sprintf('%s %s', (string) ($userRow['prenom'] ?? ''), (string) ($userRow['nom'] ?? '')));
        }

        $titleSuffix = 'Tous les utilisateurs';
        if ($selectedUserId !== null) {
            $titleSuffix = sprintf(
                'Utilisateur #%d%s',
                $selectedUserId,
                array_key_exists($selectedUserId, $userNamesById) && $userNamesById[$selectedUserId] !== ''
                    ? ' - '.$userNamesById[$selectedUserId]
                    : ''
            );
        }

        if ($kind === 'garanties') {
            $headers = ['ID Garantie', 'ID Credit', 'Utilisateur', 'Type', 'Valeur estimee', 'Valeur retenue', 'Statut', 'Date'];
            $rows = [];
            foreach ($bankingService->listGaranties() as $garantie) {
                $rowUserId = $this->nullablePositiveInt($garantie['resolved_user_id'] ?? ($garantie['idUser'] ?? null));
                if ($selectedUserId !== null && $rowUserId !== $selectedUserId) {
                    continue;
                }

                $rows[] = [
                    $garantie['idGarantie'] ?? '',
                    $garantie['idCredit'] ?? '',
                    $rowUserId !== null
                        ? sprintf(
                            '#%d%s',
                            $rowUserId,
                            (($userNamesById[$rowUserId] ?? '') !== '') ? ' - '.$userNamesById[$rowUserId] : ''
                        )
                        : '-',
                    (string) ($garantie['typeGarantie'] ?? ''),
                    number_format((float) ($garantie['valeurEstimee'] ?? 0), 2, '.', ' '),
                    number_format((float) ($garantie['valeurRetenue'] ?? 0), 2, '.', ' '),
                    (string) ($garantie['statut'] ?? ''),
                    (string) ($garantie['dateEvaluation'] ?? ''),
                ];
            }

            $title = sprintf('Rapport Garanties - %s', $titleSuffix);
            $fileKind = 'garanties';
        } else {
            $headers = ['ID Credit', 'Utilisateur', 'Compte', 'Type', 'Montant demande', 'Mensualite', 'Statut', 'Date demande'];
            $rows = [];
            foreach ($bankingService->listCredits() as $credit) {
                $rowUserId = $this->nullablePositiveInt($credit['idUser'] ?? null);
                if ($selectedUserId !== null && $rowUserId !== $selectedUserId) {
                    continue;
                }

                $rows[] = [
                    $credit['idCredit'] ?? '',
                    $rowUserId !== null
                        ? sprintf(
                            '#%d%s',
                            $rowUserId,
                            (($userNamesById[$rowUserId] ?? '') !== '') ? ' - '.$userNamesById[$rowUserId] : ''
                        )
                        : '-',
                    (string) ($credit['idCompte'] ?? ''),
                    (string) ($credit['typeCredit'] ?? ''),
                    number_format((float) ($credit['montantDemande'] ?? 0), 2, '.', ' '),
                    number_format((float) ($credit['mensualite'] ?? 0), 2, '.', ' '),
                    (string) ($credit['statut'] ?? ''),
                    (string) ($credit['dateDemande'] ?? ''),
                ];
            }

            $title = sprintf('Rapport Credits - %s', $titleSuffix);
            $fileKind = 'credits';
        }

        return new Response($exportService->buildPdf($title, $headers, $rows), 200, [
            'Content-Type' => 'application/pdf',
            'Content-Disposition' => sprintf('attachment; filename="nexora-%s%s.pdf"', $fileKind, $selectedUserId !== null ? '-user-'.$selectedUserId : '-all-users'),
        ]);
    }

    private function handleAdminAction(
        Request $request,
        AuthService $authService,
        BankingService $bankingService,
        GeminiService $geminiService,
        NotificationService $notificationService,
        array $user
    ): void {
        $action = (string) $request->request->get('action', '');

        try {
            switch ($action) {
                case 'user_save':
                    $payload = $request->request->all();
                    $imagePath = $this->handleProfileImageUpload($request, 'profile_image');
                    if ($imagePath !== null) {
                        $payload['profile_image_path'] = $imagePath;
                    }
                    $bankingService->saveUser($payload, $this->requestInt($request, 'idUser'));
                    $this->addFlash('success', 'User saved.');
                    break;
                case 'user_status':
                    $bankingService->updateUserStatus($this->requestInt($request, 'idUser') ?? 0, (string) $request->request->get('status', 'PENDING'));
                    $this->addFlash('success', 'User status updated.');
                    break;
                case 'user_delete':
                    $bankingService->deleteUser($this->requestInt($request, 'idUser') ?? 0);
                    $this->addFlash('success', 'User deleted.');
                    break;
                case 'user_ai_assist':
                    $aiResult = $geminiService->generateUserManagementAdvice([
                        'nom' => (string) $request->request->get('nom', ''),
                        'prenom' => (string) $request->request->get('prenom', ''),
                        'role' => (string) $request->request->get('role', ''),
                        'status' => (string) $request->request->get('status', ''),
                        'reason' => (string) $request->request->get('reason', ''),
                        'prompt' => (string) $request->request->get('prompt', ''),
                    ]);
                    $request->getSession()->set('nexora.users_ai_assistant', $aiResult);
                    $this->addFlash('success', sprintf('AI assistant (%s) updated.', $aiResult['provider']));
                    break;
                case 'account_save':
                    $bankingService->saveAccount($request->request->all(), $this->requestInt($request, 'idCompte'));
                    $this->addFlash('success', 'Account saved.');
                    break;
                case 'account_delete':
                    $bankingService->deleteAccount($this->requestInt($request, 'idCompte') ?? 0);
                    $this->addFlash('success', 'Account deleted.');
                    break;
                case 'transaction_save':
                    $bankingService->saveTransaction($request->request->all(), $this->requestInt($request, 'idTransaction'));
                    $this->addFlash('success', 'Transaction saved.');
                    break;
                case 'transaction_delete':
                    $bankingService->deleteTransaction($this->requestInt($request, 'idTransaction') ?? 0);
                    $this->addFlash('success', 'Transaction deleted.');
                    break;
                case 'credit_save':
                    $bankingService->saveCredit($request->request->all(), $this->requestInt($request, 'idCredit'));
                    $this->addFlash('success', 'Credit saved.');
                    break;
                case 'credit_delete':
                    $bankingService->deleteCredit($this->requestInt($request, 'idCredit') ?? 0);
                    $this->addFlash('success', 'Credit deleted.');
                    break;
                case 'garantie_save':
                    $bankingService->saveGarantie($request->request->all(), $this->requestInt($request, 'idGarantie'));
                    $this->addFlash('success', 'Garantie saved.');
                    break;
                case 'garantie_delete':
                    $bankingService->deleteGarantie($this->requestInt($request, 'idGarantie') ?? 0);
                    $this->addFlash('success', 'Garantie deleted.');
                    break;
                case 'partner_save':
                    $bankingService->savePartenaire($request->request->all(), $this->requestInt($request, 'idPartenaire'));
                    $this->addFlash('success', 'Partner saved.');
                    break;
                case 'partner_delete':
                    $bankingService->deletePartenaire($this->requestInt($request, 'idPartenaire') ?? 0);
                    $this->addFlash('success', 'Partner deleted.');
                    break;
                case 'cashback_save':
                    $bankingService->saveCashback($request->request->all(), $this->requestInt($request, 'id_cashback'));
                    $this->addFlash('success', 'Cashback saved.');
                    break;
                case 'cashback_delete':
                    $bankingService->deleteCashback($this->requestInt($request, 'id_cashback') ?? 0);
                    $this->addFlash('success', 'Cashback deleted.');
                    break;
                case 'cashback_reward':
                    $bankingService->grantCashbackReward(
                        $this->requestInt($request, 'id_cashback') ?? 0,
                        (float) $request->request->get('bonus_amount', 0),
                        (string) $request->request->get('bonus_note', '')
                    );
                    $this->addFlash('success', 'Cashback reward granted.');
                    break;
                case 'cashback_bonus':
                    $bankingService->setCashbackBonusDecision(
                        $this->requestInt($request, 'id_cashback') ?? 0,
                        (string) $request->request->get('bonus_decision', 'Rejected') === 'Approved',
                        (string) $request->request->get('bonus_note', '')
                    );
                    $this->addFlash('success', 'Cashback decision saved.');
                    break;
                case 'reclamation_save':
                    $bankingService->saveReclamation($request->request->all(), $this->requestInt($request, 'idReclamation'));
                    $this->addFlash('success', 'Reclamation saved.');
                    break;
                case 'reclamation_delete':
                    $bankingService->deleteReclamation($this->requestInt($request, 'idReclamation') ?? 0);
                    $this->addFlash('success', 'Reclamation deleted.');
                    break;
                case 'reclamation_blur':
                    $bankingService->toggleReclamationBlur(
                        $this->requestInt($request, 'idReclamation') ?? 0,
                        (string) $request->request->get('is_blurred', '0') === '1'
                    );
                    $this->addFlash('success', 'Reclamation blur status updated.');
                    break;
                case 'vault_save':
                    $bankingService->saveVault($request->request->all(), $this->requestInt($request, 'idCoffre'));
                    $this->addFlash('success', 'Vault saved.');
                    break;
                case 'vault_delete':
                    $bankingService->deleteVault($this->requestInt($request, 'idCoffre') ?? 0);
                    $this->addFlash('success', 'Vault deleted.');
                    break;
                case 'notifications_read':
                    $notificationService->markAllAsRead((int) $user['idUser'], (string) $user['role']);
                    $this->addFlash('success', 'Notifications marked as read.');
                    break;
                case 'admin_profile_save':
                    $profilePayload = [
                        'nom' => (string) $request->request->get('nom', ''),
                        'prenom' => (string) $request->request->get('prenom', ''),
                        'telephone' => (string) $request->request->get('telephone', ''),
                        'email' => (string) $request->request->get('email', ''),
                    ];
                    $profileImagePath = $this->handleProfileImageUpload($request, 'profile_image');
                    if ($profileImagePath !== null) {
                        $profilePayload['profile_image_path'] = $profileImagePath;
                    }
                    $authService->updateProfile((int) $user['idUser'], [
                        ...$profilePayload,
                    ]);
                    $this->addFlash('success', 'Admin profile updated.');
                    break;
                case 'admin_biometric_save':
                    $authService->updateProfile((int) $user['idUser'], [
                        'biometric_enabled' => (string) $request->request->get('biometric_enabled', '0'),
                        'biometric_face_descriptor' => (string) $request->request->get('face_descriptor', ''),
                    ]);
                    $this->addFlash('success', 'Admin Face ID updated.');
                    break;
                case 'admin_biometric_clear':
                    $authService->updateProfile((int) $user['idUser'], [
                        'clear_biometric_face' => '1',
                        'biometric_enabled' => '0',
                    ]);
                    $this->addFlash('success', 'Admin Face ID removed.');
                    break;
                case 'admin_password_change':
                    $newPassword = (string) $request->request->get('new_password', '');
                    $confirmPassword = (string) $request->request->get('confirm_password', '');
                    if ($newPassword !== $confirmPassword) {
                        throw new \InvalidArgumentException('New password and confirmation do not match.');
                    }
                    if (strlen($newPassword) < 8) {
                        throw new \InvalidArgumentException('New password must be at least 8 characters.');
                    }

                    $authService->changePassword(
                        (int) $user['idUser'],
                        (string) $request->request->get('current_password', ''),
                        $newPassword
                    );
                    $this->addFlash('success', 'Admin password updated.');
                    break;
            }
        } catch (\Throwable $exception) {
            $this->addFlash('error', $exception->getMessage());
        }
    }

    private function buildAdminTabData(
        string $tab,
        string $panel,
        BankingService $bankingService,
        NotificationService $notificationService,
        array $user,
        ?int $editUserId = null
    ): array {
        $summary = $bankingService->getAdminDashboard();
        $data = [
            'tab' => $tab,
            'summary' => $summary,
            'items' => [],
            'support' => [],
            'notifications' => $notificationService->getRecentNotificationsFor((int) $user['idUser'], (string) $user['role'], 20),
            'notifications_count' => $notificationService->countUnreadFor((int) $user['idUser'], (string) $user['role']),
        ];
        $data['support']['active_panel'] = $panel;

        if ($tab === 'users') {
            $data['items'] = $bankingService->listUsers();
            if ($editUserId !== null) {
                foreach ($data['items'] as $item) {
                    if ((int) ($item['idUser'] ?? 0) === $editUserId) {
                        $data['support']['selected_user'] = $item;
                        break;
                    }
                }
            }
        } elseif ($tab === 'accounts') {
            $accounts = $bankingService->listAccounts();
            $vaults = $bankingService->listVaults();
            $data['support']['users'] = $bankingService->listUsers();
            $data['support']['accounts'] = $accounts;
            $data['support']['vaults'] = $vaults;
            $data['items'] = $panel === 'coffre' ? $vaults : $accounts;
        } elseif ($tab === 'transactions') {
            $transactions = $bankingService->listTransactions();
            $reclamations = $bankingService->listReclamations();
            $data['support']['users'] = $bankingService->listUsers();
            $data['support']['accounts'] = $bankingService->listAccounts();
            $data['support']['transactions'] = $transactions;
            $data['support']['reclamations'] = $reclamations;
            $data['items'] = $panel === 'reclamation' ? $reclamations : $transactions;
        } elseif ($tab === 'credits') {
            $credits = $bankingService->listCredits();
            $garanties = $bankingService->listGaranties();
            $data['support']['users'] = $bankingService->listUsers();
            $data['support']['accounts'] = $bankingService->listAccounts();
            $data['support']['credits'] = $credits;
            $data['support']['garanties'] = $garanties;
            $data['support']['credit_type_stats'] = $bankingService->getCreditTypeDistribution();
            $data['support']['garantie_type_stats'] = $bankingService->getGarantieTypeDistribution();
            $data['items'] = $panel === 'garantie' ? $garanties : $credits;
        } elseif ($tab === 'cashback') {
            $cashbacks = $bankingService->listCashbacks();
            $partners = $bankingService->listPartenaires();
            $data['support']['users'] = $bankingService->listUsers();
            $data['support']['partners'] = $partners;
            $data['support']['partners_items'] = $partners;
            $data['support']['cashbacks'] = $cashbacks;
            $data['items'] = $panel === 'partenaire' ? $partners : $cashbacks;
        } elseif ($tab === 'notifications') {
            $data['items'] = $data['notifications'];
        }

        return $data;
    }

    private function requestInt(Request $request, string $key): ?int
    {
        $value = $request->request->get($key);
        if ($value === null) {
            return null;
        }

        $normalized = trim((string) $value);
        if ($normalized === '' || !preg_match('/^-?\d+$/', $normalized)) {
            return null;
        }

        $intValue = (int) $normalized;

        return $intValue > 0 ? $intValue : null;
    }

    private function positiveQueryInt(Request $request, string $key): ?int
    {
        $value = $request->query->get($key);
        if ($value === null) {
            return null;
        }

        $normalized = trim((string) $value);
        if ($normalized === '' || !preg_match('/^\d+$/', $normalized)) {
            return null;
        }

        $intValue = (int) $normalized;

        return $intValue > 0 ? $intValue : null;
    }

    private function nullablePositiveInt(mixed $value): ?int
    {
        if ($value === null || $value === '') {
            return null;
        }

        $intValue = (int) $value;

        return $intValue > 0 ? $intValue : null;
    }

    private function handleProfileImageUpload(Request $request, string $fieldName): ?string
    {
        $file = $request->files->get($fieldName);
        if (!$file instanceof UploadedFile) {
            return null;
        }

        if (!$file->isValid()) {
            throw new \RuntimeException('Profile image upload failed.');
        }

        $allowedMimeTypes = [
            'image/jpeg' => 'jpg',
            'image/png' => 'png',
            'image/webp' => 'webp',
            'image/gif' => 'gif',
        ];

        $mimeType = (string) $file->getMimeType();
        if (!array_key_exists($mimeType, $allowedMimeTypes)) {
            throw new \InvalidArgumentException('Only JPG, PNG, WEBP or GIF profile images are allowed.');
        }

        $projectDir = (string) $this->getParameter('kernel.project_dir');
        $targetDirectory = $projectDir.'/public/uploads/profile';
        if (!is_dir($targetDirectory) && !mkdir($targetDirectory, 0777, true) && !is_dir($targetDirectory)) {
            throw new \RuntimeException('Unable to create profile upload directory.');
        }

        $fileName = sprintf('profile_%s.%s', bin2hex(random_bytes(10)), $allowedMimeTypes[$mimeType]);
        $file->move($targetDirectory, $fileName);

        return 'uploads/profile/'.$fileName;
    }

    private function resolveAdminTabTemplate(string $tab): string
    {
        return match ($tab) {
            'users' => 'interfaces/admin/tabs/users.html.twig',
            'admin_account' => 'interfaces/admin/tabs/admin_account.html.twig',
            'accounts' => 'interfaces/admin/tabs/accounts.html.twig',
            'cashback' => 'interfaces/admin/tabs/cashback.html.twig',
            'credits' => 'interfaces/admin/tabs/credits.html.twig',
            'transactions' => 'interfaces/admin/tabs/transactions.html.twig',
            'notifications' => 'interfaces/admin/tabs/notifications.html.twig',
            default => 'interfaces/admin/tabs/dashboard.html.twig',
        };
    }

    private function resolveAdminTabStylesheets(string $tab): array
    {
        return match ($tab) {
            'users' => ['styles/interfaces/sections/admin-users.css'],
            'admin_account' => ['styles/interfaces/sections/admin-profile.css'],
            'accounts' => ['styles/interfaces/sections/admin-accounts.css'],
            default => [],
        };
    }

    private function normalizeAdminTabAndPanel(string $tab, string $panel): array
    {
        $tab = strtolower(trim($tab));
        $panel = strtolower(trim($panel));

        $legacyTabMap = [
            'vaults' => ['accounts', 'coffre'],
            'complaints' => ['transactions', 'reclamation'],
            'garanties' => ['credits', 'garantie'],
            'partners' => ['cashback', 'partenaire'],
        ];

        if (array_key_exists($tab, $legacyTabMap)) {
            [$mappedTab, $mappedPanel] = $legacyTabMap[$tab];
            $tab = $mappedTab;
            if ($panel === '') {
                $panel = $mappedPanel;
            }
        }

        $allowedTabs = ['dashboard', 'users', 'admin_account', 'accounts', 'transactions', 'credits', 'cashback', 'notifications'];
        if (!in_array($tab, $allowedTabs, true)) {
            $tab = 'dashboard';
        }

        $defaultPanelByTab = [
            'accounts' => 'compte',
            'transactions' => 'transaction',
            'credits' => 'credit',
            'cashback' => 'partenaire',
        ];

        $allowedPanelsByTab = [
            'accounts' => ['compte', 'coffre'],
            'transactions' => ['transaction', 'reclamation'],
            'credits' => ['credit', 'garantie'],
            'cashback' => ['partenaire', 'cashback'],
        ];

        if (array_key_exists($tab, $allowedPanelsByTab)) {
            if (!in_array($panel, $allowedPanelsByTab[$tab], true)) {
                $panel = $defaultPanelByTab[$tab];
            }
        } else {
            $panel = '';
        }

        return [$tab, $panel];
    }
}
