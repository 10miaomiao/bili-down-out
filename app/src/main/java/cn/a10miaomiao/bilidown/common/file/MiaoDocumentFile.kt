package cn.a10miaomiao.bilidown.common.file

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import androidx.annotation.RequiresApi
import androidx.documentfile.provider.DocumentFile
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader

class MiaoDocumentFile(
    val context: Context,
    val documentFile: DocumentFile,
): MiaoFile {

    companion object {
        const val DOC_AUTHORITY = "com.android.externalstorage.documents"
        val externalStorage = Environment.getExternalStorageDirectory()

        @JvmStatic
        fun getFolderUri(id: String, tree: Boolean): Uri {
            var rootId: String = id
//            var path = ""
//            if (id.startsWith("primary:Android/data/")) {
//                val i = id.indexOf("/", 21)
//                rootId = id.substring(0, i)
//                path = id.replace(":", "%3A").replace("/", "%2F")
//            }
//            Log.d("getFolderUri", rootId + path)
            val uri = if (tree){
                DocumentsContract.buildTreeDocumentUri(DOC_AUTHORITY, rootId)
            } else {
                DocumentsContract.buildDocumentUri(DOC_AUTHORITY, rootId)
            }
            return uri
//            Log.d("getFolderUri", uri.toString())
//            return Uri.parse(uri.toString() + "/document/" + path)
        }

        @JvmStatic
        @RequiresApi(Build.VERSION_CODES.O)
        fun requestFolderPermission(activity: Activity, requestCode: Int, id: String) {
            val i = getUriOpenIntent(getFolderUri(id, false))

            val flags = Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            i.addFlags(flags)

            activity.startActivityForResult(i, requestCode)
        }

        @JvmStatic
        @RequiresApi(Build.VERSION_CODES.O)
        fun getUriOpenIntent(uri: Uri): Intent {
            return Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                .putExtra("android.provider.extra.SHOW_ADVANCED", true)
                .putExtra("android.content.extra.SHOW_ADVANCED", true)
                .putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri)
        }

        @JvmStatic
        fun checkFolderPermission(context: Context, id: String): Boolean {
            return if (atLeastR()) {
                val treeUri: Uri = getFolderUri(id, true)
                //Log.e(TAG, "treeUri:" + treeUri)
                isInPersistedUriPermissions(context, treeUri)
            } else {
                true
            }
        }

        @JvmStatic
        @RequiresApi(Build.VERSION_CODES.KITKAT)
        fun isInPersistedUriPermissions(context: Context, uri: Uri): Boolean {
            val pList = context.contentResolver.persistedUriPermissions
            //Log.e(TAG, "pList:" + pList.size)
            for (uriPermission in pList) {
                //Log.e(TAG, "uriPermission:$uriPermission")
                if (uriPermission.uri == uri && (uriPermission.isReadPermission || uriPermission.isWritePermission)) {
                    return true
                } else {
                    //Log.e(TAG, "up:" + uriPermission.uri)
                }
            }
            return false
        }

        fun atLeastTiramisu(): Boolean {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        }

        fun atLeastR(): Boolean {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
        }
    }

    constructor(
        context: Context,
        id: String,
        path: String,
    ): this(
        context,
        DocumentFile.fromTreeUri(
            context, getFolderUri(id, true)
        )!!.let {
            DocumentFile.fromTreeUri(
                context,
                Uri.parse(it.uri.toString() + path.replace("/", "%2F"))
            )!!
        }
    )

    constructor(
        context: Context,
        documentFile: DocumentFile,
        path: String,
    ): this(
        context,
        DocumentFile.fromTreeUri(
            context,
            Uri.parse(documentFile.uri.toString() + path.replace("/", "%2F"))
        )!!
    )

    fun open() {
        context.contentResolver.openOutputStream(documentFile!!.uri)
    }

    override val path: String
        get() = documentFile.uri.toString()

    override val isDirectory: Boolean
        get() = documentFile.isDirectory

    override val name: String
        get() = documentFile.name ?: ""

    override fun exists(): Boolean {
        return documentFile.exists()
    }

    override fun listFiles(): List<MiaoFile> {
        return documentFile.listFiles().map {
            MiaoDocumentFile(context, it)
        }
    }

    override fun canRead(): Boolean {
        return documentFile.canRead()
    }

    override fun readText(): String {
//        try {
            val fileUri = documentFile.uri
            //DocumentFile输入流

            val inputStream = context.contentResolver.openInputStream(fileUri)
            val text = BufferedReader(InputStreamReader(inputStream)).useLines { lines ->
                val results = StringBuilder()
                lines.forEach { results.append(it) }
                results.toString()
            }
            return text
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
    }

    suspend fun copyToTemp(
        tempFile: File
    ) {
        val fileUri = documentFile.uri
        val fileInputStream = context.contentResolver.openInputStream(fileUri)!!
        val fileOutputStream = FileOutputStream(tempFile)
        val buffer = ByteArray(1024)
        var byteRead: Int
        while (-1 != fileInputStream.read(buffer).also { byteRead = it }) {
            fileOutputStream.write(buffer, 0, byteRead)
        }
        fileInputStream.close()
        fileOutputStream.flush()
        fileOutputStream.close()
    }

}