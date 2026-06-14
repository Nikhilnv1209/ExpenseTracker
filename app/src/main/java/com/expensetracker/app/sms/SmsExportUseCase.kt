package com.expensetracker.app.sms

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsExportUseCase @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val smsReader: SmsReader,
) {

    suspend fun exportBankSmsToFile(): Uri? {
        val messages = smsReader.readAllBankSms(limit = 1000)
        if (messages.isEmpty()) return null

        val jsonArray = JSONArray()
        messages.forEach { sms ->
            jsonArray.put(
                JSONObject().apply {
                    put("sender", sms.address)
                    put("body", sms.body)
                    put("date", sms.date)
                },
            )
        }

        val exportDir = File(context.getExternalFilesDir(null), "exports").apply {
            if (!exists()) mkdirs()
        }
        val exportFile = File(exportDir, "bank_sms_export_${System.currentTimeMillis()}.json")
        exportFile.writeText(jsonArray.toString(2))

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            exportFile,
        )
    }
}
