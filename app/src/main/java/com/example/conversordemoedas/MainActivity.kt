package com.example.conversordemoedas

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.conversordemoedas.R
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var tvRealBalance: TextView
    private lateinit var tvDollarBalance: TextView
    private lateinit var tvBitcoinBalance: TextView
    private lateinit var btnConvert: Button

    // Saldo inicial do usuário
    private var realBalance: BigDecimal = BigDecimal("100000.00")
    private var dollarBalance: BigDecimal = BigDecimal("50000.00")
    private var bitcoinBalance: BigDecimal = BigDecimal("0.5000")

    // Formatos para exibição
    private val realFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    private val dollarFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale.US)
    private val bitcoinFormat: NumberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 4
        maximumFractionDigits = 4
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvRealBalance = findViewById(R.id.tv_real_balance)
        tvDollarBalance = findViewById(R.id.tv_dollar_balance)
        tvBitcoinBalance = findViewById(R.id.tv_bitcoin_balance)
        btnConvert = findViewById(R.id.btn_convert)

        updateBalances()

        btnConvert.setOnClickListener {
            val intent = Intent(this, ConverterActivity::class.java).apply {
                putExtra("realBalance", realBalance.toPlainString())
                putExtra("dollarBalance", dollarBalance.toPlainString())
                putExtra("bitcoinBalance", bitcoinBalance.toPlainString())
            }
            startActivityForResult(intent, CONVERTER_REQUEST_CODE)
        }
    }

    private fun updateBalances() {
        tvRealBalance.text = realFormat.format(realBalance)
        tvDollarBalance.text = dollarFormat.format(dollarBalance)
        tvBitcoinBalance.text = "${bitcoinFormat.format(bitcoinBalance)} BTC"
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CONVERTER_REQUEST_CODE && resultCode == RESULT_OK) {
            // Atualizar saldos com os valores retornados da ConverterActivity
            data?.let {
                realBalance = BigDecimal(it.getStringExtra("updatedRealBalance") ?: "0.00")
                dollarBalance = BigDecimal(it.getStringExtra("updatedDollarBalance") ?: "0.00")
                bitcoinBalance = BigDecimal(it.getStringExtra("updatedBitcoinBalance") ?: "0.0000")
                updateBalances()
            }
        }
    }
    companion object {
        const val CONVERTER_REQUEST_CODE = 1
    }
}