package com.intent.app

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private val Bg = Color(0xFF111827)
private val CardBg = Color(0xFF1F2937)
private val Purple = Color(0xFF8B5CF6)
private val Green = Color(0xFF34D399)
private val Yellow = Color(0xFFFBBF24)
private val TextMain = Color(0xFFF9FAFB)
private val TextMuted = Color(0xFF9CA3AF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { IntentApp(this) } }
}

private data class Session(val id: Long, val app: String, val purpose: String, val minutes: Int, val elapsed: Int, val category: String, val status: String, val created: Long, val demo: Boolean = true)

private class LocalStore(context: Context) {
    private val prefs = context.getSharedPreferences("intent_local", Context.MODE_PRIVATE)
    fun read(): List<Session> {
        val raw = prefs.getString("sessions", "[]") ?: "[]"; val arr = JSONArray(raw); val out = mutableListOf<Session>()
        for (i in 0 until arr.length()) { val o=arr.getJSONObject(i); out += Session(o.optLong("id"),o.optString("app"),o.optString("purpose"),o.optInt("minutes"),o.optInt("elapsed"),o.optString("category"),o.optString("status"),o.optLong("created"),o.optBoolean("demo",true)) }; return out
    }
    fun write(list: List<Session>) { val arr=JSONArray(); list.forEach { s -> arr.put(JSONObject().put("id",s.id).put("app",s.app).put("purpose",s.purpose).put("minutes",s.minutes).put("elapsed",s.elapsed).put("category",s.category).put("status",s.status).put("created",s.created).put("demo",s.demo)) }; prefs.edit().putString("sessions",arr.toString()).apply() }
    fun getBool(k:String,d:Boolean=false)=prefs.getBoolean(k,d)
    fun setBool(k:String,v:Boolean)=prefs.edit().putBoolean(k,v).apply()
    fun seedIfEmpty() { if(read().isEmpty()) write(listOf(Session(1,"YouTube","Watch an engineering lecture",45,44,"Learning","Completed",System.currentTimeMillis()-86400000),Session(2,"Messages","Reply to important messages",10,9,"Communication","Completed",System.currentTimeMillis()-40000000),Session(3,"Instagram","Browse consciously",10,18,"Entertainment","Extended",System.currentTimeMillis()-20000000))) }
}

