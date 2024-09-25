package com.subhajitrajak.nfcdemo

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.R.id.message
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        enableEdgeToEdge()

        binding.btnback.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        //nfc process start
//        pendingIntent = PendingIntent.getActivity(
//            this, 0, Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0
//        )

        pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(this, 0, Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_MUTABLE)
        } else {
            PendingIntent.getActivity(this, 0, Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
        }

        val ndef = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
        try {
            ndef.addDataType("text/plain")
        } catch (e: IntentFilter.MalformedMimeTypeException) {
            throw RuntimeException("fail", e)
        }
        intentFiltersArray = arrayOf(ndef)
        if (nfcAdapter == null) {
            val builder = AlertDialog.Builder(this@WriteData, R.style.MyAlertDialogStyle)
            builder.setMessage("This device doesn't support NFC.")
            builder.setPositiveButton("Cancel", null)
            val myDialog = builder.create()
            myDialog.setCanceledOnTouchOutside(false)
            myDialog.show()
        } else if (!nfcAdapter!!.isEnabled) {
            val builder = AlertDialog.Builder(this@WriteData, R.style.MyAlertDialogStyle)
            builder.setTitle("NFC Disabled")
            builder.setMessage("Plesae Enable NFC")
            builder.setPositiveButton("Settings") { _, _ -> startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) }
            builder.setNegativeButton("Cancel", null)
            val myDialog = builder.create()
            myDialog.setCanceledOnTouchOutside(false)
            myDialog.show()
        }
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, techListsArray)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        try {
            if(!binding.txtmachineid.text.toString().equals("") && !binding.txtshopid.text.toString().equals("") ) {
                val shopid=binding.txtshopid.text.toString()
                val machineid=binding.txtmachineid.text.toString()

                if (NfcAdapter.ACTION_TECH_DISCOVERED == intent.action
                    || NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action
                ) {

                    val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG) ?: return
                    val ndef = Ndef.get(tag) ?: return

                    if (ndef.isWritable) {

                        var message = NdefMessage(
                            arrayOf(
                                NdefRecord.createTextRecord("en", shopid),
                                NdefRecord.createTextRecord("en", machineid)
//                        NdefRecord.createTextRecord("en", userid)

                            )
                        )


                        ndef.connect()
                        ndef.writeNdefMessage(message)
                        ndef.close()


                        Toast.makeText(applicationContext, "Successfully Wroted!", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
            else
            {
                Toast.makeText(applicationContext, "Write on text box!", Toast.LENGTH_SHORT).show()
            }
        }
        catch (Ex:Exception)
        {
            Toast.makeText(applicationContext, Ex.message, Toast.LENGTH_SHORT).show()
        }




    }

    override fun onPause() {
        if (this.isFinishing) {
            nfcAdapter?.disableForegroundDispatch(this)
        }
        super.onPause()
    }
}