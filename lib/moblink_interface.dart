import 'dart:async';
import 'dart:convert';
import 'package:flutter/services.dart';
import 'package:moblink/moblink_defines.dart';

class Moblink {
  static const MethodChannel _channel =
      const MethodChannel('com.yoozoo.mob/moblink');

  static const EventChannel java_to_flutter =
      const EventChannel("JAVA_TO_FLUTTER");

  static Future<dynamic> listenNativeEvent() {
    print("QQQ 我执行了");
    java_to_flutter
        .receiveBroadcastStream()
        .listen(_onEvent, onError: _onError);
    print("QQQ 我执行完了");
  }

  static Future<dynamic> _onEvent(Object event) {
    print("QQQ _onEvent");
    print("onEvent: $event ");
  }

  static Future<dynamic> _onError(Object error) {
    print("QQQ _onError");
    print(error);
  }

//get ShareSDK PrivacyPolicy
  static Future<dynamic> getPrivacyPolicy(
      String type, Function(Map data, Map error) result) {
    Map args = {"type": type};
    Future<dynamic> callback =
        _channel.invokeMethod(MobLinkMethods.getPrivacyPolicy.name, args);
    callback.then((dynamic response) {
      print(response);
      if (result != null) {
        result(response["data"], response["error"]);
      }
    });
    return callback;
  }

  ///upload user permissionStatus to Share
  static Future<dynamic> uploadPrivacyPermissionStatus(
      int status, Function(bool success) result) {
    Map args = {"status": status};
    Future<dynamic> callback = _channel.invokeMethod(
        MobLinkMethods.uploadPrivacyPermissionStatus.name, args);
    callback.then((dynamic response) {
      print(response);
      if (result != null) {
        result(response["success"]);
      }
    });
    return callback;
  }

  ///setPrivacyWindow Show
  static Future<dynamic> setAllowShowPrivacyWindow(int show) {
    Map args = {"show": show};
    return _channel.invokeMethod(
        MobLinkMethods.setAllowShowPrivacyWindow.name, args);
  }

  static Future<dynamic> setPrivacyUI(
      int backColor, List<int> operationButtonColors) {
    Map args = {
      "backColor": backColor,
      "oprationButtonColors": operationButtonColors
    };
    return _channel.invokeMethod(MobLinkMethods.setPrivacyUI.name, args);
  }

  static Future<dynamic> getMobId(MLSDKScene scene,
      Function(String mobid, String domain, MLSDKError error) result) {
    Map args = {"path": scene.path, "params": scene.params};

    Future<dynamic> callback =
        _channel.invokeMethod(MobLinkMethods.getMobId.name, args);

    callback.then((dynamic response) {
      if (result != null) {
        result(response["mobid"], response["domain"],
            MLSDKError(rawData: response["error"]));
      }
    });
    return callback;
  }

  static Future<dynamic> restoreScene() async {
    try {
      dynamic response =
          await _channel.invokeMethod(MobLinkMethods.restoreScene.name);
      print(response);
      String path = response["path"] ?? "";
      Map params;
      if (response['params'] is Map) {
        params = response['params'];
      } else {
        params = jsonDecode(response['params']);
      }
      MLSDKScene scenes = new MLSDKScene(path, params);
      scenes.mobid = response["mobid"] ?? "";
      scenes.className = response["className"];
      scenes.rawURL = response["rawURL"];
      return scenes;
    } catch (e) {}
  }
}

class MLSDKScene {
  // path (required)
  String path;

  // custom parameter (Optional)
  Map params;

  // mobid（Readonly）
  String mobid;

  // class name of the corresponding path（Readonly）
  String className;

  // original link（Readonly）
  String rawURL;

  // create scene
  MLSDKScene(this.path, this.params);
}
