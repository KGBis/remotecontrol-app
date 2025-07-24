package com.example.remote.shutdown

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.remote.shutdown.common.StopWatch
import com.example.remote.shutdown.common.UserDevices
import com.example.remote.shutdown.model.Device
import com.example.remote.shutdown.network.NetworkScanner
import com.example.remote.shutdown.ui.composable.NetworkScannerScreen
import com.example.remote.shutdown.ui.theme.RemoteShutdownTheme
import com.example.remote.shutdown.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    // private lateinit var userPreferences: UserPreferences

    private val scanner = NetworkScanner()

    override fun onCreate(savedInstanceState: Bundle?) {
        var isLoading = true

        // Must be called before super.onCreate()
        val splashScreen: SplashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        val userDevices: List<Device> = UserDevices().loadDeviceList(applicationContext)

        // Optional: keep splash longer if loading something
        splashScreen.setKeepOnScreenCondition {
            isLoading
        }

        val watch = StopWatch.createStarted()

        val strings = scanner.readOuiFile(applicationContext)
        strings.forEach { s -> Log.i("ReadLines", s) }

        enableEdgeToEdge()
        setContent {
            RemoteShutdownTheme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "main") {
                    composable("main") {
                        MainScreen(
                            scanner = scanner,
                            devices = userDevices,
                            navController = navController
                        )
                    }
                    composable("scanner") {
                        NetworkScannerScreen(
                            scanner = scanner,
                            navController = navController
                        )
                    }
                }
            }
        }

        // Quit splash screen after content is set
        @Suppress("AssignedValueIsNeverRead")
        isLoading = false

        // Watch stop and log time to load all components
        watch.stop()
        Log.d(this.localClassName, "Elapsed: ${watch.getElapsedTimeMillis()} ms")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    scanner: NetworkScanner,
    devices: List<Device> = ArrayList(),
    navController: NavHostController
) {
    Log.d("MainScreen", "scanner instance: ${scanner.toString().substringAfterLast(".")}," +
            " loaded devices: $devices")

    val padding = Modifier
        .fillMaxSize()
        .padding(16.dp)
        .offset(y = 120.dp)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.devices_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        },
        content = {
            paddingValues -> Greeting("hello", Modifier.padding(paddingValues))
        },
        bottomBar = {},
        floatingActionButton = {
            AddDeviceButton(onClick = {
                navController.navigate("scanner")
            })
        }
    )
}

@Composable
fun AddDeviceButton(onClick: () -> Unit) {
    FloatingActionButton(onClick = onClick) {
        Icon(Icons.Default.Add, contentDescription = "Add Device")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenOld(viewModel: MainViewModel, scanner: NetworkScanner) {
    Log.d(null, "Hey, networkScanner $scanner")

    val ip by viewModel.computerIp.collectAsState()
    val hasIp: Boolean = true // ip.isNotEmpty()

    val padding = Modifier
        .fillMaxSize()
        .padding(16.dp)
        .offset(y = 120.dp)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wake on LAN / Remote Shutdown") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        },
        /*bottomBar = {
            BottomAppBar {
                Button(
                    onClick = { *//* do something *//* },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Text("Continue")
                }
            }
        }*/
    ) { innerPadding ->
        val modifier = padding.padding(innerPadding)

        if(!hasIp)
            Column(modifier = modifier) {
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth(Alignment.CenterHorizontally)) {
                    Text("Main page", style = MaterialTheme.typography.headlineSmall)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth(Alignment.CenterHorizontally)) {
                    Surface(
                        shape = RectangleShape, //CircleShape,
                        color = Color.Transparent,
                        contentColor = Color.Green,
                        modifier = Modifier
                            .size(100.dp)
                            .clickable(
                                enabled = hasIp,
                                onClick = { Log.d(null, "Clicked button 1") })
                    ) {
                        Image(
                            painter = painterResource(id = R.mipmap.on_foreground),
                            contentDescription = "On button",
                            colorFilter = if(hasIp) null else ColorFilter.tint(Color.LightGray, blendMode = BlendMode.SrcIn)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RectangleShape, //CircleShape,
                        color = Color.Transparent,
                        contentColor = Color.Red,
                        modifier = Modifier
                            .size(100.dp)
                            .clickable(
                                enabled = hasIp,
                                onClick = { Log.d(null, "Clicked button 2") })
                    ) {
                        Image(
                            painter = painterResource(id = R.mipmap.off_foreground),
                            contentDescription = "Off button",
                            colorFilter = if(hasIp) null else ColorFilter.tint(Color.LightGray, blendMode = BlendMode.SrcIn)
                        )
                    }
                }
            }
        else {
            Log.d(null, "Going to scan IPs...")
            NetworkScannerScreen(modifier =  modifier)
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Composable
fun SwitchScreen(viewModel: MainViewModel) {
    val switchState by viewModel.switchState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("DataStore + StateFlow + Compose", style = MaterialTheme.typography.displayMedium)
        Spacer(modifier = Modifier.height(16.dp))

        Switch(
            checked = switchState,
            onCheckedChange = {
                viewModel.onSwitchChanged(it)
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(if (switchState) "Activado" else "Desactivado")
    }
}



@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    RemoteShutdownTheme {
        Greeting("Android")
    }
}