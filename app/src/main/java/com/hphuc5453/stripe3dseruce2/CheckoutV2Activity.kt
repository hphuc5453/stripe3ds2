package com.hphuc5453.stripe3dseruce2

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.hphuc5453.stripe3dseruce2.databinding.CheckoutV2ActivityBinding
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.payments.paymentlauncher.PaymentLauncher
import com.stripe.android.payments.paymentlauncher.PaymentResult
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class CheckoutV2Activity : AppCompatActivity() {
    private val backendUrl = "https://buttered-hickory-devourer.glitch.me"
    private val STRIPE_API_KEY =
        "pk_test_51MYOyZDkv9IZNzQZGiU75MUVmgdVzfWReA73vOeY3xr4UkIQRMm8T8w8zaNCaqvqUHkvfVhqpEsDVnNWQi4q3C6G00zHA45Pju"

    private lateinit var binding: CheckoutV2ActivityBinding

    private lateinit var paymentIntentClientSecret: String
    private lateinit var paymentLauncher: PaymentLauncher

    private fun initStripe() {
        PaymentConfiguration.init(applicationContext, STRIPE_API_KEY)
    }

    private fun fetchPaymentIntent() {
        binding.status.text = "Retrieved payment ...."

        val url = "$backendUrl/create-payment-intent"

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val request = Request.Builder()
            .url(url)
            .post("".toRequestBody(mediaType))
            .build()

        OkHttpClient()
            .newCall(request)
            .enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    binding.status.text = "Failed to load data"
                }

                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = CheckoutV2ActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initStripe()

        binding.payButton.isEnabled = false

        val paymentConfiguration = PaymentConfiguration.getInstance(applicationContext)
        paymentLauncher = PaymentLauncher.Companion.create(
            this,
            paymentConfiguration.publishableKey,
            paymentConfiguration.stripeAccountId,
            ::onPaymentResult
        )

        // Confirm the PaymentIntent with the card widget
        binding.cardMultiline.setShouldShowPostalCode(false)
        binding.payButton.setOnClickListener {
            binding.cardMultiline.paymentMethodCreateParams?.let { params ->
                val confirmParams = ConfirmPaymentIntentParams
                    .createWithPaymentMethodCreateParams(params, paymentIntentClientSecret)
                lifecycleScope.launch {
                    paymentLauncher.confirm(confirmParams)
                }
            }
        }

        fetchPaymentIntent()
    }

    private fun onPaymentResult(paymentResult: PaymentResult) {
        when (paymentResult) {
            is PaymentResult.Completed -> {
                binding.status.text = "Payment complete!"
            }
            is PaymentResult.Canceled -> {
                binding.status.text = "Payment canceled!"
            }
            is PaymentResult.Failed -> {
                binding.status.text = "Payment failed! ${paymentResult.throwable.localizedMessage}"
            }
        }
    }
}