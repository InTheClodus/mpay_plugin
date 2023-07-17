package com.chihiro.mpay_plugin

import android.app.Activity
import com.macau.pay.sdk.OpenSdk
import com.macau.pay.sdk.base.PayResult
import com.macau.pay.sdk.interfaces.OpenSdkInterfaces
import io.flutter.plugin.common.MethodChannel.Result
import java.lang.ref.WeakReference
import com.macau.pay.sdk.util.Logger

class MPayHandler(private val result: Result, activity: Activity) : OpenSdkInterfaces {
    private val mActivity: WeakReference<Activity> = WeakReference(activity)


    fun pay(data: String?) {
        /// 调用支付
        val activity = mActivity.get()
        if (activity != null) {
            try {
                Logger.i("支付參數 ----$data")
                OpenSdk.newPayAll(activity, data, this)
            } catch (e: Throwable) {
                val successMap = mutableMapOf<String, Any?>()
                successMap["resultStatus"] = "-1"
                successMap["result"] = "支付失敗"
                successMap["memo"] = "支付異常信息：${e.message}"

                result.success(successMap)
                e.printStackTrace()
            }
        } else {
            // mActivity引用的Activity对象已经被回收，处理这种情况
        }
    }

    override fun OpenSDKInterfaces(payResult: PayResult?) {
        /// 接收OpenSDK支付結果
        Logger.i("OpenSDK支付結果 ----${payResult}")
        val successMap = mutableMapOf<String, Any?>()
        successMap["resultStatus"] = payResult?.resultStatus
        successMap["result"] = payResult?.result
        successMap["memo"] = payResult?.memo

        result.success(successMap)

    }

    override fun AliPayInterfaces(payResult: PayResult?) {
        /// 接收支付寶支付結果
        Logger.i("支付寶支付結果 ----$payResult")
        val successMap = mutableMapOf<String, Any?>()
        successMap["resultStatus"] = payResult?.resultStatus
        successMap["result"] = payResult?.result
        successMap["memo"] = payResult?.memo

        result.success(successMap)
    }

    override fun MPayInterfaces(payResult: PayResult?) {
        /// 接收Mpay支付結果
        Logger.i("Mpay支付結果 ----$payResult")
        val successMap = mutableMapOf<String, Any?>()
        successMap["resultStatus"] = payResult?.resultStatus
        successMap["result"] = payResult?.result
        successMap["memo"] = payResult?.memo

        result.success(successMap)
    }

    override fun WeChatPayInterfaces(payResult: PayResult?) {
        /// 接收微信支付結果
        Logger.i("微信支付結果 ----$payResult")
        val successMap = mutableMapOf<String, Any?>()
        successMap["resultStatus"] = payResult?.resultStatus
        successMap["result"] = payResult?.result
        successMap["memo"] = payResult?.memo

        result.success(successMap)
    }
}
