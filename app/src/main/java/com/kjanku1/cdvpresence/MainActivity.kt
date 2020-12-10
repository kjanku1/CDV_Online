package com.kjanku1.cdvpresence

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.provider.CalendarContract
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.kjanku1.cdvpresence.model.CredentialsModel
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_credentials.*
import java.util.*


// Projection array. Creating indices for this array instead of doing
// dynamic lookups improves performance.
private val EVENT_PROJECTION: Array<String> = arrayOf(
    CalendarContract.Calendars._ID,                     // 0
    CalendarContract.Calendars.ACCOUNT_NAME,            // 1
    CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,   // 2
    CalendarContract.Calendars.OWNER_ACCOUNT            // 3
)

// The indices for the projection array above.
private const val PROJECTION_ID_INDEX: Int = 0
private const val PROJECTION_ACCOUNT_NAME_INDEX: Int = 1
private const val PROJECTION_DISPLAY_NAME_INDEX: Int = 2
private const val PROJECTION_OWNER_ACCOUNT_INDEX: Int = 3
private const val DAY_IN_MILLIS: Int = 86400000
private const val WEEK_IN_MILLIS: Int = 604800000

private val startMillis: Long = System.currentTimeMillis()- DAY_IN_MILLIS//-(5*60*1000)
/*val startMillis: Long =Calendar.getInstance().run {
    set(2020, 10, 25, 1, 0)
    timeInMillis
}*/
private val endMillis: Long = startMillis + DAY_IN_MILLIS*2 // change to day

private lateinit var webView: WebView
private lateinit var eventName: String
private lateinit var listView: ListView
private lateinit var closeBtn: Button