@Composable private fun IntentApp(context: Context) {
    val store = remember { LocalStore(context) }; var sessions by remember { mutableStateOf(store.read()) }; var screen by remember { mutableStateOf("Home") }; var showCreate by remember { mutableStateOf(false) }; var activeId by remember { mutableStateOf<Long?>(null) }; var purpose by remember { mutableStateOf("") }; var appName by remember { mutableStateOf("Other") }; var goal by remember { mutableStateOf("") }; var minutesText by remember { mutableStateOf("10") }; var category by remember { mutableStateOf("Learning") }; var intervention by remember { mutableStateOf(false) }; var monitoring by remember { mutableStateOf(store.getBool("monitoring", false)) }; var dailyGoal by remember { mutableStateOf("120") }; var demoElapsed by remember { mutableIntStateOf(0) }; var toast by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { store.seedIfEmpty(); sessions=store.read() }
    val active = sessions.firstOrNull { it.id == activeId }
    fun save(list: List<Session>) { sessions=list; store.write(list) }
    fun update(s: Session) { save(sessions.map { if(it.id==s.id) s else it }) }
    MaterialTheme(colorScheme = darkColorScheme(primary=Purple, background=Bg, surface=CardBg, onBackground=TextMain, onSurface=TextMain)) {
        Surface(Modifier.fillMaxSize(), color=Bg) { Column {
            Row(Modifier.fillMaxWidth().padding(horizontal=20.dp, vertical=16.dp), verticalAlignment=Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("INTENT", fontSize=24.sp, fontWeight=FontWeight.ExtraBold, color=TextMain); Text("YOUR ATTENTION. YOUR CHOICE.", fontSize=9.sp, letterSpacing=1.5.sp, color=TextMuted) }; Surface(color=Purple.copy(alpha=.16f), shape=RoundedCornerShape(20.dp)) { Text("DEMO MODE", Modifier.padding(horizontal=10.dp, vertical=6.dp), color=Purple, fontSize=10.sp, fontWeight=FontWeight.Bold) } }
            AnimatedContent(targetState=screen, label="screen") { target -> when(target) {
                "Home" -> HomeScreen(sessions, dailyGoal, {showCreate=true}, {screen="History"}, {screen="Insights"}, {screen="Settings"}, {screen="Lab"}, { s -> activeId=s.id; demoElapsed=s.elapsed; screen="Session" })
                "History" -> HistoryScreen(sessions, {screen="Home"}, { s -> activeId=s.id; demoElapsed=s.elapsed; screen="Session" })
                "Insights" -> InsightsScreen(sessions, {screen="Home"})
                "Lab" -> LabScreen({screen="Home"})
                "Settings" -> SettingsScreen(context, monitoring, { monitoring=it; store.setBool("monitoring",it) }, dailyGoal, {dailyGoal=it}, { save(emptyList()); toast="Local history cleared" }, {screen="Home"})
                "Session" -> if(active!=null) SessionScreen(active, demoElapsed, intervention, { demoElapsed += 5; val updated=active.copy(elapsed=demoElapsed,status=if(demoElapsed>=active.minutes) "Needs check-in" else "Active"); update(updated); if(demoElapsed>=active.minutes) intervention=true }, { update(active.copy(status="Completed",elapsed=demoElapsed)); intervention=false; activeId=null; screen="Home" }, { update(active.copy(status="Active",elapsed=demoElapsed+5,minutes=active.minutes+5)); demoElapsed+=5; intervention=false }, { update(active.copy(status="Break")); intervention=false; activeId=null; screen="Home" }, { intervention=false; toast="Choice saved"; if(active.status=="Needs check-in") update(active.copy(status="Active")) }, {screen="Home"}) else EmptyScreen("No active session", {screen="Home"})
                else -> HomeScreen(sessions, dailyGoal, {showCreate=true}, {screen="History"}, {screen="Insights"}, {screen="Settings"}, {screen="Lab"}, { s -> activeId=s.id; demoElapsed=s.elapsed; screen="Session" })
            } }
            if(screen!="Session") NavigationBar(containerColor=CardBg, contentColor=TextMuted) { listOf("Home" to Icons.Default.Home,"History" to Icons.Default.History,"Insights" to Icons.Default.ShowChart,"Lab" to Icons.Default.Science,"Settings" to Icons.Default.Settings).forEach { (name,icon) -> NavigationBarItem(selected=screen==name, onClick={screen=name}, icon={Icon(icon, contentDescription=name)}, label={Text(name,fontSize=10.sp)}, colors=NavigationBarItemDefaults.colors(selectedIconColor=Purple,selectedTextColor=Purple,indicatorColor=Purple.copy(alpha=.15f))) } }
        } }
        if(showCreate) AlertDialog(onDismissRequest={showCreate=false}, containerColor=CardBg, title={Text("Set an intention",fontWeight=FontWeight.Bold)}, text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)) { OutlinedTextField(appName,{appName=it},label={Text("App or activity")},singleLine=true); OutlinedTextField(purpose,{purpose=it},label={Text("What do you intend to do?")}); OutlinedTextField(goal,{goal=it},label={Text("Optional goal")}); OutlinedTextField(minutesText,{minutesText=it.filter(Char::isDigit)},label={Text("Planned minutes")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true); Text("Category",color=TextMuted); Row(horizontalArrangement=Arrangement.spacedBy(5.dp)) { listOf("Learning","Work","Social","Fun").forEach { FilterChip(selected=category==it,onClick={category=it},label={Text(it,fontSize=11.sp)}) } } } }, confirmButton={Button(onClick={ val mins=minutesText.toIntOrNull()?.coerceIn(1,720) ?: 10; val s=Session(System.currentTimeMillis(),appName.ifBlank{"Other"},purpose.ifBlank{"Use intentionally"},mins,0,category,"Active",System.currentTimeMillis()); save(listOf(s)+sessions); activeId=s.id; demoElapsed=0; intervention=false; showCreate=false; screen="Session" },colors=ButtonDefaults.buttonColors(containerColor=Purple)) {Text("Start session")} }, dismissButton={TextButton(onClick={showCreate=false}){Text("Cancel")}})
        if(toast.isNotBlank()) AlertDialog(onDismissRequest={toast=""},title={Text("INTENT")},text={Text(toast)},confirmButton={TextButton(onClick={toast=""}){Text("OK")}})
    }
}

