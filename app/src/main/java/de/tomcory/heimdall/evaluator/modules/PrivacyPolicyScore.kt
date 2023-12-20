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
import de.tomcory.heimdall.persistence.database.entity.Report
import de.tomcory.heimdall.persistence.database.entity.SubReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import org.json.JSONObject
import timber.log.Timber
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.time.LocalDateTime
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.select.Elements
import java.io.InputStream
import java.io.InputStreamReader
import java.util.Locale

class PrivacyPolicyScore : Module() {
    override val name: String= "PrivacyPolicyScore"
    val label = "Privacy Policy"
//    var successCounter:Int=0
//    var failureCounter:Int=0
//    var failureDump:String=""

    override suspend fun calculateOrLoad(app: App, context: Context, forceRecalculate: Boolean): Result<ModuleResult> {

        val policyTextInfo = getPolicyText(context, app)
        val policyTrackerInfo= appMentionsTrackersInPolicy(context,app, policyTextInfo.policyText)
        val score = if (policyTrackerInfo.allTrackers.size==0  || policyTrackerInfo.fullyMentionedTrackers.size ==0) 0f
            else policyTrackerInfo.fullyMentionedTrackers.size.toFloat()/policyTrackerInfo.allTrackers.size


        val policyDetails = buildJsonObject {
            put("packageName", policyTextInfo.packageName)
            put("playStoreLink", policyTextInfo.playStoreLink)
            put("policyLink", policyTextInfo.policyLink ?: "")
            put("errorMessage", policyTextInfo.errorMessage ?: "")

            if (!policyTextInfo.policyText.equals("no policy text")) put("hasPolicyText", true)
            else put("hasPolicyText", false)

            if (policyTrackerInfo.allTrackers.isNotEmpty()) put("allTrackers", JsonArray(policyTrackerInfo.allTrackers.map { JsonPrimitive(it) }))
            else put("allTrackers", JsonArray(emptyList()))

            if (policyTrackerInfo.fullyMentionedTrackers.isNotEmpty()) put("fullyMentionedTrackers", JsonArray(policyTrackerInfo.fullyMentionedTrackers.map { JsonPrimitive(it) }))
            else put("fullyMentionedTrackers", JsonArray(emptyList()))

            if (policyTrackerInfo.partiallyMentionedTrackers.isNotEmpty()) put("partiallyMentionedTrackers", JsonArray(policyTrackerInfo.partiallyMentionedTrackers.map { JsonPrimitive(it) }))
            else put("partiallyMentionedTrackers", JsonArray(emptyList()))

            if (policyTrackerInfo.mentionedParents.isNotEmpty()) put("mentionedParents", JsonArray(policyTrackerInfo.mentionedParents.map { JsonPrimitive(it) }))
            else put("mentionedParents", JsonArray(emptyList()))
        }

        return Result.success(ModuleResult(this.name, score, additionalDetails = policyDetails.toString()))
    }

    suspend fun getPolicyText(context: Context, app: App): PolicyTextInfo {

        val packageName=app.packageName
        val playStoreUrl = "https://play.google.com/store/apps/details?id=$packageName"
        try {
            val doc: Document = Jsoup.connect(playStoreUrl).get()
            val privacyPolicyLinkElement: Element = doc.select("a[aria-label*=Privacy Policy]").first()
                    ?: throw Exception("no link element")
            val privacyPolicyUrl = privacyPolicyLinkElement.absUrl("href")

            val client = OkHttpClient()
            val request = Request.Builder().url(privacyPolicyUrl).header("Accept-Language", "en-US").build()
            val response = client.newCall(request).execute()


            if (response.isSuccessful) {
                val html = response.body?.string() ?: ""
                val htmlOKHTTP= Jsoup.parse(html).select("body > *:not(header):not(footer):not(nav)")
                val contentText= htmlOKHTTP.text()

                return PolicyTextInfo(
                        packageName = packageName,
                        playStoreLink = playStoreUrl,
                        policyLink = privacyPolicyUrl,
                        errorMessage = null,
                        policyText = contentText
                )


            }else {
                return PolicyTextInfo(
                        packageName = packageName,
                        playStoreLink = playStoreUrl,
                        policyLink = privacyPolicyUrl,
                        errorMessage = "Error connecting to policy link",
                        policyText = "no policy text"
                )
            }



//            val document: Document = Jsoup.connect(privacyPolicyUrl)
//                    .header("Accept-Language", "en")
//                    .get()
//
//            val filteredElements = document.select("body > *:not(header):not(footer):not(nav)")
//            val contentText: String = filteredElements.text()


        }
        catch (e: Exception) {
//            failureCounter++
//            failureDump+="${packageName} " + e.message+"\n"

            return PolicyTextInfo(
                    packageName = packageName,
                    playStoreLink = playStoreUrl,
                    policyLink = null,
                    errorMessage = e.message,
                    policyText = "no policy text"
            )

        }

    }


