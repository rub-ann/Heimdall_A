package de.tomcory.heimdall.evaluator.modules

import android.content.Context
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import de.tomcory.heimdall.evaluator.ModuleResult
import de.tomcory.heimdall.persistence.database.HeimdallDatabase
import de.tomcory.heimdall.persistence.database.entity.App
import de.tomcory.heimdall.persistence.database.entity.AppXTracker
import de.tomcory.heimdall.persistence.database.entity.Report
import de.tomcory.heimdall.persistence.database.entity.SubReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.HttpStatusException
import timber.log.Timber
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.time.LocalDateTime

class PrivacyPolicyScore : Module() {
    override val name: String= "PrivacyPolicyScore"
    val label = "Privacy Policy"
    var successCounter:Int=0
    var failureCounter:Int=0
    var failureDump:String=""
    var listYes= mutableListOf<String>()
    var listNo= mutableListOf<String>()
    var listFirstWord= mutableListOf<String>()
    var score:Float=0f


    override suspend fun calculateOrLoad(app: App, context: Context, forceRecalculate: Boolean): Result<ModuleResult> {

        val text = getPolicyText(context, app)
        val score= appMentionsTrackersInPolicy(context,app, text)

        val jsonDetails = buildJsonObject {
            put("score",score)

            if (listYes.isNotEmpty()) put("mentionedTrackers", JsonArray(listYes.map { JsonPrimitive(it) }))
            else put("mentionedTrackers", JsonArray(emptyList()))

            if (listFirstWord.isNotEmpty()) put("mentionedEntities", JsonArray(listFirstWord.map { JsonPrimitive(it) }))
            else  put("mentionedEntities", JsonArray(emptyList()))
        }

        return Result.success(ModuleResult(this.name, score, additionalDetails = jsonDetails.toString()))
    }

    suspend fun getPolicyText(context: Context, app: App):String {

        val packageName=app.packageName
        val playStoreUrl = "https://play.google.com/store/apps/details?id=$packageName"
        try {
            val doc: Document = Jsoup.connect(playStoreUrl).get()
            val privacyPolicyLink: Element? = doc.select("a[aria-label*=Privacy Policy]").first()
            if (privacyPolicyLink != null) {
                val privacyPolicyUrl = privacyPolicyLink.absUrl("href")
                ////to get EN language
                val client = OkHttpClient()
                val request = Request.Builder().url(privacyPolicyUrl).header("Accept-Language", "en-US").build()
                val response = client.newCall(request).execute()
                var textContent: String

                if (response.isSuccessful) {
                    val html = response.body?.string() ?: ""
                    textContent = Jsoup.parse(html).text()
                    //Timber.i("policy for $name | $packageName "+privacyPolicyText)
                   // writePolicyTextToFile(context, "privacy_policy.txt", packageInfo, textContent)
                    return textContent

                }else Timber.e("HTTP request was not successful")
            } else {
                Timber.e ("Policy link not found for $packageName.")
            }
        }
        catch (e: Exception) {
            failureCounter++
            failureDump+="${packageName} " + e.message
            if(e is HttpStatusException){
                if (e.statusCode == 404) {
                    Timber.e ("Privacy policy for $packageName not found. The page does not exist (404).")
                } else {
                    Timber.e ("Failed to extract privacy policy for $packageName. HTTP Status: ${e.statusCode}. Message: ${e.message}")
                }
            }
            else Timber.e( "Failed to extract privacy policy for $packageName. Message: ${e.message}")
        }
        return "no policy text"
    }



