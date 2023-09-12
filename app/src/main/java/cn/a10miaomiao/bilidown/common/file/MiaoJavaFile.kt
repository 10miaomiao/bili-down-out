package cn.a10miaomiao.bilidown.common.file

import java.io.File

class MiaoJavaFile(
    private val file: File
) : MiaoFile {

    constructor(
        pathName: String
    ) : this(File(pathName))

    override val path: String
        get() = file.path

    override val isDirectory: Boolean
        get() = file.isDirectory

    override fun exists(): Boolean {
        return file.exists()
    }

    override fun listFiles(): List<MiaoFile> {
        return file.listFiles().map {
            MiaoJavaFile(it)
        }
    }

    override fun canRead(): Boolean {
        return file.canRead()
    }

    override fun readText(): String {
        return file.readText()
    }


}