@Composable private fun HomeScreen(sessions:List<Session>, goal:String, create:()->Unit, history:()->Unit, insights:()->Unit, settings:()->Unit, lab:()->Unit, open:(Session)->Unit) {
    val completed=sessions.count{it.status=="Completed"}; val overruns=sessions.count{it.elapsed>it.minutes}; val total=sessions.sumOf{it.elapsed}; LazyColumn(Modifier.fillMaxSize().padding(horizontal=18.dp), verticalArrangement=Arrangement.spacedBy(14.dp), contentPadding=PaddingValues(bottom=20.dp)) {
        item { Spacer(Modifier.height(2.dp)); Text("Your attention,
with intention.",fontSize=29.sp,lineHeight=34.sp,fontWeight=FontWeight.Bold); Text("Small choices. More meaningful time.",color=TextMuted) }
        item { Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) { MetricCard("Tracked today", "$total min", "demo + sessions", Purple, Modifier.weight(1f)); MetricCard("Completed", "$completed", "sessions", Green, Modifier.weight(1f)); MetricCard("Over plan", "$overruns", "sessions", Yellow, Modifier.weight(1f)) } }
        item { Card(colors=CardDefaults.cardColors(containerColor=CardBg),shape=RoundedCornerShape(24.dp)) { Column(Modifier.padding(18.dp)) { Row(verticalAlignment=Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Daily attention goal",color=TextMuted,fontSize=12.sp); Text("$total / $goal min",fontSize=23.sp,fontWeight=FontWeight.Bold) }; Icon(Icons.Default.TrackChanges,null,tint=Purple,modifier=Modifier.size(30.dp)) }; Spacer(Modifier.height(12.dp)); LinearProgressIndicator(progress={ (total.toFloat()/(goal.toFloatOrNull()?.coerceAtLeast(1f)?:120f)).coerceIn(0f,1f) },modifier=Modifier.fillMaxWidth().height(8.dp),color=Purple,trackColor=Bg) } } }
        item { Button(onClick=create,modifier=Modifier.fillMaxWidth().height(56.dp),shape=RoundedCornerShape(18.dp),colors=ButtonDefaults.buttonColors(containerColor=Purple)) { Icon(Icons.Default.Add,null); Spacer(Modifier.width(8.dp)); Text("Start an intention",fontWeight=FontWeight.Bold) } }
        item { Text("A gentle reminder",fontWeight=FontWeight.Bold,fontSize=18.sp); Card(colors=CardDefaults.cardColors(containerColor=Purple.copy(alpha=.12f)),shape=RoundedCornerShape(20.dp)) { Row(Modifier.padding(16.dp),verticalAlignment=Alignment.Top) { Icon(Icons.Default.AutoAwesome,null,tint=Purple); Spacer(Modifier.width(12.dp)); Column { Text("Long isn't automatically wrong.",fontWeight=FontWeight.SemiBold); Text("A focused lecture can be intentional. INTENT looks at your plan and your choices—not just the clock.",color=TextMuted,fontSize=13.sp) } } } }
        item { Row(verticalAlignment=Alignment.CenterVertically) { Text("Recent sessions",fontWeight=FontWeight.Bold,fontSize=18.sp,modifier=Modifier.weight(1f)); TextButton(onClick=history){Text("See all")} } }
        if(sessions.isEmpty()) item { EmptyCard("No sessions yet","Create your first intention to get started.") } else items(sessions.take(4)) { s -> SessionRow(s,{open(s)}) }
        item { OutlinedButton(onClick=insights,modifier=Modifier.fillMaxWidth()) { Icon(Icons.Default.ShowChart,null); Spacer(Modifier.width(8.dp)); Text("Explore insights") } }
    }
}

@Composable private fun MetricCard(title:String,value:String,sub:String,color:Color,modifier:Modifier=Modifier) { Card(modifier,colors=CardDefaults.cardColors(containerColor=CardBg),shape=RoundedCornerShape(20.dp)) { Column(Modifier.padding(12.dp)) { Text(title,color=TextMuted,fontSize=10.sp,maxLines=1); Text(value,fontSize=22.sp,fontWeight=FontWeight.Bold,color=color); Text(sub,color=TextMuted,fontSize=9.sp) } } }
@Composable private fun SessionRow(s:Session,onClick:()->Unit) { Card(onClick=onClick,colors=CardDefaults.cardColors(containerColor=CardBg),shape=RoundedCornerShape(18.dp),modifier=Modifier.fillMaxWidth()) { Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically) { Surface(color=Purple.copy(alpha=.15f),shape=RoundedCornerShape(13.dp),modifier=Modifier.size(44.dp)) { Box(contentAlignment=Alignment.Center) { Icon(Icons.Default.Bolt,null,tint=Purple) } }; Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(s.purpose,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis); Text("${s.app} · ${s.elapsed}/${s.minutes} min",color=TextMuted,fontSize=12.sp) }; Text(s.status, color=if(s.status=="Completed") Green else Yellow,fontSize=10.sp) } } }

@Composable private fun SessionScreen(s:Session,elapsed:Int,intervention:Boolean,advance:()->Unit,finish:()->Unit,extend:()->Unit,breakNow:()->Unit,dismiss:()->Unit,back:()->Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(16.dp)) { Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.fillMaxWidth()) { IconButton(onClick=back){Icon(Icons.Default.ArrowBack,null)}; Text("Active intention",fontSize=20.sp,fontWeight=FontWeight.Bold) }; Spacer(Modifier.height(8.dp)); Surface(color=Purple.copy(alpha=.15f),shape=RoundedCornerShape(50.dp)) { Text(s.category.uppercase(),Modifier.padding(horizontal=14.dp,vertical=7.dp),color=Purple,fontSize=11.sp,fontWeight=FontWeight.Bold) }; Text(s.purpose,fontSize=24.sp,fontWeight=FontWeight.Bold); Text(s.app,color=TextMuted); Box(Modifier.size(210.dp),contentAlignment=Alignment.Center) { CircularProgressIndicator(progress={(elapsed.toFloat()/s.minutes).coerceIn(0f,1f)},modifier=Modifier.fillMaxSize(),strokeWidth=10.dp,color=if(elapsed>=s.minutes) Yellow else Purple,trackColor=CardBg); Column(horizontalAlignment=Alignment.CenterHorizontally) { Text("$elapsed",fontSize=46.sp,fontWeight=FontWeight.Bold); Text("of ${s.minutes} min",color=TextMuted) } }; Text(if(elapsed<s.minutes) "Stay with what you came here to do." else "Your planned time is up. What feels right now?",color=TextMuted); Button(onClick=advance,modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=Purple)) { Icon(Icons.Default.FastForward,null); Spacer(Modifier.width(8.dp)); Text("Advance demo time (+5 min)") }; OutlinedButton(onClick=finish,modifier=Modifier.fillMaxWidth()) { Icon(Icons.Default.CheckCircle,null); Spacer(Modifier.width(8.dp)); Text("I'm done — finish session") }; OutlinedButton(onClick=extend,modifier=Modifier.fillMaxWidth()) { Icon(Icons.Default.AddAlarm,null); Spacer(Modifier.width(8.dp)); Text("Continue intentionally (+5 min)") }; OutlinedButton(onClick=breakNow,modifier=Modifier.fillMaxWidth()) { Icon(Icons.Default.Coffee,null); Spacer(Modifier.width(8.dp)); Text("Take a short break") }; if(intervention) AlertDialog(onDismissRequest=dismiss,containerColor=CardBg,title={Text("A quick check-in",fontWeight=FontWeight.Bold)},text={Text("You planned ${s.minutes} minutes for ${s.purpose.lowercase()}. Is that still what you want to do?")},confirmButton={TextButton(onClick=finish){Text("I'm done")}},dismissButton={TextButton(onClick=dismiss){Text("Continue intentionally")}}) }
}

