package com.example.conversordemoedas

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.example.conversordemoedas.R
import com.example.conversordemoedas.data.api.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

class ConverterActivity : AppCompatActivity() {

    private lateinit var spinnerOriginCurrency: Spinner
    private lateinit var spinnerDestCurrency: Spinner
    private lateinit var etAmount: TextInputEditText
    private lateinit var btnPerformConversion: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvConvertedValue: TextView
    private var realBalance: BigDecimal = BigDecimal("0.00")
    private var dollarBalance: BigDecimal = BigDecimal("0.00")
    private var bitcoinBalance: BigDecimal = BigDecimal("0.0000")
    private val currencies = arrayOf("BRL", "USD", "BTC")
    private var selectedOriginCurrency: String = "BRL"
    private var selectedDestCurrency: String = "USD"
    private val realFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    private val dollarFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale.US)
    private val bitcoinFormat: NumberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 4
        maximumFractionDigits = 4
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_converter)

        intent.getStringExtra("realBalance")?.let { realBalance = BigDecimal(it) }
        intent.getStringExtra("dollarBalance")?.let { dollarBalance = BigDecimal(it) }
        intent.getStringExtra("bitcoinBalance")?.let { bitcoinBalance = BigDecimal(it) }

        spinnerOriginCurrency = findViewById(R.id.spinner_origin_currency)
        spinnerDestCurrency = findViewById(R.id.spinner_dest_currency)
        etAmount = findViewById(R.id.et_amount)
        btnPerformConversion = findViewById(R.id.btn_perform_conversion)
        progressBar = findViewById(R.id.progress_bar)
        tvConvertedValue = findViewById(R.id.tv_converted_value)

        setupSpinners()

        btnPerformConversion.setOnClickListener {
            performConversion()
        }
    }

    private fun setupSpinners() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, currencies)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        spinnerOriginCurrency.adapter = adapter
        spinnerDestCurrency.adapter = adapter

        spinnerOriginCurrency.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedOriginCurrency = currencies[position]
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerDestCurrency.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedDestCurrency = currencies[position]
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun performConversion() {
        val amountStr = etAmount.text.toString()
        if (amountStr.isEmpty()) {
            etAmount.error = "Informe o valor!"
            return
        }

        val amount = BigDecimal(amountStr)

        if (!hasSufficientFunds(selectedOriginCurrency, amount)) {
            Toast.makeText(this, "Saldo insuficiente para a transação em ${selectedOriginCurrency}", Toast.LENGTH_LONG).show()
            return
        }

        val coinPair = when {
            selectedOriginCurrency == selectedDestCurrency -> {
                Toast.makeText(this, "Moedas de origem e destino são as mesmas.", Toast.LENGTH_SHORT).show()
                return
            }
            (selectedOriginCurrency == "USD" && selectedDestCurrency == "BRL") ||
                    (selectedOriginCurrency == "BRL" && selectedDestCurrency == "USD") ->
                "${selectedOriginCurrency}${selectedDestCurrency}"

            selectedOriginCurrency == "BTC" && selectedDestCurrency == "BRL" -> "BTC-USD"
            selectedOriginCurrency == "BTC" && selectedDestCurrency == "USD" -> "BTC-USD"
            selectedOriginCurrency == "BRL" && selectedDestCurrency == "BTC" -> "USD-BRL"
            selectedOriginCurrency == "USD" && selectedDestCurrency == "BTC" -> "USD-BRL"
            else -> {
                Toast.makeText(this, "Conversão não suportada diretamente ou com intermediário definido para ${selectedOriginCurrency} para ${selectedDestCurrency}.", Toast.LENGTH_LONG).show()
                return
            }
        }

        progressBar.visibility = View.VISIBLE
        btnPerformConversion.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.getCurrencyQuote(coinPair)
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnPerformConversion.isEnabled = true

                    if (response.isSuccessful) {
                        val quotes = response.body()
                        val quoteEntry = quotes?.values?.firstOrNull()

                        if (quoteEntry != null) {
                            val bidPrice = BigDecimal(quoteEntry.bid)
                            val askPrice = BigDecimal(quoteEntry.ask)

                            var convertedValue: BigDecimal
                            var finalPrice: BigDecimal = BigDecimal.ZERO
                            when {
                                (selectedOriginCurrency == "BRL" && selectedDestCurrency == "USD") -> {
                                    finalPrice = bidPrice
                                    convertedValue = amount.divide(finalPrice, 2, BigDecimal.ROUND_HALF_UP)
                                }
                                (selectedOriginCurrency == "USD" && selectedDestCurrency == "BRL") -> {
                                    finalPrice = askPrice
                                    convertedValue = amount.multiply(finalPrice).setScale(2, BigDecimal.ROUND_HALF_UP)
                                }
                                (selectedOriginCurrency == "BTC" && selectedDestCurrency == "USD") -> {
                                    finalPrice = askPrice
                                    convertedValue = amount.multiply(finalPrice).setScale(2, BigDecimal.ROUND_HALF_UP)
                                }
                                (selectedOriginCurrency == "USD" && selectedDestCurrency == "BTC") -> {
                                    finalPrice = bidPrice
                                    convertedValue = amount.divide(finalPrice, 4, BigDecimal.ROUND_HALF_UP)
                                }
                                (selectedOriginCurrency == "BTC" && selectedDestCurrency == "BRL") -> {
                                    val usdPriceForBtc = BigDecimal(quoteEntry.ask)
                                    val amountInUsd = amount.multiply(usdPriceForBtc).setScale(2, BigDecimal.ROUND_HALF_UP)

                                    val brlToUsdResponse = RetrofitClient.instance.getCurrencyQuote("USD-BRL")
                                    if (brlToUsdResponse.isSuccessful) {
                                        val brlToUsdQuotes = brlToUsdResponse.body()
                                        val brlToUsdQuoteEntry = brlToUsdQuotes?.values?.firstOrNull()
                                        if (brlToUsdQuoteEntry != null) {
                                            val bidUsdToBrl = BigDecimal(brlToUsdQuoteEntry.ask)
                                            convertedValue = amountInUsd.multiply(bidUsdToBrl).setScale(2, BigDecimal.ROUND_HALF_UP)
                                        } else {
                                            Toast.makeText(this@ConverterActivity, "Erro ao obter cotação USD-BRL para conversão intermediária.", Toast.LENGTH_SHORT).show()
                                            return
                                        }
                                    } else {
                                        Toast.makeText(this@ConverterActivity, "Erro na chamada da API para USD-BRL: ${brlToUsdResponse.code()}", Toast.LENGTH_SHORT).show()
                                        return
                                    }
                                }
                                (selectedOriginCurrency == "BRL" && selectedDestCurrency == "BTC") -> {
                                    // BRL -> USD
                                    val usdToBrlResponse = RetrofitClient.instance.getCurrencyQuote("BRL-USD")
                                    if (usdToBrlResponse.isSuccessful) {
                                        val usdToBrlQuotes = usdToBrlResponse.body()
                                        val usdToBrlQuoteEntry = usdToBrlQuotes?.values?.firstOrNull()
                                        if (usdToBrlQuoteEntry != null) {
                                            val askBrlToUsd = BigDecimal(usdToBrlQuoteEntry.bid)
                                            val amountInUsd = amount.divide(askBrlToUsd, 2, BigDecimal.ROUND_HALF_UP)
                                            val btcToUsdResponse = RetrofitClient.instance.getCurrencyQuote("USD-BTC")  dá BTC-USD
                                            if (btcToUsdResponse.isSuccessful) {
                                                val btcToUsdQuotes = btcToUsdResponse.body()
                                                val btcToUsdQuoteEntry = btcToUsdQuotes?.values?.firstOrNull()
                                                if (btcToUsdQuoteEntry != null) {
                                                    val bidUsdToBtc = BigDecimal(btcToUsdQuoteEntry.bid)
                                                    convertedValue = amountInUsd.divide(bidUsdToBtc, 4, BigDecimal.ROUND_HALF_UP)
                                                } else {
                                                    Toast.makeText(this@ConverterActivity, "Erro ao obter cotação USD-BTC para conversão intermediária.", Toast.LENGTH_SHORT).show()
                                                    return
                                                }
                                            } else {
                                                Toast.makeText(this@ConverterActivity, "Erro na chamada da API para USD-BTC: ${btcToUsdResponse.code()}", Toast.LENGTH_SHORT).show()
                                                return
                                            }
                                        } else {
                                            Toast.makeText(this@ConverterActivity, "Erro ao obter cotação BRL-USD para conversão intermediária.", Toast.LENGTH_SHORT).show()
                                            return
                                        }
                                    } else {
                                        Toast.makeText(this@ConverterActivity, "Erro na chamada da API para BRL-USD: ${usdToBrlResponse.code()}", Toast.LENGTH_SHORT).show()
                                        return
                                    }
                                }
                                else -> {
                                    Toast.makeText(this@ConverterActivity, "Combinação de moedas não esperada.", Toast.LENGTH_SHORT).show()
                                    return
                                }
                            }
                            updateBalancesAfterConversion(selectedOriginCurrency, selectedDestCurrency, amount, convertedValue)
                            val formattedConvertedValue = when (selectedDestCurrency) {
                                "BRL" -> realFormat.format(convertedValue)
                                "USD" -> dollarFormat.format(convertedValue)
                                "BTC" -> "${bitcoinFormat.format(convertedValue)} BTC"
                                else -> convertedValue.toPlainString()
                            }
                            tvConvertedValue.text = "Valor Convertido: $formattedConvertedValue"

                            val resultIntent = Intent().apply {
                                putExtra("updatedRealBalance", realBalance.toPlainString())
                                putExtra("updatedDollarBalance", dollarBalance.toPlainString())
                                putExtra("updatedBitcoinBalance", bitcoinBalance.toPlainString())
                            }
                            setResult(RESULT_OK, resultIntent)

                        } else {
                            Toast.makeText(this@ConverterActivity, "Não foi possível obter a cotação.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@ConverterActivity, "Erro na chamada da API: ${response.code()}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnPerformConversion.isEnabled = true
                    Toast.makeText(this@ConverterActivity, "Erro: ${e.message}", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
            }
        }
    }

    private fun hasSufficientFunds(currency: String, amount: BigDecimal): Boolean {
        return when (currency) {
            "BRL" -> realBalance >= amount
            "USD" -> dollarBalance >= amount
            "BTC" -> bitcoinBalance >= amount
            else -> false
        }
    }

    private fun updateBalancesAfterConversion(
        originCurrency: String,
        destCurrency: String,
        amountToDeduct: BigDecimal,
        amountToAdd: BigDecimal
    ) {
        when (originCurrency) {
            "BRL" -> realBalance = realBalance.subtract(amountToDeduct)
            "USD" -> dollarBalance = dollarBalance.subtract(amountToDeduct)
            "BTC" -> bitcoinBalance = bitcoinBalance.subtract(amountToDeduct)
        }

        when (destCurrency) {
            "BRL" -> realBalance = realBalance.add(amountToAdd)
            "USD" -> dollarBalance = dollarBalance.add(amountToAdd)
            "BTC" -> bitcoinBalance = bitcoinBalance.add(amountToAdd)
        }
        realBalance = realBalance.setScale(2, BigDecimal.ROUND_HALF_UP)
        dollarBalance = dollarBalance.setScale(2, BigDecimal.ROUND_HALF_UP)
        bitcoinBalance = bitcoinBalance.setScale(4, BigDecimal.ROUND_HALF_UP)
    }
    override fun onBackPressed() {
        val resultIntent = Intent().apply {
            putExtra("updatedRealBalance", realBalance.toPlainString())
            putExtra("updatedDollarBalance", dollarBalance.toPlainString())
            putExtra("updatedBitcoinBalance", bitcoinBalance.toPlainString())
        }
        setResult(RESULT_OK, resultIntent)
        super.onBackPressed()
    }
}