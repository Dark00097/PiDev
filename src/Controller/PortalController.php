<?php

namespace App\Controller;

use App\Service\ActivityService;
use App\Service\AuthService;
use App\Service\BankingService;
use App\Service\GamificationService;
use App\Service\InsightsService;
use App\Service\NotificationService;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\File\UploadedFile;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Attribute\Route;

final class PortalController extends AbstractController
{
    #[Route('/portal', name: 'portal_dashboard', methods: ['GET', 'POST'])]
    public function index(
        Request $request,
        AuthService $authService,
        BankingService $bankingService,
        NotificationService $notificationService,
        ActivityService $activityService,
        InsightsService $insightsService,
        GamificationService $gamificationService,
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

        if (strtoupper((string) ($user['role'] ?? '')) === 'ROLE_ADMIN') {
            return $this->redirectToRoute('admin_dashboard');
        }

        $tab = (string) $request->query->get('tab', 'dashboard');
        if ($tab === 'overview') {
            $tab = 'dashboard';
        }

        if ($request->isMethod('POST')) {
            $tab = (string) $request->request->get('tab', $tab);
            $panel = trim((string) $request->request->get('panel', ''));
            $this->handlePortalAction($request, $authService, $bankingService, $notificationService, $insightsService, $gamificationService, $user);

            $routeParams = ['tab' => $tab];
            if ($panel !== '') {
                $routeParams['panel'] = $panel;
            }

            return $this->redirectToRoute('portal_dashboard', $routeParams);
        }

        $data = $this->buildPortalTabData($tab, $bankingService, $notificationService, $activityService, $gamificationService, $user);
        if ($tab === 'profile') {
            $profileAi = $request->getSession()->get('nexora.profile_ai_data');
            if (!is_array($profileAi)) {
                $profileAi = $this->buildProfileAiData((int) $user['idUser'], $insightsService);
                $request->getSession()->set('nexora.profile_ai_data', $profileAi);
            }
            $data['support']['profile_ai'] = $profileAi;
        }

        $tabTemplate = $this->resolvePortalTabTemplate($tab);
        $tabStylesheets = $this->resolvePortalTabStylesheets($tab);

        return $this->render('interfaces/portal/UserDashboard.html.twig', array_merge($data, [
            'mode' => 'portal',
            'route_name' => 'portal_dashboard',
            'tab_template' => $tabTemplate,
            'tab_stylesheets' => $tabStylesheets,
            'current_user' => $user,
            'feature_links' => [
                ['label' => 'Insights', 'href' => $this->generateUrl('portal_features', ['section' => 'insights'])],
                ['label' => 'Games', 'href' => $this->generateUrl('portal_features', ['section' => 'games'])],
                ['label' => 'Payments', 'href' => $this->generateUrl('portal_features', ['section' => 'payments'])],
                ['label' => 'Exports', 'href' => $this->generateUrl('portal_features', ['section' => 'exports'])],
            ],
            'tabs' => [
                ['key' => 'dashboard', 'label' => 'Dashboard'],
                ['key' => 'accounts', 'label' => 'Comptes'],
                ['key' => 'transactions', 'label' => 'Transactions'],
                ['key' => 'credits', 'label' => 'Credits'],
                ['key' => 'cashback', 'label' => 'Recompenses'],
                ['key' => 'garanties', 'label' => 'Garanties'],
                ['key' => 'complaints', 'label' => 'Reclamations'],
                ['key' => 'vaults', 'label' => 'Coffres'],
                ['key' => 'profile', 'label' => 'Profil'],
                ['key' => 'notifications', 'label' => 'Notifications'],
            ],
        ]));
    }

