<?php

namespace App\Service;

use Dompdf\Dompdf;
use Dompdf\Options;
use Endroid\QrCode\Builder\Builder;
use Endroid\QrCode\Encoding\Encoding;
use Endroid\QrCode\ErrorCorrectionLevel;
use Endroid\QrCode\RoundBlockSizeMode;
use Endroid\QrCode\Writer\SvgWriter;

final class ExportService
{
    public function buildPdf(string $title, array $headers, array $rows): string
    {
        $options = new Options();
        $options->set('isRemoteEnabled', true);

        $dompdf = new Dompdf($options);
        $html = '<html><body style="font-family: DejaVu Sans, sans-serif; color:#0a2540;">';
        $html .= sprintf('<h1 style="margin-bottom:8px;">%s</h1>', htmlspecialchars($title, ENT_QUOTES));
        $html .= sprintf('<p style="color:#66788a;">Generated at %s</p>', date('Y-m-d H:i:s'));
        $html .= '<table style="width:100%; border-collapse:collapse; margin-top:16px;">';
        $html .= '<thead><tr>';
        foreach ($headers as $header) {
            $html .= sprintf('<th style="border:1px solid #dbe3ef; padding:8px; background:#eef4fb; text-align:left;">%s</th>', htmlspecialchars($header, ENT_QUOTES));
        }
        $html .= '</tr></thead><tbody>';
        foreach ($rows as $row) {
            $html .= '<tr>';
            foreach ($row as $cell) {
                $html .= sprintf('<td style="border:1px solid #dbe3ef; padding:8px;">%s</td>', htmlspecialchars((string) $cell, ENT_QUOTES));
            }
            $html .= '</tr>';
        }
        $html .= '</tbody></table></body></html>';

        $dompdf->loadHtml($html);
        $dompdf->setPaper('A4', 'portrait');
        $dompdf->render();

        return $dompdf->output();
    }

    public function buildTransactionQrSvg(array $transaction): string
    {
        $payload = sprintf(
            "Transaction #%s\nCategory: %s\nDate: %s\nAmount: %s DT\nStatus: %s",
            $transaction['idTransaction'] ?? '-',
            $transaction['categorie'] ?? '-',
            $transaction['dateTransaction'] ?? '-',
            $transaction['montant_value'] ?? '0.00',
            $transaction['statutTransaction'] ?? '-'
        );

        $builder = new Builder(
            writer: new SvgWriter(),
            data: $payload,
            encoding: new Encoding('UTF-8'),
            errorCorrectionLevel: ErrorCorrectionLevel::High,
            size: 260,
            margin: 10,
            roundBlockSizeMode: RoundBlockSizeMode::Margin,
        );
        $result = $builder->build();

        return $result->getString();
    }
}
