package com.hphuc5453.stripe3dseruce2

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.hphuc5453.stripe3dseruce2.databinding.CheckoutActivityBinding
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.utils.ContextUtils.packageInfo
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class CheckoutActivity : AppCompatActivity() {
    private val backendUrl = "https://buttered-hickory-devourer.glitch.me"
    private val STRIPE_API_KEY =
        "pk_test_51MYOyZDkv9IZNzQZGiU75MUVmgdVzfWReA73vOeY3xr4UkIQRMm8T8w8zaNCaqvqUHkvfVhqpEsDVnNWQi4q3C6G00zHA45Pju"

    private lateinit var binding: CheckoutActivityBinding

    private lateinit var paymentIntentClientSecret: String
    private lateinit var paymentSheet: PaymentSheet

    private fun initStripe() {
        PaymentConfiguration.init(applicationContext, STRIPE_API_KEY)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = CheckoutActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initStripe()
        // Hook up the pay button
        binding.payButton.setOnClickListener(::onPayClicked)
        binding.payButton.isEnabled = false
        binding.cardMultiline.postalCodeRequired = false

        paymentSheet = PaymentSheet(this, ::onPaymentSheetResult)

        binding.createButton.setOnClickListener(::fetchPaymentIntent)
    }

    private fun fetchPaymentIntent(view: View) {
        binding.status.text = "Retrieved payment ...."
        binding.createButton.isEnabled = false

        val url = "$backendUrl/create-payment-intent"

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val request = Request.Builder()
            .url(url)
            .post("".toRequestBody(mediaType))
            .build()

        OkHttpClient()
            .newCall(request)
            .enqueue(object: Callback {
                override fun onFailure(call: Call, e: IOException) {
                    binding.createButton.isEnabled = true
                    binding.status.text = "Failed to load data"
                }

                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        binding.createButton.isEnabled = true
                        binding.status.text = "Failed to load page"
                    } else {
                        val responseData = response.body?.string()
                        val responseJson = responseData?.let { JSONObject(it) } ?: JSONObject()
                        paymentIntentClientSecret = responseJson.getString("clientSecret")
                        runOnUiThread {
                            binding.payButton.isEnabled = true
                            binding.status.text = "Retrieved payment success"
                        }
                    }
                }
            })
    }

    private fun onPayClicked(view: View) {
        val configuration = PaymentSheet.Configuration("Merchant display name")

        // Present Payment Sheet
        paymentSheet.presentWithPaymentIntent(paymentIntentClientSecret, configuration)
    }

    private fun onPaymentSheetResult(paymentResult: PaymentSheetResult) {
        when (paymentResult) {
            is PaymentSheetResult.Completed -> {
                binding.status.text = "Payment complete!"
            }
            is PaymentSheetResult.Canceled -> {
                binding.status.text = "Payment canceled!"
            }
            is PaymentSheetResult.Failed -> {
                binding.status.text = "Payment failed! ${paymentResult.error.localizedMessage}"
            }
        }
    }
}