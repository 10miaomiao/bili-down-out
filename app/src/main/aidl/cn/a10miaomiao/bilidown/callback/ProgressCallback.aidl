// ProgressCallback.aidl
package cn.a10miaomiao.bilidown.callback;

import cn.a10miaomiao.bilidown.entity.VideoOutInfo;

interface ProgressCallback {
    void onStart(in VideoOutInfo info);
    void onFinish(in VideoOutInfo info);
    void onCancel(in VideoOutInfo info);
    void onProgress(in VideoOutInfo info, int progress, long progressTime);
    void onError(in VideoOutInfo info, String message);
}