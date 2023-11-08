package de.tomcory.heimdall.evaluator.modules

import android.content.Context
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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

class PrivacyPolicyScore_deprecated : Module() {
    override val name: String= "PrivacyPolicyScore_deprecated"
    val label = "Privacy Policy"

    var successCounter:Int=0
    var failureCounter:Int=0
    var failureDump:String=""
    var listYes= mutableListOf<String>()
    var listNo= mutableListOf<String>()
    var listFirstWord= mutableListOf<String>()
    var score:Float=0f


    override suspend fun calculateOrLoad(app: App, context: Context, forceRecalculate: Boolean): Result<ModuleResult> {

        val score = getPolicyTextAndAnalyze(context, app)

        // Create a JSON object with the relevant data
        val jsonDetails = buildJsonObject {
            put("score",score)

            if (listYes.isNotEmpty()) {
                put("mentionedTrackers", JsonArray(listYes.map { JsonPrimitive(it) }))
            } else {
                put("mentionedTrackers", JsonArray(emptyList())) // Represent as an empty JSON array
            }

            if (listFirstWord.isNotEmpty()) {
                put("mentionedEntities", JsonArray(listFirstWord.map { JsonPrimitive(it) }))
            } else {
                put("mentionedEntities", JsonArray(emptyList())) // Represent as an empty JSON array
            }


        }


        return Result.success(ModuleResult(this.name, score, additionalDetails = jsonDetails.toString()))
//        listYes=mutableListOf<String>()
//        listNo= mutableListOf<String>()
//        listFirstWord= mutableListOf<String>()
//        score=0f
//        return Result.success(ModuleResult(this.name, getPolicyTextAndAnalyze(context,app), additionalDetails = "testtest"))
    }

    suspend fun getPolicyTextAndAnalyze(context: Context, app: App):Float {

        val packageName=app.packageName
        val playStoreUrl = "https://play.google.com/store/apps/details?id=$packageName"
        try {
            val doc: Document = Jsoup.connect(playStoreUrl).get()
            //val privacyPolicyLink: Element? = doc.select("a:contains(Privacy Policy)").first()
            //val privacyPolicyLink: Element? = doc.select("a.Si6A0c.RrSxVb[area-label*=Privacy Policy]").first()
            val privacyPolicyLink: Element? = doc.select("a[aria-label*=Privacy Policy]").first()
            if (privacyPolicyLink != null) {
                val privacyPolicyUrl = privacyPolicyLink.absUrl("href")
                ////to get EN language
                val client = OkHttpClient()
                val request = Request.Builder().url(privacyPolicyUrl).header("Accept-Language", "en-US").build()
                val response = client.newCall(request).execute()
                var textContent: String=""

                if (response.isSuccessful) {
                    val html = response.body?.string() ?: ""
                    textContent = Jsoup.parse(html).text()
                    //Timber.i("policy for $name | $packageName "+privacyPolicyText)
                   // writePolicyTextToFile(context, "privacy_policy.txt", packageInfo, textContent)
                    return appMentionsTrackersInPolicy(context,app,textContent)

                }else Timber.e("okhttp was not successful")

//                    val privacyPolicyDoc: Document = Jsoup.connect(privacyPolicyUrl).get()
//                    val privacyPolicyText = privacyPolicyDoc.text()
//                    //Timber.i("policy for $name | $packageName "+privacyPolicyText)
//                    writeTextToFile(context, "privacy_policy.txt", packageInfo, privacyPolicyText)
//                    appMentionsTrackersInPolicy(context,packageInfo,privacyPolicyText)

            } else {
                Timber.e ("element not found for $packageName.")
            }
        }catch (e: HttpStatusException) {
            //writeFailureToFile(context,packageInfo,e.message.toString())
            if (e.statusCode == 404) {
                Timber.e ("Privacy policy for $packageName not found. The page does not exist (404).")
            } else {
                Timber.e ("Failed to extract privacy policy for $packageName. HTTP Status: ${e.statusCode}. Message: ${e.message}")
            }
        }
        catch (e: Exception) {
            //writeFailureToFile(context,packageInfo,e.message.toString())
            e.printStackTrace()
            Timber.e( "Failed to extract privacy policy.")
        }
        return 0.0f
    }



    suspend fun appMentionsTrackersInPolicy(context: Context, app:App, text:String): Float{
        val trackers = HeimdallDatabase.instance?.appDao?.getAppWithTrackersFromPackageName(app.packageName)?.trackers
        Timber.e("---------${app.packageName} trackers: "+trackers.toString())
        listYes=mutableListOf<String>()
        listNo= mutableListOf<String>()
        listFirstWord= mutableListOf<String>()

        var firstWord=""
        if (trackers != null) {
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

        var totalTrackers= HeimdallDatabase.instance?.appDao?.getAppWithTrackersFromPackageName(app.packageName)?.trackers?.size
        var mentionedTrackers= HeimdallDatabase.instance?.appXTrackerDao?.getMentionedTrackersCount(app.packageName)
        var percentOfMentionedTrackers=111.0f
        var shortened="!"
        if(mentionedTrackers!=null && totalTrackers!=null && totalTrackers!=0 ){
            percentOfMentionedTrackers= (mentionedTrackers.toFloat() / totalTrackers)
            shortened= String.format("%.2f", percentOfMentionedTrackers)
        }else {
            mentionedTrackers=0
            totalTrackers=0
            percentOfMentionedTrackers=0f
        }

        //Timber.e("${packageInfo.packageName} mentions ${percentOfMentionedTrackers.toString().substring(4)} of ${totalTrackers} trackers in policy")


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
            outputStreamWriter.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        score= percentOfMentionedTrackers
        return percentOfMentionedTrackers.toFloat()
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

        LaunchedEffect(key1 = 2, block = {
            this.launch(Dispatchers.IO) {
                subreport = report?.let { loadSubReportFromDB(it) }!!
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
                Text("App mentions " +subreport.score.toString()+" of trackers in its policy")

                if (subreport.mentionedTrackers.isNotEmpty()){
                    Text("Specifically " + subreport.mentionedTrackers)
                }
                if (subreport.mentionedEntities.isNotEmpty()){
                    Text("Additionally, following entities are mentioned in the policy: " + subreport.mentionedEntities)
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



