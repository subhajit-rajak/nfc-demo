package com.subhajitrajak.nfcdemo

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NfcF
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.subhajitrajak.nfcdemo.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    private var intentFiltersArray: Array<IntentFilter>? = null
    private val techListsArray = arrayOf(arrayOf(NfcF::class.java.name))
    private val nfcAdapter: NfcAdapter? by lazy {
        NfcAdapter.getDefaultAdapter(this)
    }
    private var pendingIntent: PendingIntent? = null
    private lateinit var database: DatabaseReference
    private var ndefMessage: NdefMessage? = null// Firebase database reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        enableEdgeToEdge()

        // Initialize Firebase database reference
        database = FirebaseDatabase.getInstance().reference

        try {
            // Click listener for NFC writing
            binding.btnwrite.setOnClickListener {
                val dataToWrite = binding.txtuserid.text.toString()

                // Ensure the user has entered data
                if (dataToWrite.isNotEmpty()) {
                    writeToNfcTag(dataToWrite)
                } else {
                    Toast.makeText(
                        this,
                        "Please enter data to write to NFC tag.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)
            } else {
                PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
                )
            }

            val ndef = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
            try {
                ndef.addDataType("text/plain")
            } catch (e: IntentFilter.MalformedMimeTypeException) {
                throw RuntimeException("fail", e)
            }
            intentFiltersArray = arrayOf(ndef)

            // Check NFC availability and settings
            if (nfcAdapter == null) {
                showNfcNotSupportedDialog()
            } else if (!nfcAdapter!!.isEnabled) {
                showNfcDisabledDialog()
            }
        } catch (ex: Exception) {
            Toast.makeText(applicationContext, ex.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showNfcNotSupportedDialog() {
        val builder = AlertDialog.Builder(this@MainActivity, R.style.MyAlertDialogStyle)
        builder.setMessage("This device doesn't support NFC.")
        builder.setPositiveButton("Cancel", null)
        val myDialog = builder.create()
        myDialog.setCanceledOnTouchOutside(false)
        myDialog.show()
        binding.txtviewshopid.text =
            "THIS DEVICE DOESN'T SUPPORT NFC. PLEASE TRY WITH ANOTHER DEVICE!"
        binding.txtviewmachineid.visibility = View.INVISIBLE
    }

    private fun showNfcDisabledDialog() {
        val builder = AlertDialog.Builder(this@MainActivity, R.style.MyAlertDialogStyle)
        builder.setTitle("NFC Disabled")
        builder.setMessage("Please Enable NFC")
        binding.txtviewshopid.text = "NFC IS NOT ENABLED. PLEASE ENABLE NFC IN SETTINGS->NFC"
        binding.txtviewmachineid.visibility = View.INVISIBLE
        builder.setPositiveButton("Settings") { _, _ -> startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) }
        builder.setNegativeButton("Cancel", null)
        val myDialog = builder.create()
        myDialog.setCanceledOnTouchOutside(false)
        myDialog.show()
    }

    // Write data to NFC tag
    private fun writeToNfcTag(data: String) {
        // Prepare the data for writing to NFC tag
        val ndefMessage =
            NdefMessage(arrayOf(NdefRecord.createMime("text/plain", data.toByteArray())))

        // Save data to Firebase Realtime Database
        val key = database.push().key ?: return
        database.child("nfcTags").child(key).setValue(data).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(
                    this,
                    "Data saved to Firebase and ready to be written to NFC tag.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(this, "Failed to save data to Firebase.", Toast.LENGTH_SHORT).show()
            }
        }

        // Enable foreground dispatch to handle NFC intents
        enableForegroundDispatch()

        // Save data to NFC tag when detected
        Toast.makeText(this, "Bring NFC tag closer to write data.", Toast.LENGTH_SHORT).show()
    }

    // Enable foreground dispatch to capture NFC tag when the app is in the foreground
    private fun enableForegroundDispatch() {
        if (nfcAdapter != null && pendingIntent != null && intentFiltersArray != null) {
            nfcAdapter!!.enableForegroundDispatch(
                this,
                pendingIntent,
                intentFiltersArray,
                techListsArray
            )
        }
    }

    // Handle NFC tag writing when NFC tag is detected
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
            val ndef = Ndef.get(tag)

            if (ndef != null && ndefMessage != null) {
                try {
                    ndef.connect()
                    ndef.writeNdefMessage(ndefMessage)
                    Toast.makeText(
                        this,
                        "Data successfully written to NFC tag!",
                        Toast.LENGTH_SHORT
                    ).show()
                    ndef.close()
                } catch (e: Exception) {
                    Toast.makeText(
                        this,
                        "Failed to write data to NFC tag: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }


    // Disable foreground dispatch when the activity is paused
    override fun onPause() {
        super.onPause()
        if (nfcAdapter != null) {
            nfcAdapter!!.disableForegroundDispatch(this)
        }
    }

    // Enable foreground dispatch when the activity is resumed
    override fun onResume() {
        super.onResume()
        enableForegroundDispatch()
    }
}