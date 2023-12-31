package com.chihiro.mpay_plugin

import android.app.Activity
import android.content.Context
import android.os.AsyncTask
import com.alipay.sdk.app.EnvUtils
import com.alipay.sdk.app.PayTask
import com.chihiro.mpay_plugin.handlers.WXAPiHandler
import com.macau.pay.sdk.OpenSdk
import com.macau.pay.sdk.util.Logger
import com.tencent.mm.opensdk.modelbiz.WXOpenBusinessWebview
import com.tencent.mm.opensdk.modelpay.PayReq

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import java.lang.ref.WeakReference




/** MpayPlugin */
class MpayPlugin : FlutterPlugin, MethodCallHandler, ActivityAware {

    private lateinit var channel: MethodChannel

    // 上下文 Context
    private lateinit var mContext: Context
    private var mActivity: Activity? = null
    private var initializationParams: Map<String, Any>? = null
    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "mpay_plugin")
        channel.setMethodCallHandler(this)
        mContext = flutterPluginBinding.applicationContext
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "getPlatformVersion" -> {
                result.success("Android ${android.os.Build.VERSION.RELEASE}")
            }

            "mPay" -> {
                val mPayHandler = MPayHandler(result, mActivity!!)
                /// 接收Flutter端支付參數
                val arguments = call.arguments as Map<*, *>
                // 支付參數
                val data = arguments["data"] as String
                // 支付通道，當前android用不上
                var channel = arguments["channel"] as Int
                mPayHandler.pay(data)
                return
            }

            "init" -> {
                setInitializationParams(call.arguments as Map<String, Any>)
                return
            }
            /// 純支付寶支付，不走mPay通道
            "aliPay" -> {
                val payInfo = call.argument<String>("payInfo")
                pay(mActivity, payInfo, result)
            }
            "wechatPay" ->{
                payWechat(call, result)
            }
            "wechatPayHongKongWallet" ->{
                payWithHongKongWallet(call, result)
            }
            else -> {
                result.notImplemented()
            }
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        mActivity = binding.activity
    }

    override fun onDetachedFromActivityForConfigChanges() {
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    }

    override fun onDetachedFromActivity() {
    }

    /**
     * 使用支付寶直接支付，不通過mpay通道
     * @param activity 生命週期context
     * @param payInfo 支付信息
     * @param callback 支付回調
     */
    private fun aliPay(activity: Activity?, payInfo: String?, callback: Result) {
        val activityRef = WeakReference(activity)
        val mActivity = activityRef.get()
        val alipay = PayTask(mActivity)
        try {
            val result = alipay.payV2(payInfo, true)
            callback.success(result)
        } catch (e: Exception) {
            val result: MutableMap<String?, String?> =
                HashMap()
            result["resultStatus"] = "-1"
            result["result"] = "支付失敗"
            result["memo"] = e.message
            result["type"] = "openSDK"
            callback.success(result)
        }
    }

    fun pay(currentActivity: Activity?, payInfo: String?, callback: Result) {
        object : AsyncTask<String?, Any?, Map<String?, String?>>() {
            override fun onPostExecute(result: Map<String?, String?>) {
                val error = result["\$error"]
                if (error != null) {
                    callback.error(error, "支付发生错误", null)
                } else {
                    callback.success(result)
                }
            }

            override fun doInBackground(vararg params: String?): Map<String?, String?> {
                return try {
                    val alipay = PayTask(currentActivity)
                    alipay.payV2(payInfo, true)
                } catch (e: Exception) {
                    val result: MutableMap<String?, String?> =
                        HashMap()
                    result["\$error"] = e.message
                    result
                }
            }
        }.execute()


//    Thread payThread = new Thread(payRunnable);
//    payThread.start();
    }

    private fun initializePlugin(params: Map<String, Any>) {
        /// 支付宝环境
        val envEnum = params["aliEnv"] as Int
        /// Mpay 生产或者测试环境
        val envType = params["mpyEnv"] as Int
        if (envEnum == 0) {
            EnvUtils.setEnv(EnvUtils.EnvEnum.ONLINE)

        } else {
            EnvUtils.setEnv(EnvUtils.EnvEnum.SANDBOX)
        }
        OpenSdk.setMPayAppId(envType)
        OpenSdk.setEnvironmentType(envType)
        Logger.i("支付宝环境：$envEnum")
        Logger.i("Mpay环境：$envType")
    }

    private fun setInitializationParams(params: Map<String, Any>) {
        // 则设置参数并初始化插件
        initializationParams = params
        initializationParams?.let { initializePlugin(it) }
    }

    private fun payWechat(call: MethodCall, result: Result) {

        if (WXAPiHandler.wxApi == null) {
            result.error("Unassigned WxApi", "please config wxapi first", null)
            return
        } else {
            // 将该app注册到微信
            val request = PayReq()
            request.appId = call.argument("appId")
            request.partnerId = call.argument("partnerId")
            request.prepayId = call.argument("prepayId")
            request.packageValue = call.argument("packageValue")
            request.nonceStr = call.argument("nonceStr")
            request.timeStamp = call.argument<Long>("timeStamp").toString()
            request.sign = call.argument("sign")
            request.signType = call.argument("signType")
            request.extData = call.argument("extData")
            val done = WXAPiHandler.wxApi?.sendReq(request)
            result.success(done)
        }
    }
    private fun payWithHongKongWallet(call: MethodCall, result: Result) {
        val prepayId = call.argument<String>("prepayId") ?: ""
        val request = WXOpenBusinessWebview.Req()
        request.businessType = 1
        request.queryInfo = hashMapOf(
            "token" to prepayId
        )
        result.success(WXAPiHandler.wxApi?.sendReq(request))
    }
}
