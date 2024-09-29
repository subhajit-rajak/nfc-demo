package com.subhajitrajak.nfcdemo

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.subhajitrajak.nfcdemo.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private val pendingOrdersList = ArrayList<Order>()
    private lateinit var database: FirebaseDatabase
    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        enableEdgeToEdge()
        binding.enterMenu.setOnClickListener {
            val intent = Intent(this, WriteData::class.java)
            startActivity(intent)
        }

        binding.completeOrdersBtn.setOnClickListener {
            val intent = Intent(this, CompleteOrders::class.java)
            startActivity(intent)
        }

        binding.menuBtn.setOnClickListener {
            val intent = Intent(this, MenuActivity::class.java)
            startActivity(intent)
        }

        getOrderDetails()
    }

    private fun getOrderDetails() {
        database = FirebaseDatabase.getInstance()
        val databaseRef = database.reference.child("orders").orderByChild("currentTime")
        databaseRef.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                pendingOrdersList.clear()
                for (orders in snapshot.children) {
                    val order = orders.getValue(Order::class.java)
                    order?.let {
                        pendingOrdersList.add(it)
                    }
                }
                setAdapter()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, error.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setAdapter() {
        val adapter = PendingOrdersAdapter(pendingOrdersList)
        binding.pendingOrders.adapter = adapter
        binding.pendingOrders.layoutManager = LinearLayoutManager(this)
    }
}