    private function handlePortalAction(
        Request $request,
        AuthService $authService,
        BankingService $bankingService,
        NotificationService $notificationService,
        InsightsService $insightsService,
        GamificationService $gamificationService,
        array $user
    ): void {
        $action = (string) $request->request->get('action', '');
        $userId = (int) $user['idUser'];

        try {
            switch ($action) {
                case 'account_save':
                    $bankingService->saveAccount($request->request->all(), $this->requestInt($request, 'idCompte'), $userId);
                    $this->addFlash('success', 'Account saved.');
                    break;
                case 'account_delete':
                    $bankingService->deleteAccount($this->requestInt($request, 'idCompte') ?? 0, $userId);
                    $this->addFlash('success', 'Account deleted.');
                    break;
                case 'transaction_save':
                    $bankingService->saveTransaction($request->request->all(), $this->requestInt($request, 'idTransaction'), $userId);
                    $this->addFlash('success', 'Transaction saved.');
                    break;
                case 'transaction_delete':
                    $bankingService->deleteTransaction($this->requestInt($request, 'idTransaction') ?? 0, $userId);
                    $this->addFlash('success', 'Transaction deleted.');
                    break;
                case 'credit_save':
                    $bankingService->saveCredit($request->request->all(), $this->requestInt($request, 'idCredit'), $userId);
                    $this->addFlash('success', 'Credit saved.');
                    break;
                case 'credit_delete':
                    $bankingService->deleteCredit($this->requestInt($request, 'idCredit') ?? 0, $userId);
                    $this->addFlash('success', 'Credit deleted.');
                    break;
                case 'garantie_save':
                    $bankingService->saveGarantie($request->request->all(), $this->requestInt($request, 'idGarantie'), $userId);
                    $this->addFlash('success', 'Garantie saved.');
                    break;
                case 'garantie_delete':
                    $bankingService->deleteGarantie($this->requestInt($request, 'idGarantie') ?? 0, $userId);
                    $this->addFlash('success', 'Garantie deleted.');
                    break;
                case 'cashback_save':
                    $bankingService->saveCashback($request->request->all(), $this->requestInt($request, 'id_cashback'), $userId);
                    $this->addFlash('success', 'Cashback saved.');
                    break;
                case 'cashback_delete':
                    $bankingService->deleteCashback($this->requestInt($request, 'id_cashback') ?? 0, $userId);
                    $this->addFlash('success', 'Cashback deleted.');
                    break;
                case 'cashback_rating':
                    $bankingService->submitCashbackRating(
                        $this->requestInt($request, 'id_cashback') ?? 0,
                        $userId,
                        (float) $request->request->get('user_rating', 0),
                        (string) $request->request->get('user_rating_comment', '')
                    );
                    $this->addFlash('success', 'Rating submitted.');
                    break;
                case 'reclamation_save':
                    $bankingService->saveReclamation($request->request->all(), $this->requestInt($request, 'idReclamation'), $userId);
                    $this->addFlash('success', 'Reclamation saved.');
                    break;
                case 'reclamation_delete':
                    $bankingService->deleteReclamation($this->requestInt($request, 'idReclamation') ?? 0, $userId);
                    $this->addFlash('success', 'Reclamation deleted.');
                    break;
                case 'vault_save':
                    $bankingService->saveVault($request->request->all(), $this->requestInt($request, 'idCoffre'), $userId);
                    $this->addFlash('success', 'Vault saved.');
                    break;
                case 'vault_delete':
                    $bankingService->deleteVault($this->requestInt($request, 'idCoffre') ?? 0, $userId);
                    $this->addFlash('success', 'Vault deleted.');
                    break;
                case 'profile_save':
                    $profilePayload = $request->request->all();
                    $profileImagePath = $this->handleProfileImageUpload($request, 'profile_image');
                    if ($profileImagePath !== null) {
                        $profilePayload['profile_image_path'] = $profileImagePath;
                    }
                    $authService->updateProfile($userId, $profilePayload);
                    $this->addFlash('success', 'Profile updated.');
                    break;
                case 'profile_biometric_save':
                    $authService->updateProfile($userId, [
                        'biometric_enabled' => (string) $request->request->get('biometric_enabled', '0'),
                        'biometric_face_descriptor' => (string) $request->request->get('face_descriptor', ''),
                    ]);
                    $this->addFlash('success', 'Biometric Face ID updated.');
                    break;
                case 'profile_biometric_clear':
                    $authService->updateProfile($userId, [
                        'clear_biometric_face' => '1',
                        'biometric_enabled' => '0',
                    ]);
                    $this->addFlash('success', 'Biometric Face ID removed.');
                    break;
                case 'profile_ai_refresh':
                    $request->getSession()->set('nexora.profile_ai_data', $this->buildProfileAiData($userId, $insightsService));
                    $this->addFlash('success', 'AI profile analysis updated.');
                    break;
                case 'profile_ai_ack_surplus':
                    $month = trim((string) $request->request->get('month', ''));
                    if ($month !== '') {
                        $insightsService->acknowledgeMonthlySurplus($userId, $month);
                    }
                    $request->getSession()->set('nexora.profile_ai_data', $this->buildProfileAiData($userId, $insightsService));
                    $this->addFlash('success', 'Monthly surplus suggestion acknowledged.');
                    break;
                case 'wheel_spin':
                    $wheelResult = $gamificationService->spinWheel($userId);
                    $spinMessage = (string) ($wheelResult['spin_result']['message'] ?? '');
                    $this->addFlash('success', $spinMessage !== '' ? $spinMessage : 'Wheel spin completed.');
                    break;
                case 'wheel_bonus':
                    $wheelResult = $gamificationService->claimWheelBonus($userId, $this->requestInt($request, 'idCompte') ?? 0);
                    $this->addFlash('success', (bool) ($wheelResult['bonus_ready'] ?? false) ? 'Wheel bonus is still available.' : 'Wheel bonus credited (+50 DT).');
                    break;
                case 'profile_password_change':
                    $newPassword = (string) $request->request->get('new_password', '');
                    $confirmPassword = (string) $request->request->get('confirm_password', '');
                    if ($newPassword !== $confirmPassword) {
                        throw new \InvalidArgumentException('New password and confirmation do not match.');
                    }
                    if (strlen($newPassword) < 8) {
                        throw new \InvalidArgumentException('New password must be at least 8 characters.');
                    }

                    $authService->changePassword(
                        $userId,
                        (string) $request->request->get('current_password', ''),
                        $newPassword
                    );
                    $this->addFlash('success', 'Password updated.');
                    break;
                case 'profile_send_reset_otp':
                    $authService->sendPasswordResetOtp((string) ($user['email'] ?? ''), $request->getSession());
                    $this->addFlash('success', 'Reset OTP sent by email.');
                    break;
                case 'profile_reset_password':
                    $newPassword = (string) $request->request->get('reset_new_password', '');
                    $confirmPassword = (string) $request->request->get('reset_confirm_password', '');
                    if ($newPassword !== $confirmPassword) {
                        throw new \InvalidArgumentException('Reset password and confirmation do not match.');
                    }
                    if (strlen($newPassword) < 8) {
                        throw new \InvalidArgumentException('Reset password must be at least 8 characters.');
                    }

                    $email = (string) ($user['email'] ?? '');
                    $otp = (string) $request->request->get('reset_otp', '');
                    if (!$authService->verifyPasswordResetOtp($email, $otp, $request->getSession())) {
                        throw new \InvalidArgumentException('Reset OTP is invalid or expired.');
                    }
                    $authService->resetPasswordByVerifiedEmail($email, $newPassword, $request->getSession());
                    $this->addFlash('success', 'Password reset completed.');
                    break;
                case 'notifications_read':
                    $notificationService->markAllAsRead($userId, (string) $user['role']);
                    $this->addFlash('success', 'Notifications marked as read.');
                    break;
            }
        } catch (\Throwable $exception) {
            $this->addFlash('error', $exception->getMessage());
        }
    }

