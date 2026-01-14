package io.github.kgbis.remotecontrol.app.features.about

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.annotation.RawRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.halilibo.richtext.markdown.Markdown
import com.halilibo.richtext.ui.material3.RichText
import io.github.kgbis.remotecontrol.app.BuildConfig
import io.github.kgbis.remotecontrol.app.R
import io.github.kgbis.remotecontrol.app.ui.components.SectionCard

@SuppressLint("DiscouragedApi", "LocalContextResourcesRead")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    navController: NavController,
    aboutViewModel: AboutViewModel = viewModel()
) {

    val versionName = BuildConfig.VERSION_NAME
    val sections = aboutViewModel.aboutKeys
    val context = LocalContext.current

    val locale = LocalConfiguration.current.locales[0].language

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo
            Image(
                painter = painterResource(R.drawable.computer),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.size(72.dp)
            )

            Spacer(Modifier.height(16.dp))

            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Versión $versionName",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(32.dp))

            sections.forEach { (key, value) ->
                val headerId = context.resources.getIdentifier(key, "string", context.packageName)
                val contentId =
                    context.resources.getIdentifier("${value}_$locale", "raw", context.packageName)
                if (headerId == 0 || contentId == 0) {
                    Log.w("AboutScreen", "Missing string for $key = $value")
                    return@forEach
                }

                val markdown = remember(contentId) {
                    readRawText(context, contentId)
                }

                SectionCard(title = stringResource(headerId)) {
                    RichText(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, end = 4.dp, bottom = 12.dp),
                    ) {
                        Markdown(
                            content = markdown
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

fun readRawText(context: Context, @RawRes resId: Int): String =
    context.resources.openRawResource(resId)
        .bufferedReader()
        .use { it.readText() }