    suspend fun appMentionsTrackersInPolicy(context: Context, app:App, text:String): Float{
        //val trackers = HeimdallDatabase.instance?.appDao?.getAppWithTrackersFromPackageName(app.packageName)?.trackers
        val trackers= HeimdallDatabase.instance?.appXTrackerDao?.getAppWithTrackers(app.packageName)?.trackers ?: listOf()
        listYes=mutableListOf<String>()
        listNo= mutableListOf<String>()
        listFirstWord= mutableListOf<String>()

        var firstWord=""
        if (trackers.isNotEmpty()) {
            for (tracker in trackers){
                firstWord=tracker.name.split(" ")[0]
                if (text.contains(tracker.name)){
                    listYes.add(tracker.name)
                    val appXTracker = AppXTracker(app.packageName, tracker.id, true)
                    HeimdallDatabase.instance?.appXTrackerDao?.updateMentions(appXTracker)
                }
                else if (text.contains(firstWord)) {
                    listNo.add(tracker.name)
                    listFirstWord.add(firstWord)
                }
                else  listNo.add(tracker.name)
            }
        }
        listFirstWord= listFirstWord.distinct().toMutableList()

        var totalTrackers= HeimdallDatabase.instance?.appXTrackerDao?.getAppWithTrackers(app.packageName)?.trackers?.size ?: 0
        //val totalTrackers= HeimdallDatabase.instance?.appDao?.getAppWithTrackersFromPackageName(app.packageName)?.trackers?.size
        var mentionedTrackers= HeimdallDatabase.instance?.appXTrackerDao?.getMentionedTrackersCount(app.packageName)

        if (trackers.isNotEmpty()) {
            Timber.e("---------${app.packageName} has ${totalTrackers} trackers: "+ trackers.map { tracker -> tracker.name })
        }


        var shareOfMentionedTrackers:Float
        var shortened="0"
        if(mentionedTrackers!=null && totalTrackers!=null && totalTrackers!=0 ){
            shareOfMentionedTrackers= (mentionedTrackers.toFloat() / totalTrackers)
            shortened= String.format("%.2f", shareOfMentionedTrackers)
        }else {
            mentionedTrackers=0
            totalTrackers=0
            shareOfMentionedTrackers=0f
        }

        //Timber.e("${packageInfo.packageName} mentions ${percentOfMentionedTrackers.toString().substring(4)} of ${totalTrackers} trackers in policy")

        //write info to file for monitoring purposes
        try {
            val fileOutputStream: FileOutputStream = context.openFileOutput("appWithTrackers.txt", Context.MODE_APPEND)
            val outputStreamWriter = OutputStreamWriter(fileOutputStream)
            outputStreamWriter.write("\n")
            outputStreamWriter.write(LocalDateTime.now().toString()+" on "+ Build.MODEL)
            outputStreamWriter.write("\n")
            outputStreamWriter.write((++successCounter).toString() +"+--------"+app.label+" | "+app.packageName)
            outputStreamWriter.write("\n")
            outputStreamWriter.write("${app.packageName} mentions "+shortened+ "% (${mentionedTrackers})"+ "of ${totalTrackers} trackers in policy")
            outputStreamWriter.write("\n")
            outputStreamWriter.write("MENTIONS NAME OF TRACKERS:" + listYes.toString())
            outputStreamWriter.write("\n")
            outputStreamWriter.write("NOT MENTIONS NAME OF TRACKERS:" + listNo.toString())
            outputStreamWriter.write("\n")
            outputStreamWriter.write("MENTIONS FIRST WORDS OF TRACKER NAME:" + listFirstWord.toString())
            outputStreamWriter.write("\n")
            outputStreamWriter.write("${failureCounter} FAILURES:" + failureDump)
            outputStreamWriter.write("\n")
            outputStreamWriter.close()
        } catch (e: Exception) {
            Timber.e("something wrong with writing to file in Policy")
        }
        score= shareOfMentionedTrackers
        return shareOfMentionedTrackers
    }

    fun loadSubReportFromDB(report: Report): PolicyInfo {
        val rep= HeimdallDatabase.instance?.subReportDao?.getSubReportsByPackageNameAndModule(
                report.appPackageName,
                name
        )
        var obj:PolicyInfo
        if (rep!=null){
            obj= Json.decodeFromString<PolicyInfo>(rep.additionalDetails)
            return obj
        }else {
            return PolicyInfo(0f, listOf("idk"), listOf("idk"))
        }

    }


    @Composable
    override fun BuildUICard(report: Report?) {
        super.UICard(
                title = this.label,
                infoText = "something with policies"
        ) {
            val context = LocalContext.current
            PrivacyPolicyUICard(report = report, context)
        }
    }

    @Composable
    fun PrivacyPolicyUICard(report: Report?, context: Context) {
        var subreport: PolicyInfo by remember { mutableStateOf(PolicyInfo(0f, listOf<String>("idk"), listOf<String>("idk"))) }
        var loadingSubReport by remember { mutableStateOf(true) }

        val scorePercent= if ((subreport.score*100)<10)  ((subreport.score*100).toString().substring(0,3) + "%") else  ((subreport.score*100).toString().substring(0,4) + "%")

        LaunchedEffect(key1 = 2, block = {
            this.launch(Dispatchers.IO) {
                subreport = report?.let { loadSubReportFromDB(it) }!! //todo null
                loadingSubReport = false
            }
        })

        AnimatedVisibility(visible = loadingSubReport, enter = fadeIn(), exit = fadeOut()) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        AnimatedVisibility(
                visible = !loadingSubReport,
                enter = slideInVertically(),
                exit = slideOutVertically()
        ) {
            Column {
                Text("App mentions " +scorePercent+" of found trackers in its policy")

                if (subreport.mentionedTrackers.isNotEmpty()){
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Specifically " + subreport.mentionedTrackers.toString().substring(1,subreport.mentionedTrackers.toString().length-1))
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (subreport.mentionedEntities.isNotEmpty()){
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Additionally, following entities are mentioned in the policy: " + subreport.mentionedEntities.toString().substring(1,subreport.mentionedEntities.toString().length-1))
                }

            }

        }


    }

    override fun exportToJsonObject(subReport: SubReport?): JsonObject {
        return buildJsonObject {
            put("score", score)
        }
    }

    override fun exportToJson(subReport: SubReport?): String {
        return "{\"score\": ${score}}"
    }
}

@Serializable
data class PolicyInfo(
        val score: Float,
        val mentionedTrackers: List<String>,
        val mentionedEntities: List<String>
)