    fun checkCombinations(trackerName: String, text: String): List<String> {
        val found:MutableList<String> = mutableListOf<String>()

        for (combination in getCombinations(trackerName))
            if (text.contains(Regex("\\b${Regex.escape(combination)}\\b", RegexOption.IGNORE_CASE))) {
                found.add(combination)
            }

        return found.filter { tracker ->
            found.none { otherTracker ->
                otherTracker.contains(tracker) && otherTracker != tracker
            }
        }.toList()
    }

    fun getCombinations(trackerName:String): List<String>{
        val blackList: List<String> = listOf("analytics", "login", "ads", "share", "mobile", "services", "and" , "tag", "manager"
                , "location", "advertisement", "ad", "app", "center", "open", "marketing", "measurement")

        val words = trackerName.split(" ")
        val combinations = mutableListOf<String>()

        // all single words + combinations of all adjacent words (not limited to 2)
        for (i in words.indices) {
            for (j in i + 1 until words.size + 1) {
                var toAdd=words.subList(i, j).joinToString(" ")
                if (!blackList.contains(toAdd.lowercase(Locale.ROOT))){
                    combinations.add(toAdd)
                }
            }
        }

        //Timber.e("!!!!!!!COMB: " + combinations)
        return combinations
    }

    suspend fun appMentionsTrackersInPolicy(context: Context, app:App, text:String): PolicyTrackerInfo {
        val trackers= HeimdallDatabase.instance?.appXTrackerDao?.getAppWithTrackers(app.packageName)?.trackers ?: listOf()
        //val trackers = HeimdallDatabase.instance?.appDao?.getAppWithTrackersFromPackageName(app.packageName)?.trackers
        val trackerNames= trackers.map { t->t.name }
        //if no policy dont continue
        if (text.equals("no policy text")) return PolicyTrackerInfo(
                trackerNames,
                listOf(),
                listOf(),
                listOf()
        )
        val listYes=mutableListOf<String>()
        val listNo= mutableListOf<String>()
        var listCombinations= mutableListOf<String>()
        var listParent= mutableListOf<String>()
        var mentionedTrackers=0


        var firstWord=""
        if (trackers.isNotEmpty()) {
            var fullMention=false
            var partMentioned=false
            var parentMentioned=false
            for (tracker in trackers){
                val pattern = Regex("\\b${Regex.escape(tracker.name)}\\b", RegexOption.IGNORE_CASE)

                //tracker name mentioned by full name
                if (text.contains(pattern)){
                    listYes.add(tracker.name)
                    mentionedTrackers++
                    fullMention=true
                }else{
                    val combis=checkCombinations(tracker.name, text)
                    if (combis.isNotEmpty())
                        partMentioned=true
                    listCombinations.addAll(combis)
                }

                if (!fullMention && !partMentioned){
                    for (combi in getCombinations(tracker.name)){
                        var resultOneCombi= findInXrayJSON(combi,context)
                        if (resultOneCombi.isNotEmpty()) {
                            for (knownParent in resultOneCombi){
                                if (text.contains(Regex("\\b${Regex.escape(knownParent)}\\b", RegexOption.IGNORE_CASE))){
                                    listParent.add(knownParent)
                                    parentMentioned=true
                                }
                            }
                            Timber.e(combi+" | ${resultOneCombi}")
                        }
                    }
                }

                if(!fullMention && !partMentioned && !parentMentioned) listNo.add(tracker.name)
            }
        }
        listCombinations= listCombinations.distinct().toMutableList()
        listParent= listParent.distinct().toMutableList()

        var totalTrackers= trackers.size

        //todo score move to calculateOrLoad
        //here just for my file
        var shareOfMentionedTrackers:Float=0f
        var shortened="0"
        if(mentionedTrackers!=0 && totalTrackers!=0 ){
            shareOfMentionedTrackers= (mentionedTrackers.toFloat() / totalTrackers)
            shortened= String.format("%.2f", shareOfMentionedTrackers)
        }

        //write info to file for testing purposes
//        try {
//            val fileOutputStream: FileOutputStream = context.openFileOutput("appWithTrackers.txt", Context.MODE_APPEND)
//            val outputStreamWriter = OutputStreamWriter(fileOutputStream)
//            outputStreamWriter.write("\n"+LocalDateTime.now().toString()+" on "+ Build.MODEL+"\n")
//            outputStreamWriter.write((++successCounter).toString() +"+--------"+app.label+" | "+app.packageName+"\n")
//            outputStreamWriter.write("${app.packageName} mentions "+shortened+ "% (${mentionedTrackers}) "+ "of ${totalTrackers} trackers in policy"+"\n")
//            outputStreamWriter.write("all TRACKERS:" + trackerNames+"\n")
//            outputStreamWriter.write("MENTIONS NAME OF TRACKERS:" + listYes.toString()+"\n")
//            outputStreamWriter.write("NOT MENTIONS NAME OF TRACKERS:" + listNo.toString()+"\n")
//            outputStreamWriter.write("MENTIONS COMBINATIONS OF TRACKER NAME(${listCombinations.size}):" + listCombinations.toString()+"\n")
//            outputStreamWriter.write("XRAY PARENT SEARCH(${listParent.size}):" + listParent.toString()+"\n")
//            outputStreamWriter.write("${failureCounter} FAILURES:" + failureDump +"\n")
//            outputStreamWriter.close()
//
//            val fileOutputStream1: FileOutputStream = context.openFileOutput("policy.txt", Context.MODE_APPEND)
//            val outputStreamWriter1 = OutputStreamWriter(fileOutputStream1)
//            outputStreamWriter1.write("\n"+LocalDateTime.now().toString()+" on "+ Build.MODEL+"\n")
//            outputStreamWriter1.write("--------"+app.label+" | "+app.packageName+"\n")
//            var yes= findSentence(listYes,text)
//            outputStreamWriter1.write("MENTIONS NAME OF TRACKERS: "+ (if (yes.isEmpty()) "none" else "\n"+yes) +"\n")
//            var c= findSentence(listCombinations,text)
//            outputStreamWriter1.write("MENTIONS COMBINATIONS:"+(if (c.isEmpty()) "none" else "\n"+c)+"\n")
//            var p=findSentence(listParent,text)
//            outputStreamWriter1.write("MENTIONS PARENT:"+(if (p.isEmpty()) "none" else "\n"+p)+"\n")
//            outputStreamWriter1.close()
//
//        } catch (e: Exception) {
//            Timber.e("something wrong with writing to file in Policy: ${e.message}")
//        }

        return PolicyTrackerInfo(
                trackerNames,
                listYes.toList(),
                listCombinations.toList(),
                listParent.toList()
        )
    }