@Composable private fun HistoryScreen(sessions:List<Session>,back:()->Unit,open:(Session)->Unit) { LazyColumn(Modifier.fillMaxSize().padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) { item { ScreenHeader("Session history",back); Text("Your sessions, without judgment.",color=TextMuted) }; if(sessions.isEmpty()) item { EmptyCard("No history yet","Your completed sessions will appear here.") } else items(sessions) { SessionRow(it){open(it)} } } }
@Composable private fun InsightsScreen(sessions:List<Session>,back:()->Unit) { val planned=sessions.sumOf{it.minutes}; val actual=sessions.sumOf{it.elapsed}; val completed=sessions.count{it.status=="Completed"}; val rate=if(sessions.isEmpty()) 0 else completed*100/sessions.size; LazyColumn(Modifier.fillMaxSize().padding(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) { item { ScreenHeader("Insights",back); Text("Patterns, not judgments.",color=TextMuted) }; item { MetricCard("Session completion rate","$rate%","based on saved sessions",Green,Modifier.fillMaxWidth()) }; item { MetricCard("Planned time","$planned min","total planned duration",Purple,Modifier.fillMaxWidth()) }; item { MetricCard("Actual session time","$actual min","manual/demo session timers",Yellow,Modifier.fillMaxWidth()) }; item { Text("What this means",fontWeight=FontWeight.Bold,fontSize=18.sp) }; item { EmptyCard("Use context, not just minutes","A long session can be intentional. Compare what you planned, whether you reached your goal, and the choice you made when prompted.") }; item { Text("Saved sessions: ${sessions.size}",color=TextMuted) } } }
@Composable private fun LabScreen(back:()->Unit) { var selected by remember { mutableStateOf("Goal reflection") }; val prompts=mapOf("Gentle reminder" to "Your planned time is up. Would you like to finish or continue?", "Goal reflection" to "Did you accomplish what you came here to do?", "Time check" to "You've reached your time estimate. Is this still intentional?"); LazyColumn(Modifier.fillMaxSize().padding(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) { item { ScreenHeader("Intervention Lab",back); Text("Compare respectful prompt styles.",color=TextMuted) }; items(prompts.keys.toList()) { p -> Card(onClick={selected=p},colors=CardDefaults.cardColors(containerColor=if(selected==p) Purple.copy(alpha=.16f) else CardBg),shape=RoundedCornerShape(18.dp)) { Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically) { RadioButton(selected==p,{selected=p}); Spacer(Modifier.width(8.dp)); Text(p,fontWeight=FontWeight.SemiBold) } } }; item { Card(colors=CardDefaults.cardColors(containerColor=CardBg),shape=RoundedCornerShape(20.dp)) { Column(Modifier.padding(18.dp)) { Text("Preview · $selected",color=Purple,fontWeight=FontWeight.Bold); Spacer(Modifier.height(10.dp)); Text(prompts[selected] ?: "Choose a prompt style.",fontSize=18.sp); Spacer(Modifier.height(10.dp)); Text("Demo preview only. Results are not real experiment outcomes.",color=TextMuted,fontSize=12.sp) } } } } }
@Composable private fun SettingsScreen(context:Context,monitoring:Boolean,onMonitoring:(Boolean)->Unit,goal:String,onGoal:(String)->Unit,clear:()->Unit,back:()->Unit) { var confirm by remember { mutableStateOf(false) }; LazyColumn(Modifier.fillMaxSize().padding(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) { item { ScreenHeader("Settings & privacy",back); Text("You control what INTENT measures.",color=TextMuted) }; item { Card(colors=CardDefaults.cardColors(containerColor=CardBg)) { Column(Modifier.padding(16.dp)) { Text("Usage monitoring",fontWeight=FontWeight.Bold); Text("Reads Android usage summaries only after you grant Usage Access.",color=TextMuted,fontSize=12.sp); Row(verticalAlignment=Alignment.CenterVertically) { Text("Enable usage access",Modifier.weight(1f)); Switch(checked=monitoring,onCheckedChange=onMonitoring) }; Button(onClick={context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))},modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=Purple)) { Text("Open Android Usage Access settings") }; Text("Status: ${if(hasUsageAccess(context)) "permission granted" else "permission not granted"}",color=if(hasUsageAccess(context)) Green else Yellow,fontSize=12.sp) } } }; item { OutlinedTextField(value=goal,onValueChange={onGoal(it.filter(Char::isDigit))},label={Text("Daily attention goal (minutes)")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.fillMaxWidth()) }; item { Text("Privacy commitments",fontWeight=FontWeight.Bold,fontSize=18.sp); Text("• No message contents or keystrokes are collected.
• Monitoring can be turned off at any time.
• Demo records are simulated.
• The drift score is a heuristic, not a diagnosis.",color=TextMuted) }; item { OutlinedButton(onClick={confirm=true},modifier=Modifier.fillMaxWidth()) { Icon(Icons.Default.DeleteOutline,null); Spacer(Modifier.width(8.dp)); Text("Clear local session history") } } }; if(confirm) AlertDialog(onDismissRequest={confirm=false},title={Text("Clear local history?")},text={Text("This removes saved sessions from this device.")},confirmButton={TextButton(onClick={clear();confirm=false}){Text("Clear")}},dismissButton={TextButton(onClick={confirm=false}){Text("Cancel")}}) }
@Composable private fun ScreenHeader(title:String,back:()->Unit) { Row(verticalAlignment=Alignment.CenterVertically) { IconButton(onClick=back){Icon(Icons.Default.ArrowBack,null)}; Text(title,fontSize=23.sp,fontWeight=FontWeight.Bold) } }
@Composable private fun EmptyCard(title:String,description:String) { Card(colors=CardDefaults.cardColors(containerColor=CardBg),shape=RoundedCornerShape(20.dp)) { Column(Modifier.padding(18.dp)) { Text(title,fontWeight=FontWeight.Bold); Spacer(Modifier.height(5.dp)); Text(description,color=TextMuted,fontSize=13.sp) } } }
@Composable private fun EmptyScreen(title:String,back:()->Unit) { Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally) { Text(title); TextButton(onClick=back){Text("Go home")} } }

private fun hasUsageAccess(context:Context):Boolean { return try { val ops=context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager; val mode=ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,android.os.Process.myUid(),context.packageName); mode==AppOpsManager.MODE_ALLOWED } catch(_:Exception){ false } }
@Suppress("unused") private fun readUsageMinutes(context:Context,from:Long,to:Long):Long { if(!hasUsageAccess(context)) return 0; val mgr=context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager; return mgr.queryUsageStats(UsageStatsManager.INTERVAL_DAILY,from,to).orEmpty().sumOf { TimeUnit.MILLISECONDS.toMinutes(it.totalTimeInForeground) } }
