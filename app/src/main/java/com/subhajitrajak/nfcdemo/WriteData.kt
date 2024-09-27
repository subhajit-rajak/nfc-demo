package com.subhajitrajak.nfcdemo

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
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
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.subhajitrajak.nfcdemo.databinding.ActivityWriteDataBinding

import com.google.firebase.database.*

class WriteData : AppCompatActivity() {
    private val binding: ActivityWriteDataBinding by lazy {
        ActivityWriteDataBinding.inflate(layoutInflater)
    }
    private var intentFiltersArray: Array<IntentFilter>? = null
    private val techListsArray = arrayOf(arrayOf(NfcF::class.java.name))
    private val nfcAdapter: NfcAdapter? by lazy {
        NfcAdapter.getDefaultAdapter(this)
    }
    private var pendingIntent: PendingIntent? = null

    // Firebase reference
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        enableEdgeToEdge()

        // prepare pending Intent
        val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)
        } else {
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
        }

        val ndef = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
        try {
            ndef.addDataType("text/plain")
        } catch (e: IntentFilter.MalformedMimeTypeException) {
            throw RuntimeException("fail", e)
        }
        intentFiltersArray = arrayOf(ndef)

        // Check NFC availability
        if (nfcAdapter == null) {
            showNfcNotSupportedDialog()
        } else if (!nfcAdapter!!.isEnabled) {
            showNfcDisabledDialog()
        }

        // Button to fetch and display stored data from Firebase
        binding.btnback.setOnClickListener {
            fetchDataFromFirebase()
        }
    }

    private fun showNfcNotSupportedDialog() {
        val builder = AlertDialog.Builder(this@WriteData, R.style.MyAlertDialogStyle)
        builder.setMessage("This device doesn't support NFC.")
        builder.setPositiveButton("Cancel", null)
        val myDialog = builder.create()
        myDialog.setCanceledOnTouchOutside(false)
        myDialog.show()
    }

    private fun showNfcDisabledDialog() {
        val builder = AlertDialog.Builder(this@WriteData, R.style.MyAlertDialogStyle)
        builder.setTitle("NFC Disabled")
        builder.setMessage("Please Enable NFC")
        builder.setPositiveButton("Settings") { _, _ -> startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) }
        builder.setNegativeButton("Cancel", null)
        val myDialog = builder.create()
        myDialog.setCanceledOnTouchOutside(false)
        myDialog.show()
    }

    private fun showNfcDisabledDialog() {
        val builder = AlertDialog.Builder(this@WriteData, R.style.MyAlertDialogStyle)
        builder.setTitle("NFC Disabled")
        builder.setMessage("Please Enable NFC")
        builder.setPositiveButton("Settings") { _, _ -> startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) }
        builder.setNegativeButton("Cancel", null)
        val myDialog = builder.create()
        myDialog.setCanceledOnTouchOutside(false)
        myDialog.show()
    }

    private fun showNfcNotSupportedDialog() {
        val builder = AlertDialog.Builder(this@WriteData, R.style.MyAlertDialogStyle)
        builder.setMessage("This device doesn't support NFC.")
        builder.setPositiveButton("Cancel", null)
        val myDialog = builder.create()
        myDialog.setCanceledOnTouchOutside(false)
        myDialog.show()
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, techListsArray)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        try {
            val shopId="restaurant"
            val menuId=binding.txtmenuid.text.toString()
            if(menuId != "") {
                if (NfcAdapter.ACTION_TECH_DISCOVERED == intent.action
                    || NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action
                ) {
                    val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG) ?: return
                    val ndef = Ndef.get(tag) ?: return

                    if (ndef.isWritable) {
                        val message = NdefMessage(
                            arrayOf(
                                NdefRecord.createTextRecord("en", shopId),
                                NdefRecord.createTextRecord("en", menuId)
                            )
                        )

                        ndef.connect()
                        ndef.writeNdefMessage(message)
                        ndef.close()

                        Toast.makeText(applicationContext, "Successfully Written!", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(applicationContext, "Write on text box!", Toast.LENGTH_SHORT).show()
            }
        }
        catch (e:Exception) {
            Toast.makeText(applicationContext, e.message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPause() {
        if (this.isFinishing) {
            nfcAdapter?.disableForegroundDispatch(this)
        }
        super.onPause()
    }

    // Fetch stored data from Firebase and display it in EditText fields
    private fun fetchDataFromFirebase() {
        database.child("nfcTags").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val fetchedShopId = snapshot.child("shopId").getValue(String::class.java)
                val fetchedMachineId = snapshot.child("machineId").getValue(String::class.java)

                if (fetchedShopId != null && fetchedMachineId != null) {
                    binding.txtshopid.setText(fetchedShopId)
                    binding.txtmachineid.setText(fetchedMachineId)
                    Toast.makeText(applicationContext, "Data fetched and displayed!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(applicationContext, "No data found in Firebase", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(applicationContext, "Failed to fetch data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