    fun loadSubReportFromDB(report: Report): FullPolicyInfo {
        val rep= HeimdallDatabase.instance?.subReportDao?.getSubReportsByPackageNameAndModule(
                report.appPackageName,
                name
        )
        var obj: FullPolicyInfo
        if (rep!=null){
            obj= Json.decodeFromString<FullPolicyInfo>(rep.additionalDetails)
            return obj
        }else {
            return FullPolicyInfo("error", "error","error", "error", false, listOf(), listOf(), listOf(), listOf())
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

        var subreport: FullPolicyInfo by remember { mutableStateOf( FullPolicyInfo("error", "error","error", "error", false, listOf(), listOf(), listOf(), listOf())) }
        var loadingSubReport by remember { mutableStateOf(true) }

        val score = if (subreport.allTrackers.size==0  || subreport.fullyMentionedTrackers.size ==0) 0f
        else subreport.fullyMentionedTrackers.size.toFloat()/subreport.allTrackers.size

        val percent= if (subreport.allTrackers.size==0  || subreport.fullyMentionedTrackers.size ==0) 0f
            else subreport.fullyMentionedTrackers.size.toFloat()/ subreport.allTrackers.size
        val percent1= if (subreport.allTrackers.size==0  || subreport.partiallyMentionedTrackers.size ==0) 0f
            else subreport.partiallyMentionedTrackers.size.toFloat()/subreport.allTrackers.size
        val percent2= if (subreport.allTrackers.size==0  || subreport.mentionedParents.size ==0) 0f
            else subreport.mentionedParents.size.toFloat()/subreport.allTrackers.size

        //todo numbers formatting
        val scorePercent=if ((percent*100)>=10) (percent*100).toString().substring(0,2) else (percent*100).toString().substring(0,1)   //if ((percent*100)<10)  ((percent*100).toString().substring(0,3) + "%") else  ((percent*100).toString().substring(0,4) + "%")
        val scorePercent1=if ((percent1*100)>=10) (percent1*100).toString().substring(0,2) else (percent1*100).toString().substring(0,1)  //if ((percent1*100)<10)  ((percent1*100).toString().substring(0,3) + "%") else  ((percent1*100).toString().substring(0,4) + "%")
        val scorePercent2= if ((percent2*100)>=10) (percent2*100).toString().substring(0,2) else (percent2*100).toString().substring(0,1)  //if ((percent2*100)<10)  ((percent2*100).toString().substring(0,3) + "%") else  ((percent2*100).toString().substring(0,4) + "%")


        LaunchedEffect(key1 = 2, block = {
            this.launch(Dispatchers.IO) {
                subreport = report?.let { loadSubReportFromDB(it) }!! //todo ??
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

                if (subreport.hasPolicyText){

                    if (subreport.fullyMentionedTrackers.isNotEmpty()){
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Following trackers are mentioned by full name in the privacy policy ($scorePercent% of all trackers): "
                                + subreport.fullyMentionedTrackers.toString().substring(1,subreport.fullyMentionedTrackers.toString().length-1))
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (subreport.partiallyMentionedTrackers.isNotEmpty()){
                        Text("Following combinations of the tracker's full name are mentioned in the policy:\n"
                                + subreport.partiallyMentionedTrackers.toString().substring(1,subreport.partiallyMentionedTrackers.toString().length-1))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (subreport.mentionedParents.isNotEmpty()){
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Following combinations of the tracker's full name are mentioned in the policy:\n"
                                + subreport.mentionedParents.toString().substring(1,subreport.mentionedParents.toString().length-1))
                    }
                    if (subreport.mentionedParents.isEmpty() && subreport.partiallyMentionedTrackers.isEmpty() && subreport.fullyMentionedTrackers.isEmpty()){
                        Text("This app doesn't mention any trackers in its policy, neither by full name nor by the name of its parent entity")
                    }
                }
                else{
                    Text("Privacy policy could not be extracted from Google Play store")
                }

            }

        }


    }

    override fun exportToJsonObject(subReport: SubReport?): JsonObject {
        if (subReport == null) return buildJsonObject {
            put(name, JSONObject.NULL as JsonElement)
        }
        val policyInfo: FullPolicyInfo = Json.decodeFromString<FullPolicyInfo>(subReport.additionalDetails)
        val policyInfoJson = Json.encodeToJsonElement(policyInfo).jsonObject

        var serializedJsonObject: JsonObject = Json.encodeToJsonElement(subReport).jsonObject
        serializedJsonObject = JsonObject(serializedJsonObject.minus("additionalDetails"))
        val additionalPair = Pair("additionalDetails", policyInfoJson)
        return JsonObject(serializedJsonObject.plus(additionalPair))
    }

    override fun exportToJson(subReport: SubReport?): String {
        return exportToJsonObject(subReport).toString()
    }

    fun findInXrayJSON(name: String, context: Context): MutableList<String> {
        var entries: List<Entry> = listOf<Entry>()
        val pN= context.packageName
        try {
            val resourceId = context.resources.getIdentifier("parent_subsidiary", "raw", pN)
            val inputStream: InputStream = context.resources.openRawResource(resourceId)
            val reader = InputStreamReader(inputStream)
            val gson = Gson()

            val result: MutableList<String> = mutableListOf<String>()
            val entryListType = object : TypeToken<List<Entry>>() {}.type
            entries = gson.fromJson(reader, entryListType)

            val current = entries.filter { e -> e.owner_name!=null && e.owner_name.uppercase(Locale.ROOT).equals(name.uppercase(Locale.ROOT)) }

            if (current.isNotEmpty()) {
                current[0].parent?.let { result.add(it) }
                current[0].root_parent?.let { result.add(it) }
                //if (result.isNotEmpty()) Timber.e("found parent for ${name} : $result")
                return result
            } else return mutableListOf()


        }catch (e: Exception){
            Timber.e(e.message)
        }

        return mutableListOf()

    }

    fun findSentence(names: List<String>, text: String): List<String> {
        val sentences = text.split(Regex("[.!?]"))
        val result = mutableListOf<String>()
        for (name in names){
            for (sentence in sentences) {
                if (sentence.contains(Regex("\\b${Regex.escape(name)}\\b", RegexOption.IGNORE_CASE))) {
                    result.add(name+" : " +sentence.trim()+"\n")
                }
            }
        }


        return result.toList()
    }
}



@Serializable
data class Entry(
        val country: String,
        val doms: List<String>,
        val owner_name: String,
        val root_parent: String?,
        val parent: String?
)

@Serializable
data class PolicyTextInfo(
        val packageName: String,
        val playStoreLink: String,
        val policyLink: String?,
        val errorMessage: String?,
        val policyText: String
)

@Serializable
data class PolicyTrackerInfo(
        val allTrackers: List<String>,
        val fullyMentionedTrackers: List<String>,
        val partiallyMentionedTrackers: List<String>,
        val mentionedParents: List<String>
)

@Serializable
data class FullPolicyInfo(
        val packageName: String,
        val playStoreLink: String,
        val policyLink: String,
        val errorMessage: String,
        val hasPolicyText: Boolean,
        val allTrackers: List<String>,
        val fullyMentionedTrackers: List<String>,
        val partiallyMentionedTrackers: List<String>,
        val mentionedParents: List<String>
)
