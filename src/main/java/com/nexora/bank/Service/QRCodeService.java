package com.nexora.bank.Service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.util.HashMap;
import java.util.Map;

public class QRCodeService {

    private static final int QR_SIZE = 300;

    public Image generateQRCode(String content) {
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 2);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(
                content, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE, hints);

            WritableImage image = new WritableImage(QR_SIZE, QR_SIZE);
            PixelWriter pixelWriter = image.getPixelWriter();

            for (int x = 0; x < QR_SIZE; x++)
                for (int y = 0; y < QR_SIZE; y++)
                    pixelWriter.setColor(x, y,
                        bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);

            return image;

        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String formatTransactionData(
            int id, String categorie, double montant,
            String type, String statut, String date) {

        return "Nexora Bank - Recu de Transaction"
             + "\n--------------------------------"
             + "\nTransaction  : #" + id
             + "\nCategorie    : " + categorie
             + "\nType         : " + type
             + "\nMontant      : " + String.format("%.2f DT", montant)
             + "\nStatut       : " + statut
             + "\nDate         : " + date
             + "\n--------------------------------"
             + "\nMerci de votre confiance !";
    }
}
