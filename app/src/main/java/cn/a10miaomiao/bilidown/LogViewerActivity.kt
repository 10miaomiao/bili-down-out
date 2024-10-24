package cn.a10miaomiao.bilidown

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cn.a10miaomiao.bilidown.ui.theme.BiliDownTheme

class LogViewerActivity : ComponentActivity() {

    private val mLogSummary by lazy {
        intent.getStringExtra("log_summary") ?: "null"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BiliDownTheme {
                LogViewerView()
            }
        }
    }

    private fun copyLogText() {
        val logSummary = mLogSummary
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("log", logSummary)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "复制成功(●'◡'●)", Toast.LENGTH_SHORT).show()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun LogViewerView() {
        val scrollState = rememberScrollState()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(text = stringResource(id = R.string.log_viewer))
                    },
                    actions = {
                        TextButton(onClick = ::copyLogText) {
                            Text(
                                text = stringResource(id = R.string.copy_log)
                            )
                        }
                    }
                )
            },
            content = { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(innerPadding)
                        .padding(10.dp)
                ) {
                    SelectionContainer {
                        Text(
                            text = mLogSummary
                        )
                    }
                }
            }
        )
    }
}