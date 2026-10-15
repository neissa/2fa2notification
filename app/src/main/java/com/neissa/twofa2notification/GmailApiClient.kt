package com.neissa.twofa2notification

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Base64
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Tasks
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Lit les mails récents via l'API Gmail (lecture seule), sans ouvrir Gmail.
 * La requête `newer_than:10m` ne renvoie que les mails reçus dans les 10 dernières minutes.
 * [checkRecentMails] doit être appelée hors du thread principal.
 */
object GmailApiClient {
    const val GMAIL_SCOPE = "https://www.googleapis.com/auth/gmail.readonly"
    private const val MESSAGES_URL = "https://gmail.googleapis.com/gmail/v1/users/me/messages"
    private const val PREFS_NAME = "2fa_prefs"
    private const val KEY_AUTHORIZED = "gmail_authorized"

    fun isAuthorized(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_AUTHORIZED, false)

    /** Lance l'autorisation Google (une seule fois). Le résultat arrive dans onActivityResult. */
    fun authorize(activity: Activity, requestCode: Int) {
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(GMAIL_SCOPE)))
            .build()
        Identity.getAuthorizationClient(activity).authorize(request)
            .addOnSuccessListener { result ->
                val sender = result.pendingIntent?.intentSender
                if (result.hasResolution() && sender != null) {
                    try {
                        activity.startIntentSenderForResult(sender, requestCode, null, 0, 0, 0)
                    } catch (e: Exception) {
                        DebugLogger.log(activity, "Gmail API", "Impossible d'ouvrir l'autorisation: ${e.message}")
                    }
                } else {
                    setAuthorized(activity, true)
                    DebugLogger.log(activity, "Gmail API", "Gmail déjà autorisé (lecture seule)")
                }
            }
            .addOnFailureListener { e ->
                DebugLogger.log(activity, "Gmail API", "Demande d'autorisation échouée (${e.javaClass.simpleName}): ${e.message}")
            }
    }

    /** Traite le retour de l'autorisation. Retourne true si Gmail est autorisé. */
    fun handleAuthorizationResult(context: Context, data: Intent?): Boolean {
        DebugLogger.log(context, "Gmail API", "Retour d'autorisation: data=${if (data == null) "null" else "présent"}")
        return try {
            Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(data)
            setAuthorized(context, true)
            DebugLogger.log(context, "Gmail API", "Autorisation Gmail accordée (lecture seule)")
            true
        } catch (e: ApiException) {
            setAuthorized(context, false)
            DebugLogger.log(
                context,
                "Gmail API",
                "Autorisation refusée ou échouée: code ${e.statusCode}, message: ${e.status.statusMessage ?: e.message}",
            )
            false
        }
    }

    fun checkRecentMails(context: Context) {
        if (!isAuthorized(context)) {
            DebugLogger.log(context, "Gmail API", "Non autorisé: appuyez sur « Se connecter à Gmail »")
            return
        }

        val token = try {
            val request = AuthorizationRequest.builder()
                .setRequestedScopes(listOf(Scope(GMAIL_SCOPE)))
                .build()
            val result = Tasks.await(Identity.getAuthorizationClient(context).authorize(request))
            if (result.hasResolution()) {
                setAuthorized(context, false)
                DebugLogger.log(context, "Gmail API", "Autorisation à renouveler: ouvrez l'app et appuyez sur « Se connecter à Gmail »")
                return
            }
            result.accessToken ?: return
        } catch (e: Exception) {
            DebugLogger.log(context, "Gmail API", "Jeton Google indisponible: ${e.message}")
            return
        }

        // Exclut explicitement la corbeille et les spams
        val query = URLEncoder.encode("newer_than:10m -in:trash -in:spam", "UTF-8")
        val list = getJson(context, "$MESSAGES_URL?q=$query&maxResults=10", token) ?: return
        val ids = list.optJSONArray("messages")
        if (ids == null) {
            DebugLogger.log(context, "Gmail API", "Aucun mail reçu dans les 10 dernières minutes")
            return
        }
        DebugLogger.log(context, "Gmail API", "${ids.length()} mail(s) reçu(s) dans les 10 dernières minutes")

        val mails = mutableListOf<Triple<Long, String, String>>()
        for (i in 0 until ids.length()) {
            val id = ids.getJSONObject(i).getString("id")
            val msg = getJson(context, "$MESSAGES_URL/$id?format=full", token) ?: continue
            val received = msg.optString("internalDate").toLongOrNull() ?: continue
            mails += Triple(received, senderOf(msg), buildText(msg))
        }

        // Du plus ancien au plus récent : le plus récent code gagne
        mails.sortedBy { it.first }.forEach { (time, sender, text) ->
            CodeProcessor.process(context, "Gmail API", text, time, sender)
        }
    }

    private fun setAuthorized(context: Context, value: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_AUTHORIZED, value)
            .apply()
    }

    private fun getJson(context: Context, url: String, token: String): JSONObject? {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            setRequestProperty("Authorization", "Bearer $token")
            connectTimeout = 10_000
            readTimeout = 10_000
        }
        return try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code in 200..299) {
                JSONObject(body)
            } else {
                DebugLogger.log(context, "Gmail API", "Erreur HTTP $code: ${body.take(200)}")
                null
            }
        } catch (e: Exception) {
            DebugLogger.log(context, "Gmail API", "Erreur réseau: ${e.message}")
            null
        } finally {
            conn.disconnect()
        }
    }

    /** Nom de l'expéditeur (ou adresse si pas de nom) d'après l'en-tête From. */
    private fun senderOf(msg: JSONObject): String {
        val headers = msg.optJSONObject("payload")?.optJSONArray("headers") ?: return "inconnu"
        for (i in 0 until headers.length()) {
            val header = headers.getJSONObject(i)
            if (header.optString("name").equals("From", ignoreCase = true)) {
                val from = header.optString("value")
                val name = from.substringBefore('<').trim().trim('"')
                return name.ifEmpty { from.substringAfter('<', from).trim('>', ' ') }.ifEmpty { "inconnu" }
            }
        }
        return "inconnu"
    }

    /** Sujet + extrait + parties texte brut du mail. */
    private fun buildText(msg: JSONObject): String {
        val sb = StringBuilder()
        val payload = msg.optJSONObject("payload")
        payload?.optJSONArray("headers")?.let { headers ->
            for (i in 0 until headers.length()) {
                val header = headers.getJSONObject(i)
                if (header.optString("name").equals("Subject", ignoreCase = true)) {
                    sb.append(header.optString("value")).append("\n")
                }
            }
        }
        sb.append(msg.optString("snippet")).append("\n")
        payload?.let { collectPlainText(it, sb) }
        return sb.toString()
    }

    private fun collectPlainText(part: JSONObject, sb: StringBuilder) {
        val data = part.optJSONObject("body")?.optString("data").orEmpty()
        if (part.optString("mimeType") == "text/plain" && data.isNotEmpty()) {
            val decoded = Base64.decode(data, Base64.URL_SAFE or Base64.NO_WRAP)
            sb.append(String(decoded, Charsets.UTF_8)).append("\n")
        }
        part.optJSONArray("parts")?.let { parts ->
            for (i in 0 until parts.length()) collectPlainText(parts.getJSONObject(i), sb)
        }
    }
}