    private function buildPortalTabData(
        string $tab,
        BankingService $bankingService,
        NotificationService $notificationService,
        ActivityService $activityService,
        GamificationService $gamificationService,
        array $user
    ): array {
        $userId = (int) $user['idUser'];
        $summary = $bankingService->getUserDashboard($userId);
        $data = [
            'tab' => $tab,
            'summary' => $summary,
            'items' => [],
            'support' => [],
            'notifications' => $notificationService->getRecentNotificationsFor($userId, (string) $user['role'], 20),
            'notifications_count' => $notificationService->countUnreadFor($userId, (string) $user['role']),
        ];

        if ($tab === 'accounts') {
            $data['items'] = $bankingService->listAccounts($userId);
            $data['support']['vaults'] = $bankingService->listVaults($userId);
            $data['support']['wheel'] = $gamificationService->getWheelStatus($userId);
            $data['support']['activity'] = $activityService->listRecent($userId, 40);
        } elseif ($tab === 'transactions') {
            $data['items'] = $bankingService->listTransactions($userId);
            $data['support']['accounts'] = $bankingService->listAccounts($userId);
        } elseif ($tab === 'credits') {
            $data['items'] = $bankingService->listCredits($userId);
            $data['support']['accounts'] = $bankingService->listAccounts($userId);
            $data['support']['garanties'] = $bankingService->listGaranties($userId);
        } elseif ($tab === 'garanties') {
            $data['items'] = $bankingService->listGaranties($userId);
            $data['support']['credits'] = $bankingService->listCredits($userId);
        } elseif ($tab === 'cashback') {
            $data['items'] = $bankingService->listCashbacks($userId);
            $data['support']['partners'] = $bankingService->listPartenaires();
        } elseif ($tab === 'complaints') {
            $data['items'] = $bankingService->listReclamations($userId);
            $data['support']['transactions'] = $bankingService->listTransactions($userId);
        } elseif ($tab === 'vaults') {
            $data['items'] = $bankingService->listVaults($userId);
            $data['support']['accounts'] = $bankingService->listAccounts($userId);
        } elseif ($tab === 'profile') {
            $data['support']['activity'] = $activityService->listRecent($userId, 25);
        } elseif ($tab === 'notifications') {
            $data['items'] = $data['notifications'];
        }

        return $data;
    }

