
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.android.volley.AuthFailureError
import com.android.volley.Response
import com.android.volley.VolleyLog
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.*


interface ServiceInterface {
    fun postRequest(path: String, params: JSONObject, completionHandler: (response: JSONObject?) -> Unit)
}

class FxRate(c: Context, currencyViewModel: CurrencyViewModel) : ServiceInterface {

    private var context:Context?=null
    private var currencyViewModel:CurrencyViewModel?=null

    init {
        this.context = c
        this.currencyViewModel = currencyViewModel
    }

    fun requestFxData(currency: List<Currency>, defaultSaveCurrency: Currency?) {

        var isCurrencyObjOrVoucher:Boolean?=false

        var sb = StringBuffer()
        sb.append(defaultSaveCurrency!!.code).append("/").append("USD").append(",").append("USD").append("/").append(defaultSaveCurrency!!.code)

        for (c in currency){
            if(!c!!.currencySelectedForRates!! )
                sb.append(",").append(c.code).append("/").append("USD").append(",").append("USD").append("/").append(c.code)
        }

        val ccyList = sb.toString()

        ccyList.splitToSequence(",").forEach { it ->
            val jsonObj = JSONObject()
            jsonObj.accumulate("symbol", it)
        }

        val ccyPairs = JSONArray()
        ccyList.splitToSequence(",").forEach { it ->
            val jsonObj = JSONObject()
            jsonObj.accumulate("symbol", it)
            ccyPairs.put(jsonObj)
        }

        val jsonBody = JSONObject()
        jsonBody.put("requestId", UUID.randomUUID())
        jsonBody.put("currencyPairs", ccyPairs)

        postRequest("", jsonBody) { it ->
            Log.d("Fx rate response ", it.toString())
            val jsonArray = it?.getJSONArray("currencyPairs")
            if (jsonArray != null) {
                (0..(jsonArray.length()-1))
                        .map { jsonArray[it] as JSONObject }
                        .forEach { savePrice(it) }
            }
        }
    }

    private fun savePrice(obj: JSONObject) {

        try {

            if (obj != null && obj.get("priceStatus") == ("VALID")) {
                var ccyPairCode = obj.get("symbol").toString()
                var isInverseRate = true
                if (ccyPairCode.substring(0, 3) == ("USD")) {
                    isInverseRate = false
                }
                val code = ccyPairCode.replace("/", "").replace("USD", "")

                var fxRate = if (isInverseRate) {
                    obj.get("price").toString().toDouble()
                } else {
                    1 / obj.get("price").toString().toDouble()
                }

                launch {
                    val r = async {
                        val savedCcyCurrency = currencyViewModel!!.getCurrencyByCode(code)
                        if (!savedCcyCurrency.currencySelectedForRates!!)
                            savedCcyCurrency.fxRate = fxRate
                        currencyViewModel!!.updateCurrency(savedCcyCurrency)
                    }.await()
                }
            }
        }catch (e:Exception){
            Logger.printD("TAG","Might be Database issue..")
        }
    }

    override fun postRequest(path: String, params: JSONObject, completionHandler: (response: JSONObject?) -> Unit) {
        val url = "url"
        val tag = "Demo"
        val jsonObjReq = object : JsonObjectRequest(Method.POST, url, null, Response.Listener<JSONObject> { response ->
            Log.d(tag, "/post request OK! Response: $response")
            completionHandler(response)
        }, Response.ErrorListener { error ->
            VolleyLog.e(tag, "/post request fail! Error: ${error.message}")
            completionHandler(null)
        }) {
            @Throws(AuthFailureError::class) override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json"
                headers["Accept"] = "application/json"
                headers["apiKey"] = "udcahminseaskdfsgvxnf9183duv082kj/gma15"
                headers["clientId"] = "mpricemobile"
                return headers
            }
            override fun getBody(): ByteArray {
                return params.toString().toByteArray(Charsets.UTF_8)
            }
        }
        App.INSTANCE?.addToRequestQueue(jsonObjReq, tag)
    }
}