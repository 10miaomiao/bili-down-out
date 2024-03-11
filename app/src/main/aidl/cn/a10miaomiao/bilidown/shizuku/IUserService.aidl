package cn.a10miaomiao.bilidown.shizuku;

import cn.a10miaomiao.bilidown.entity.BiliDownloadEntryAndPathInfo;
import cn.a10miaomiao.bilidown.callback.ProgressCallback;

interface IUserService {

    void destroy() = 16777114; // Destroy method defined by Shizuku server

    void exit() = 1; // Exit method defined by user

    String doSomething() = 2;

    List<BiliDownloadEntryAndPathInfo> readDownloadList(String path) = 3;

    List<BiliDownloadEntryAndPathInfo> readDownloadDirectory(String path) = 4;

    String exportBiliVideo(String entryDirPath, String outFilePath, ProgressCallback callback) = 5;
}