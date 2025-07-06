package com.yuanseen.shuati.ui.gallery.addqes.bankinfo;

import android.content.Context;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

public class BankInfoManager {
    private static final String INFO_SUFFIX = "_info.json";
    private File localBankDirectory;

    public BankInfoManager(Context context) {
        localBankDirectory = new File(context.getFilesDir(), "question_banks");
        if (!localBankDirectory.exists()) {
            localBankDirectory.mkdirs();
        }
    }

    // Get BankInfo
    public BankInfo getBankInfo(String bankId) {
        File infoFile = new File(localBankDirectory, bankId + INFO_SUFFIX);
        if (!infoFile.exists()) {
            return new BankInfo(bankId);
        }

        try {
            String jsonStr = new String(Files.readAllBytes(infoFile.toPath()));
            return BankInfo.fromJsonString(jsonStr);
        } catch (IOException e) {
            e.printStackTrace();
            return new BankInfo(bankId);
        }
    }

    // Save BankInfo
    public boolean saveBankInfo(BankInfo bankInfo) {
        File infoFile = new File(localBankDirectory, bankInfo.getBankId() + INFO_SUFFIX);
        try (FileWriter writer = new FileWriter(infoFile)) {
            writer.write(bankInfo.toJsonString());
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Add favorite with type
    public boolean addFavorite(String bankId, String type, int questionId) {
        BankInfo info = getBankInfo(bankId);
        info.addFavorite(type, questionId);
        return saveBankInfo(info);
    }

    // Remove favorite with type
    public boolean removeFavorite(String bankId, String type, int questionId) {
        BankInfo info = getBankInfo(bankId);
        info.removeFavorite(type, questionId);
        return saveBankInfo(info);
    }

    // Add wrong question with type
    public boolean addWrongQuestion(String bankId, String type, int questionId) {
        BankInfo info = getBankInfo(bankId);
        info.addWrongQuestion(type, questionId);
        return saveBankInfo(info);
    }

    // Remove wrong question with type
    public boolean removeWrongQuestion(String bankId, String type, int questionId) {
        BankInfo info = getBankInfo(bankId);
        info.removeWrongQuestion(type, questionId);
        return saveBankInfo(info);
    }

    // Add generated paper (sequential)
    public boolean addGeneratedPaper(String bankId) {
        BankInfo info = getBankInfo(bankId);
        info.addGeneratedPaper();
        return saveBankInfo(info);
    }

    // Remove generated paper
    public boolean removeGeneratedPaper(String bankId, String paperId) {
        BankInfo info = getBankInfo(bankId);
        info.removeGeneratedPaper(paperId);
        return saveBankInfo(info);
    }
}