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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        enableEdgeToEdge()

        try {

            binding.btnwrite.setOnClickListener {
                val intent = Intent(this, WriteData::class.java)
                startActivity(intent)
            }
            //nfc process start
//            pendingIntent = PendingIntent.getActivity(this, 0, Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0)

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
                val builder = AlertDialog.Builder(this@MainActivity, R.style.MyAlertDialogStyle)
                builder.setMessage("This device doesn't support NFC.")
                builder.setPositiveButton("Cancel", null)
                val myDialog = builder.create()
                myDialog.setCanceledOnTouchOutside(false)
                myDialog.show()
                binding.txtviewshopid.text = "THIS DEVICE DOESN'T SUPPORT NFC. PLEASE TRY WITH ANOTHER DEVICE!"
                binding.txtviewmachineid.visibility = View.INVISIBLE

            } else if (!nfcAdapter!!.isEnabled) {
                val builder = AlertDialog.Builder(this@MainActivity, R.style.MyAlertDialogStyle)
                builder.setTitle("NFC Disabled")
                builder.setMessage("Plesae Enable NFC")
                binding.txtviewshopid.text = "NFC IS NOT ENABLED. PLEASE ENABLE NFC IN SETTINGS->NFC"
                binding.txtviewmachineid.visibility = View.INVISIBLE

                builder.setPositiveButton("Settings") { _, _ -> startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) }
                builder.setNegativeButton("Cancel", null)
                val myDialog = builder.create()
                myDialog.setCanceledOnTouchOutside(false)
                myDialog.show()
            }
        }
        catch (ex:Exception)
        {
            Toast.makeText(applicationContext, ex.message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, techListsArray)
    }


    var iswrite = "0"

    var machineid="";
    var shopid="";
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)


        val action = intent.action
        if (NfcAdapter.ACTION_NDEF_DISCOVERED == action) {

            val parcelables = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            with(parcelables) {
                try {
                    val inNdefMessage = this?.get(0) as NdefMessage
                    val inNdefRecords = inNdefMessage.records
                    //if there are many records, you can call inNdefRecords[1] as array
                    var ndefRecord_0 = inNdefRecords[0]
                    var inMessage = String(ndefRecord_0.payload)
                    shopid = inMessage.drop(3);
                    binding.txtviewshopid.text = "SHOP ID: " + shopid

                    ndefRecord_0 = inNdefRecords[1]
                    inMessage = String(ndefRecord_0.payload)
                    machineid = inMessage.drop(3);
                    binding.txtviewmachineid.text = "MACHINE ID: " + machineid

                    if (!binding.txtuserid.text.toString().equals("")) {
                        if (NfcAdapter.ACTION_TECH_DISCOVERED == intent.action
                            || NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action
                        ) {

                            val tag =
                                intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG) ?: return
                            val ndef = Ndef.get(tag) ?: return

                            if (ndef.isWritable) {

                                var message = NdefMessage(
                                    arrayOf(
                                        NdefRecord.createTextRecord("en", shopid),
                                        NdefRecord.createTextRecord("en", machineid),
                                        NdefRecord.createTextRecord(
                                            "en",
                                            binding.txtuserid.text.toString()
                                        )

                                    )
                                )


                                ndef.connect()
                                ndef.writeNdefMessage(message)
                                ndef.close()

                                binding.txtviewuserid.text = "USER ID: "+binding.txtuserid.text.toString();
                                Toast.makeText(
                                    applicationContext,
                                    "Successfully Wrote!",
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                            }
                        }
                    } else {
                        try {


                            ndefRecord_0 = inNdefRecords[2]
                            inMessage = String(ndefRecord_0.payload)

                            binding.txtviewuserid.text = "USER ID: " + inMessage.drop(3)
                        }
                        catch (ex:Exception){
                            Toast.makeText(applicationContext, "User ID not written!", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (ex: Exception) {
                    Toast.makeText(
                        applicationContext,
                        "There are no Machine and Shop information found!, please click write data to write those!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }


        }

    }

    override fun onPause() {
        if (this.isFinishing) {
            nfcAdapter?.disableForegroundDispatch(this)
        }
        super.onPause()
    }
}