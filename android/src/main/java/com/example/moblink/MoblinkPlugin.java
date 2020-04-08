package com.example.moblink;

import android.app.Activity;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import com.mob.MobSDK;
import com.mob.moblink.ActionListener;
import com.mob.moblink.MobLink;
import com.mob.moblink.RestoreSceneListener;
import com.mob.moblink.Scene;
import com.mob.moblink.SceneRestorable;
import com.mob.tools.utils.Hashon;
import com.mob.tools.utils.SharePrefrenceHelper;
import java.util.HashMap;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/**
 * MoblinkPlugin
 */
public class MoblinkPlugin extends Object implements MethodCallHandler, SceneRestorable {
    private static final String getMobId = "getMobId";
    private static final String restoreScene = "restoreScene";

    private static final String EventChannel = "JAVA_TO_FLUTTER";
    private static EventChannel.EventSink mEventSink;
    private static Activity activity;
    private SharePrefrenceHelper sp;
    private static final String SP_NAME = "MoblinkPlugin";
    private static final String SP_KEY_PATH = "path";
    private static final String SP_KEY_PARAMS = "params";
    private static final String SP_VALUE_CLEAN = "clean";
    private static HashMap<String, Object> onReturnSceneDataMap;

    private MoblinkPlugin(Activity activity) {
        Log.e("WWW", " MoblinkPlugin构造方法 ");
        if (MobSDK.getContext() != null) {
            sp = new SharePrefrenceHelper(MobSDK.getContext());
            sp.open(SP_NAME);
        } else {
            sp = new SharePrefrenceHelper(activity.getApplication().getApplicationContext());
            sp.open(SP_NAME);
        }

        //场景还原监听
        MobLink.setRestoreSceneListener(new SceneListener());
    }

    //Java代码
    class SceneListener extends Object implements RestoreSceneListener {
        @Override
        public Class<? extends Activity> willRestoreScene(Scene scene) {
            Log.e("WWW", " willRestoreScene path===> " + scene.getPath() + " params===> " + scene.getParams());
            onReturnSceneDataMap = new HashMap<>();
            onReturnSceneDataMap.put("path", scene.getPath());
            onReturnSceneDataMap.put("params", scene.getParams());
            try {
                String pathStr = scene.getPath();
                if (!TextUtils.isEmpty(pathStr)) {
                    put(SP_KEY_PATH, pathStr);
                    Log.e("WWW", "onReturnSceneData  SP存入了还原的场景信息path " + pathStr);
                }
            } catch (Throwable t) {
                Log.e("WWW", " onReturnSceneData catch 前端path传入的类型需要是String类型 " + t);
            }

            try {
                HashMap<String, Object> paramsMap = scene.getParams();
                if (paramsMap != null) {
                    String value = String.valueOf(new Hashon().fromHashMap(paramsMap));
                    put(SP_KEY_PARAMS, value);
                    Log.e("WWW", "onReturnSceneData  SP存入了还原的参数信息Params " + new Hashon().fromHashMap(paramsMap));
                }
            } catch (Throwable t) {
                Log.e("WWW", " onReturnSceneData catch 前端params传入的类型需要是HashMap<String, Object> 类型 " + t);
            }
            restoreScene();
            return null;
        }

        @Override
        public void notFoundScene(Scene scene) {
            //TODO 未找到处理scene的activity时回调
        }

        @Override
        public void completeRestore(Scene scene) {
            // TODO 在"拉起"处理场景的Activity之后调用
        }
    }

    private void put(String key, String value) {
        sp.putString(key, value);
    }

    private void putClean(String key, Object object) {
        sp.put(key, object);
    }

    private String getSPString(String key) {
        return sp.getString(key);
    }

    /**
     * 提供给java层传递数据到flutter层的方法
     **/
    public void setEvent(Object data) {
        if (mEventSink != null) {
            mEventSink.success(data);
        } else {
            Log.e("WWW", " ===== FlutterEventChannel.eventSink 为空 需要检查一下 ===== ");
        }
    }