var username: String = ""//""
var password: String = ""//""

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //check for permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.READ_CALENDAR
                )) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_CALENDAR),
                    1
                )
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_CALENDAR),
                    1
                )
            }
        }

        closeBtn = findViewById(R.id.btn)
        btn.setOnClickListener{
            webview.visibility = View.GONE
            btn.visibility = View.GONE
            listView.visibility = View.VISIBLE
        }
        //setup ListView
        listView = findViewById<ListView>(R.id.ls1)
        val listArg = readCalendarEvent()
        val adp: ArrayAdapter<String> = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            listArg
        )
        listView.adapter = adp
        listView.setOnItemClickListener { parent, view, position, id ->
            loadDetails(position)
        }
        //login data
        val sharedPreferences: SharedPreferences = getSharedPreferences(
            "sharedCreds", Context.MODE_PRIVATE
        )
        val email = sharedPreferences.getString("email", "")
        val pass = sharedPreferences.getString("pass", "")

        var savedCredentials = CredentialsModel(email!!, pass!!)
        Log.d("CredentialsModel", savedCredentials.email)
        if(email.isNotEmpty() && pass.isNotEmpty()) {
        savedCredentials = CredentialsModel(email, pass) // create object instance
        } else {
            Toast.makeText(
                applicationContext,
                "Podaj swoje dane do logowania w ustawieniach",
                Toast.LENGTH_LONG
            ).show()
            updateCredentialsDialog(CredentialsModel(email, pass))
            savedCredentials = CredentialsModel(email, pass)
        }

        //setup webview
        webView = findViewById(R.id.webview)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = WebViewClient()

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                view.loadUrl(url)
                return true
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url);

                // load credentials
                username = savedCredentials.email
                password = savedCredentials.password

                Log.d("DATA:", password)                // Perform clicks by injecting js
                webView.loadUrl(
                    "javascript: window.onload = (function(){" +
                            "let x = document.getElementById('username').value = \"" + username + "\";" +
                            "let y = document.getElementById('password').value = \"" + password + "\";" +

                            "let z = document.getElementById('loginbtn').click();" +
                            /*"var el = document.getElementsByClassName(\"btn btn-primary btn-block do-login\");" +// wersja firefox
                            "for (var i=0;i<el.length; i++) {" +
                            "   el[i].click();" +
                            "}" +*/

                            /*
                            "var dom_element = document.evaluate(\"//a[contains(., 'Zarejestruj obecność')]\", document, null, XPathResult.ANY_TYPE, null );"+
                            "l = dom_element.iterateNext();"+
                            "l.click();" +
                            "dom_element = document.evaluate(\"//span[contains(., 'Zapisz zmiany')]\", document, null, XPathResult.ANY_TYPE, null );"+
                            "l = dom_element.iterateNext();"+
                                "l.click();" +
                            "dom_element = document.evaluate(\"//span[contains(., 'Obecny')]\", document, null, XPathResult.ANY_TYPE, null );"+
                            "l = dom_element.iterateNext();"+
                            "l.click();"+*/
                    "})()"
                )
            }
        }
    }

    private fun readCalendarEvent(): ArrayList<String> {
        var nameOfEvent: ArrayList<String> = arrayListOf()

        val projection: Array<String> = arrayOf(
            CalendarContract.Events.CALENDAR_ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.ALL_DAY,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Calendars.ACCOUNT_NAME
        )

        val selection = "((${CalendarContract.Calendars.ACCOUNT_NAME} LIKE ?) AND (" +
                "${CalendarContract.Events.DTSTART} >= ?) AND (" +
                "${CalendarContract.Events.DTEND} <= ?))"
        val selectionArgs: Array<String> =
            arrayOf("%@edu.cdv.pl", startMillis.toString(), endMillis.toString())

        var cursor: Cursor? =
            contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection, selection,
                selectionArgs, null
            )

        if (cursor!!.moveToFirst()) {
            do {
                nameOfEvent.add(cursor!!.getString(1))
                eventName = cursor!!.getString(1)
                Log.d("CALENDAR EVENT", eventName)
            } while (cursor.moveToNext());
        }
        return nameOfEvent;
    }

    private fun loadDetails(pos: Int){
        var lista = readCalendarEvent()
        val item = lista[pos]
        Toast.makeText(this, item, Toast.LENGTH_SHORT).show()
        var e_id = getId(item)
        webView.loadUrl("https://moodle.cdv.pl/mod/attendance/view.php?mode=0&id=$e_id")
        webview.visibility = View.VISIBLE
        btn.visibility = View.VISIBLE
        listView.visibility = View.GONE
    }
    private fun getId(name: String): String{
        return when {
            name.contains("Architektura") -> {
                "996"
            }
            name.contains("Komunikacja człowiek-komputer") -> {
                "972"
            }
            name.contains("Praktyczne") -> {
                "968"
            }
            name.contains("Programowanie obiektowe") -> {
                "1016"
            }
            else -> ""
        }
    }
    private fun saveEvents(){

        val sharedPreferences: SharedPreferences = getSharedPreferences(
            "sharedPrefs",
            Context.MODE_PRIVATE
        )
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.apply{
            putString("events", readCalendarEvent().toString())
        }
    }
    private fun loadEvents(){
        val sharedPreferences: SharedPreferences = getSharedPreferences(
            "sharedPrefs",
            Context.MODE_PRIVATE
        )
        val savedEvents: String? = sharedPreferences.getString("events", null)
    }
    private fun saveCredentials(e: String, p: String){
        val sharedPreferences: SharedPreferences = getSharedPreferences(
            "sharedCreds",
            Context.MODE_PRIVATE
        )
            sharedPreferences.edit().
            putString("email", e).
            putString("pass", p).
            apply()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_action, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if(item.itemId == R.id.prefs_btn) {
            //val prefsIntent = Intent(this, PreferencesActivity::class.java)
            //startActivity(prefsIntent)
        } else if(item.itemId == R.id.credentials_btn){
            updateCredentialsDialog(CredentialsModel("", ""))
        } else if (item.itemId == R.id.about_btn){
            // TODO
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Method is used to show the custom update dialog.
     */
    private fun updateCredentialsDialog(dataclassmodel: CredentialsModel) {
        val updateDialog = Dialog(this, R.style.Theme_Dialog)
        updateDialog.setCancelable(false)
        /*Set the screen content from a layout resource.
         The resource will be inflated, adding all top-level views to the screen.*/
        updateDialog.setContentView(R.layout.dialog_credentials)

        updateDialog.etUpdateEmail.setText(dataclassmodel.email)
        updateDialog.etUpdatePass.setText(dataclassmodel.password)

        updateDialog.tvUpdate.setOnClickListener(View.OnClickListener {
            val email = updateDialog.etUpdateEmail.text.toString()
            val pass = updateDialog.etUpdatePass.text.toString()

            if (!email.isEmpty() && !pass.isEmpty()) {
                val status =
                    saveCredentials(email, pass)
                if (status != null) {
                    Toast.makeText(applicationContext, "Zapisane!", Toast.LENGTH_LONG).show()
                    updateDialog.dismiss() // Dialog will be dismissed
                }
            } else {
                Toast.makeText(
                    applicationContext,
                    "Email or password cannot be blank",
                    Toast.LENGTH_LONG
                ).show()
            }
        })

        updateDialog.tvCancel.setOnClickListener(View.OnClickListener {
            updateDialog.dismiss()
        })
        //Start the dialog and display it on screen.
        updateDialog.show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            1 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                    if ((ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) === PackageManager.PERMISSION_GRANTED)
                    ) {
                        Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                }
                return
            }
        }
    }
}

