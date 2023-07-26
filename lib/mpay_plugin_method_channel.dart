import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'mpay_plugin_platform_interface.dart';

/// An implementation of [MpayPluginPlatform] that uses method channels.
class MethodChannelMpayPlugin extends MpayPluginPlatform {
  @visibleForTesting
  final methodChannel = const MethodChannel('mpay_plugin');

  @override
  Future<String?> getPlatformVersion() async {
    final version =
        await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }

  @override
  Future<Map> mPay(
    String? data, {
    PayChannel channel = PayChannel.mPay,
    String? withScheme,
  }) async {
    var response = await methodChannel.invokeMethod<dynamic>('mPay', {
      "data": data,
      "channel": channel.value,
      "withScheme": withScheme,
    });
    return response;
  }

  @override
  Future<Map> aliPay(String payInfo, String setIosUrlSchema) async {
    var response = await methodChannel.invokeMethod<dynamic>('aliPay', {
      "payInfo": payInfo,
      "setIosUrlSchema": setIosUrlSchema,
    });
    return response;
  }

  @override
  Future<Map> wechatPay(String payInfo) async {
    var response = await methodChannel
        .invokeMethod<dynamic>('wechatPay', {"payInfo": payInfo});
    return response;
  }

  @override
  Future<void> init(
      {AliPayEnv envEnum = AliPayEnv.ONLINE,
      EnvType envType = EnvType.PRODUCTION}) async {
    await methodChannel.invokeMethod<void>('init', {
      'aliEnv': envEnum.value,
      'mpyEnv': envType.value,
    });
  }
}