    /**
     * Plugin registration.
     */
    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "com.yoozoo.mob/moblink");
        channel.setMethodCallHandler(new MoblinkPlugin(registrar.activity()));
        activity = registrar.activity();

   /* final EventChannel eventChannel = new EventChannel(registrar.messenger(), EventChannel);
    MoblinkPlugin instance = new MoblinkPlugin(registrar.activity());
    eventChannel.setStreamHandler((io.flutter.plugin.common.EventChannel.StreamHandler) instance);*/

        // MobLink.setActivityDelegate(activity, MoblinkPlugin.this);
        Log.e("WWW", " registerWith() ");
        final EventChannel eventChannel = new EventChannel(registrar.messenger(), EventChannel);
        eventChannel.setStreamHandler(new EventChannel.StreamHandler() {
            @Override
            public void onListen(Object o, EventChannel.EventSink eventSink) {
                Log.e("WWW", " onListen===mEventSink不为null");
                mEventSink = eventSink;
            }

            @Override
            public void onCancel(Object o) {

            }
        });
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        Log.e("WWW", " onMethodCall() ");
        MobLink.setActivityDelegate(activity, MoblinkPlugin.this);
        switch (call.method) {
            case getMobId:
                getMobId(call, result);
                break;
            case restoreScene:
                restoreScene(result);
                break;
        }

    }

    private void getMobId(MethodCall call, final Result result) {
        HashMap<String, Object> map = call.arguments();
        HashMap<String, Object> params = (HashMap<String, Object>) map.get("params");
        String path = String.valueOf(map.get("path"));

        // 新建场景
        Scene s = new Scene();
        s.path = path;
        s.params = params;

        // 请求场景ID
        MobLink.getMobID(s, new ActionListener() {
            @Override
            public void onResult(Object o) {
                Log.e("WWW", " onResult ===> " + o);
                HashMap resposon = new HashMap<String, String>();
                resposon.put("mobid", o);
                resposon.put("domain", "");
                result.success(resposon);
            }

            public void onError(Throwable throwable) {
                result.error(throwable.getMessage().toString(), null, null);
            }
        });
    }

    private void restoreScene(Result result) {
        Log.e("WWW", " 测试result " + result);
        if (result != null) {
            Log.e("WWW", " result != null ");
            if (onReturnSceneDataMap != null) {
                Log.e("WWW", "onReturnSceneDataMap != null" + onReturnSceneDataMap.toString());
                result.success(onReturnSceneDataMap);
                onReturnSceneDataMap = null;
                Log.e("WWW", " onReturnSceneDataMap 回调成功");
            } else if (sp != null) {
                Log.e("WWW", "sp != null");
                onReturnSceneDataMap = new HashMap<>();
                try {
                    String pathStr = getSPString(SP_KEY_PATH);
                    if (!pathStr.equals(SP_VALUE_CLEAN)) {
                        if (!TextUtils.isEmpty(pathStr)) {
                            onReturnSceneDataMap.put("path", pathStr);
                            Log.e("WWW", " restoreScene 取出SP中的path放入回调 path===> " + pathStr);
                            putClean(SP_KEY_PATH, SP_VALUE_CLEAN);
                            Log.e("WWW", " restoreScene 清空SP中的path");
                        }
                    }

                    String paramsMapStr = getSPString(SP_KEY_PARAMS);
                    Log.e("WWW", " paramsMapStr ===> " + paramsMapStr);
                    if (!paramsMapStr.equals(SP_VALUE_CLEAN)) {
                        //HashMap<String, Object> paramsMap = (HashMap<String, Object>) getSPObject(SP_KEY_PARAMS);
                        if (!TextUtils.isEmpty(paramsMapStr)) {
                            onReturnSceneDataMap.put("params", paramsMapStr);
                            Log.e("WWW", " restoreScene 取出SP中的params放入回调 params===> " + paramsMapStr);
                            putClean(SP_KEY_PARAMS, SP_VALUE_CLEAN);
                            Log.e("WWW", " restoreScene 清空SP中的path");
                        }
                    }
                } catch (Throwable t) {
                    Log.e("WWW", " restoreScene 补充取值的时候异常可以忽略" + t);
                }
                if (onReturnSceneDataMap != null && onReturnSceneDataMap.size() > 0) {
                    result.success(onReturnSceneDataMap);
                    Log.e("WWW", " result.success(onReturnSceneDataMap)  ===> " +
                            "path===> " + onReturnSceneDataMap.get("path") +
                            " params===>  " + onReturnSceneDataMap.get("params"));
                } else {
                    Log.e("WWW", " onReturnSceneDataMap ====> " + onReturnSceneDataMap.size());
                }
            } else {
                Log.e("WWW", " onReturnSceneDataMap 为空 不需要回调");
            }
        }
    }

    @Override
    public void onReturnSceneData(Scene scene) {
        Log.e("WWW", " onReturnSceneData path===> " + scene.getPath() + " params===> " + scene.getParams());
        onReturnSceneDataMap = new HashMap<>();
        onReturnSceneDataMap.put("path", scene.getPath());
        onReturnSceneDataMap.put("params", scene.getParams());

        try {
            String pathStr = scene.getPath();
            if (!TextUtils.isEmpty(pathStr)) {
                put(SP_KEY_PATH, pathStr);
                Log.e("WWW", "onReturnSceneData  SP存入了还原的场景信息path " + pathStr);
            }
        } catch (Throwable t) {
            Log.e("WWW", " onReturnSceneData catch 前端path传入的类型需要是String类型 " + t);
        }


        try {
            HashMap<String, Object> paramsMap = scene.getParams();
            if (paramsMap != null) {
                String value = String.valueOf(new Hashon().fromHashMap(paramsMap));
                put(SP_KEY_PARAMS, value);
                Log.e("QQQ", "onReturnSceneData  SP存入了还原的参数信息Params " + new Hashon().fromHashMap(paramsMap));
            }
        } catch (Throwable t) {
            Log.e("QQQ", " onReturnSceneData catch 前端params传入的类型需要是HashMap<String, Object> 类型 " + t);
        }
    }

    private void restoreScene() {
        if (mEventSink != null) {
            if (onReturnSceneDataMap != null) {
                mEventSink.success(onReturnSceneDataMap);
                onReturnSceneDataMap = null;
            } else if (sp != null) {
                onReturnSceneDataMap = new HashMap<>();
                try {
                    String pathStr = getSPString(SP_KEY_PATH);
                    if (!pathStr.equals(SP_VALUE_CLEAN)) {
                        if (!TextUtils.isEmpty(pathStr)) {
                            onReturnSceneDataMap.put("path", pathStr);
                            Log.e("WWW", " restoreScene 取出SP中的path放入回调 path===> " + pathStr);
                            putClean(SP_KEY_PATH, SP_VALUE_CLEAN);
                            Log.e("WWW", " restoreScene 清空SP中的path");
                        }
                    }

                    String paramsMapStr = getSPString(SP_KEY_PARAMS);
                    Log.e("WWW", " paramsMapStr ===> " + paramsMapStr);
                    if (!paramsMapStr.equals(SP_VALUE_CLEAN)) {
                        //HashMap<String, Object> paramsMap = (HashMap<String, Object>) getSPObject(SP_KEY_PARAMS);
                        if (!TextUtils.isEmpty(paramsMapStr)) {
                            onReturnSceneDataMap.put("params", paramsMapStr);
                            Log.e("WWW", " restoreScene 取出SP中的params放入回调 params===> " + paramsMapStr);
                            putClean(SP_KEY_PARAMS, SP_VALUE_CLEAN);
                            Log.e("WWW", " restoreScene 清空SP中的path");
                        }
                    }
                } catch (Throwable t) {
                    Log.e("WWW", " restoreScene 补充取值的时候异常可以忽略" + t);
                }
                if (onReturnSceneDataMap != null && onReturnSceneDataMap.size() > 0) {
                    mEventSink.success(onReturnSceneDataMap);
                    Log.e("WWW", " result.success(onReturnSceneDataMap)  ===> " +
                            "path===> " + onReturnSceneDataMap.get("path") +
                            " params===>  " + onReturnSceneDataMap.get("params"));
                } else {
                    Log.e("WWW", " onReturnSceneDataMap ====> " + onReturnSceneDataMap.size());
                }
            } else {
                Log.e("WWW", " onReturnSceneDataMap 为空 不需要回调");
            }
        }
    }
}