    private function requestInt(Request $request, string $key): ?int
    {
        $value = $request->request->get($key);
        if ($value === null || $value === '') {
            return null;
        }

        return (int) $value;
    }

    private function buildProfileAiData(int $userId, InsightsService $insightsService): array
    {
        return [
            'generated_at' => (new \DateTimeImmutable())->format('Y-m-d H:i:s'),
            'security_analysis' => $insightsService->getAccountSecurityAnalysis($userId),
            'prediction' => $insightsService->getSpendingPrediction($userId),
            'account_advice' => $insightsService->getAccountAdvisor($userId),
            'cashback_advice' => $insightsService->getCashbackAdvisor($userId),
            'surplus' => $insightsService->detectMonthlySurplus($userId),
        ];
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

    private function resolvePortalTabTemplate(string $tab): string
    {
        return match ($tab) {
            'accounts' => 'interfaces/portal/tabs/accounts.html.twig',
            'transactions' => 'interfaces/portal/tabs/transactions.html.twig',
            'credits' => 'interfaces/portal/tabs/credits.html.twig',
            'cashback' => 'interfaces/portal/tabs/cashback.html.twig',
            'garanties' => 'interfaces/portal/tabs/garanties.html.twig',
            'complaints' => 'interfaces/portal/tabs/complaints.html.twig',
            'vaults' => 'interfaces/portal/tabs/vaults.html.twig',
            'profile' => 'interfaces/portal/tabs/profile.html.twig',
            'notifications' => 'interfaces/portal/tabs/notifications.html.twig',
            default => 'interfaces/portal/tabs/dashboard.html.twig',
        };
    }

    private function resolvePortalTabStylesheets(string $tab): array
    {
        return match ($tab) {
            'dashboard' => ['styles/interfaces/sections/portal-dashboard.css'],
            'accounts' => ['styles/interfaces/sections/portal-accounts.css'],
            'transactions' => ['styles/interfaces/sections/portal-transactions.css'],
            'credits' => ['styles/interfaces/sections/portal-credits.css'],
            'profile' => ['styles/interfaces/sections/portal-profile.css'],
            default => [],
        };
    }
}
